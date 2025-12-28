package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Inventory组主机关联实体
 * 用于关联组和主机的关系
 */
@Entity
@Table(name = "inventory_group_hosts")
public class InventoryGroupHost extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    public InventoryGroup group;

    @ManyToOne
    @JoinColumn(name = "host_id")
    public InventoryHost host;

    public InventoryGroupHost() {
    }

    public InventoryGroupHost(InventoryGroup group, InventoryHost host) {
        this.group = group;
        this.host = host;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryGroup getGroup() {
        return group;
    }

    public void setGroup(InventoryGroup group) {
        this.group = group;
    }

    public InventoryHost getHost() {
        return host;
    }

    public void setHost(InventoryHost host) {
        this.host = host;
    }
}
