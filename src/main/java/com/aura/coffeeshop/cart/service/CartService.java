package com.aura.coffeeshop.cart.service;

import com.aura.coffeeshop.cart.dto.CartDtos.AddCartItemRequest;
import com.aura.coffeeshop.cart.dto.CartDtos.CartResponse;
import com.aura.coffeeshop.cart.dto.CartDtos.CreateCartRequest;
import com.aura.coffeeshop.cart.entity.Cart;
import com.aura.coffeeshop.cart.entity.CartItem;
import com.aura.coffeeshop.cart.repository.CartRepository;
import com.aura.coffeeshop.catalog.PricedItem;
import com.aura.coffeeshop.catalog.entity.Product;
import com.aura.coffeeshop.catalog.repository.ProductRepository;
import com.aura.coffeeshop.catalog.service.PricingService;
import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    static final int MAX_LINE_QTY = 50;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final CustomerService customerService;

    public CartResponse create(CreateCartRequest request) {
        Cart cart = new Cart();
        if (request != null && request.customerId() != null) {
            cart.setCustomer(customerService.getCustomer(request.customerId()));
        }
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartResponse get(String cartId) {
        return CartResponse.from(getCart(cartId));
    }

    /** Adds a configured line; an identical configuration is merged into the existing line (like cart.ts). */
    public CartResponse addItem(String cartId, AddCartItemRequest request) {
        Cart cart = getCart(cartId);
        Product product = resolveProduct(request);
        if (!product.isLive()) {
            throw new BusinessRuleException("'" + product.getName() + "' is not currently on the menu");
        }
        int qty = request.qty() == null ? 1 : request.qty();
        PricedItem priced = pricingService.price(product, request.customization());

        cart.findByVariant(product.getId(), priced.variantKey()).ifPresentOrElse(existing -> {
            int newQty = existing.getQty() + qty;
            if (newQty > MAX_LINE_QTY) {
                throw BusinessRuleException.badRequest("A single line cannot exceed " + MAX_LINE_QTY + " items");
            }
            existing.setQty(newQty);
            existing.setUnitPrice(priced.unitPrice());
            cart.touch();
        }, () -> {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setVariantKey(priced.variantKey());
            item.setUnitPrice(priced.unitPrice());
            item.setQty(qty);
            item.setDetailsLabel(priced.detailsLabel());
            item.setTag(priced.tag());
            cart.addItem(item);
        });
        return CartResponse.from(cartRepository.saveAndFlush(cart));
    }

    public CartResponse updateQty(String cartId, String itemId, int qty) {
        Cart cart = getCart(cartId);
        CartItem item = cart.findItem(itemId).orElseThrow(() -> NotFoundException.of("Cart item", itemId));
        if (qty <= 0) {
            cart.removeItem(item);
        } else {
            item.setQty(qty);
            cart.touch();
        }
        return CartResponse.from(cartRepository.saveAndFlush(cart));
    }

    public CartResponse removeItem(String cartId, String itemId) {
        return updateQty(cartId, itemId, 0);
    }

    public CartResponse clear(String cartId) {
        Cart cart = getCart(cartId);
        cart.clear();
        return CartResponse.from(cartRepository.saveAndFlush(cart));
    }

    public Cart getCart(String cartId) {
        return cartRepository.findWithItemsById(cartId).orElseThrow(() -> NotFoundException.of("Cart", cartId));
    }

    private Product resolveProduct(AddCartItemRequest request) {
        if (request.productId() != null) {
            return productRepository.findById(request.productId())
                    .orElseThrow(() -> NotFoundException.of("Product", request.productId()));
        }
        if (request.productSlug() != null && !request.productSlug().isBlank()) {
            return productRepository.findBySlug(request.productSlug())
                    .orElseThrow(() -> NotFoundException.of("Product", request.productSlug()));
        }
        throw BusinessRuleException.badRequest("productId or productSlug is required");
    }
}
