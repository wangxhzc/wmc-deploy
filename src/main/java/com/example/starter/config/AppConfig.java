package com.example.starter.config;

import com.example.starter.entity.User;
import com.example.starter.service.auth.UserService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Inject
    UserService userService;

    @Transactional
    public void init(@Observes StartupEvent event) {
        LOGGER.info("Initializing application configuration...");

        // 检查是否存在管理员账户，如果不存在则创建
        if (!User.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.username = "admin";
            adminUser.password = "admin"; // 实际项目中应该使用加密密码
            adminUser.role = "ADMIN";

            adminUser.persist();
            LOGGER.info("Created default admin user: admin/admin");
        } else {
            LOGGER.info("Admin user already exists, skipping creation.");
        }
    }

    // 应用名称
    public static final String APP_NAME = "WMC-DEPLOY";

    // 应用版本
    public static final String APP_VERSION = "1.0.0";

    // 默认登录页面路径
    public static final String LOGIN_PATH = "login";

    // 默认主页路径
    public static final String HOME_PATH = "";

    // 管理员角色标识
    public static final String ROLE_ADMIN = "ADMIN";

    // 普通用户角色标识
    public static final String ROLE_USER = "USER";
}
