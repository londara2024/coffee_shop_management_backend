package com.aura.coffeeshop.customer.repository;

import com.aura.coffeeshop.customer.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, String> {
}
