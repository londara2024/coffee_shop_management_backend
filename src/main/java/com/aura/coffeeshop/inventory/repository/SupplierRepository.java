package com.aura.coffeeshop.inventory.repository;

import com.aura.coffeeshop.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {

    List<Supplier> findAllByOrderByNameAsc();

    Optional<Supplier> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
