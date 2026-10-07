package com.aura.coffeeshop.inventory.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.inventory.InventoryEnums.InventoryGroup;
import com.aura.coffeeshop.store.entity.Store;
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
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Null for quick-added materials with no supplier assigned yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 250)
    private String meta;

    @Column(name = "group_name", nullable = false, length = 40)
    private InventoryGroup group;

    @Column(name = "unit_label", nullable = false, length = 20)
    private String unitLabel = "kg";

    /** Target stock level. */
    @Column(name = "par_level", nullable = false, precision = 10, scale = 2)
    private BigDecimal parLevel;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    /** True for materials added via the "+ បន្ថែមសម្ភារះ" quick-add flow. */
    @Column(name = "is_custom", nullable = false)
    private boolean custom;

}
