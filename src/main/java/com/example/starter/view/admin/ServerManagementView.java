package com.example.starter.view.admin;

import com.example.starter.entity.Server;
import com.example.starter.repository.ServerRepository;
import com.example.starter.service.auth.UserService;
import com.example.starter.service.server.SSHConnectionService;
import com.example.starter.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * 主机管理视图 - 用于管理服务器和检测SSH连接
 */
@Route(value = "servers", layout = MainLayout.class)
public class ServerManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    ServerRepository serverRepository;

    @Inject
    SSHConnectionService sshConnectionService;

    @Inject
    UserService userService;

    private Grid<Server> serverGrid;
    private H2 title;

    public ServerManagementView() {
        addClassName("server-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 标题
        title = new H2("主机管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加服务器按钮
        Button addButton = new Button("添加服务器", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddServerDialog());

        // 工具栏
        HorizontalLayout toolbar = new HorizontalLayout(title, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "20px");

        // 创建服务器列表网格
        serverGrid = new Grid<>();
        serverGrid.setSizeFull();
        serverGrid.addColumn(Server::getName).setHeader("名称").setAutoWidth(true);
        serverGrid.addColumn(Server::getHost).setHeader("主机").setAutoWidth(true);
        serverGrid.addColumn(Server::getPort).setHeader("端口").setAutoWidth(true);
        serverGrid.addColumn(Server::getUsername).setHeader("用户名").setAutoWidth(true);
        serverGrid.addColumn(new ComponentRenderer<>(server -> createConnectionStatusBadge(server)))
                .setHeader("状态").setAutoWidth(true);
        serverGrid.addColumn(new ComponentRenderer<>(server -> {
            Span span = new Span(
                    server.getLastChecked() != null ? server.getLastChecked().toString().substring(0, 19) : "未检测");
            span.getStyle().set("font-size", "12px");
            return span;
        })).setHeader("最后检测").setAutoWidth(true);
        serverGrid.addColumn(new ComponentRenderer<>(server -> createActionButtons(server)))
                .setHeader("操作").setAutoWidth(true);

        // 添加组件到布局
        add(toolbar, serverGrid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 创建连接状态徽章
     */
    private Span createConnectionStatusBadge(Server server) {
        if (server == null) {
            Span badge = new Span("未知");
            badge.getStyle().set("padding", "4px 8px");
            badge.getStyle().set("border-radius", "4px");
            badge.getStyle().set("font-size", "12px");
            badge.getStyle().set("font-weight", "bold");
            badge.getStyle().set("background-color", "#9ca3af");
            badge.getStyle().set("color", "white");
            return badge;
        }

        Span badge = new Span();
        badge.getStyle().set("padding", "4px 8px");
        badge.getStyle().set("border-radius", "4px");
        badge.getStyle().set("font-size", "12px");
        badge.getStyle().set("font-weight", "bold");

        if (server.isConnected()) {
            badge.setText("已连接");
            badge.getStyle().set("background-color", "#10b981");
            badge.getStyle().set("color", "white");
        } else {
            badge.setText("未连接");
            badge.getStyle().set("background-color", "#ef4444");
            badge.getStyle().set("color", "white");
        }

        return badge;
    }

    /**
     * 创建操作按钮
     */
    private HorizontalLayout createActionButtons(Server server) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "编辑");
        editButton.addClickListener(e -> openEditServerDialog(server));

        Button testButton = new Button(VaadinIcon.REFRESH.create());
        testButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        testButton.getElement().setAttribute("title", "测试连接");
        testButton.addClickListener(e -> testServerConnection(server));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除");
        deleteButton.addClickListener(e -> deleteServer(server));

        HorizontalLayout actions = new HorizontalLayout(editButton, testButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 打开添加服务器对话框
     */
    private void openAddServerDialog() {
        openServerDialog(null);
    }

    /**
     * 打开编辑服务器对话框
     */
    private void openEditServerDialog(Server server) {
        if (server == null) {
            showNotification("服务器对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }
        openServerDialog(server);
    }

    /**
     * 打开服务器对话框（新增或编辑）
     */
    private void openServerDialog(Server server) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();

        TextField nameField = new TextField("名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("输入服务器名称");

        TextField hostField = new TextField("主机/IP");
        hostField.setRequired(true);
        hostField.setPlaceholder("输入IP地址或主机名");

        IntegerField portField = new IntegerField("端口");
        portField.setRequired(true);
        portField.setValue(22);
        portField.setMin(1);
        portField.setMax(65535);

        TextField usernameField = new TextField("用户名");
        usernameField.setRequired(true);
        usernameField.setPlaceholder("输入SSH用户名");

        PasswordField passwordField = new PasswordField("密码");
        passwordField.setRequired(true);
        passwordField.setPlaceholder("输入SSH密码");

        TextField descField = new TextField("描述");
        descField.setPlaceholder("可选的描述信息");

        formLayout.add(nameField, hostField, portField, usernameField, passwordField, descField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 如果是编辑模式，填充现有数据
        boolean isEditMode = (server != null);
        if (isEditMode && server != null) {
            nameField.setValue(server.getName());
            nameField.setReadOnly(true);
            hostField.setValue(server.getHost());
            portField.setValue(server.getPort());
            usernameField.setValue(server.getUsername());
            passwordField.setValue(server.getPassword());
            if (server.getDescription() != null) {
                descField.setValue(server.getDescription());
            }
        }

        // 按钮栏
        Button saveButton = new Button("保存", e -> {
            if (isEditMode && server != null) {
                // 编辑模式
                if (validateFormForEdit(nameField, hostField, portField, usernameField, passwordField, server)) {
                    updateServer(
                            server,
                            hostField.getValue(),
                            portField.getValue(),
                            usernameField.getValue(),
                            passwordField.getValue(),
                            descField.getValue());
                    dialog.close();
                }
            } else if (!isEditMode) {
                // 新增模式
                if (validateForm(nameField, hostField, portField, usernameField, passwordField)) {
                    saveServer(
                            nameField.getValue(),
                            hostField.getValue(),
                            portField.getValue(),
                            usernameField.getValue(),
                            passwordField.getValue(),
                            descField.getValue());
                    dialog.close();
                }
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("取消", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();

        nameField.focus();
    }

    /**
     * 验证表单（新增）
     */
    private boolean validateForm(TextField name, TextField host, IntegerField port,
            TextField username, PasswordField password) {
        if (name.getValue() == null || name.getValue().trim().isEmpty()) {
            showNotification("请输入服务器名称", NotificationVariant.LUMO_ERROR);
            name.focus();
            return false;
        }
        if (host.getValue() == null || host.getValue().trim().isEmpty()) {
            showNotification("请输入主机地址", NotificationVariant.LUMO_ERROR);
            host.focus();
            return false;
        }
        if (port.getValue() == null || port.getValue() < 1 || port.getValue() > 65535) {
            showNotification("请输入有效的端口号(1-65535)", NotificationVariant.LUMO_ERROR);
            port.focus();
            return false;
        }
        if (username.getValue() == null || username.getValue().trim().isEmpty()) {
            showNotification("请输入用户名", NotificationVariant.LUMO_ERROR);
            username.focus();
            return false;
        }
        if (password.getValue() == null || password.getValue().trim().isEmpty()) {
            showNotification("请输入密码", NotificationVariant.LUMO_ERROR);
            password.focus();
            return false;
        }

        // 检查名称是否已存在
        if (serverRepository.findByName(name.getValue().trim()) != null) {
            showNotification("服务器名称已存在", NotificationVariant.LUMO_ERROR);
            name.focus();
            return false;
        }

        return true;
    }

    /**
     * 验证表单（编辑）
     */
    private boolean validateFormForEdit(TextField name, TextField host, IntegerField port,
            TextField username, PasswordField password, Server existingServer) {
        if (host.getValue() == null || host.getValue().trim().isEmpty()) {
            showNotification("请输入主机地址", NotificationVariant.LUMO_ERROR);
            host.focus();
            return false;
        }
        if (port.getValue() == null || port.getValue() < 1 || port.getValue() > 65535) {
            showNotification("请输入有效的端口号(1-65535)", NotificationVariant.LUMO_ERROR);
            port.focus();
            return false;
        }
        if (username.getValue() == null || username.getValue().trim().isEmpty()) {
            showNotification("请输入用户名", NotificationVariant.LUMO_ERROR);
            username.focus();
            return false;
        }
        if (password.getValue() == null || password.getValue().trim().isEmpty()) {
            showNotification("请输入密码", NotificationVariant.LUMO_ERROR);
            password.focus();
            return false;
        }

        return true;
    }

    /**
     * 保存服务器
     */
    @Transactional
    public void saveServer(String name, String host, int port, String username, String password, String description) {
        Server server = new Server(name.trim(), host.trim(), username.trim(), password);
        server.setPort(port);
        if (description != null && !description.trim().isEmpty()) {
            server.setDescription(description.trim());
        }

        serverRepository.persist(server);
        showNotification("服务器添加成功", NotificationVariant.LUMO_SUCCESS);
        refreshGrid();

        // 自动测试连接
        testServerConnection(server);
    }

    /**
     * 更新服务器
     */
    @Transactional
    public void updateServer(Server server, String host, int port, String username, String password,
            String description) {
        if (server == null) {
            showNotification("服务器对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        server.setHost(host.trim());
        server.setPort(port);
        server.setUsername(username.trim());
        server.setPassword(password);
        if (description != null && !description.trim().isEmpty()) {
            server.setDescription(description.trim());
        } else {
            server.setDescription(null);
        }

        // 重置连接状态
        server.setConnected(false);
        server.setLastChecked(null);

        // 使用merge而不是persist，因为这是更新已存在的实体
        jakarta.persistence.EntityManager em = serverRepository.getEntityManager();
        em.merge(server);

        showNotification("服务器更新成功", NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
    }

    /**
     * 测试服务器连接
     */
    private void testServerConnection(Server server) {
        if (server == null) {
            showNotification("服务器对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        // 显示加载提示
        Notification.show("正在测试连接...", 1000, Notification.Position.TOP_CENTER);

        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(server);

        // 显示结果
        if (result.isSuccess()) {
            showNotification(result.getMessage(), NotificationVariant.LUMO_SUCCESS);
        } else {
            showNotification(result.getMessage(), NotificationVariant.LUMO_ERROR);
        }

        // 刷新网格
        refreshGrid();
    }

    /**
     * 删除服务器
     */
    private void deleteServer(Server server) {
        if (server == null) {
            showNotification("服务器对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span message = new Span("确定要删除服务器 '" + server.getName() + "' 吗？");
        message.getStyle().set("font-size", "14px");

        Button confirmButton = new Button("确定", e -> {
            deleteServerFromDatabase(server);
            showNotification("服务器删除成功", NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            confirmDialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("取消", e -> confirmDialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        layout.add(message, buttonLayout);
        confirmDialog.add(layout);
        confirmDialog.open();
    }

    /**
     * 从数据库删除服务器
     */
    @Transactional
    public void deleteServerFromDatabase(Server server) {
        if (server == null) {
            return;
        }
        serverRepository.delete(server);
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        List<Server> servers = serverRepository.findAllActive();
        serverGrid.setItems(servers);
    }

    /**
     * 显示通知
     */
    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
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
