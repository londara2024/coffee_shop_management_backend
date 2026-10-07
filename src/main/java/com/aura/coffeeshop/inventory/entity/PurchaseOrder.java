package com.aura.coffeeshop.inventory.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.inventory.InventoryEnums.PurchaseOrderStatus;
import com.aura.coffeeshop.store.entity.Staff;
import com.aura.coffeeshop.store.entity.Store;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    /** e.g. {@code PO-2025-084}. */
    @Column(name = "po_code", nullable = false, unique = true, length = 20)
    private String poCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "funding_account", length = 100)
    private String fundingAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_staff_id")
    private Staff createdByStaff;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        line.setPurchaseOrder(this);
        lines.add(line);
    }

    /** Line prices are line totals (the "តម្លៃសរុប" field in create-po-sheet.tsx). */
    public BigDecimal total() {
        return lines.stream().map(PurchaseOrderLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
