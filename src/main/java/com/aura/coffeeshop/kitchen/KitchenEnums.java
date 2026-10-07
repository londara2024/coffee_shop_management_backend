package com.aura.coffeeshop.kitchen;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.aura.coffeeshop.common.persistence.CodedEnumConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Converter;

public final class KitchenEnums {

    private KitchenEnums() {
    }

    /** {@code kitchen_tickets.status}. Archived tickets keep {@code ready}; the order moves to {@code completed}. */
    public enum TicketStatus implements CodedEnum {
        SCHEDULED("scheduled"), NEW("new"), BREWING("brewing"), READY("ready");

        private final String code;

        TicketStatus(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static TicketStatus from(String value) {
            return CodedEnum.fromCode(TicketStatus.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<TicketStatus> {
            public JpaConverter() {
                super(TicketStatus.class);
            }
        }
    }

    /** {@code kitchen_tickets.flag_tone}. */
    public enum FlagTone implements CodedEnum {
        AMBER("amber"), NEUTRAL("neutral");

        private final String code;

        FlagTone(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static FlagTone from(String value) {
            return CodedEnum.fromCode(FlagTone.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<FlagTone> {
            public JpaConverter() {
                super(FlagTone.class);
            }
        }
    }
}
