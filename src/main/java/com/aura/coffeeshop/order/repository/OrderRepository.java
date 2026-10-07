package com.aura.coffeeshop.order.repository;

import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import com.aura.coffeeshop.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    @EntityGraph(attributePaths = {"customer", "store", "payment", "promoCode", "discount"})
    Optional<Order> findByOrderCode(String orderCode);

    @Query(value = "select nextval('order_code_seq')", nativeQuery = true)
    long nextOrderNumber();

    @EntityGraph(attributePaths = {"customer", "store", "payment"})
    @Query("""
            select o from Order o
            where (:customerId is null or o.customer.id = :customerId)
              and (:status is null or o.status = :status)
            """)
    Page<Order> search(@Param("customerId") String customerId, @Param("status") OrderStatus status, Pageable pageable);
}
