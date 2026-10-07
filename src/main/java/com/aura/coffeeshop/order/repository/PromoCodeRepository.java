package com.aura.coffeeshop.order.repository;

import com.aura.coffeeshop.order.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, String> {

    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    List<PromoCode> findByActiveTrueOrderByAmountAsc();
}
