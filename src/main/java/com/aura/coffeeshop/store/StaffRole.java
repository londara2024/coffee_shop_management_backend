package com.aura.coffeeshop.store;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.aura.coffeeshop.common.persistence.CodedEnumConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Converter;

/** {@code staff.role}: the two roles managed on the admin settings page. */
public enum StaffRole implements CodedEnum {
    MANAGER("manager", "Store Manager", "អ្នកគ្រប់គ្រងហាង"),
    STAFF("staff", "Staff", "បុគ្គលិក");

    private final String code;
    private final String label;
    private final String labelKm;

    StaffRole(String code, String label, String labelKm) {
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
    public static StaffRole from(String value) {
        return CodedEnum.fromCode(StaffRole.class, value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends CodedEnumConverter<StaffRole> {
        public JpaConverter() {
            super(StaffRole.class);
        }
    }
}
