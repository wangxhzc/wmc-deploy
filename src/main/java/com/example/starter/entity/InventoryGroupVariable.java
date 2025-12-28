package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Inventory组变量实体
 * 用于存储组级别的变量
 */
@Entity
@Table(name = "inventory_group_variables")
public class InventoryGroupVariable extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String variableName;

    @Column(length = 2000)
    public String variableValue;

    @ManyToOne
    @JoinColumn(name = "group_id")
    public InventoryGroup group;

    public InventoryGroupVariable() {
    }

    public InventoryGroupVariable(String variableName, String variableValue) {
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

    public InventoryGroup getGroup() {
        return group;
    }

    public void setGroup(InventoryGroup group) {
        this.group = group;
    }
}
