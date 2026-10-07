package com.aura.coffeeshop.catalog.repository;

import com.aura.coffeeshop.catalog.entity.MilkOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilkOptionRepository extends JpaRepository<MilkOption, String> {

    List<MilkOption> findAllByOrderBySortOrderAsc();
}
