package com.aura.coffeeshop.order.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
public class PromoCode extends Auditable {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 60)
    private String label;

    /** Flat discount amount. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean active = true;
}
