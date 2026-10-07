package com.aura.coffeeshop.catalog;

import com.aura.coffeeshop.catalog.entity.Product;

import java.math.BigDecimal;

/**
 * Result of pricing a configured product.
 *
 * @param variantKey    distinguishes identical products configured differently (cart line identity)
 * @param detailsLabel  e.g. "ធម្មតា · Hot · Oat Milk · Extra Wildflower Honey Drizzle"
 * @param tag           short label shown next to the line, e.g. "House"
 */
public record PricedItem(Product product, BigDecimal unitPrice, String variantKey, String detailsLabel, String tag) {
}
