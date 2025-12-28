package com.example.starter.service;

import com.example.starter.entity.*;
import com.example.starter.entity.Task.TaskStatus;
import com.example.starter.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务 - 提供各种统计数据
 */
@ApplicationScoped
public class StatisticsService {

    @Inject
    InventoryHostRepository hostRepository;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    TemplateRepository templateRepository;

    @Inject
    TaskRepository taskRepository;

    /**
     * 获取主机统计信息
     */
    public Map<String, Object> getHostStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<InventoryHost> hosts = hostRepository.listAll();
        long totalHosts = hosts.size();

        long connectedHosts = hosts.stream()
                .filter(InventoryHost::getConnected)
                .count();

        long disconnectedHosts = totalHosts - connectedHosts;

        stats.put("total", totalHosts);
        stats.put("connected", connectedHosts);
        stats.put("disconnected", disconnectedHosts);
        stats.put("connectionRate", totalHosts > 0 ? (connectedHosts * 100.0 / totalHosts) : 0.0);

        return stats;
    }

    /**
     * 获取清单统计信息
     */
    public long getInventoryCount() {
        return inventoryRepository.count();
    }

    /**
     * 获取项目统计信息
     */
    public long getProjectCount() {
        return projectRepository.count();
    }

    /**
     * 获取模板统计信息
     */
    public long getTemplateCount() {
        return templateRepository.count();
    }

    /**
     * 获取任务统计信息
     */
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalTasks = taskRepository.count();
        long pendingTasks = taskRepository.countByStatus(TaskStatus.PENDING);
        long runningTasks = taskRepository.countByStatus(TaskStatus.RUNNING);
        long successTasks = taskRepository.countByStatus(TaskStatus.SUCCESS);
        long failedTasks = taskRepository.countByStatus(TaskStatus.FAILED);
        long cancelledTasks = taskRepository.countByStatus(TaskStatus.CANCELLED);

        stats.put("total", totalTasks);
        stats.put("pending", pendingTasks);
        stats.put("running", runningTasks);
        stats.put("success", successTasks);
        stats.put("failed", failedTasks);
        stats.put("cancelled", cancelledTasks);

        long completedTasks = successTasks + failedTasks + cancelledTasks;
        stats.put("completed", completedTasks);
        stats.put("successRate", completedTasks > 0 ? (successTasks * 100.0 / completedTasks) : 0.0);

        return stats;
    }

    /**
     * 获取最近的任务
     */
    public List<Task> getRecentTasks(int limit) {
        List<Task> allTasks = taskRepository.findAllOrderByCreatedAtDesc();
        if (allTasks.size() > limit) {
            return allTasks.subList(0, limit);
        }
        return allTasks;
    }

    /**
     * 获取所有统计数据
     */
    public Map<String, Object> getAllStatistics() {
        Map<String, Object> allStats = new HashMap<>();

        allStats.put("hosts", getHostStatistics());
        allStats.put("inventories", getInventoryCount());
        allStats.put("projects", getProjectCount());
        allStats.put("templates", getTemplateCount());
        allStats.put("tasks", getTaskStatistics());
        allStats.put("recentTasks", getRecentTasks(5));

        return allStats;
    }
}
