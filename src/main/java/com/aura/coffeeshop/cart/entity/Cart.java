package com.aura.coffeeshop.cart.entity;

import com.aura.coffeeshop.common.Money;
import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.customer.entity.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class Cart extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    /** Null for guest carts. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Optional<CartItem> findItem(String itemId) {
        return items.stream().filter(i -> i.getId().equals(itemId)).findFirst();
    }

    public Optional<CartItem> findByVariant(String productId, String variantKey) {
        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId) && i.getVariantKey().equals(variantKey))
                .findFirst();
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
        touch();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        touch();
    }

    public void clear() {
        items.clear();
        touch();
    }

    /** Forces updated_at to change even when only child rows were modified. */
    public void touch() {
        markModified();
    }

    public int itemCount() {
        return items.stream().mapToInt(CartItem::getQty).sum();
    }

    public BigDecimal subtotal() {
        return Money.scale(items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
