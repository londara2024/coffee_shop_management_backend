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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin settings page: shop info, branding, open/closed switch, staff roles. */
@SpringBootTest(properties = {
        "app.storage.upload-dir=target/test-uploads",
        "app.storage.public-url=http://localhost:8080/uploads"
})
@AutoConfigureMockMvc
@Testcontainers
class ShopSettingsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String SETTINGS = "/api/v1/admin/settings";
    static final String ELENA_VASQUEZ = "SF002"; // the only manager
    static final String MATEO_SILVA = "SF001";   // assigned to KDS ticket ACR-8942
    static final String TIMBERYARD = "ST002";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void getSettingsForDefaultStore() throws Exception {
        mvc.perform(get(SETTINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value("Free Shop Coffee"))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.latitude").value(13.640854603672658))
                .andExpect(jsonPath("$.mapsUrl").value(containsString("query=13.640854603672658,103.1190348130734")))
                // other tests in this class add/remove staff, so check content rather than count
                .andExpect(jsonPath("$.staff[?(@.name == 'Marco')].role", hasItem("staff")))
                .andExpect(jsonPath("$.staffRoles[0].code").value("manager"))
                .andExpect(jsonPath("$.staffRoles[0].labelKm").value("អ្នកគ្រប់គ្រងហាង"));
    }

    @Test
    void updateShopInfoOnAnotherStore() throws Exception {
        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("shopName", "Riverside Roastery", "address", " 1 Riverside "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value("Riverside Roastery"))
                .andExpect(jsonPath("$.address").value("1 Riverside"));
        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("shopName", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.shopName").exists());
        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("latitude", 11.5))
                .andExpect(status().isBadRequest());
        // the public store endpoint reflects the change
        mvc.perform(get("/api/v1/stores/" + TIMBERYARD)).andExpect(jsonPath("$.name").value("Riverside Roastery"));
        // renaming another store never changes which store is the default
        mvc.perform(get(SETTINGS)).andExpect(jsonPath("$.storeId").value("ST001"));
    }

    @Test
    void brandColorMustBeAPreset() throws Exception {
        mvc.perform(get(SETTINGS).param("storeId", TIMBERYARD))
                .andExpect(jsonPath("$.brandColor").value("#8d4f06"))
                .andExpect(jsonPath("$.brandColors", hasSize(6)))
                .andExpect(jsonPath("$.brandColors[0].isDefault").value(true))
                .andExpect(jsonPath("$.brandColors[1].labelKm").value("បៃតង"));

        // upper-case input is normalised; the public store data carries the colour for the site theme
        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("brandColor", "#3A5A40"))
                .andExpect(jsonPath("$.brandColor").value("#3a5a40"));
        mvc.perform(get("/api/v1/stores/" + TIMBERYARD)).andExpect(jsonPath("$.brandColor").value("#3a5a40"));

        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("brandColor", "#123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("preset")));
        send(patch(SETTINGS).param("storeId", TIMBERYARD), Map.of("brandColor", "green"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.brandColor").exists());
    }

    @Test
    void uploadReplaceAndRemoveLogo() throws Exception {
        String first = read(uploadLogo(PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(startsWith("http://localhost:8080/uploads/logos/")))).get("logoUrl").asText();
        assertThat(logoFile(first)).exists();
        mvc.perform(get("/api/v1/stores/" + TIMBERYARD)).andExpect(jsonPath("$.logoUrl").value(first));

        String second = read(uploadLogo(PNG)).get("logoUrl").asText();
        assertThat(logoFile(second)).exists();
        assertThat(logoFile(first)).as("previous logo is cleaned up").doesNotExist();

        mvc.perform(delete(SETTINGS + "/logo").param("storeId", TIMBERYARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").doesNotExist());
        assertThat(logoFile(second)).doesNotExist();

        uploadLogo("not an image".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest());
    }

    private ResultActions uploadLogo(byte[] bytes) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, SETTINGS + "/logo")
                .file(new MockMultipartFile("file", "logo.png", "image/png", bytes))
                .param("storeId", TIMBERYARD));
    }

    private static Path logoFile(String url) {
        return Path.of("target/test-uploads/logos", url.substring(url.lastIndexOf('/') + 1));
    }

    @Test
    void closedShopRefusesCheckout() throws Exception {
        String cartId = read(send(post("/api/v1/carts"), Map.of())).get("id").asText();
        send(post("/api/v1/carts/" + cartId + "/items"), Map.of("productSlug", "basque-burnt-cheesecake"));
        Map<String, Object> order = Map.of("cartId", cartId, "fulfillmentMethod", "store", "paymentMethod", "cash");

        send(patch(SETTINGS + "/status"), Map.of("open", false)).andExpect(jsonPath("$.open").value(false));
        try {
            mvc.perform(get("/api/v1/stores")).andExpect(jsonPath("$[0].open").value(false));
            send(post("/api/v1/orders"), order)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(containsString("closed")));
        } finally {
            send(patch(SETTINGS + "/status"), Map.of("open", true)).andExpect(jsonPath("$.open").value(true));
        }
        send(post("/api/v1/orders"), order).andExpect(status().isCreated());
    }

    @Test
    void staffLifecycleAndLastManagerRule() throws Exception {
        String id = read(send(post(SETTINGS + "/staff"), Map.of("name", " Sophia Vance ", "role", "staff"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sophia Vance"))
                .andExpect(jsonPath("$.roleLabelKm").value("បុគ្គលិក"))).get("id").asText();

        // Elena is the only manager: she can be neither demoted nor removed
        send(patch(SETTINGS + "/staff/" + ELENA_VASQUEZ), Map.of("role", "staff")).andExpect(status().isConflict());
        mvc.perform(delete(SETTINGS + "/staff/" + ELENA_VASQUEZ)).andExpect(status().isConflict());

        // once Sophia is promoted, Elena can step down
        send(patch(SETTINGS + "/staff/" + id), Map.of("role", "manager")).andExpect(jsonPath("$.role").value("manager"));
        send(patch(SETTINGS + "/staff/" + ELENA_VASQUEZ), Map.of("role", "staff")).andExpect(status().isOk());

        send(post(SETTINGS + "/staff"), Map.of("name", "", "role", "chef"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(SETTINGS + "/staff/ZZZZZ")).andExpect(status().isNotFound());
    }

    @Test
    void removingStaffKeepsTheirHistory() throws Exception {
        mvc.perform(get("/api/v1/kitchen/tickets/KT942"))
                .andExpect(jsonPath("$.assignedStaffName").value("Mateo Silva"));

        mvc.perform(delete(SETTINGS + "/staff/" + MATEO_SILVA)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/kitchen/tickets/KT942"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedStaffName").doesNotExist());
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
