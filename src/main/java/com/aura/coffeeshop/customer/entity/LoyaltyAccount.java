package com.aura.coffeeshop.customer.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loyalty_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyAccount extends Auditable {

    public static final String DEFAULT_TIER = "Member";
    public static final int DEFAULT_STARS_GOAL = 200;

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 40)
    private String tier = DEFAULT_TIER;

    @Column(nullable = false)
    private int stars;

    @Column(name = "stars_goal", nullable = false)
    private int starsGoal = DEFAULT_STARS_GOAL;

    public LoyaltyAccount(Customer customer) {
        this.customer = customer;
    }

}
