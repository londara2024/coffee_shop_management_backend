package com.aura.coffeeshop.inventory.repository;

import com.aura.coffeeshop.inventory.InventoryEnums.InventoryGroup;
import com.aura.coffeeshop.inventory.InventoryEnums.RelationshipType;
import com.aura.coffeeshop.inventory.entity.InventoryItem;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String>, JpaSpecificationExecutor<InventoryItem> {

    boolean existsByStoreIdAndNameIgnoreCase(String storeId, String name);

    static Specification<InventoryItem> inStore(String storeId) {
        return (root, query, cb) -> storeId == null ? null : cb.equal(root.get("store").get("id"), storeId);
    }

    static Specification<InventoryItem> inGroup(InventoryGroup group) {
        return (root, query, cb) -> group == null ? null : cb.equal(root.get("group"), group);
    }

    static Specification<InventoryItem> supplierType(RelationshipType type) {
        return (root, query, cb) -> type == null ? null
                : cb.equal(root.join("supplier", JoinType.INNER).get("relationshipType"), type);
    }

    /** Matches name, meta or supplier name (the ledger's search box). */
    static Specification<InventoryItem> matching(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase() + "%";
            var supplier = root.join("supplier", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("meta"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(supplier.get("name"), "")), pattern));
        };
    }
}
