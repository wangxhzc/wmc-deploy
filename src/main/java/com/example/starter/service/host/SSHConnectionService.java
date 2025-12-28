package com.example.starter.service.host;

import com.example.starter.entity.InventoryHost;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Properties;
import java.util.concurrent.*;

/**
 * SSH连接服务 - 用于测试主机SSH连接
 */
@ApplicationScoped
public class SSHConnectionService {

    /**
     * 连接超时时间（毫秒）- 5秒
     */
    private static final int CONNECTION_TIMEOUT = 5000;

    /**
     * 线程池
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 连接结果
     */
    public static class ConnectionResult {
        private final boolean success;
        private final String message;

        public ConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 测试SSH连接（带超时控制）
     *
     * @param host 主机对象
     * @return 连接结果
     */
    public ConnectionResult testConnection(InventoryHost host) {
        if (host == null) {
            return new ConnectionResult(false, "主机对象不能为空");
        }

        if (host.getHost() == null || host.getHost().trim().isEmpty()) {
            return new ConnectionResult(false, "主机地址不能为空");
        }

        if (host.getUsername() == null || host.getUsername().trim().isEmpty()) {
            return new ConnectionResult(false, "用户名不能为空");
        }

        if (host.getPassword() == null || host.getPassword().trim().isEmpty()) {
            return new ConnectionResult(false, "密码不能为空");
        }

        // 使用 Future 和 Executor 来实现超时控制
        Future<ConnectionResult> future = executorService.submit(() -> doTestConnection(host));

        try {
            // 等待连接测试完成，最多等待 CONNECTION_TIMEOUT + 2 秒
            return future.get(CONNECTION_TIMEOUT + 2000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return new ConnectionResult(false, "连接超时（超过" + (CONNECTION_TIMEOUT / 1000) + "秒）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConnectionResult(false, "连接被中断");
        } catch (ExecutionException e) {
            return new ConnectionResult(false,
                    "连接失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * 实际执行SSH连接测试
     *
     * @param host 主机对象
     * @return 连接结果
     */
    private ConnectionResult doTestConnection(InventoryHost host) {
        JSch jsch = new JSch();
        Session session = null;

        try {
            int port = host.getPort() != null ? host.getPort() : 22;

            // 创建SSH会话
            session = jsch.getSession(host.getUsername().trim(), host.getHost().trim(), port);
            session.setPassword(host.getPassword().trim());

            // 设置超时配置 - 更严格的超时控制
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password,publickey");
            config.put("ConnectTimeout", String.valueOf(CONNECTION_TIMEOUT));
            config.put("ServerAliveInterval", "1000");
            config.put("ServerAliveCountMax", "3");
            config.put("MaxAuthTries", "1");
            session.setConfig(config);

            // 尝试连接，设置超时
            session.connect(CONNECTION_TIMEOUT);

            // 如果没有抛出异常，说明连接成功
            session.disconnect();
            return new ConnectionResult(true, "SSH连接成功");

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null) {
                errorMessage = e.getClass().getSimpleName();
            }
            return new ConnectionResult(false, "SSH连接失败: " + errorMessage);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 测试SSH连接（使用单独的参数）
     *
     * @param hostAddr 主机地址
     * @param port     端口
     * @param username 用户名
     * @param password 密码
     * @return 连接结果
     */
    public ConnectionResult testConnection(String hostAddr, int port, String username, String password) {
        InventoryHost testHost = new InventoryHost("test", hostAddr, port, username, password);
        return testConnection(testHost);
    }
}
