package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板实体类 - 类似于Ansible AWX的Job Template
 * 绑定一个项目的Playbook和一个清单的Inventory
 */
@Entity
@Table(name = "templates")
public class Template extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(length = 500)
    public String description;

    /**
     * 关联的项目（Playbook）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    /**
     * 关联的清单（Inventory）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    public Inventory inventory;

    /**
     * 额外的变量（可选）
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "template", fetch = FetchType.LAZY)
    public List<TemplateVariable> variables = new ArrayList<>();

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public Template() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Template(String name, Project project, Inventory inventory) {
        this();
        this.name = name;
        this.project = project;
        this.inventory = inventory;
    }

    public Template(String name, String description, Project project, Inventory inventory) {
        this(name, project, inventory);
        this.description = description;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Template findByName(String name) {
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<TemplateVariable> variables) {
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
}
