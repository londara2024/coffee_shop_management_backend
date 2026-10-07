package com.aura.coffeeshop.common.persistence;

import jakarta.persistence.AttributeConverter;

/** Base JPA converter storing a {@link CodedEnum} by its code. Subclass once per enum with {@code @Converter(autoApply = true)}. */
public abstract class CodedEnumConverter<E extends Enum<E> & CodedEnum> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected CodedEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return CodedEnum.fromCode(type, dbData);
    }
}
