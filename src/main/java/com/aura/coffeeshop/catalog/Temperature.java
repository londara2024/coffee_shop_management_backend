package com.aura.coffeeshop.catalog;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.fasterxml.jackson.annotation.JsonCreator;

/** Drink temperature chosen in the configurator. Not persisted on its own; it becomes part of {@code details_label}. */
public enum Temperature implements CodedEnum {
    HOT("hot", "Hot", "ក្តៅ"),
    ICED("iced", "Iced", "ទឹកកក");

    private final String code;
    private final String label;
    private final String labelKm;

    Temperature(String code, String label, String labelKm) {
        this.code = code;
        this.label = label;
        this.labelKm = labelKm;
    }

    @Override
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelKm() {
        return labelKm;
    }

    @JsonCreator
    public static Temperature from(String value) {
        return CodedEnum.fromCode(Temperature.class, value);
    }
}
