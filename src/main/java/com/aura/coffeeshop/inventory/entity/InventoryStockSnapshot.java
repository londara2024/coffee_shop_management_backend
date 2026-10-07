package com.aura.coffeeshop.inventory.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.inventory.InventoryEnums.StockState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Point-in-time stock reading; the newest row per item is its current stock. */
@Entity
@Table(name = "inventory_stock_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class InventoryStockSnapshot extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "on_hand_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal onHandQty;

    /** Cached for fast dashboard reads. */
    @Column(name = "pct_of_par", nullable = false)
    private short pctOfPar;

    @Column(nullable = false, length = 20)
    private StockState state;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
