package com.aura.coffeeshop.catalog.controller;

import com.aura.coffeeshop.catalog.dto.CatalogDtos.CatalogOptionsResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.CategoryResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.PairingResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.aura.coffeeshop.catalog.service.CatalogService;
import com.aura.coffeeshop.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Catalog")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    @Operation(summary = "List menu categories", description = "Includes the number of live products in each category.")
    public List<CategoryResponse> categories() {
        return catalogService.listCategories();
    }

    @GetMapping("/products")
    @Operation(summary = "Browse the live menu", description = "Only items switched on (`is_live`) are returned.")
    public PageResponse<ProductSummaryResponse> products(
            @Parameter(description = "Category id", example = "coffee") @RequestParam(required = false) String category,
            @Parameter(description = "Search text (English / Khmer name, kicker)") @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return catalogService.searchProducts(category, q, true, null, page, size);
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get a product by slug", description = "Includes sizes, available toppings and tags.")
    public ProductDetailResponse product(@PathVariable String slug) {
        return catalogService.getLiveProduct(slug);
    }

    @GetMapping("/catalog/options")
    @Operation(summary = "Drink configurator options", description = "Milk options, default toppings, temperatures and sweetness levels.")
    public CatalogOptionsResponse options() {
        return catalogService.options();
    }

    @GetMapping("/pairings")
    @Operation(summary = "Suggested pairings shown on the product page")
    public List<PairingResponse> pairings() {
        return catalogService.listPairings();
    }
}
