package com.aura.coffeeshop.catalog.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** {@code product == null} marks a global default topping (today's {@code defaultToppings}). */
@Entity
@Table(name = "product_toppings")
@Getter
@Setter
@NoArgsConstructor
public class ProductTopping extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(name = "label_km", length = 80)
    private String labelKm;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta;

    /** Pre-selected and non-removable (e.g. cinnamon). */
    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    public ProductTopping(String label, String labelKm, BigDecimal priceDelta, boolean locked) {
        this.label = label;
        this.labelKm = labelKm;
        this.priceDelta = priceDelta;
        this.locked = locked;
    }
}
