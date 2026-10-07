package com.aura.coffeeshop.inventory.repository;

import com.aura.coffeeshop.inventory.InventoryEnums.PurchaseOrderStatus;
import com.aura.coffeeshop.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    @EntityGraph(attributePaths = {"store", "createdByStaff"})
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"store", "createdByStaff"})
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);

    @Query(value = "select nextval('po_code_seq')", nativeQuery = true)
    long nextPoNumber();
}
