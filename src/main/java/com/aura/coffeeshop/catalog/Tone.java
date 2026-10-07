package com.aura.coffeeshop.catalog;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.aura.coffeeshop.common.persistence.CodedEnumConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Converter;

/** {@code product_tags.tone}. */
public enum Tone implements CodedEnum {
    FOREST("forest"), AMBER("amber"), NEUTRAL("neutral");

    private final String code;

    Tone(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Tone from(String value) {
        return CodedEnum.fromCode(Tone.class, value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends CodedEnumConverter<Tone> {
        public JpaConverter() {
            super(Tone.class);
        }
    }
}
