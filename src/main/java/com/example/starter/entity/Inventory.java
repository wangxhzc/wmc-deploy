package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ansible Inventory清单实体
 * 每个Inventory可以包含主机、组和变量
 */
@Entity
@Table(name = "inventories")
public class Inventory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(length = 500)
    public String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "inventory", fetch = FetchType.LAZY)
    public List<InventoryVariable> variables = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "inventory", fetch = FetchType.LAZY)
    public List<InventoryGroup> groups = new ArrayList<>();

    // 主机与清单的多对多关系
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "inventory", fetch = FetchType.LAZY)
    public List<InventoryHostInventory> hostInventories = new ArrayList<>();

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public Inventory() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Inventory(String name) {
        this();
        this.name = name;
    }

    public Inventory(String name, String description) {
        this(name);
        this.description = description;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Inventory findByName(String name) {
        return find("name", name).firstResult();
    }

    public static boolean existsByName(String name) {
        return find("name", name).count() > 0;
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

    public List<InventoryVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<InventoryVariable> variables) {
        this.variables = variables;
    }

    public List<InventoryGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<InventoryGroup> groups) {
        this.groups = groups;
    }

    public List<InventoryHostInventory> getHostInventories() {
        return hostInventories;
    }

    public void setHostInventories(List<InventoryHostInventory> hostInventories) {
        this.hostInventories = hostInventories;
    }

    /**
     * 获取主机列表（便捷方法）
     * 从 hostInventories 关联中提取主机
     */
    public List<InventoryHost> getHosts() {
        return hostInventories.stream()
                .map(InventoryHostInventory::getHost)
                .toList();
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
