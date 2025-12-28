package com.example.starter.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 任务实体类 - 类似于Ansible AWX的Job
 * 是模板的实例化，用于执行ansible playbook
 */
@Entity
@Table(name = "tasks")
public class Task extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 任务名称
     */
    @Column(nullable = false)
    public String name;

    /**
     * 关联的模板
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    public Template template;

    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TaskStatus status = TaskStatus.PENDING;

    /**
     * 临时目录路径（用于存放playbook和inventory文件）
     */
    @Column(length = 500)
    public String tempDirectory;

    /**
     * 执行日志文件路径
     */
    @Column(length = 500)
    public String logFilePath;

    /**
     * 开始时间
     */
    public LocalDateTime startedAt;

    /**
     * 完成时间
     */
    public LocalDateTime finishedAt;

    /**
     * 错误信息（如果失败）
     */
    @Column(columnDefinition = "TEXT")
    public String errorMessage;

    /**
     * 创建时间
     */
    public LocalDateTime createdAt;

    /**
     * 更新时间
     */
    public LocalDateTime updatedAt;

    public Task() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Task(String name, Template template) {
        this();
        this.name = name;
        this.template = template;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING, // 等待执行
        RUNNING, // 执行中
        SUCCESS, // 成功
        FAILED, // 失败
        CANCELLED // 已取消
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

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    /**
     * 计算任务持续时间（秒）
     */
    public Long getDurationInSeconds() {
        if (startedAt == null) {
            return 0L;
        }
        LocalDateTime end = finishedAt != null ? finishedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).getSeconds();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                '}';
    }
}
