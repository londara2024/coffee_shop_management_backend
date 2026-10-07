package com.aura.coffeeshop.catalog.repository;

import com.aura.coffeeshop.catalog.entity.Pairing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PairingRepository extends JpaRepository<Pairing, String> {

    List<Pairing> findAllByOrderByPriceAsc();
}
