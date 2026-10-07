package com.aura.coffeeshop.inventory.service;

import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.inventory.InventoryEnums.PurchaseOrderStatus;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.CreatePurchaseOrderRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.PoLineRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.PurchaseOrderResponse;
import com.aura.coffeeshop.inventory.entity.InventoryItem;
import com.aura.coffeeshop.inventory.entity.PurchaseOrder;
import com.aura.coffeeshop.inventory.entity.PurchaseOrderLine;
import com.aura.coffeeshop.inventory.repository.PurchaseOrderRepository;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;

/** Purchase order lifecycle: draft (editable) → approved → sent. */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryService inventoryService;
    private final StoreService storeService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(PurchaseOrderStatus status) {
        List<PurchaseOrder> orders = status == null
                ? purchaseOrderRepository.findAllByOrderByCreatedAtDesc()
                : purchaseOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(PurchaseOrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(String id) {
        return PurchaseOrderResponse.from(find(id));
    }

    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoCode(String.format("PO-%d-%03d", Year.now().getValue(), purchaseOrderRepository.nextPoNumber()));
        po.setStore(storeService.resolveStore(request.storeId()));
        po.setFundingAccount(request.fundingAccount());
        if (request.createdByStaffId() != null) {
            po.setCreatedByStaff(storeService.getStaff(request.createdByStaffId()));
        }
        request.lines().forEach(line -> po.addLine(toLine(line)));
        return PurchaseOrderResponse.from(purchaseOrderRepository.save(po));
    }

    public PurchaseOrderResponse addLine(String id, PoLineRequest request) {
        PurchaseOrder po = findDraft(id);
        po.addLine(toLine(request));
        return PurchaseOrderResponse.from(purchaseOrderRepository.saveAndFlush(po));
    }

    public PurchaseOrderResponse removeLine(String id, String lineId) {
        PurchaseOrder po = findDraft(id);
        PurchaseOrderLine line = po.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Purchase order line", lineId));
        po.getLines().remove(line);
        return PurchaseOrderResponse.from(purchaseOrderRepository.saveAndFlush(po));
    }

    public PurchaseOrderResponse approve(String id) {
        PurchaseOrder po = findDraft(id);
        if (po.getLines().isEmpty()) {
            throw new BusinessRuleException("Cannot approve a purchase order with no lines");
        }
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedAt(Instant.now());
        return PurchaseOrderResponse.from(po);
    }

    public PurchaseOrderResponse send(String id) {
        PurchaseOrder po = find(id);
        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException("Only approved purchase orders can be sent (current: " + po.getStatus().getCode() + ")");
        }
        po.setStatus(PurchaseOrderStatus.SENT);
        return PurchaseOrderResponse.from(po);
    }

    private PurchaseOrderLine toLine(PoLineRequest request) {
        PurchaseOrderLine line = new PurchaseOrderLine();
        InventoryItem item = request.inventoryItemId() == null ? null : inventoryService.findItem(request.inventoryItemId());
        String description = request.description() != null && !request.description().isBlank()
                ? request.description().trim()
                : item != null ? item.getName() : null;
        if (description == null) {
            throw BusinessRuleException.badRequest("Each line needs an inventoryItemId or a description");
        }
        line.setInventoryItem(item);
        line.setDescription(description);
        line.setSupplier(request.supplierId() != null ? inventoryService.getSupplier(request.supplierId())
                : item != null ? item.getSupplier() : null);
        line.setQty(request.qty() == null ? 1 : request.qty());
        line.setPrice(request.price());
        return line;
    }

    private PurchaseOrder find(String id) {
        return purchaseOrderRepository.findById(id).orElseThrow(() -> NotFoundException.of("Purchase order", id));
    }

    private PurchaseOrder findDraft(String id) {
        PurchaseOrder po = find(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessRuleException("Purchase order " + po.getPoCode() + " is " + po.getStatus().getCode()
                    + " and can no longer be edited");
        }
        return po;
    }
}
