package com.example.starter.repository;

import com.example.starter.entity.Template;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * 模板数据访问接口
 */
@ApplicationScoped
public class TemplateRepository implements PanacheRepository<Template> {

    /**
     * 根据名称查找模板
     */
    public Optional<Template> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * 根据名称前缀查找模板
     */
    public List<Template> findByNameStartingWith(String prefix) {
        return list("name like ?1", prefix + "%");
    }

    /**
     * 根据项目ID查找模板
     */
    public List<Template> findByProjectId(Long projectId) {
        return list("project.id = ?1", projectId);
    }

    /**
     * 根据清单ID查找模板
     */
    public List<Template> findByInventoryId(Long inventoryId) {
        return list("inventory.id = ?1", inventoryId);
    }

    /**
     * 检查模板名称是否已存在
     */
    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    /**
     * 根据名称查找，排除指定ID（用于更新时检查重名）
     */
    public boolean existsByNameExcludingId(String name, Long id) {
        return count("name = ?1 and id != ?2", name, id) > 0;
    }

    /**
     * 查找所有模板并关联加载项目和清单
     */
    public List<Template> findAllWithAssociations() {
        return findAll().list();
    }
}
