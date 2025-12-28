package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Inventory主机变量实体
 * 用于存储主机级别的变量
 */
@Entity
@Table(name = "inventory_host_variables")
public class InventoryHostVariable extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String variableName;

    @Column(length = 2000)
    public String variableValue;

    @ManyToOne
    @JoinColumn(name = "host_id")
    public InventoryHost host;

    public InventoryHostVariable() {
    }

    public InventoryHostVariable(String variableName, String variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    public InventoryHost getHost() {
        return host;
    }

    public void setHost(InventoryHost host) {
        this.host = host;
    }
}
