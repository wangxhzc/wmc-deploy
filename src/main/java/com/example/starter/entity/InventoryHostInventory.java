package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Inventory主机-清单关联实体
 * 用于主机和清单的多对多关系
 * 主机可以同时属于多个清单
 */
@Entity
@Table(name = "inventory_host_inventories")
public class InventoryHostInventory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_id", nullable = false)
    public Inventory inventory;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    public InventoryHost host;

    public InventoryHostInventory() {
    }

    public InventoryHostInventory(Inventory inventory, InventoryHost host) {
        this.inventory = inventory;
        this.host = host;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public InventoryHost getHost() {
        return host;
    }

    public void setHost(InventoryHost host) {
        this.host = host;
    }
}
