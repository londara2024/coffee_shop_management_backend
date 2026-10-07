package com.aura.coffeeshop.inventory;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.aura.coffeeshop.common.persistence.CodedEnumConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Converter;

public final class InventoryEnums {

    private InventoryEnums() {
    }

    /** {@code suppliers.relationship_type}. */
    public enum RelationshipType implements CodedEnum {
        DIRECT_TRADE("direct_trade"), DISTRIBUTOR("distributor");

        private final String code;

        RelationshipType(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static RelationshipType from(String value) {
            return CodedEnum.fromCode(RelationshipType.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<RelationshipType> {
            public JpaConverter() {
                super(RelationshipType.class);
            }
        }
    }

    /** {@code inventory_items.group_name}. */
    public enum InventoryGroup implements CodedEnum {
        SINGLE_ORIGIN_BEANS("Single Origin Beans"),
        MILK_AND_DAIRY("Milk & Dairy"),
        SYRUPS_AND_BOTANICALS("Syrups & Botanicals"),
        BAKERY("Bakery"),
        PACKAGING("Packaging");

        private final String code;

        InventoryGroup(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static InventoryGroup from(String value) {
            return CodedEnum.fromCode(InventoryGroup.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<InventoryGroup> {
            public JpaConverter() {
                super(InventoryGroup.class);
            }
        }
    }

    /** {@code inventory_stock_snapshots.state}. */
    public enum StockState implements CodedEnum {
        CRITICAL("critical"), LOW("low"), ADEQUATE("adequate"), HEALTHY("healthy"), FRESH("fresh");

        private final String code;

        StockState(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        /** Derived state from % of par (same thresholds as the inventory ledger's sample data). */
        public static StockState fromPctOfPar(int pct) {
            if (pct <= 40) {
                return CRITICAL;
            }
            if (pct < 70) {
                return LOW;
            }
            return pct < 100 ? ADEQUATE : HEALTHY;
        }

        public boolean needsReorder() {
            return this == CRITICAL || this == LOW;
        }

        @JsonCreator
        public static StockState from(String value) {
            return CodedEnum.fromCode(StockState.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<StockState> {
            public JpaConverter() {
                super(StockState.class);
            }
        }
    }

    /** {@code purchase_orders.status}. */
    public enum PurchaseOrderStatus implements CodedEnum {
        DRAFT("draft"), APPROVED("approved"), SENT("sent");

        private final String code;

        PurchaseOrderStatus(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static PurchaseOrderStatus from(String value) {
            return CodedEnum.fromCode(PurchaseOrderStatus.class, value);
        }

        @Converter(autoApply = true)
        public static class JpaConverter extends CodedEnumConverter<PurchaseOrderStatus> {
            public JpaConverter() {
                super(PurchaseOrderStatus.class);
            }
        }
    }
}
