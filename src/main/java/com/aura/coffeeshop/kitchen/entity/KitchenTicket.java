package com.aura.coffeeshop.kitchen.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.kitchen.KitchenEnums.FlagTone;
import com.aura.coffeeshop.kitchen.KitchenEnums.TicketStatus;
import com.aura.coffeeshop.order.entity.Order;
import com.aura.coffeeshop.store.entity.Staff;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Prep-specific, mutable state for an order on the kitchen display. */
@Entity
@Table(name = "kitchen_tickets")
@Getter
@Setter
@NoArgsConstructor
public class KitchenTicket extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 20)
    private TicketStatus status;

    /** e.g. "អាទិភាពរហ័ស", "ចំណតទទួលក្រៅរថយន្ត". */
    @Column(name = "channel_label", length = 80)
    private String channelLabel;

    @Column(name = "sla_minutes", nullable = false)
    private int slaMinutes = 10;

    /** Used to compute elapsed time on the board. */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "flag_tone", length = 10)
    private FlagTone flagTone;

    @Column(name = "flag_text", length = 300)
    private String flagText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Staff assignedStaff;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<KitchenTicketItem> items = new ArrayList<>();

    public void addItem(KitchenTicketItem item) {
        item.setTicket(this);
        items.add(item);
    }
}
