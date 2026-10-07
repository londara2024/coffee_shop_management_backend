package com.aura.coffeeshop.store.repository;

import com.aura.coffeeshop.store.StaffRole;
import com.aura.coffeeshop.store.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, String> {

    List<Staff> findByStoreIdOrderByNameAsc(String storeId);

    long countByStoreIdAndRole(String storeId, StaffRole role);
}
