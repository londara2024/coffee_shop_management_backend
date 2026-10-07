package com.aura.coffeeshop.common.persistence;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * An enum whose persisted / JSON value is a "code" taken verbatim from DATABASE_DESIGN.md
 * (e.g. {@code brewing}, {@code Bar A}, {@code Single Origin Beans}) instead of the Java constant name.
 */
public interface CodedEnum {

    @JsonValue
    String getCode();

    /** Resolves by code first, then by constant name (case-insensitive), so {@code brewing} and {@code BREWING} both work. */
    static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.getCode().equalsIgnoreCase(trimmed) || e.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown " + type.getSimpleName() + " '" + value + "'. Allowed: "
                                + Arrays.stream(type.getEnumConstants()).map(CodedEnum::getCode).toList()));
    }
}
