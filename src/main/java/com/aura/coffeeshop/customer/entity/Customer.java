package com.aura.coffeeshop.customer.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, length = 30)
    private String phone;

    @Column(unique = true, length = 200)
    private String email;

    @Column(name = "avatar_url", length = 300)
    private String avatarUrl;

    /** Extension to the design doc: BCrypt hash; null for customers created without an account. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @OneToOne(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    private LoyaltyAccount loyalty;

}
