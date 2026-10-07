package com.aura.coffeeshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the whole monolith against PostgreSQL 16 with the real Flyway migrations. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CoffeeShopApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final String ELENA = "CU001";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void catalogIsSeededFromMigrations() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value("coffee"))
                .andExpect(jsonPath("$[0].productCount").value(8));

        mvc.perform(get("/api/v1/products/honey-cinnamon-oat-latte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizes", hasSize(3)))
                .andExpect(jsonPath("$.sizes[2].price").value(6.85))
                .andExpect(jsonPath("$.toppings", hasSize(6)))
                .andExpect(jsonPath("$.tags[0].tone").value("amber"));

        mvc.perform(get("/api/v1/catalog/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milks", hasSize(5)));
    }

    @Test
    void checkoutFlowPricesServerSideAndDrivesTheKitchenBoard() throws Exception {
        JsonNode latte = read(mvc.perform(get("/api/v1/products/honey-cinnamon-oat-latte")));
        String largeSize = latte.at("/sizes/2/id").asText();
        String honey = findTopping(latte, "Extra Wildflower Honey Drizzle");

        String cartId = read(send(post("/api/v1/carts"), Map.of("customerId", ELENA))).get("id").asText();

        Map<String, Object> latteLine = Map.of("productSlug", "honey-cinnamon-oat-latte", "qty", 1,
                "customization", Map.of("sizeId", largeSize, "temperature", "iced", "milkCode", "almond",
                        "toppingIds", new String[]{honey}));
        // large 6.85 + almond 0.60 + honey 0.50 (+ locked cinnamon 0.00) = 7.95
        send(post("/api/v1/carts/" + cartId + "/items"), latteLine)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(7.95));
        // identical configuration merges into the same line
        send(post("/api/v1/carts/" + cartId + "/items"), latteLine)
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].qty").value(2));
        send(post("/api/v1/carts/" + cartId + "/items"), Map.of("productSlug", "basque-burnt-cheesecake"))
                .andExpect(jsonPath("$.subtotal").value(22.40));

        // 22.40 + tip 2.00 - promo 0.75 - discount 1.00
        Map<String, Object> adjustments = Map.of("cartId", cartId, "tip", 2, "promoCode", "WELCOME5",
                "discountId", "DC001");
        send(post("/api/v1/checkout/quote"), adjustments)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(22.65));

        Map<String, Object> order = new java.util.HashMap<>(adjustments);
        order.put("fulfillmentMethod", "store");
        order.put("paymentMethod", "aba");
        JsonNode placed = read(send(post("/api/v1/orders"), order)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("new"))
                .andExpect(jsonPath("$.total").value(22.65))
                .andExpect(jsonPath("$.items[0].station").value("Bar A"))
                .andExpect(jsonPath("$.payment.paid").value(false)));
        String code = placed.get("orderCode").asText();
        assertThat(code).startsWith("ACR-");

        mvc.perform(get("/api/v1/carts/" + cartId)).andExpect(jsonPath("$.items", hasSize(0)));

        JsonNode board = read(mvc.perform(get("/api/v1/kitchen/tickets").param("status", "new")));
        String ticketId = null;
        for (JsonNode t : board) {
            if (t.get("orderCode").asText().equals(code)) {
                ticketId = t.get("id").asText();
            }
        }
        assertThat(ticketId).as("ticket for " + code).isNotNull();

        mvc.perform(post("/api/v1/kitchen/tickets/" + ticketId + "/advance")).andExpect(jsonPath("$.status").value("brewing"));
        mvc.perform(post("/api/v1/kitchen/tickets/" + ticketId + "/advance")).andExpect(jsonPath("$.status").value("ready"));
        mvc.perform(get("/api/v1/orders/" + code)).andExpect(jsonPath("$.status").value("ready"));
        mvc.perform(post("/api/v1/kitchen/tickets/" + ticketId + "/advance")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/" + code)).andExpect(jsonPath("$.status").value("completed"));
        mvc.perform(post("/api/v1/orders/" + code + "/cancel")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/orders/" + code + "/payment/confirm")).andExpect(jsonPath("$.payment.paid").value(true));

        // 178 seeded stars + 22 for a $22.65 order
        mvc.perform(get("/api/v1/customers/" + ELENA)).andExpect(jsonPath("$.loyalty.stars").value(200));
    }

    @Test
    void rejectsInvalidCheckoutInput() throws Exception {
        String cartId = read(send(post("/api/v1/carts"), Map.of())).get("id").asText();
        send(post("/api/v1/orders"), Map.of("cartId", cartId, "fulfillmentMethod", "store", "paymentMethod", "cash"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/checkout/quote"), Map.of("cartId", cartId, "promoCode", "NOPE"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/carts/" + cartId + "/items"), Map.of("productSlug", "velvet-flat-white",
                "customization", Map.of("milkCode", "goat")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUpAndSignIn() throws Exception {
        send(post("/api/v1/auth/sign-in"), Map.of("email", "elena.rostova@botanica.com", "password", "Password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyalty.tier").value("Gold Member"));
        send(post("/api/v1/auth/sign-in"), Map.of("email", "elena.rostova@botanica.com", "password", "wrong-pass1"))
                .andExpect(status().isUnauthorized());

        send(post("/api/v1/auth/sign-up"), Map.of("name", "S", "email", "bad", "password", "short", "acceptedTerms", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.acceptedTerms").exists());
        Map<String, Object> signUp = Map.of("name", "Sophia Vance", "email", "sophia@botanica.com",
                "password", "Botanica2026", "acceptedTerms", true);
        send(post("/api/v1/auth/sign-up"), signUp)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loyalty.tier").value("Member"));
        send(post("/api/v1/auth/sign-up"), signUp).andExpect(status().isConflict());
    }

    @Test
    void adminMenuInventoryAndPurchasing() throws Exception {
        JsonNode created = read(send(post("/api/v1/admin/products"), Map.of(
                "categoryId", "coffee", "name", "Lavender Honey Latte", "basePrice", 6.40,
                "imageUrl", "/images/latte-cup.jpg", "customizable", true,
                "sizes", new Object[]{Map.of("sizeLabel", "ធម្មតា", "price", 6.40)},
                "tags", new Object[]{Map.of("label", "Floral", "tone", "forest")}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("lavender-honey-latte"))
                .andExpect(jsonPath("$.custom").value(true)));
        String productId = created.get("id").asText();
        send(patch("/api/v1/admin/products/" + productId + "/live"), Map.of("live", false)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/lavender-honey-latte")).andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/admin/inventory/items").param("reorderOnly", "true"))
                .andExpect(jsonPath("$.totalElements").value(2));
        JsonNode cups = read(mvc.perform(get("/api/v1/admin/inventory/items").param("group", "Packaging")));
        String cupsId = cups.at("/content/0/id").asText();
        send(post("/api/v1/admin/inventory/items/" + cupsId + "/stock"), Map.of("onHandQty", 300))
                .andExpect(jsonPath("$.stock.state").value("critical"))
                .andExpect(jsonPath("$.stock.pctOfPar").value(30));

        JsonNode po = read(send(post("/api/v1/admin/purchase-orders"), Map.of(
                "fundingAccount", "Flagship Roastery Operating Fund (#***849)",
                "lines", new Object[]{Map.of("inventoryItemId", cupsId, "qty", 700, "price", 210.00),
                        Map.of("description", "Guatemala Huehuetenango (20kg)", "price", 280.00)}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.total").value(490.0))
                .andExpect(jsonPath("$.lines[0].supplierName").value("EcoWare Global")));
        String poId = po.get("id").asText();
        mvc.perform(post("/api/v1/admin/purchase-orders/" + poId + "/send")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/purchase-orders/" + poId + "/approve")).andExpect(jsonPath("$.status").value("approved"));
        mvc.perform(post("/api/v1/admin/purchase-orders/" + poId + "/send")).andExpect(jsonPath("$.status").value("sent"));
    }

    @Test
    void auditColumnsAreFilledAutomatically() throws Exception {
        String id = read(send(post("/api/v1/admin/products"), Map.of(
                "categoryId", "tea", "name", "Audited Tea", "basePrice", 3.00, "imageUrl", "/images/latte-cup.jpg")))
                .get("id").asText();
        assertThat(id).as("ids are 5 random characters").matches("^[0-9A-Za-z]{5}$");
        Map<String, Object> created = jdbc.queryForMap(
                "select created_at, updated_at, created_by, updated_by from products where id = ?", id);
        assertThat(created.get("created_at")).isNotNull();
        assertThat(created.get("created_by")).isEqualTo("system");
        assertThat(created.get("updated_by")).isEqualTo("system");

        Thread.sleep(20);
        send(patch("/api/v1/admin/products/" + id + "/live"), Map.of("live", false)).andExpect(status().isOk());
        Map<String, Object> updated = jdbc.queryForMap("select created_at, updated_at from products where id = ?", id);
        assertThat(updated.get("created_at")).as("created_at never changes").isEqualTo(created.get("created_at"));
        assertThat((java.sql.Timestamp) updated.get("updated_at")).isAfter((java.sql.Timestamp) created.get("updated_at"));

        // a cart's updated_at moves when only its lines change (child rows)
        String cartId = read(send(post("/api/v1/carts"), Map.of())).get("id").asText();
        java.sql.Timestamp before = jdbc.queryForObject("select updated_at from carts where id = ?", java.sql.Timestamp.class, cartId);
        Thread.sleep(20);
        send(post("/api/v1/carts/" + cartId + "/items"), Map.of("productSlug", "espresso-tiramisu-jar"));
        java.sql.Timestamp after = jdbc.queryForObject("select updated_at from carts where id = ?", java.sql.Timestamp.class, cartId);
        assertThat(after).isAfter(before);

        // seeded rows were backfilled by the V5 audit migration
        assertThat(jdbc.queryForObject("select count(*) from stores where created_by is null or created_at is null", Integer.class))
                .isZero();
    }

    @Test
    void swaggerDocsArePublished() throws Exception {
        mvc.perform(get("/v3/api-docs/0-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Aura Coffee Roasters API"))
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists())
                // enums are documented by their wire codes, not Java constant names
                .andExpect(jsonPath("$.components.schemas.TicketResponse.properties.status.enum[2]").value("brewing"))
                .andExpect(jsonPath("$.components.schemas.TicketItemResponse.properties.station.enum[0]").value("Bar A"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               Object body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String findTopping(JsonNode product, String label) {
        for (JsonNode t : product.get("toppings")) {
            if (t.get("label").asText().equals(label)) {
                return t.get("id").asText();
            }
        }
        throw new AssertionError("topping not found: " + label);
    }
}
