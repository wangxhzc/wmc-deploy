package com.example.starter.view.auth;

import com.example.starter.config.AppConfig;
import com.example.starter.service.auth.AuthService;
import com.example.starter.service.auth.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.QueryParameters;

@Route("login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private AuthService authService;

    @Inject
    private UserService userService;

    private TextField username;
    private PasswordField password;
    private Paragraph errorMessage;

    public LoginView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("centered-content");
        setSizeFull();
    }

    @PostConstruct
    public void init() {
        // 设置页面标题
        H2 title = new H2("登录 " + AppConfig.APP_NAME);

        // 会话过期提示
        H3 sessionInfo = new H3();
        sessionInfo.getStyle().set("text-align", "center");

        // 用户名输入框
        username = new TextField("用户名");
        username.setPlaceholder("请输入用户名");
        username.setValue("admin"); // 默认显示admin，方便测试
        username.setAutofocus(true);
        username.setClearButtonVisible(true);
        username.setWidthFull(); // 设置全宽

        // 密码输入框
        password = new PasswordField("密码");
        password.setPlaceholder("请输入密码");
        password.setValue("admin"); // 默认显示admin，方便测试
        password.setClearButtonVisible(true);
        password.setWidthFull(); // 设置全宽

        // 登录按钮
        Button loginButton = new Button("登录", e -> {
            performLogin();
        });

        // 设置按钮样式和快捷键
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickShortcut(Key.ENTER);
        loginButton.setWidthFull(); // 设置全宽

        // 错误消息显示区域
        errorMessage = new Paragraph();
        errorMessage.getStyle().set("color", "red");
        errorMessage.setVisible(false);

        // 创建登录表单布局
        VerticalLayout loginForm = new VerticalLayout();
        loginForm.addClassName("login-form");
        loginForm.setWidth("400px");
        loginForm.setMaxWidth("90%");
        loginForm.setPadding(true);
        loginForm.setSpacing(true);

        // 添加轮廓样式
        loginForm.getStyle().set("border", "1px solid #d1d5db");
        loginForm.getStyle().set("border-radius", "8px");
        loginForm.getStyle().set("background-color", "#ffffff");
        loginForm.getStyle().set("box-shadow", "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)");

        // 添加组件到表单
        loginForm.add(title, sessionInfo, username, password, loginButton, errorMessage);

        // 居中显示表单
        add(loginForm);

        // 检查会话状态
        updateSessionInfo(sessionInfo);
    }

    private void performLogin() {
        String userValue = username.getValue().trim();
        String passValue = password.getValue().trim();

        // 验证输入
        if (userValue.isEmpty() || passValue.isEmpty()) {
            errorMessage.setText("请输入用户名和密码");
            errorMessage.setVisible(true);
            Notification.show("请输入用户名和密码", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // 尝试登录
        if (authService.authenticate(userValue, passValue)) {
            errorMessage.setVisible(false);

            // 显示成功通知
            Notification notification = Notification.show("欢迎, " + userValue + "!",
                    2000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // 记录登录尝试
            authService.logAuthenticationAttempt(userValue, true);

            // 延迟跳转，让用户看到成功消息
            UI.getCurrent().getPage().executeJs("setTimeout(function(){" +
                    "window.location.href='/';" +
                    "}, 1000)");
        } else {
            errorMessage.setText("用户名或密码错误");
            errorMessage.setVisible(true);

            // 显示失败通知
            Notification.show("登录失败", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);

            // 记录失败的登录尝试
            authService.logAuthenticationAttempt(userValue, false);

            // 清空密码框并重新聚焦
            password.setValue("");
            password.focus();
        }
    }

    private void updateSessionInfo(H3 sessionInfo) {
        if (userService != null && userService.isSessionExpired()) {
            sessionInfo.setText("您的会话已过期，请重新登录");
            sessionInfo.getStyle().set("color", "orange");
        } else {
            sessionInfo.setText("");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 检查查询参数
        QueryParameters queryParameters = event.getLocation().getQueryParameters();
        if (queryParameters != null && queryParameters.getParameters().containsKey("logout")) {
            // 从登出页面跳转过来
            Notification.show("您已成功退出登录", 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        }

        // 检查查询参数 - session expired
        if (queryParameters != null && queryParameters.getParameters().containsKey("expired")) {
            Notification.show("您的会话已过期，请重新登录。",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        }

        // 如果用户已经登录且会话有效，重定向到主页
        if (userService != null && userService.isLoggedIn()) {
            event.forwardTo("");
        }
    }
}
