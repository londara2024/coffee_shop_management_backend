package com.aura.coffeeshop.inventory.repository;

import com.aura.coffeeshop.inventory.entity.InventoryStockSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventoryStockSnapshotRepository extends JpaRepository<InventoryStockSnapshot, String> {

    /** Newest snapshot for each of the given items. */
    @Query("""
            select s from InventoryStockSnapshot s
            where s.item.id in :itemIds
              and s.recordedAt = (select max(s2.recordedAt) from InventoryStockSnapshot s2 where s2.item = s.item)
            """)
    List<InventoryStockSnapshot> findLatestForItems(@Param("itemIds") Collection<String> itemIds);

    List<InventoryStockSnapshot> findByItemIdOrderByRecordedAtDesc(String itemId, Pageable pageable);
}
