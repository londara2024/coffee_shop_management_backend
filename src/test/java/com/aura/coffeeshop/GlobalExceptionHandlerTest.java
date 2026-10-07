package com.aura.coffeeshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every error, whatever its origin, must come back as an RFC 7807 problem+json body. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(GlobalExceptionHandlerTest.FailingController.class)
class GlobalExceptionHandlerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    /** Test-only endpoint that simulates an unexpected bug (served under the base path: /api/v1/test/boom). */
    @TestConfiguration
    @RestController
    static class FailingController {
        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("database password is hunter2");
        }
    }

    @Test
    void domainNotFound() throws Exception {
        problem(mvc.perform(get("/api/v1/orders/ACR-0000")), 404)
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Order 'ACR-0000' not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/orders/ACR-0000"));
    }

    @Test
    void businessRuleConflict() throws Exception {
        // ACR-8942 is seeded as 'brewing', so it can no longer be cancelled
        problem(mvc.perform(post("/api/v1/orders/ACR-8942/cancel")), 409)
                .andExpect(jsonPath("$.title").value("Business Rule Violation"));
    }

    @Test
    void bodyValidationListsEveryField() throws Exception {
        problem(json(post("/api/v1/auth/sign-in"), "{\"email\":\"not-an-email\"}"), 400)
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void queryParamOutOfRange() throws Exception {
        problem(mvc.perform(get("/api/v1/products").param("size", "1000")), 400)
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.size").exists());
    }

    @Test
    void queryParamWrongType() throws Exception {
        problem(mvc.perform(get("/api/v1/orders").param("page", "abc")), 400);
    }

    @Test
    void unknownShortId() throws Exception {
        problem(mvc.perform(get("/api/v1/carts/ZZZZZ")), 404)
                .andExpect(jsonPath("$.detail").value("Cart 'ZZZZZ' not found"));
    }

    @Test
    void unknownEnumInQueryParam() throws Exception {
        problem(mvc.perform(get("/api/v1/orders").param("status", "teleported")), 400);
    }

    @Test
    void malformedJsonDoesNotLeakInternals() throws Exception {
        problem(json(post("/api/v1/auth/sign-in"), "{\"email\": "), 400)
                .andExpect(jsonPath("$.title").value("Malformed Request Body"))
                .andExpect(jsonPath("$.detail").value(not(containsString("com.aura"))));
    }

    @Test
    void unknownEnumInBodyNamesFieldAndAllowedValues() throws Exception {
        String body = "{\"cartId\":\"ZZZZZ\",\"fulfillmentMethod\":\"drone\",\"paymentMethod\":\"aba\"}";
        problem(json(post("/api/v1/orders"), body), 400)
                .andExpect(jsonPath("$.detail").value(containsString("fulfillmentMethod")))
                .andExpect(jsonPath("$.detail").value(containsString("store, curbside")));
    }

    @Test
    void unknownRoute() throws Exception {
        problem(mvc.perform(get("/api/v1/does-not-exist")), 404);
    }

    @Test
    void wrongHttpMethod() throws Exception {
        problem(mvc.perform(delete("/api/v1/categories")), 405);
    }

    @Test
    void wrongContentType() throws Exception {
        problem(mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.TEXT_PLAIN).content("hi")), 415);
    }

    @Test
    void unexpectedErrorIsGeneric500WithoutLeakingTheMessage() throws Exception {
        problem(mvc.perform(get("/api/v1/test/boom")), 500)
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value(not(containsString("hunter2"))));
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common contract for every error response. */
    private static ResultActions problem(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
