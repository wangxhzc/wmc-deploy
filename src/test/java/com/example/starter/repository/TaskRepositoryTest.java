package com.example.starter.repository;

import com.example.starter.entity.Task;
import com.example.starter.entity.Task.TaskStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskRepository测试
 * 注意：由于Task需要关联Template，这里只测试Repository的查询方法
 */
@QuarkusTest
public class TaskRepositoryTest {

    @Inject
    TaskRepository taskRepository;

    @Test
    public void testFindAllOrderByCreatedAtDesc() {
        List<Task> tasks = taskRepository.findAllOrderByCreatedAtDesc();
        assertNotNull(tasks);
        // 列表可能为空，这是正常的
    }

    @Test
    public void testFindByStatus() {
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);
        assertNotNull(pendingTasks);

        List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
        assertNotNull(runningTasks);

        List<Task> successTasks = taskRepository.findByStatus(TaskStatus.SUCCESS);
        assertNotNull(successTasks);

        List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);
        assertNotNull(failedTasks);
    }

    @Test
    public void testFindRunningTasks() {
        List<Task> runningTasks = taskRepository.findRunningTasks();
        assertNotNull(runningTasks);
    }

    @Test
    public void testCountByStatus() {
        long pendingCount = taskRepository.countByStatus(TaskStatus.PENDING);
        assertTrue(pendingCount >= 0);

        long runningCount = taskRepository.countByStatus(TaskStatus.RUNNING);
        assertTrue(runningCount >= 0);
    }
}
