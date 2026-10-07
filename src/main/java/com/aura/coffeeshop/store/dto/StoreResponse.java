package com.aura.coffeeshop.store.dto;

import com.aura.coffeeshop.store.entity.Store;

/**
 * Public store info. {@code open} drives the "kitchen open" badge at checkout; {@code logoUrl} and
 * {@code brandColor} are the shop's branding, applied site-wide (header logo, theme colour).
 */
public record StoreResponse(String id, String name, String address, String hoursLabel, Double latitude,
                            Double longitude, String mapsUrl, boolean open, String logoUrl, String brandColor) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getAddress(), store.getHoursLabel(),
                store.getLatitude(), store.getLongitude(), store.mapsUrl(), store.isOpen(), store.getLogoUrl(),
                store.getBrandColor());
    }
}
