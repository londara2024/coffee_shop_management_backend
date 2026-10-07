package com.aura.coffeeshop.setting.repository;

import com.aura.coffeeshop.setting.entity.BrandColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandColorRepository extends JpaRepository<BrandColor, String> {

    List<BrandColor> findAllByOrderBySortOrderAsc();

    Optional<BrandColor> findByHex(String hex);

    Optional<BrandColor> findByDefaultColorTrue();
}
