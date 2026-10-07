package com.aura.coffeeshop.setting.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A preset theme colour offered on the settings page. */
@Entity
@Table(name = "brand_colors")
@Getter
@Setter
@NoArgsConstructor
public class BrandColor extends Auditable {

    /** e.g. {@code amber}, {@code forest}. */
    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "label_km", length = 60)
    private String labelKm;

    /** Lower-case {@code #rrggbb}. */
    @Column(nullable = false, unique = true, length = 7)
    private String hex;

    @Column(name = "is_default", nullable = false)
    private boolean defaultColor;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;
}
