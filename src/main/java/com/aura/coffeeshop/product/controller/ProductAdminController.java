package com.aura.coffeeshop.product.controller;

import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.aura.coffeeshop.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.aura.coffeeshop.product.dto.ProductUpsertRequest;
import com.aura.coffeeshop.catalog.service.CatalogService;
import com.aura.coffeeshop.product.service.ProductAdminService;
import com.aura.coffeeshop.common.storage.ImageStorage.StoredImage;
import com.aura.coffeeshop.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin - Menu")
public class ProductAdminController {

    private final CatalogService catalogService;
    private final ProductAdminService productAdminService;

    @GetMapping
    @Operation(summary = "List all menu items", description = "Includes withdrawn items. Filter by live / custom status.")
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @Parameter(description = "true = live only, false = withdrawn only, omitted = both") @RequestParam(required = false) Boolean live,
            @Parameter(description = "true = admin-created items only") @RequestParam(required = false) Boolean custom,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return catalogService.searchProducts(category, q, live, custom, page, size);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get any menu item (including withdrawn)")
    public ProductDetailResponse get(@PathVariable String productId) {
        return catalogService.getProductDetail(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a menu item", description = "Created items are flagged `custom = true`.")
    public ProductDetailResponse create(@Valid @RequestBody ProductUpsertRequest request) {
        return productAdminService.create(request);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a menu item", description = "Null size/topping/tag lists are left unchanged; empty lists clear them.")
    public ProductDetailResponse update(@PathVariable String productId, @Valid @RequestBody ProductUpsertRequest request) {
        return productAdminService.update(productId, request);
    }

    @PatchMapping("/{productId}/live")
    @Operation(summary = "Publish or withdraw a menu item")
    public ProductDetailResponse setLive(@PathVariable String productId, @Valid @RequestBody LiveRequest request) {
        return productAdminService.setLive(productId, request.live());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a menu item", description = "Returns 409 if the item appears in orders or carts — withdraw it instead.")
    public void delete(@PathVariable String productId) {
        productAdminService.delete(productId);
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a product image",
            description = "For the \"Add item\" form: upload first, then send the returned `url` as `imageUrl` when creating the product. "
                    + "JPEG, PNG, WebP or GIF up to 5 MB (type is checked from the file content). 413 when too large.")
    public ImageUploadResponse uploadImage(@RequestPart("file") MultipartFile file) {
        return ImageUploadResponse.from(productAdminService.uploadImage(file));
    }

    @PutMapping(value = "/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace a product's image",
            description = "Stores the new image, points the product at it and deletes the previously uploaded file.")
    public ProductDetailResponse replaceImage(@PathVariable String productId, @RequestPart("file") MultipartFile file) {
        return productAdminService.replaceImage(productId, file);
    }

    public record ImageUploadResponse(String url, String contentType, long size) {
        static ImageUploadResponse from(StoredImage image) {
            return new ImageUploadResponse(image.url(), image.contentType(), image.size());
        }
    }

    public record LiveRequest(@NotNull Boolean live) {
    }
}
