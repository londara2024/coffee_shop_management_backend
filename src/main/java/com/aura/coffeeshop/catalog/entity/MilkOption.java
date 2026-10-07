package com.aura.coffeeshop.catalog.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Extension to the design doc: milk choice affects drink price, so it is stored server-side. */
@Entity
@Table(name = "milk_options")
@Getter
@Setter
@NoArgsConstructor
public class MilkOption extends Auditable {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(name = "label_km", length = 80)
    private String labelKm;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;
}
