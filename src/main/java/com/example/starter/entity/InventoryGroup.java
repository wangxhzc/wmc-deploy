package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory组实体
 * 每个组可以包含主机、子组和变量
 * 支持Ansible Inventory的层级结构
 * 同一个Inventory中的组名必须唯一
 */
@Entity
@Table(name = "inventory_groups", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "inventory_id", "name" }, name = "uk_inventory_group_name")
})
public class InventoryGroup extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(length = 500)
    public String description;

    // 所属的Inventory
    @ManyToOne
    @JoinColumn(name = "inventory_id")
    public Inventory inventory;

    // 父组（支持层级结构）
    @ManyToOne
    @JoinColumn(name = "parent_group_id")
    public InventoryGroup parentGroup;

    // 组级别的变量
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group", fetch = FetchType.LAZY)
    public List<InventoryGroupVariable> variables = new ArrayList<>();

    // 组内的主机
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "group", fetch = FetchType.LAZY)
    public List<InventoryGroupHost> groupHosts = new ArrayList<>();

    // 子组
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parentGroup", fetch = FetchType.LAZY)
    public List<InventoryGroup> childGroups = new ArrayList<>();

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public InventoryGroup() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public InventoryGroup(String name) {
        this();
        this.name = name;
    }

    public InventoryGroup(String name, String description) {
        this(name);
        this.description = description;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public InventoryGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(InventoryGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    public List<InventoryGroupVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<InventoryGroupVariable> variables) {
        this.variables = variables;
    }

    public List<InventoryGroupHost> getGroupHosts() {
        return groupHosts;
    }

    public void setGroupHosts(List<InventoryGroupHost> groupHosts) {
        this.groupHosts = groupHosts;
    }

    public List<InventoryGroup> getChildGroups() {
        return childGroups;
    }

    public void setChildGroups(List<InventoryGroup> childGroups) {
        this.childGroups = childGroups;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
