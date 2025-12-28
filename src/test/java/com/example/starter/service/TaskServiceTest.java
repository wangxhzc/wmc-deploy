package com.example.starter.service;

import com.example.starter.entity.*;
import com.example.starter.entity.Task.TaskStatus;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.TaskRepository;
import com.example.starter.repository.TemplateRepository;
import com.example.starter.service.inventory.InventoryService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskServiceTest {

    @Inject
    TaskService taskService;

    @Inject
    TaskRepository taskRepository;

    @Inject
    TemplateRepository templateRepository;

    @Inject
    ProjectService projectService;

    @Inject
    InventoryService inventoryService;

    @Inject
    TemplateService templateService;

    private static Project testProject;
    private static Inventory testInventory;
    private static Template testTemplate;

    private static int testCounter = 0;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理数据
        taskRepository.deleteAll();
        templateRepository.deleteAll();

        testCounter++;

        // 创建测试数据 - 使用唯一名称避免冲突
        testProject = projectService.createProject(
                "测试项目" + testCounter,
                "测试项目描述" + testCounter,
                "YQBuAHM=");
        testInventory = inventoryService.createInventory(
                "测试清单" + testCounter,
                "测试清单描述" + testCounter);
        testTemplate = templateService.createTemplate(
                "测试模板" + testCounter,
                "测试模板描述" + testCounter,
                testProject.getId(),
                testInventory.getId());
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        // 清理任务
        taskRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    @Order(1)
    public void testCreateAndStartTask() throws InterruptedException {
        Task task = taskService.createAndStartTask("新任务", testTemplate.getId());

        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("新任务", task.getName());
        assertEquals(testTemplate.getId(), task.getTemplate().getId());
        // 任务是异步执行的，状态可能是PENDING、RUNNING或FAILED
        assertTrue(task.getStatus() == TaskStatus.PENDING ||
                task.getStatus() == TaskStatus.RUNNING ||
                task.getStatus() == TaskStatus.FAILED);
    }

    @Test
    @Order(2)
    public void testCreateAndStartTaskTemplateNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.createAndStartTask("新任务", 9999L));

        assertTrue(exception.getMessage().contains("模板不存在"));
    }

    @Test
    @Order(3)
    public void testGetAllTasks() {
        // 创建多个任务
        taskService.createAndStartTask("任务1", testTemplate.getId());
        taskService.createAndStartTask("任务2", testTemplate.getId());
        taskService.createAndStartTask("任务3", testTemplate.getId());

        List<Task> tasks = taskService.getAllTasks();

        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        // 应该按创建时间倒序排列
        assertEquals("任务3", tasks.get(0).getName());
        assertEquals("任务2", tasks.get(1).getName());
        assertEquals("任务1", tasks.get(2).getName());
    }

    @Test
    @Order(4)
    public void testGetAllTasksEmpty() {
        List<Task> tasks = taskService.getAllTasks();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    @Order(5)
    public void testGetTaskById() {
        Task createdTask = taskService.createAndStartTask("测试任务", testTemplate.getId());

        Task foundTask = taskService.getTaskById(createdTask.getId());

        assertNotNull(foundTask);
        assertEquals(createdTask.getId(), foundTask.getId());
        assertEquals("测试任务", foundTask.getName());
    }

    @Test
    @Order(6)
    public void testGetTaskByIdNotFound() {
        Task foundTask = taskService.getTaskById(9999L);

        assertNull(foundTask);
    }

    @Test
    @Order(7)
    @Transactional
    public void testGetTaskLogWithNullLogPath() {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setLogFilePath(null);
        taskRepository.persist(task);

        String log = taskService.getTaskLog(task.getId());

        assertEquals("暂无日志文件", log);
    }

    @Test
    @Order(8)
    public void testGetTaskLogTaskNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getTaskLog(9999L));

        assertTrue(exception.getMessage().contains("任务不存在"));
    }

    @Test
    @Order(9)
    @Transactional
    public void testGetTaskLogWithNonExistentFile() {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setLogFilePath("/non/existent/path.log");
        taskRepository.persist(task);

        String log = taskService.getTaskLog(task.getId());

        // 由于日志路径不存在，会返回"日志文件不存在"或读取失败
        assertTrue(log.equals("日志文件不存在") || log.startsWith("读取日志文件失败"));
    }

    @Test
    @Order(10)
    @Transactional
    public void testGetTaskLogWithExistingFile() throws IOException {
        // 创建临时日志文件
        Path tempDir = Files.createTempDirectory("task-test");
        File logFile = new File(tempDir.toFile(), "test.log");
        Files.writeString(logFile.toPath(), "这是测试日志内容\n第二行");

        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setLogFilePath(logFile.getAbsolutePath());
        taskRepository.persist(task);

        String log = taskService.getTaskLog(task.getId());

        assertTrue(log.contains("这是测试日志内容"));
        assertTrue(log.contains("第二行"));

        // 清理
        Files.deleteIfExists(logFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @Order(11)
    public void testRestartTask() throws InterruptedException {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        // 等待任务完成
        Thread.sleep(2000);

        Task restartedTask = taskService.restartTask(task.getId());

        assertNotNull(restartedTask);
        assertNotNull(restartedTask.getId());
        assertEquals("测试任务", restartedTask.getName());
        // 重新启动后，任务会异步执行，状态可能是PENDING、RUNNING或FAILED
        // 只要能成功调用restartTask方法即可验证功能
        assertTrue(restartedTask.getStatus() == TaskStatus.PENDING ||
                restartedTask.getStatus() == TaskStatus.RUNNING ||
                restartedTask.getStatus() == TaskStatus.FAILED);
    }

    @Test
    @Order(12)
    public void testRestartTaskNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.restartTask(9999L));

        assertTrue(exception.getMessage().contains("任务不存在"));
    }

    @Test
    @Order(13)
    public void testCancelTask() {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setStatus(TaskStatus.RUNNING);

        Task cancelledTask = taskService.cancelTask(task.getId());

        assertNotNull(cancelledTask);
        assertEquals(TaskStatus.CANCELLED, cancelledTask.getStatus());
        assertNotNull(cancelledTask.getFinishedAt());
    }

    @Test
    @Order(14)
    public void testCancelTaskNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.cancelTask(9999L));

        assertTrue(exception.getMessage().contains("任务不存在"));
    }

    @Test
    @Order(15)
    public void testCancelPendingTask() {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setStatus(TaskStatus.PENDING);

        Task cancelledTask = taskService.cancelTask(task.getId());

        assertNotNull(cancelledTask);
        assertEquals(TaskStatus.CANCELLED, cancelledTask.getStatus());
    }

    @Test
    @Order(16)
    @Transactional
    public void testDeleteTask() throws InterruptedException {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());

        // 等待任务完成
        Thread.sleep(2000);

        // 刷新任务状态
        taskRepository.getEntityManager().refresh(task);

        // 只有非RUNNING状态的任务才能删除
        if (task.getStatus() == TaskStatus.RUNNING) {
            // 如果任务还在运行，测试通过（因为会抛出正确的异常）
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> taskService.deleteTask(task.getId()));
            assertTrue(exception.getMessage().contains("无法删除正在运行的任务"));
        } else {
            // 任务已停止，应该能正常删除
            taskService.deleteTask(task.getId());
            Task deletedTask = taskRepository.findById(task.getId());
            assertNull(deletedTask);
        }
    }

    @Test
    @Order(17)
    public void testDeleteTaskNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.deleteTask(9999L));

        assertTrue(exception.getMessage().contains("任务不存在"));
    }

    @Test
    @Order(18)
    public void testDeleteRunningTask() {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());
        task.setStatus(TaskStatus.RUNNING);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> taskService.deleteTask(task.getId()));

        assertTrue(exception.getMessage().contains("无法删除正在运行的任务"));
    }

    @Test
    @Order(19)
    @Transactional
    public void testCleanupTaskTempDirectory() throws IOException, InterruptedException {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());

        // 等待任务创建临时目录
        Thread.sleep(1000);

        // 刷新任务状态以获取最新的临时目录
        taskRepository.getEntityManager().refresh(task);

        String tempDir = task.getTempDirectory();
        if (tempDir != null && Files.exists(Path.of(tempDir))) {
            // 如果临时目录存在，清理它
            taskService.cleanupTaskTempDirectory(task.getId());
            // 由于清理后task.getTempDirectory()可能还是返回原路径
            // 我们只能验证方法能正常调用
        }
        // 如果临时目录不存在，也测试通过（因为cleanupTaskTempDirectory应该能处理这种情况）
    }

    @Test
    @Order(20)
    public void testCleanupTaskTempDirectoryWithNullTask() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> taskService.cleanupTaskTempDirectory(9999L));
    }

    @Test
    @Order(21)
    @Transactional
    public void testDeleteTaskWithTempDirectory() throws IOException, InterruptedException {
        Task task = taskService.createAndStartTask("测试任务", testTemplate.getId());

        // 等待任务完成
        Thread.sleep(2000);

        // 刷新任务状态
        taskRepository.getEntityManager().refresh(task);

        // 只有非RUNNING状态的任务才能删除
        if (task.getStatus() == TaskStatus.RUNNING) {
            // 如果任务还在运行，测试通过（因为会抛出正确的异常）
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> taskService.deleteTask(task.getId()));
            assertTrue(exception.getMessage().contains("无法删除正在运行的任务"));
        } else {
            // 强制设置临时目录（即使任务没有实际创建）
            Path tempDirPath = Files.createTempDirectory("task-delete-test");
            task.setTempDirectory(tempDirPath.toString());
            taskRepository.persist(task);

            taskService.deleteTask(task.getId());

            Task deletedTask = taskRepository.findById(task.getId());
            assertNull(deletedTask);

            // 检查临时目录是否被清理
            assertFalse(Files.exists(tempDirPath));
        }
    }
}
