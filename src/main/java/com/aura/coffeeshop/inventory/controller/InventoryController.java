package com.aura.coffeeshop.inventory.controller;

import com.aura.coffeeshop.common.web.PageResponse;
import com.aura.coffeeshop.inventory.InventoryEnums.InventoryGroup;
import com.aura.coffeeshop.inventory.InventoryEnums.RelationshipType;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.QuickAddMaterialsRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.RecordStockRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.StockResponse;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.SupplierRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.SupplierResponse;
import com.aura.coffeeshop.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin - Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/suppliers")
    @Operation(summary = "List suppliers")
    public List<SupplierResponse> suppliers() {
        return inventoryService.listSuppliers();
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a supplier")
    public SupplierResponse createSupplier(@Valid @RequestBody SupplierRequest request) {
        return inventoryService.createSupplier(request);
    }

    @GetMapping("/inventory/items")
    @Operation(summary = "Stock ledger", description = "Inventory items with their latest stock reading.")
    public PageResponse<InventoryItemResponse> items(
            @RequestParam(required = false) String storeId,
            @Parameter(description = "Single Origin Beans | Milk & Dairy | Syrups & Botanicals | Bakery | Packaging")
            @RequestParam(required = false) InventoryGroup group,
            @Parameter(description = "direct_trade | distributor") @RequestParam(required = false) RelationshipType supplierType,
            @Parameter(description = "Search name, meta or supplier") @RequestParam(required = false) String q,
            @Parameter(description = "Only items whose latest state is low or critical") @RequestParam(defaultValue = "false") boolean reorderOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventoryService.searchItems(storeId, group, supplierType, q, reorderOnly, page, size);
    }

    @GetMapping("/inventory/items/{itemId}")
    @Operation(summary = "Get an inventory item")
    public InventoryItemResponse item(@PathVariable String itemId) {
        return inventoryService.getItem(itemId);
    }

    @PostMapping("/inventory/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tracked inventory item", description = "Optionally records an opening stock count.")
    public InventoryItemResponse createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        return inventoryService.createItem(request);
    }

    @PostMapping("/inventory/items/quick-add")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Quick-add materials by name", description = "Duplicates are skipped; returns only newly created items.")
    public List<InventoryItemResponse> quickAdd(@Valid @RequestBody QuickAddMaterialsRequest request) {
        return inventoryService.quickAdd(request);
    }

    @PostMapping("/inventory/items/{itemId}/stock")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a stock count", description = "Stores a snapshot; % of par and state are derived unless state is overridden.")
    public InventoryItemResponse recordStock(@PathVariable String itemId, @Valid @RequestBody RecordStockRequest request) {
        return inventoryService.recordStock(itemId, request);
    }

    @GetMapping("/inventory/items/{itemId}/stock")
    @Operation(summary = "Stock count history", description = "Newest first.")
    public List<StockResponse> stockHistory(@PathVariable String itemId,
                                            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int limit) {
        return inventoryService.stockHistory(itemId, limit);
    }
}
