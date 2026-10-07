package com.aura.coffeeshop.cart.repository;

import com.aura.coffeeshop.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findWithItemsById(String id);
}
