package com.aura.coffeeshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.storage.upload-dir=target/test-uploads",
        "app.storage.public-url=http://localhost:8080/uploads",
        "app.storage.max-image-size=1KB"
})
@AutoConfigureMockMvc
@Testcontainers
class ProductImageUploadTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String PRODUCTS = "/api/v1/admin/products";
    static final String PUBLIC_URL = "http://localhost:8080/uploads/products/";
    /** Smallest valid PNG header + some payload; type detection only needs the signature. */
    static final byte[] PNG = concat(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, new byte[64]);
    static final byte[] JPEG = concat(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, new byte[64]);

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void uploadThenCreateProductWithTheReturnedUrl() throws Exception {
        String url = read(upload(PRODUCTS + "/images", HttpMethod.POST, "latte.png", MediaType.IMAGE_PNG_VALUE, PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith(PUBLIC_URL)))
                .andExpect(jsonPath("$.url").value(endsWith(".png")))
                .andExpect(jsonPath("$.contentType").value("image/png"))).get("url").asText();

        // the file is served back by the backend, cacheable forever
        mvc.perform(get(url.replace("http://localhost:8080", "")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", startsWith("max-age=31536000")));

        mvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "categoryId", "coffee", "name", "Uploaded Latte", "basePrice", 5.00, "imageUrl", url))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(url));
    }

    @Test
    void replaceImageDeletesThePreviousUpload() throws Exception {
        String productId = read(mvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("categoryId", "food", "name", "Photo Toast", "basePrice", 4.00, "imageUrl", "/images/avocado-tartine.jpg")))))
                .get("id").asText();

        String first = read(upload(PRODUCTS + "/" + productId + "/image", HttpMethod.PUT, "a.png", "image/png", PNG)
                .andExpect(status().isOk())).get("imageUrl").asText();
        Path firstFile = fileFor(first);
        assertThat(firstFile).exists();

        // the client claims PNG but sends a JPEG: the real type wins
        String second = read(upload(PRODUCTS + "/" + productId + "/image", HttpMethod.PUT, "b.png", "image/png", JPEG)
                .andExpect(jsonPath("$.imageUrl").value(endsWith(".jpg")))).get("imageUrl").asText();
        assertThat(fileFor(second)).exists();
        assertThat(firstFile).doesNotExist();

        // seed images under /images/... are never touched
        String seeded = read(mvc.perform(get(PRODUCTS).param("q", "Basque"))).at("/content/0/id").asText();
        upload(PRODUCTS + "/" + seeded + "/image", HttpMethod.PUT, "c.png", "image/png", PNG).andExpect(status().isOk());
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        upload(PRODUCTS + "/images", HttpMethod.POST, "evil.png", "image/png", script)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only JPEG, PNG, WebP or GIF images are allowed"));
        upload(PRODUCTS + "/images", HttpMethod.POST, "empty.png", "image/png", new byte[0])
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsImagesOverTheSizeLimit() throws Exception {
        upload(PRODUCTS + "/images", HttpMethod.POST, "big.png", "image/png", concat(PNG, new byte[2048]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void missingFilePartAndUnknownProduct() throws Exception {
        mvc.perform(multipart(PRODUCTS + "/images")).andExpect(status().isBadRequest());
        upload(PRODUCTS + "/ZZZZZ/image", HttpMethod.PUT, "a.png", "image/png", PNG)
                .andExpect(status().isNotFound());
    }

    private ResultActions upload(String url, HttpMethod method, String name, String contentType, byte[] bytes) throws Exception {
        return mvc.perform(multipart(method, url).file(new MockMultipartFile("file", name, contentType, bytes)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Path fileFor(String url) {
        return Path.of("target/test-uploads/products", url.substring(url.lastIndexOf('/') + 1));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static {
        try {
            Files.createDirectories(Path.of("target/test-uploads"));
        } catch (Exception ignored) {
            // the storage creates it too
        }
    }
}
