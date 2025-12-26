package com.example.starter.view.admin;

import com.example.starter.service.auth.UserService;
import jakarta.inject.Inject;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("admin")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    UserService userService;

    public AdminView() {
        addClassName("centered-content");

        H1 title = new H1("Admin Dashboard");
        Paragraph description = new Paragraph("This is the admin-only section of the application.");

        add(title, description);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 检查用户是否已登录且具有管理员权限
        if (!userService.isLoggedIn() || !userService.isAdmin()) {
            event.rerouteTo("login");
        }
    }
}
