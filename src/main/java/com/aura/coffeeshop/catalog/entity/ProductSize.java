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

@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
public class ProductSize extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** {@code តូច} / {@code ធម្មតា} / {@code ធំ}. */
    @Column(name = "size_label", nullable = false, length = 20)
    private String sizeLabel;

    /** 0 = small, 1 = regular, 2 = large. */
    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public ProductSize(String sizeLabel, short sortOrder, BigDecimal price) {
        this.sizeLabel = sizeLabel;
        this.sortOrder = sortOrder;
        this.price = price;
    }
}
