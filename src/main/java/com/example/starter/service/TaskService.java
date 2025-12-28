package com.example.starter.service;

import com.example.starter.entity.*;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.TaskRepository;
import com.example.starter.repository.TemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务服务 - 处理任务的创建和后台执行
 */
@ApplicationScoped
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Inject
    TaskRepository taskRepository;

    @Inject
    TemplateRepository templateRepository;

    @ConfigProperty(name = "ansible.path", defaultValue = "/usr/bin/ansible-playbook")
    String ansiblePath;

    @ConfigProperty(name = "python.path", defaultValue = "/usr/bin/python3")
    String pythonPath;

    @ConfigProperty(name = "task.temp.directory", defaultValue = "/tmp/wmc-deploy-tasks")
    String taskTempDirectory;

    // 线程池用于并发执行任务
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 存储正在运行的任务进程，用于取消任务
    private final Map<Long, Process> runningProcesses = new ConcurrentHashMap<>();

    /**
     * 创建并启动任务
     */
    @Transactional
    public Task createAndStartTask(String taskName, Long templateId) {
        // 查找模板
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new ResourceNotFoundException("模板不存在，ID: " + templateId);
        }

        // 创建任务
        Task task = new Task(taskName, template);
        taskRepository.persist(task);
        taskRepository.flush(); // 确保获取到ID

        // 初始化模板的关联数据
        templateRepository.getEntityManager().refresh(template);

        // 异步执行任务
        executorService.submit(() -> executeTask(task));

        return task;
    }

    /**
     * 重新启动任务
     */
    @Transactional
    public Task restartTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在，ID: " + taskId);
        }

        // 重置任务状态
        task.setStatus(Task.TaskStatus.PENDING);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setErrorMessage(null);
        taskRepository.persist(task);

        // 异步执行任务
        executorService.submit(() -> executeTask(task));

        return task;
    }

    /**
     * 取消任务
     */
    @Transactional
    public Task cancelTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在，ID: " + taskId);
        }

        // 如果任务正在运行，终止进程
        if (task.getStatus() == Task.TaskStatus.RUNNING) {
            Process process = runningProcesses.get(taskId);
            if (process != null) {
                process.destroy();
                runningProcesses.remove(taskId);
            }
        }

        // 更新任务状态
        task.setStatus(Task.TaskStatus.CANCELLED);
        task.setFinishedAt(LocalDateTime.now());
        taskRepository.persist(task);

        return task;
    }

    /**
     * 获取所有任务（按创建时间倒序）
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * 根据ID获取任务
     */
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * 读取任务日志
     */
    public String getTaskLog(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在，ID: " + taskId);
        }

        if (task.getLogFilePath() == null) {
            return "暂无日志文件";
        }

        try {
            Path logPath = Paths.get(task.getLogFilePath());
            if (!Files.exists(logPath)) {
                return "日志文件不存在";
            }

            return Files.readString(logPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取日志文件失败: " + task.getLogFilePath(), e);
            return "读取日志文件失败: " + e.getMessage();
        }
    }

    /**
     * 执行任务（在独立线程中运行）
     */
    private void executeTask(Task task) {
        ProcessBuilder processBuilder = null;
        Process process = null;
        File logFile = null;

        try {
            // 更新任务状态为运行中
            task.setStatus(Task.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.persist(task);

            // 创建临时目录
            String tempDirPath = createTaskTempDirectory(task.getId());
            task.setTempDirectory(tempDirPath);
            taskRepository.persist(task);

            // 生成playbook文件
            String playbookPath = generatePlaybookFile(task.getTemplate().getProject(), tempDirPath);

            // 生成inventory文件（YAML格式）
            String inventoryPath = generateInventoryFile(task.getTemplate().getInventory(), tempDirPath);

            // 创建日志文件
            logFile = new File(tempDirPath, "execution.log");
            task.setLogFilePath(logFile.getAbsolutePath());
            taskRepository.persist(task);

            // 构建ansible-playbook命令
            processBuilder = new ProcessBuilder(
                    ansiblePath,
                    "-i", inventoryPath,
                    playbookPath,
                    "-v");

            processBuilder.directory(new File(tempDirPath));
            processBuilder.redirectErrorStream(true);

            // 启动进程
            process = processBuilder.start();
            runningProcesses.put(task.getId(), process);

            // 读取进程输出并写入日志文件
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            // 更新任务状态
            task.setFinishedAt(LocalDateTime.now());
            if (exitCode == 0) {
                task.setStatus(Task.TaskStatus.SUCCESS);
            } else {
                task.setStatus(Task.TaskStatus.FAILED);
                task.setErrorMessage("执行失败，退出码: " + exitCode);
            }
            taskRepository.persist(task);

            logger.info("任务执行完成: {} (ID: {}), 状态: {}, 退出码: {}",
                    task.getName(), task.getId(), task.getStatus(), exitCode);

        } catch (Exception e) {
            logger.error("任务执行出错: " + task.getName() + " (ID: " + task.getId() + ")", e);

            // 更新任务状态为失败
            task.setStatus(Task.TaskStatus.FAILED);
            task.setFinishedAt(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            taskRepository.persist(task);

        } finally {
            // 清理
            if (process != null) {
                runningProcesses.remove(task.getId());
                process.destroy();
            }
        }
    }

    /**
     * 创建任务临时目录
     */
    private String createTaskTempDirectory(Long taskId) throws IOException {
        Path tempDir = Paths.get(taskTempDirectory, "task-" + taskId + "-" + System.currentTimeMillis());
        Files.createDirectories(tempDir);
        logger.info("创建临时目录: {}", tempDir);
        return tempDir.toAbsolutePath().toString();
    }

    /**
     * 生成playbook文件
     */
    private String generatePlaybookFile(Project project, String tempDir) throws IOException {
        // 解码Base64内容
        byte[] decodedBytes = Base64.getDecoder().decode(project.getYamlContent());
        String yamlContent = new String(decodedBytes, StandardCharsets.UTF_8);

        // 写入文件
        Path playbookPath = Paths.get(tempDir, "playbook.yml");
        Files.writeString(playbookPath, yamlContent, StandardCharsets.UTF_8);

        logger.info("生成playbook文件: {}", playbookPath);
        return playbookPath.toAbsolutePath().toString();
    }

    /**
     * 生成inventory文件（YAML格式）
     */
    private String generateInventoryFile(Inventory inventory, String tempDir) throws IOException {
        StringBuilder yaml = new StringBuilder();
        yaml.append("---\n");

        // 添加清单级别的变量
        if (!inventory.getVariables().isEmpty()) {
            yaml.append("all:\n");
            yaml.append("  vars:\n");
            for (InventoryVariable variable : inventory.getVariables()) {
                String value = formatVariableValue(variable.getVariableValue());
                yaml.append("    ").append(variable.getVariableName()).append(": ").append(value).append("\n");
            }
            yaml.append("\n");
        }

        // 添加组
        for (InventoryGroup group : inventory.getGroups()) {
            yaml.append(group.getName()).append(":\n");

            // 组的变量
            if (!group.getVariables().isEmpty()) {
                yaml.append("  vars:\n");
                for (InventoryGroupVariable variable : group.getVariables()) {
                    String value = formatVariableValue(variable.getVariableValue());
                    yaml.append("    ").append(variable.getVariableName()).append(": ").append(value).append("\n");
                }
            }

            // 组的主机
            if (!group.getGroupHosts().isEmpty()) {
                yaml.append("  hosts:\n");
                for (InventoryGroupHost groupHost : group.getGroupHosts()) {
                    InventoryHost host = groupHost.getHost();
                    yaml.append("    ").append(host.getName()).append(":\n");

                    // 主机变量
                    if (!host.getVariables().isEmpty()) {
                        yaml.append("      ansible_host: ").append(host.getHost()).append("\n");
                        yaml.append("      vars:\n");
                        for (InventoryHostVariable variable : host.getVariables()) {
                            String value = formatVariableValue(variable.getVariableValue());
                            yaml.append("        ").append(variable.getVariableName()).append(": ").append(value)
                                    .append("\n");
                        }
                    } else {
                        yaml.append("      ansible_host: ").append(host.getHost()).append("\n");
                    }
                }
            }
            yaml.append("\n");
        }

        // 添加不在任何组中的主机
        List<InventoryHost> ungroupedHosts = inventory.getHosts();
        if (!ungroupedHosts.isEmpty()) {
            boolean hasUngrouped = false;
            for (InventoryHost host : ungroupedHosts) {
                boolean inAnyGroup = inventory.getGroups().stream()
                        .anyMatch(g -> g.getGroupHosts().stream()
                                .anyMatch(gh -> gh.getHost().getId().equals(host.getId())));
                if (!inAnyGroup) {
                    hasUngrouped = true;
                    break;
                }
            }

            if (hasUngrouped) {
                yaml.append("ungrouped:\n");
                yaml.append("  hosts:\n");
                for (InventoryHost host : ungroupedHosts) {
                    boolean inAnyGroup = inventory.getGroups().stream()
                            .anyMatch(g -> g.getGroupHosts().stream()
                                    .anyMatch(gh -> gh.getHost().getId().equals(host.getId())));
                    if (!inAnyGroup) {
                        yaml.append("    ").append(host.getName()).append(":\n");
                        yaml.append("      ansible_host: ").append(host.getHost()).append("\n");
                        if (!host.getVariables().isEmpty()) {
                            yaml.append("      vars:\n");
                            for (InventoryHostVariable variable : host.getVariables()) {
                                String value = formatVariableValue(variable.getVariableValue());
                                yaml.append("        ").append(variable.getVariableName()).append(": ")
                                        .append(value).append("\n");
                            }
                        }
                    }
                }
            }
        }

        // 写入文件
        Path inventoryPath = Paths.get(tempDir, "inventory.yml");
        Files.writeString(inventoryPath, yaml.toString(), StandardCharsets.UTF_8);

        logger.info("生成inventory文件: {}", inventoryPath);
        return inventoryPath.toAbsolutePath().toString();
    }

    /**
     * 格式化变量值（根据类型添加引号）
     */
    private String formatVariableValue(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        // 尝试判断是否为数字或布尔值
        if (value.equals("true") || value.equals("false") ||
                value.matches("\\d+") || value.matches("\\d+\\.\\d+")) {
            return value;
        }
        // 字符串需要加引号
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value;
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            return value;
        }
        return "\"" + value + "\"";
    }

    /**
     * 删除任务（包括数据库记录和临时目录）
     */
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("任务不存在，ID: " + taskId);
        }

        // 如果任务正在运行，不允许删除
        if (task.getStatus() == Task.TaskStatus.RUNNING) {
            throw new IllegalStateException("无法删除正在运行的任务");
        }

        // 清理临时目录
        if (task.getTempDirectory() != null) {
            try {
                Path tempDir = Paths.get(task.getTempDirectory());
                if (Files.exists(tempDir)) {
                    deleteDirectory(tempDir);
                    logger.info("清理临时目录: {}", tempDir);
                }
            } catch (IOException e) {
                logger.error("清理临时目录失败: " + task.getTempDirectory(), e);
            }
        }

        // 删除数据库记录
        taskRepository.delete(task);
        logger.info("删除任务: {} (ID: {})", task.getName(), taskId);
    }

    /**
     * 清理临时目录（可选）
     */
    public void cleanupTaskTempDirectory(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null || task.getTempDirectory() == null) {
            return;
        }

        try {
            Path tempDir = Paths.get(task.getTempDirectory());
            if (Files.exists(tempDir)) {
                deleteDirectory(tempDir);
                logger.info("清理临时目录: {}", tempDir);
            }
        } catch (IOException e) {
            logger.error("清理临时目录失败: " + task.getTempDirectory(), e);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b)) // 反向排序，先删除文件
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.error("删除文件失败: " + path, e);
                    }
                });
    }
}
