package com.example.starter.view;

import com.example.starter.config.AppConfig;
import com.example.starter.service.auth.UserService;
import com.example.starter.view.admin.HostManagementView;
import com.example.starter.view.admin.InventoryManagementView;
import com.example.starter.view.admin.ProjectManagementView;
import com.example.starter.view.admin.TaskManagementView;
import com.example.starter.view.admin.TemplateManagementView;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * 主布局 - 包含顶部导航栏和左侧菜单栏
 */
public class MainLayout extends VerticalLayout implements RouterLayout, AfterNavigationListener {

    @Inject
    UserService userService;

    private Div content;
    private HorizontalLayout mainLayout;
    private VerticalLayout sidebar;
    private HorizontalLayout breadcrumbs;

    public MainLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @PostConstruct
    public void init() {
        // 创建顶部导航栏
        HorizontalLayout header = createHeader();
        header.setWidthFull();
        header.setHeight("60px");
        header.setPadding(true);
        header.getStyle().set("background-color", "#f8f9fa");
        header.getStyle().set("border-bottom", "1px solid #dee2e6");
        header.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        // 创建左侧菜单栏
        sidebar = createSidebar();
        sidebar.setWidth("250px");
        sidebar.setPadding(false);
        sidebar.getStyle().set("background-color", "#2c3e50");
        sidebar.getStyle().set("border-right", "1px solid #dee2e6");

        // 创建面包屑
        breadcrumbs = createBreadcrumbs();

        // 创建内容区域
        content = new Div();
        content.getStyle().set("flex", "1");
        content.getStyle().set("overflow", "auto");
        content.getStyle().set("background-color", "#ffffff");
        content.addClassName("main-content");

        // 主布局
        mainLayout = new HorizontalLayout(sidebar, content);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.getStyle().set("height", "calc(100vh - 60px)");

        // 添加所有组件
        add(header, mainLayout);
        setPadding(false);
        setSpacing(false);
    }

    /**
     * 创建顶部导航栏
     */
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        // 左侧：应用标题
        H2 title = new H2(AppConfig.APP_NAME);
        title.getStyle().set("margin", "0");
        title.getStyle().set("font-size", "24px");
        title.getStyle().set("color", "#2c3e50");

        // 右侧：用户信息和登出按钮
        if (userService != null && userService.isLoggedIn()) {
            String username = userService.getCurrentUser();

            // 显示用户名
            com.vaadin.flow.component.html.Span userLabel = new com.vaadin.flow.component.html.Span(
                    "欢迎, " + username);
            userLabel.getStyle().set("margin-right", "20px");
            userLabel.getStyle().set("font-size", "14px");
            userLabel.getStyle().set("color", "#495057");

            // 登出按钮
            Button logoutButton = new Button("退出登录", VaadinIcon.SIGN_OUT.create(), e -> {
                handleLogout();
            });
            logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            logoutButton.getStyle().set("color", "#dc3545");

            header.add(title, userLabel, logoutButton);
        } else {
            header.add(title);
        }

        return header;
    }

    /**
     * 创建左侧菜单栏
     */
    private VerticalLayout createSidebar() {
        VerticalLayout menuLayout = new VerticalLayout();
        menuLayout.setPadding(false);
        menuLayout.setSpacing(false);
        menuLayout.getStyle().set("height", "100%");
        menuLayout.getStyle().set("overflow-y", "auto");
        menuLayout.getStyle().set("padding-top", "20px");
        menuLayout.getStyle().set("padding-right", "15px");
        menuLayout.getStyle().set("padding-left", "15px");

        // 菜单标题
        H2 menuTitle = new H2("菜单");
        menuTitle.getStyle().set("margin", "0 0 25px 0");
        menuTitle.getStyle().set("font-size", "18px");
        menuTitle.getStyle().set("color", "#ffffff");
        menuTitle.getStyle().set("font-weight", "normal");
        menuLayout.add(menuTitle);

        // 菜单项
        createMenuItem(menuLayout, "资源预览", VaadinIcon.GRID, "", true);
        createMenuItem(menuLayout, "主机管理", VaadinIcon.DESKTOP, "hosts", false);
        createMenuItem(menuLayout, "清单管理", VaadinIcon.SERVER, "inventories", false);
        createMenuItem(menuLayout, "项目管理", VaadinIcon.FOLDER, "projects", false);
        createMenuItem(menuLayout, "模板管理", VaadinIcon.CLIPBOARD_TEXT, "templates", false);
        createMenuItem(menuLayout, "任务管理", VaadinIcon.CLOCK, "tasks", false);

        return menuLayout;
    }

    /**
     * 创建菜单项
     */
    private void createMenuItem(VerticalLayout menuLayout, String text, VaadinIcon icon,
            String navigationTarget, boolean isDefault) {

        // 使用HorizontalLayout包装菜单项
        HorizontalLayout itemLayout = new HorizontalLayout();
        itemLayout.setAlignItems(Alignment.CENTER);
        itemLayout.setSpacing(false);
        itemLayout.getStyle().set("width", "100%");
        itemLayout.getStyle().set("box-sizing", "border-box");
        itemLayout.getStyle().set("padding", "12px 20px");
        itemLayout.getStyle().set("border-radius", "4px");
        itemLayout.getStyle().set("margin", "0 0 12px 0");
        itemLayout.getStyle().set("transition", "background-color 0.2s");
        itemLayout.getStyle().set("cursor", "pointer");

        if (isDefault) {
            itemLayout.getStyle().set("background-color", "rgba(255,255,255,0.1)");
        }

        // 添加图标
        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("18px");
        iconComponent.getStyle().set("color", "#ffffff");

        // 添加文本
        com.vaadin.flow.component.html.Span textSpan = new com.vaadin.flow.component.html.Span(text);
        textSpan.getStyle().set("font-size", "14px");
        textSpan.getStyle().set("color", "#ffffff");

        itemLayout.add(iconComponent, textSpan);

        // 添加点击事件
        if (isDefault) {
            itemLayout.addClickListener(e -> {
                UI.getCurrent().navigate(ResourcePreviewView.class);
            });
        } else {
            itemLayout.addClickListener(e -> {
                if (navigationTarget.equals("hosts")) {
                    UI.getCurrent().navigate(HostManagementView.class);
                } else if (navigationTarget.equals("inventories")) {
                    UI.getCurrent().navigate(InventoryManagementView.class);
                } else if (navigationTarget.equals("projects")) {
                    UI.getCurrent().navigate(ProjectManagementView.class);
                } else if (navigationTarget.equals("templates")) {
                    UI.getCurrent().navigate(TemplateManagementView.class);
                } else if (navigationTarget.equals("tasks")) {
                    UI.getCurrent().navigate(TaskManagementView.class);
                }
            });
        }

        // 添加悬停效果
        itemLayout.getElement().executeJs(
                "this.addEventListener('mouseenter', () => this.style.backgroundColor = 'rgba(255,255,255,0.1)');");
        itemLayout.getElement().executeJs(
                "this.addEventListener('mouseleave', () => this.style.backgroundColor = '');");

        menuLayout.add(itemLayout);
    }

    /**
     * 创建面包屑导航
     */
    private HorizontalLayout createBreadcrumbs() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.getStyle().set("padding", "15px 20px");
        layout.getStyle().set("background-color", "#f8f9fa");
        layout.getStyle().set("border-bottom", "1px solid #dee2e6");
        layout.setAlignItems(Alignment.CENTER);
        layout.setSpacing(false);

        return layout;
    }

    /**
     * 更新面包屑导航
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        breadcrumbs.removeAll();

        // 主页链接
        RouterLink homeLink = new RouterLink("首页", ResourcePreviewView.class);
        homeLink.getStyle().set("color", "#6c757d");
        homeLink.getStyle().set("text-decoration", "none");
        breadcrumbs.add(homeLink);

        // 分隔符
        Span separator = new Span("/");
        separator.getStyle().set("margin", "0 10px");
        separator.getStyle().set("color", "#adb5bd");
        breadcrumbs.add(separator);

        // 当前页面
        String location = event.getLocation().getPath();
        Span currentPage;

        if (location == null || location.isEmpty() || location.equals("/")) {
            currentPage = new Span("资源预览");
        } else if (location.contains("hosts")) {
            currentPage = new Span("主机管理");
        } else if (location.contains("inventories")) {
            currentPage = new Span("清单管理");
        } else if (location.contains("projects")) {
            currentPage = new Span("项目管理");
        } else if (location.contains("templates")) {
            currentPage = new Span("模板管理");
        } else if (location.contains("tasks")) {
            currentPage = new Span("任务管理");
        } else if (location.contains("admin")) {
            currentPage = new Span("系统管理");
        } else {
            currentPage = new Span("资源预览");
        }

        currentPage.getStyle().set("color", "#2c3e50");
        currentPage.getStyle().set("font-weight", "500");
        breadcrumbs.add(currentPage);
    }

    /**
     * 处理登出
     */
    private void handleLogout() {
        com.vaadin.flow.component.notification.Notification.show(
                "正在退出...", 1000, com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER);

        // 延迟跳转到登出页面
        UI.getCurrent().getPage().executeJs("setTimeout(function(){" +
                "window.location.href='/logout';" +
                "}, 500)");
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        this.content.removeAll();
        // 先添加面包屑
        this.content.add(breadcrumbs);
        // 再添加页面内容
        if (content != null && content.getElement() != null) {
            this.content.getElement().appendChild(content.getElement());
        }
    }
}
