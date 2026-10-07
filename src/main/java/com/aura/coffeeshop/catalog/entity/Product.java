package com.aura.coffeeshop.catalog.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.store.entity.Staff;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Built-in and admin-created menu items share this table, distinguished by {@link #custom}. */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "name_km", length = 150)
    private String nameKm;

    @Column(length = 150)
    private String kicker;

    @Column(name = "kicker_km", length = 150)
    private String kickerKm;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_km", columnDefinition = "text")
    private String descriptionKm;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl;

    @Column(name = "image_tag", length = 80)
    private String imageTag;

    @Column(length = 40)
    private String badge;

    @Column(length = 40)
    private String extra;

    @Column(nullable = false)
    private boolean customizable;

    @Column(name = "is_custom", nullable = false)
    private boolean custom;

    @Column(name = "is_live", nullable = false)
    private boolean live = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_staff_id")
    private Staff createdByStaff;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ProductSize> sizes = new ArrayList<>();

    /** Product-specific toppings; when empty the global defaults apply. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ProductTopping> toppings = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ProductTag> tags = new ArrayList<>();

    public void replaceSizes(List<ProductSize> newSizes) {
        sizes.clear();
        newSizes.forEach(s -> {
            s.setProduct(this);
            sizes.add(s);
        });
    }

    public void replaceToppings(List<ProductTopping> newToppings) {
        toppings.clear();
        newToppings.forEach(t -> {
            t.setProduct(this);
            toppings.add(t);
        });
    }

    public void replaceTags(List<ProductTag> newTags) {
        tags.clear();
        newTags.forEach(t -> {
            t.setProduct(this);
            tags.add(t);
        });
    }
}
