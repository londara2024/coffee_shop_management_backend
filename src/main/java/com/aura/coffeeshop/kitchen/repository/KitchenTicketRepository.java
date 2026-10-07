package com.aura.coffeeshop.kitchen.repository;

import com.aura.coffeeshop.kitchen.entity.KitchenTicket;
import com.aura.coffeeshop.order.OrderEnums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, String> {

    /** Tickets whose order is still open (not completed / cancelled), oldest first. */
    @Query("""
            select t from KitchenTicket t
            join fetch t.order o
            left join fetch o.customer
            left join fetch t.assignedStaff
            where o.status in :statuses
            order by t.startedAt asc
            """)
    List<KitchenTicket> findByOrderStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select count(t) from KitchenTicket t
            where t.order.status = :status
              and t.readyAt >= :since
            """)
    long countByOrderStatusSince(@Param("status") OrderStatus status, @Param("since") Instant since);
}
