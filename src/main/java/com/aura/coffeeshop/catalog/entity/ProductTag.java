package com.aura.coffeeshop.catalog.entity;

import com.aura.coffeeshop.catalog.Tone;
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

@Entity
@Table(name = "product_tags")
@Getter
@Setter
@NoArgsConstructor
public class ProductTag extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "label_km", length = 60)
    private String labelKm;

    @Column(nullable = false, length = 10)
    private Tone tone;

    public ProductTag(String label, String labelKm, Tone tone) {
        this.label = label;
        this.labelKm = labelKm;
        this.tone = tone;
    }
}
