package com.example.starter.view.auth;

import com.example.starter.service.auth.UserService;
import jakarta.inject.Inject;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import org.jboss.logging.Logger;

@Route("logout")
public class LogoutView extends com.vaadin.flow.component.orderedlayout.VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOGGER = Logger.getLogger(LogoutView.class);

    @Inject
    private UserService userService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 执行登出操作
        if (userService != null && userService.isLoggedIn()) {
            String username = userService.getCurrentUser();
            userService.logout();

            // 显示登出成功消息
            Notification.show("Goodbye, " + username + "!", 2000, Notification.Position.TOP_CENTER);

            // 记录登出时间
            LOGGER.info("User logged out: " + username + " at " + java.time.LocalDateTime.now());
        }

        // 重定向到登录页面
        event.forwardTo("login");
    }
}
