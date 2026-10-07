package com.aura.coffeeshop.cart.controller;

import com.aura.coffeeshop.cart.dto.CartDtos.AddCartItemRequest;
import com.aura.coffeeshop.cart.dto.CartDtos.CartResponse;
import com.aura.coffeeshop.cart.dto.CartDtos.CreateCartRequest;
import com.aura.coffeeshop.cart.dto.CartDtos.UpdateCartItemRequest;
import com.aura.coffeeshop.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a cart", description = "Pass a customerId for a signed-in customer, or an empty body for a guest cart.")
    public CartResponse create(@RequestBody(required = false) CreateCartRequest request) {
        return cartService.create(request);
    }

    @GetMapping("/{cartId}")
    @Operation(summary = "Get a cart with its lines and subtotal")
    public CartResponse get(@PathVariable String cartId) {
        return cartService.get(cartId);
    }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add a configured item",
            description = "The server computes the unit price from size, milk and toppings. Identical configurations merge into one line.")
    public CartResponse addItem(@PathVariable String cartId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(cartId, request);
    }

    @PatchMapping("/{cartId}/items/{itemId}")
    @Operation(summary = "Change a line's quantity", description = "Quantity 0 removes the line.")
    public CartResponse updateQty(@PathVariable String cartId, @PathVariable String itemId,
                                  @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQty(cartId, itemId, request.qty());
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    @Operation(summary = "Remove a line")
    public CartResponse removeItem(@PathVariable String cartId, @PathVariable String itemId) {
        return cartService.removeItem(cartId, itemId);
    }

    @DeleteMapping("/{cartId}/items")
    @Operation(summary = "Empty the cart")
    public CartResponse clear(@PathVariable String cartId) {
        return cartService.clear(cartId);
    }
}
