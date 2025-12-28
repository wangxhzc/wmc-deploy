package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory主机实体
 * 独立管理主机信息
 * 主机可以同时属于多个清单和组
 */
@Entity
@Table(name = "inventory_hosts")
public class InventoryHost extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public String host;

    @Column
    public Integer port;

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String password;

    // 主机所属的清单（多对多关系）
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "host", fetch = FetchType.LAZY)
    public List<InventoryHostInventory> inventories = new ArrayList<>();

    // 主机级别的变量
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "host", fetch = FetchType.LAZY)
    public List<InventoryHostVariable> variables = new ArrayList<>();

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    // 连接状态
    public Boolean connected;

    // 最后检测时间
    public LocalDateTime lastChecked;

    public InventoryHost() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.port = 22;
        this.connected = false;
    }

    public InventoryHost(String name, String host) {
        this();
        this.name = name;
        this.host = host;
    }

    public InventoryHost(String name, String host, Integer port) {
        this(name, host);
        this.port = port;
    }

    public InventoryHost(String name, String host, Integer port, String username, String password) {
        this(name, host, port);
        this.username = username;
        this.password = password;
    }

    /**
     * 拷贝构造函数 - 用于创建主机副本
     */
    public InventoryHost(InventoryHost other) {
        this(other.getName(), other.getHost(), other.getPort(), other.getUsername(), other.getPassword());
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<InventoryHostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InventoryHostInventory> inventories) {
        this.inventories = inventories;
    }

    public List<InventoryHostVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<InventoryHostVariable> variables) {
        this.variables = variables;
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

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
}
