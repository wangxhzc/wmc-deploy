package com.example.starter.repository;

import com.example.starter.entity.Project;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * 项目数据访问接口
 */
@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {

    /**
     * 根据名称查找项目
     */
    public Optional<Project> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * 根据名称前缀查找项目
     */
    public List<Project> findByNameStartingWith(String prefix) {
        return list("name like ?1", prefix + "%");
    }

    /**
     * 检查项目名称是否已存在
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
}
