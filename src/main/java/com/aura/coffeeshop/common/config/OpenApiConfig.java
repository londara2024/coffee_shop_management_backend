package com.aura.coffeeshop.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI coffeeShopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aura Coffee Roasters API")
                        .version("v1")
                        .description("""
                                Monolithic REST backend for the Aura Coffee Roasters shop (Next.js frontend).

                                **Customer flow:** browse catalog → create cart → add configured items → quote → place order → track order.
                                **Back office:** kitchen display (KDS) tickets, menu management, inventory & purchase orders.

                                Money values are USD with 2 decimals. Enum values use the codes from DATABASE_DESIGN.md
                                (e.g. `brewing`, `Bar A`, `Single Origin Beans`).""")
                        .contact(new Contact().name("Aura Coffee Roasters"))
                        .license(new License().name("Proprietary")))
                .tags(List.of(
                        new Tag().name("Catalog").description("Menu categories, products, options and pairings"),
                        new Tag().name("Stores").description("Store locations"),
                        new Tag().name("Auth").description("Customer sign-up / sign-in"),
                        new Tag().name("Customers").description("Customer profile & loyalty"),
                        new Tag().name("Cart").description("Shopping cart with server-side pricing"),
                        new Tag().name("Checkout & Orders").description("Quote, place and track orders"),
                        new Tag().name("Promotions").description("Promo codes and staff discounts"),
                        new Tag().name("Kitchen (KDS)").description("Kitchen display tickets and prep workflow"),
                        new Tag().name("Admin - Menu").description("Create / edit / withdraw menu items"),
                        new Tag().name("Admin - Inventory").description("Suppliers, stock items and stock counts"),
                        new Tag().name("Admin - Purchasing").description("Purchase orders"),
                        new Tag().name("Admin - Settings").description("Shop info, open/closed status and staff roles")));
    }

    @Bean
    GroupedOpenApi storefrontApi(AppProperties properties) {
        String base = properties.api().basePath();
        return GroupedOpenApi.builder()
                .group("1-storefront")
                .displayName("Storefront (customer)")
                .pathsToMatch(base + "/**")
                .pathsToExclude(base + "/admin/**", base + "/kitchen/**")
                .build();
    }

    @Bean
    GroupedOpenApi backOfficeApi(AppProperties properties) {
        String base = properties.api().basePath();
        return GroupedOpenApi.builder()
                .group("2-back-office")
                .displayName("Back office (staff)")
                .pathsToMatch(base + "/admin/**", base + "/kitchen/**")
                .build();
    }

    @Bean
    GroupedOpenApi allApi(AppProperties properties) {
        return GroupedOpenApi.builder()
                .group("0-all")
                .displayName("All endpoints")
                .pathsToMatch(properties.api().basePath() + "/**")
                .build();
    }
}
