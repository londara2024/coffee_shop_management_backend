package com.aura.coffeeshop.catalog.entity;

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

@Entity
@Table(name = "pairings")
@Getter
@Setter
@NoArgsConstructor
public class Pairing extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "name_km", length = 150)
    private String nameKm;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_km", columnDefinition = "text")
    private String descriptionKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl;

    @Column(name = "cta_label", length = 60)
    private String ctaLabel;

    @Column(name = "cta_label_km", length = 60)
    private String ctaLabelKm;
}
