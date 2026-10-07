package com.aura.coffeeshop.catalog.repository;

import com.aura.coffeeshop.catalog.entity.ProductTopping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductToppingRepository extends JpaRepository<ProductTopping, String> {

    /** Global default toppings (product_id IS NULL), locked ones first. */
    List<ProductTopping> findByProductIsNullOrderByLockedDescPriceDeltaAsc();
}
