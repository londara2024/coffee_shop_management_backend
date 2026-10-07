package com.aura.coffeeshop.cart.dto;

import com.aura.coffeeshop.cart.entity.Cart;
import com.aura.coffeeshop.cart.entity.CartItem;
import com.aura.coffeeshop.catalog.dto.ItemCustomization;
import com.aura.coffeeshop.common.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record CreateCartRequest(@Schema(description = "Omit for a guest cart") String customerId) {
    }

    /** Clients never send prices: the server prices the line from the product and its configuration. */
    public record AddCartItemRequest(
            @Schema(description = "Product id or slug is required") String productId,
            @Schema(example = "honey-cinnamon-oat-latte") String productSlug,
            @Schema(defaultValue = "1") @Min(1) @Max(50) Integer qty,
            @Valid ItemCustomization customization) {
    }

    public record UpdateCartItemRequest(
            @Schema(description = "0 removes the line") @NotNull @Min(0) @Max(50) Integer qty) {
    }

    public record CartItemResponse(String id, String productId, String productSlug, String name, String nameKm,
                                   String imageUrl, String variantKey, BigDecimal unitPrice, int qty,
                                   BigDecimal lineTotal, String detailsLabel, String tag) {
        public static CartItemResponse from(CartItem i) {
            var p = i.getProduct();
            return new CartItemResponse(i.getId(), p.getId(), p.getSlug(), p.getName(), p.getNameKm(),
                    p.getImageUrl(), i.getVariantKey(), i.getUnitPrice(), i.getQty(),
                    Money.scale(i.lineTotal()), i.getDetailsLabel(), i.getTag());
        }
    }

    public record CartResponse(String id, String customerId, List<CartItemResponse> items, int itemCount,
                               BigDecimal subtotal, Instant updatedAt) {
        public static CartResponse from(Cart c) {
            return new CartResponse(c.getId(), c.getCustomer() == null ? null : c.getCustomer().getId(),
                    c.getItems().stream().map(CartItemResponse::from).toList(), c.itemCount(), c.subtotal(),
                    c.getUpdatedAt());
        }
    }
}
