package com.aura.coffeeshop.kitchen.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.order.OrderEnums.Station;
import com.aura.coffeeshop.order.entity.OrderItem;
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

/** Links a ticket to an order line so each station can mark its part done independently. */
@Entity
@Table(name = "kitchen_ticket_items")
@Getter
@Setter
@NoArgsConstructor
public class KitchenTicketItem extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private KitchenTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false, length = 20)
    private Station station;

    @Column(nullable = false)
    private boolean done;
}
