package com.aura.coffeeshop.inventory.controller;

import com.aura.coffeeshop.inventory.InventoryEnums.PurchaseOrderStatus;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.CreatePurchaseOrderRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.PoLineRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.PurchaseOrderResponse;
import com.aura.coffeeshop.inventory.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/admin/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Purchasing")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @Operation(summary = "List purchase orders", description = "Newest first.")
    public List<PurchaseOrderResponse> list(
            @Parameter(description = "draft | approved | sent") @RequestParam(required = false) PurchaseOrderStatus status) {
        return purchaseOrderService.list(status);
    }

    @GetMapping("/{poId}")
    @Operation(summary = "Get a purchase order")
    public PurchaseOrderResponse get(@PathVariable String poId) {
        return purchaseOrderService.get(poId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a draft purchase order")
    public PurchaseOrderResponse create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return purchaseOrderService.create(request);
    }

    @PostMapping("/{poId}/lines")
    @Operation(summary = "Add a line to a draft")
    public PurchaseOrderResponse addLine(@PathVariable String poId, @Valid @RequestBody PoLineRequest request) {
        return purchaseOrderService.addLine(poId, request);
    }

    @DeleteMapping("/{poId}/lines/{lineId}")
    @Operation(summary = "Remove a line from a draft")
    public PurchaseOrderResponse removeLine(@PathVariable String poId, @PathVariable String lineId) {
        return purchaseOrderService.removeLine(poId, lineId);
    }

    @PostMapping("/{poId}/approve")
    @Operation(summary = "Approve a draft", description = "draft → approved; lines can no longer be edited.")
    public PurchaseOrderResponse approve(@PathVariable String poId) {
        return purchaseOrderService.approve(poId);
    }

    @PostMapping("/{poId}/send")
    @Operation(summary = "Send an approved order to the supplier", description = "approved → sent.")
    public PurchaseOrderResponse send(@PathVariable String poId) {
        return purchaseOrderService.send(poId);
    }
}
