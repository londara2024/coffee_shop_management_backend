package com.aura.coffeeshop.inventory.dto;

import com.aura.coffeeshop.inventory.InventoryEnums.InventoryGroup;
import com.aura.coffeeshop.inventory.InventoryEnums.PurchaseOrderStatus;
import com.aura.coffeeshop.inventory.InventoryEnums.RelationshipType;
import com.aura.coffeeshop.inventory.InventoryEnums.StockState;
import com.aura.coffeeshop.inventory.entity.InventoryItem;
import com.aura.coffeeshop.inventory.entity.InventoryStockSnapshot;
import com.aura.coffeeshop.inventory.entity.PurchaseOrder;
import com.aura.coffeeshop.inventory.entity.PurchaseOrderLine;
import com.aura.coffeeshop.inventory.entity.Supplier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    // ---------------------------------------------------------------- suppliers

    public record SupplierRequest(
            @Schema(example = "Highland Coop Direct") @NotBlank @Size(max = 150) String name,
            @Size(max = 200) String note,
            @Schema(example = "direct_trade") @NotNull RelationshipType relationshipType) {
    }

    public record SupplierResponse(String id, String name, String note, RelationshipType relationshipType) {
        public static SupplierResponse from(Supplier s) {
            return s == null ? null : new SupplierResponse(s.getId(), s.getName(), s.getNote(), s.getRelationshipType());
        }
    }

    // ---------------------------------------------------------------- items & stock

    public record CreateInventoryItemRequest(
            @Schema(description = "Defaults to the flagship store") String storeId,
            String supplierId,
            @Schema(example = "Guatemala Huehuetenango") @NotBlank @Size(max = 150) String name,
            @Size(max = 250) String meta,
            @Schema(example = "Single Origin Beans") @NotNull InventoryGroup group,
            @Schema(example = "kg", defaultValue = "kg") @Size(max = 20) String unitLabel,
            @Schema(example = "25") @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal parLevel,
            @Size(max = 300) String imageUrl,
            @Schema(description = "Optional opening stock count") @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal initialOnHandQty) {
    }

    /** The "+ បន្ថែមសម្ភារះ" quick-add: names only, flagged {@code is_custom}. */
    public record QuickAddMaterialsRequest(
            String storeId,
            @Schema(example = "Packaging") @NotNull InventoryGroup group,
            @Schema(example = "[\"Bamboo Stir Sticks\", \"Kraft Pastry Bags\"]") @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 150) String> names,
            @Schema(description = "Links an existing supplier by name, if found") @Size(max = 150) String supplierName,
            @Schema(defaultValue = "unit") @Size(max = 20) String unitLabel,
            @Schema(defaultValue = "1") @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal parLevel) {
    }

    public record RecordStockRequest(
            @Schema(example = "14") @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal onHandQty,
            @Schema(description = "Override the derived state, e.g. 'fresh' for same-day bakery deliveries") StockState state) {
    }

    public record StockResponse(BigDecimal onHandQty, int pctOfPar, StockState state, boolean needsReorder,
                                Instant recordedAt) {
        public static StockResponse from(InventoryStockSnapshot s) {
            return s == null ? null
                    : new StockResponse(s.getOnHandQty(), s.getPctOfPar(), s.getState(), s.getState().needsReorder(), s.getRecordedAt());
        }
    }

    public record InventoryItemResponse(String id, String storeId, String name, String meta, InventoryGroup group,
                                        String unitLabel, BigDecimal parLevel, String imageUrl, boolean custom,
                                        SupplierResponse supplier, StockResponse stock, Instant createdAt) {
        public static InventoryItemResponse from(InventoryItem i, InventoryStockSnapshot latest) {
            return new InventoryItemResponse(i.getId(), i.getStore().getId(), i.getName(), i.getMeta(), i.getGroup(),
                    i.getUnitLabel(), i.getParLevel(), i.getImageUrl(), i.isCustom(),
                    SupplierResponse.from(i.getSupplier()), StockResponse.from(latest), i.getCreatedAt());
        }
    }

    // ---------------------------------------------------------------- purchase orders

    public record PoLineRequest(
            @Schema(description = "Tracked stock item; omit for a brand-new material") String inventoryItemId,
            @Schema(description = "Required when inventoryItemId is omitted", example = "Minor Figures Barista Oat (12cs)") @Size(max = 200) String description,
            @Schema(description = "Defaults to the inventory item's supplier") String supplierId,
            @Schema(defaultValue = "1") @Min(1) @Max(100000) Integer qty,
            @Schema(description = "Line total in USD", example = "336.00") @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal price) {
    }

    public record CreatePurchaseOrderRequest(
            String storeId,
            @Schema(example = "Flagship Roastery Operating Fund (#***849)") @Size(max = 100) String fundingAccount,
            String createdByStaffId,
            @NotEmpty @Valid List<PoLineRequest> lines) {
    }

    public record PoLineResponse(String id, String inventoryItemId, String description, String supplierId,
                                 String supplierName, int qty, BigDecimal price) {
        public static PoLineResponse from(PurchaseOrderLine l) {
            return new PoLineResponse(l.getId(), l.getInventoryItem() == null ? null : l.getInventoryItem().getId(),
                    l.getDescription(), l.getSupplier() == null ? null : l.getSupplier().getId(),
                    l.getSupplier() == null ? null : l.getSupplier().getName(), l.getQty(), l.getPrice());
        }
    }

    public record PurchaseOrderResponse(String id, String poCode, String storeId, String storeName,
                                        PurchaseOrderStatus status, String fundingAccount, String createdBy,
                                        Instant createdAt, Instant approvedAt, List<PoLineResponse> lines,
                                        BigDecimal total) {
        public static PurchaseOrderResponse from(PurchaseOrder po) {
            return new PurchaseOrderResponse(po.getId(), po.getPoCode(), po.getStore().getId(), po.getStore().getName(),
                    po.getStatus(), po.getFundingAccount(),
                    po.getCreatedByStaff() == null ? null : po.getCreatedByStaff().getName(),
                    po.getCreatedAt(), po.getApprovedAt(), po.getLines().stream().map(PoLineResponse::from).toList(),
                    po.total());
        }
    }
}
