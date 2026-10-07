package com.aura.coffeeshop.store.repository;

import com.aura.coffeeshop.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, String> {

    List<Store> findAllByOrderByNameAsc();

    /** The main store: the first one created (never changes when stores are renamed). */
    Optional<Store> findFirstByOrderByCreatedAtAscIdAsc();
}
