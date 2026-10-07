package com.aura.coffeeshop.inventory.service;

import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.common.web.PageResponse;
import com.aura.coffeeshop.inventory.InventoryEnums.InventoryGroup;
import com.aura.coffeeshop.inventory.InventoryEnums.RelationshipType;
import com.aura.coffeeshop.inventory.InventoryEnums.StockState;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.CreateInventoryItemRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.QuickAddMaterialsRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.RecordStockRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.StockResponse;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.SupplierRequest;
import com.aura.coffeeshop.inventory.dto.InventoryDtos.SupplierResponse;
import com.aura.coffeeshop.inventory.entity.InventoryItem;
import com.aura.coffeeshop.inventory.entity.InventoryStockSnapshot;
import com.aura.coffeeshop.inventory.entity.Supplier;
import com.aura.coffeeshop.inventory.repository.InventoryItemRepository;
import com.aura.coffeeshop.inventory.repository.InventoryStockSnapshotRepository;
import com.aura.coffeeshop.inventory.repository.SupplierRepository;
import com.aura.coffeeshop.store.entity.Store;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryStockSnapshotRepository snapshotRepository;
    private final StoreService storeService;

    // ---------------------------------------------------------------- suppliers

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers() {
        return supplierRepository.findAllByOrderByNameAsc().stream().map(SupplierResponse::from).toList();
    }

    public SupplierResponse createSupplier(SupplierRequest request) {
        if (supplierRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessRuleException("Supplier '" + request.name() + "' already exists");
        }
        Supplier supplier = new Supplier();
        supplier.setName(request.name().trim());
        supplier.setNote(request.note());
        supplier.setRelationshipType(request.relationshipType());
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    Supplier getSupplier(String id) {
        return supplierRepository.findById(id).orElseThrow(() -> NotFoundException.of("Supplier", id));
    }

    // ---------------------------------------------------------------- items

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> searchItems(String storeId, InventoryGroup group, RelationshipType supplierType,
                                                           String query, boolean reorderOnly, int page, int size) {
        Specification<InventoryItem> spec = Specification.where(InventoryItemRepository.inStore(storeId))
                .and(InventoryItemRepository.inGroup(group))
                .and(InventoryItemRepository.supplierType(supplierType))
                .and(InventoryItemRepository.matching(query));
        Sort sort = Sort.by("group", "name");

        if (reorderOnly) {
            // Stock state lives on the newest snapshot, so filter after loading the candidate set.
            List<InventoryItem> all = itemRepository.findAll(spec, sort);
            Map<String, InventoryStockSnapshot> latest = latestSnapshots(all);
            List<InventoryItemResponse> matches = all.stream()
                    .filter(i -> latest.containsKey(i.getId()) && latest.get(i.getId()).getState().needsReorder())
                    .map(i -> InventoryItemResponse.from(i, latest.get(i.getId())))
                    .toList();
            int from = Math.min(page * size, matches.size());
            int to = Math.min(from + size, matches.size());
            return new PageResponse<>(matches.subList(from, to), page, size, matches.size(),
                    (int) Math.ceil(matches.size() / (double) size));
        }

        Page<InventoryItem> items = itemRepository.findAll(spec, PageRequest.of(page, size, sort));
        Map<String, InventoryStockSnapshot> latest = latestSnapshots(items.getContent());
        return PageResponse.of(items, i -> InventoryItemResponse.from(i, latest.get(i.getId())));
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getItem(String id) {
        InventoryItem item = findItem(id);
        return InventoryItemResponse.from(item, latestSnapshots(List.of(item)).get(id));
    }

    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {
        Store store = storeService.resolveStore(request.storeId());
        String name = request.name().trim();
        requireUniqueName(store, name);

        InventoryItem item = new InventoryItem();
        item.setStore(store);
        item.setSupplier(request.supplierId() == null ? null : getSupplier(request.supplierId()));
        item.setName(name);
        item.setMeta(request.meta());
        item.setGroup(request.group());
        item.setUnitLabel(request.unitLabel() == null || request.unitLabel().isBlank() ? "kg" : request.unitLabel().trim());
        item.setParLevel(request.parLevel());
        item.setImageUrl(request.imageUrl());
        item = itemRepository.save(item);

        InventoryStockSnapshot snapshot = request.initialOnHandQty() == null ? null
                : saveSnapshot(item, request.initialOnHandQty(), null);
        return InventoryItemResponse.from(item, snapshot);
    }

    /** Bulk quick-add; names that already exist in the store are skipped (as the frontend store does). */
    public List<InventoryItemResponse> quickAdd(QuickAddMaterialsRequest request) {
        Store store = storeService.resolveStore(request.storeId());
        Supplier supplier = request.supplierName() == null || request.supplierName().isBlank() ? null
                : supplierRepository.findByNameIgnoreCase(request.supplierName().trim()).orElse(null);
        BigDecimal par = request.parLevel() == null ? BigDecimal.ONE : request.parLevel();
        String unit = request.unitLabel() == null || request.unitLabel().isBlank() ? "unit" : request.unitLabel().trim();

        Set<String> seen = new HashSet<>();
        List<InventoryItemResponse> created = new ArrayList<>();
        for (String raw : request.names()) {
            String name = raw.trim();
            if (!seen.add(name.toLowerCase()) || itemRepository.existsByStoreIdAndNameIgnoreCase(store.getId(), name)) {
                continue;
            }
            InventoryItem item = new InventoryItem();
            item.setStore(store);
            item.setSupplier(supplier);
            item.setName(name);
            item.setGroup(request.group());
            item.setUnitLabel(unit);
            item.setParLevel(par);
            item.setCustom(true);
            created.add(InventoryItemResponse.from(itemRepository.save(item), null));
        }
        return created;
    }

    // ---------------------------------------------------------------- stock

    public InventoryItemResponse recordStock(String itemId, RecordStockRequest request) {
        InventoryItem item = findItem(itemId);
        return InventoryItemResponse.from(item, saveSnapshot(item, request.onHandQty(), request.state()));
    }

    @Transactional(readOnly = true)
    public List<StockResponse> stockHistory(String itemId, int limit) {
        findItem(itemId);
        return snapshotRepository.findByItemIdOrderByRecordedAtDesc(itemId, PageRequest.of(0, limit)).stream()
                .map(StockResponse::from).toList();
    }

    InventoryItem findItem(String id) {
        return itemRepository.findById(id).orElseThrow(() -> NotFoundException.of("Inventory item", id));
    }

    private InventoryStockSnapshot saveSnapshot(InventoryItem item, BigDecimal onHand, StockState override) {
        int pct = onHand.multiply(BigDecimal.valueOf(100))
                .divide(item.getParLevel(), 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(Short.MAX_VALUE)).intValue();
        InventoryStockSnapshot snapshot = new InventoryStockSnapshot();
        snapshot.setItem(item);
        snapshot.setOnHandQty(onHand);
        snapshot.setPctOfPar((short) pct);
        snapshot.setState(override != null ? override : StockState.fromPctOfPar(pct));
        return snapshotRepository.save(snapshot);
    }

    private Map<String, InventoryStockSnapshot> latestSnapshots(List<InventoryItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        return snapshotRepository.findLatestForItems(items.stream().map(InventoryItem::getId).toList()).stream()
                .collect(Collectors.toMap(s -> s.getItem().getId(), Function.identity(), (a, b) -> a));
    }

    private void requireUniqueName(Store store, String name) {
        if (itemRepository.existsByStoreIdAndNameIgnoreCase(store.getId(), name)) {
            throw new BusinessRuleException("'" + name + "' already exists in " + store.getName());
        }
    }
}
