package com.example.starter.repository;

import com.example.starter.entity.Task;
import com.example.starter.entity.Task.TaskStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * 任务Repository
 */
@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {

    /**
     * 查找所有任务，按创建时间倒序排列（最新的在前面）
     */
    public List<Task> findAllOrderByCreatedAtDesc() {
        return list("order by createdAt desc");
    }

    /**
     * 按状态查找任务
     */
    public List<Task> findByStatus(TaskStatus status) {
        return list("status", status);
    }

    /**
     * 查找正在运行的任务
     */
    public List<Task> findRunningTasks() {
        return list("status", TaskStatus.RUNNING);
    }

    /**
     * 根据ID查找任务
     */
    public Task findById(Long id) {
        return find("id", id).firstResult();
    }

    /**
     * 查找指定模板的任务
     */
    public List<Task> findByTemplateId(Long templateId) {
        return list("template.id", templateId);
    }

    /**
     * 统计指定状态的任务数量
     */
    public long countByStatus(TaskStatus status) {
        return count("status", status);
    }
}
