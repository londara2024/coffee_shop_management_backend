package com.aura.coffeeshop.inventory.entity;

import com.aura.coffeeshop.common.persistence.Auditable;
import com.aura.coffeeshop.common.persistence.ShortId;
import com.aura.coffeeshop.inventory.InventoryEnums.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends Auditable {

    @Id
    @ShortId
    @Column(length = 5)
    private String id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 200)
    private String note;

    @Column(name = "relationship_type", nullable = false, length = 20)
    private RelationshipType relationshipType;
}
