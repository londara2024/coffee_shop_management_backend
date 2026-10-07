package com.aura.coffeeshop.order;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.aura.coffeeshop.common.persistence.CodedEnumConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Converter;

/** Enum-like columns of the order tables, stored with the codes from DATABASE_DESIGN.md. */
public final class OrderEnums {

    private OrderEnums() {
    }

    /** {@code orders.status}. */
    public enum OrderStatus implements CodedEnum {
        NEW("new"), BREWING("brewing"), READY("ready"), COMPLETED("completed"), CANCELLED("cancelled");

        private final String code;

        OrderStatus(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        public boolean isOpen() {
            return this != COMPLETED && this != CANCELLED;
        }

        @JsonCreator
        public static OrderStatus from(String value) {
            return CodedEnum.fromCode(OrderStatus.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<OrderStatus> {
            public JpaConverter() {
                super(OrderStatus.class);
            }
        }
    }

    /** {@code orders.fulfillment_method}. */
    public enum FulfillmentMethod implements CodedEnum {
        STORE("store", "Pick-Up Bar A"), CURBSIDE("curbside", "Curbside Bay");

        private final String code;
        private final String pickupLabel;

        FulfillmentMethod(String code, String pickupLabel) {
            this.code = code;
            this.pickupLabel = pickupLabel;
        }

        @Override
        public String getCode() {
            return code;
        }

        public String getPickupLabel() {
            return pickupLabel;
        }

        @JsonCreator
        public static FulfillmentMethod from(String value) {
            return CodedEnum.fromCode(FulfillmentMethod.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<FulfillmentMethod> {
            public JpaConverter() {
                super(FulfillmentMethod.class);
            }
        }
    }

    /** {@code orders.fulfillment_timing}. */
    public enum FulfillmentTiming implements CodedEnum {
        ASAP("asap"), LATER("later");

        private final String code;

        FulfillmentTiming(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static FulfillmentTiming from(String value) {
            return CodedEnum.fromCode(FulfillmentTiming.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<FulfillmentTiming> {
            public JpaConverter() {
                super(FulfillmentTiming.class);
            }
        }
    }

    /** {@code payments.method}; labels as shown in checkout-view.tsx. */
    public enum PaymentMethod implements CodedEnum {
        ABA("aba", "ABA ឬស្កេន QR"), CASH("cash", "សាច់ប្រាក់");

        private final String code;
        private final String label;

        PaymentMethod(String code, String label) {
            this.code = code;
            this.label = label;
        }

        @Override
        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static PaymentMethod from(String value) {
            return CodedEnum.fromCode(PaymentMethod.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<PaymentMethod> {
            public JpaConverter() {
                super(PaymentMethod.class);
            }
        }
    }

    /** Kitchen routing for {@code order_items.station} / {@code kitchen_ticket_items.station}. */
    public enum Station implements CodedEnum {
        BAR_A("Bar A"), BAR_B("Bar B"), HEARTH("Hearth"), BAKERY("Bakery"), COLD_LAB("Cold Lab"), TEA_BAR("Tea Bar");

        private final String code;

        Station(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        /** Default routing by menu category (coffee → espresso bar, food → hearth, ...). */
        public static Station forCategory(String categoryId) {
            return switch (categoryId) {
                case "tea" -> TEA_BAR;
                case "food" -> HEARTH;
                case "desserts" -> BAKERY;
                default -> BAR_A;
            };
        }

        @JsonCreator
        public static Station from(String value) {
            return CodedEnum.fromCode(Station.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<Station> {
            public JpaConverter() {
                super(Station.class);
            }
        }
    }
}
