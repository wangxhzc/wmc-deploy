package com.example.starter.service.auth;

import com.example.starter.config.AppConfig;
import com.example.starter.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.time.LocalDateTime;

@ApplicationScoped
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class);

    @Inject
    UserService userService;

    public boolean authenticate(String username, String password) {
        // 验证输入
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            LOGGER.debug("Login attempt failed: Empty username or password");
            return false;
        }

        // 从数据库查找用户
        User user = User.findByUsername(username);

        if (user == null) {
            LOGGER.debug("Login attempt failed: User not found - " + username);
            return false;
        }

        // 验证密码（实际项目中应该使用加密密码，如 BCrypt）
        if (user.password.equals(password)) {
            // 登录成功
            userService.setCurrentUser(username);
            userService.setUserRole(user.role);

            // 记录登录成功
            LOGGER.info("User logged in: " + username + " at " + LocalDateTime.now());
            LOGGER.debug(userService.getSessionInfo());

            return true;
        } else {
            // 记录登录失败
            LOGGER.info("Login attempt failed: Invalid password for user - " + username + " at " + LocalDateTime.now());
            return false;
        }
    }

    public boolean isAdmin(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        User user = User.findByUsername(username);
        return user != null &&
                user.role != null &&
                AppConfig.ROLE_ADMIN.equals(user.role) &&
                user.password.equals(password);
    }

    public void logAuthenticationAttempt(String username, boolean success) {
        String timestamp = LocalDateTime.now().toString();
        String status = success ? "SUCCESS" : "FAILURE";
        LOGGER.info(String.format("[%s] Authentication %s for user: %s", timestamp, status, username));
    }
}
