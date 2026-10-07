package com.aura.coffeeshop.order;

import com.aura.coffeeshop.order.entity.Order;

/**
 * Published (synchronously, inside the checkout transaction) after an order is persisted.
 * The kitchen module listens to it to open a KDS ticket, keeping order → kitchen decoupled.
 */
public record OrderPlacedEvent(Order order) {
}
