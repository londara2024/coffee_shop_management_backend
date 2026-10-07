package com.aura.coffeeshop.catalog.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends Auditable {

    /** e.g. {@code coffee}, {@code tea}, {@code food}, {@code desserts}. */
    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "label_km", length = 100)
    private String labelKm;

    @Column(length = 150)
    private String eyebrow;

    @Column(length = 150)
    private String title;

    @Column(name = "title_km", length = 150)
    private String titleKm;

    @Column(length = 200)
    private String note;

    @Column(name = "note_km", length = 200)
    private String noteKm;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;
}
