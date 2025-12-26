package com.example.starter.service.auth;

import com.example.starter.config.AppConfig;
import jakarta.enterprise.context.SessionScoped;
import org.jboss.logging.Logger;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Duration;

@SessionScoped
public class UserService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserService.class);

    private String currentUser;
    private String currentUserRole;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    public void setCurrentUser(String user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
    }

    public String getCurrentUser() {
        updateLastActivity();
        return currentUser;
    }

    public void setUserRole(String role) {
        this.currentUserRole = role;
    }

    public String getCurrentUserRole() {
        updateLastActivity();
        return currentUserRole;
    }

    public boolean isLoggedIn() {
        updateLastActivity();
        return currentUser != null && !currentUser.isEmpty() && !isSessionExpired();
    }

    public boolean isAdmin() {
        updateLastActivity();
        return isLoggedIn() && AppConfig.ROLE_ADMIN.equals(currentUserRole);
    }

    public void logout() {
        // 记录登出时间
        if (currentUser != null) {
            Duration sessionDuration = Duration.between(loginTime, LocalDateTime.now());
            LOGGER.info("User " + currentUser + " logged out. Session duration: " +
                    sessionDuration.toMinutes() + " minutes");
        }

        this.currentUser = null;
        this.currentUserRole = null;
        this.loginTime = null;
        this.lastActivityTime = null;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public boolean isSessionExpired() {
        if (lastActivityTime == null) {
            return true;
        }

        Duration inactiveDuration = Duration.between(lastActivityTime, LocalDateTime.now());
        return inactiveDuration.toMinutes() >= SESSION_TIMEOUT_MINUTES;
    }

    public long getSessionTimeoutMinutes() {
        return SESSION_TIMEOUT_MINUTES;
    }

    public long getRemainingSessionTime() {
        if (lastActivityTime == null) {
            return 0;
        }

        Duration elapsed = Duration.between(lastActivityTime, LocalDateTime.now());
        long remaining = SESSION_TIMEOUT_MINUTES - elapsed.toMinutes();
        return Math.max(0, remaining);
    }

    private void updateLastActivity() {
        if (currentUser != null && !currentUser.isEmpty() && lastActivityTime != null) {
            this.lastActivityTime = LocalDateTime.now();
        }
    }

    public String getSessionInfo() {
        if (!isLoggedIn()) {
            return "Not logged in";
        }

        Duration totalDuration = Duration.between(loginTime, LocalDateTime.now());
        return String.format("User: %s, Role: %s, Login time: %s, Session duration: %d minutes, " +
                "Remaining: %d minutes",
                currentUser,
                currentUserRole,
                loginTime,
                totalDuration.toMinutes(),
                getRemainingSessionTime());
    }
}
