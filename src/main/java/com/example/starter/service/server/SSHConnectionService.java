package com.example.starter.service.server;

import com.example.starter.entity.Server;
import com.example.starter.repository.ServerRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

@ApplicationScoped
public class SSHConnectionService {

    private static final Logger LOG = Logger.getLogger(SSHConnectionService.class);
    private static final int CONNECTION_TIMEOUT = 5000; // 5秒超时

    @Inject
    ServerRepository serverRepository;

    /**
     * 测试SSH连接
     * 
     * @param server 服务器对象
     * @return 连接结果对象
     */
    public ConnectionResult testConnection(Server server) {
        ConnectionResult result = new ConnectionResult();
        Session session = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());
            session.setPassword(server.getPassword());

            // 避免主机密钥检查
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("UserKnownHostsFile", "/dev/null");

            LOG.infof("正在尝试连接服务器: %s (%s:%d)", server.getName(), server.getHost(), server.getPort());

            // 尝试连接
            session.connect(CONNECTION_TIMEOUT);

            // 连接成功
            result.setSuccess(true);
            result.setMessage("连接成功");
            LOG.infof("服务器 %s 连接成功", server.getName());

        } catch (JSchException e) {
            // 连接失败
            result.setSuccess(false);

            // 解析错误信息
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("Auth fail")) {
                    result.setMessage("认证失败: 用户名或密码错误");
                } else if (errorMessage.contains("UnknownHostException")) {
                    result.setMessage("主机不存在: " + server.getHost());
                } else if (errorMessage.contains("Connection refused")) {
                    result.setMessage("连接被拒绝: 端口 " + server.getPort() + " 不可用");
                } else if (errorMessage.contains("timeout")) {
                    result.setMessage("连接超时: 无法连接到服务器");
                } else {
                    result.setMessage("连接失败: " + errorMessage);
                }
            } else {
                result.setMessage("连接失败: 未知错误");
            }

            LOG.errorf("服务器 %s 连接失败: %s", server.getName(), result.getMessage());

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        // 更新服务器状态
        updateServerStatus(server, result.isSuccess());

        return result;
    }

    /**
     * 更新服务器连接状态
     */
    @jakarta.transaction.Transactional
    public void updateServerStatus(Server server, boolean connected) {
        server.setConnected(connected);
        server.setLastChecked(LocalDateTime.now());

        // 使用merge而不是persist，因为这是更新已存在的实体
        jakarta.persistence.EntityManager em = serverRepository.getEntityManager();
        em.merge(server);
    }

    /**
     * 连接结果类
     */
    public static class ConnectionResult {
        private boolean success;
        private String message;

        public ConnectionResult() {
        }

        public ConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
