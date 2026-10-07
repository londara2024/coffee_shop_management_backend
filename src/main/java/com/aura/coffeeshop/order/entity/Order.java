package com.aura.coffeeshop.order.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.customer.entity.Customer;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentMethod;
import com.aura.coffeeshop.order.OrderEnums.FulfillmentTiming;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.store.entity.Store;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Mirrors {@code PlacedOrder} in the frontend's order.ts, extended with fulfillment fields. */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    /** Display code, e.g. {@code ACR-8942}. */
    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "fulfillment_method", nullable = false, length = 20)
    private FulfillmentMethod fulfillmentMethod;

    @Column(name = "fulfillment_timing", nullable = false, length = 20)
    private FulfillmentTiming fulfillmentTiming = FulfillmentTiming.ASAP;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    /** Snapshot; survives later edits of the discount. */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code")
    private PromoCode promoCode;

    @Column(name = "promo_discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal promoDiscountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Payment payment;

    @PrePersist
    void onCreate() {
        if (placedAt == null) {
            placedAt = Instant.now();
        }
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void attachPayment(Payment payment) {
        payment.setOrder(this);
        this.payment = payment;
    }

    public String pickupLabel() {
        return fulfillmentMethod.getPickupLabel();
    }
}
