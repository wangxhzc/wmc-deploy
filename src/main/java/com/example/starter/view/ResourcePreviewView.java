package com.example.starter.view;

import com.example.starter.config.AppConfig;
import com.example.starter.service.auth.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * 资源预览页面 - 登录后的默认主页
 */
@Route(value = "", layout = MainLayout.class)
public class ResourcePreviewView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    UserService userService;

    public ResourcePreviewView() {
        addClassName("resource-preview-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 页面标题
        H1 title = new H1("资源预览");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 欢迎信息
        H3 welcome = new H3("欢迎使用 " + AppConfig.APP_NAME);
        welcome.getStyle().set("margin-bottom", "30px");
        welcome.getStyle().set("color", "#34495e");

        // 功能介绍
        Paragraph intro1 = new Paragraph(
                "本系统为您提供服务器管理和资源监控功能。您可以通过左侧菜单访问各个模块。");
        intro1.getStyle().set("margin-bottom", "20px");
        intro1.getStyle().set("line-height", "1.6");

        Paragraph intro2 = new Paragraph(
                "主要功能包括：");
        intro2.getStyle().set("margin-bottom", "15px");
        intro2.getStyle().set("line-height", "1.6");

        // 功能列表
        VerticalLayout features = new VerticalLayout();
        features.getStyle().set("margin-left", "20px");
        features.getStyle().set("margin-bottom", "30px");

        Paragraph feature1 = new Paragraph("• 主机管理：添加、管理和监控服务器，实时检测SSH连接状态");
        feature1.getStyle().set("margin", "10px 0");

        Paragraph feature2 = new Paragraph("• 资源预览：查看服务器资源和系统状态（开发中）");
        feature2.getStyle().set("margin", "10px 0");

        features.add(feature1, feature2);

        // 添加所有组件
        add(title, welcome, intro1, intro2, features);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 检查用户是否已登录
        if (userService == null || !userService.isLoggedIn()) {
            if (userService != null && userService.isSessionExpired()) {
                UI.getCurrent().getPage().setLocation("/login?expired=true");
            } else {
                event.forwardTo("login");
            }
        }
    }
}
