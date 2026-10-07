package com.aura.coffeeshop.order.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Staff-applied discount at checkout (distinct from customer-entered promo codes). */
@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
public class Discount extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
}
