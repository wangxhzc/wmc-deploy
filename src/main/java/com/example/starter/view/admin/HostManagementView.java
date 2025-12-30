package com.example.starter.view.admin;

import com.example.starter.entity.InventoryHost;
import com.example.starter.repository.InventoryHostRepository;
import com.example.starter.service.auth.UserService;
import com.example.starter.service.host.SSHConnectionService;
import com.example.starter.util.UIBroadcaster;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 主机管理视图 - 用于管理所有主机
 */
@Route(value = "hosts", layout = MainLayout.class)
public class HostManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    UserService userService;

    @Inject
    InventoryHostRepository hostRepository;

    @Inject
    SSHConnectionService sshConnectionService;

    private Grid<InventoryHost> hostGrid;
    private H2 title;

    public HostManagementView() {
        addClassName("host-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 初始化 WebSocket 连接
        initWebSocketConnection();

        // 标题
        title = new H2("主机管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加主机按钮
        Button addButton = new Button("添加主机", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddHostDialog());

        // 工具栏
        HorizontalLayout toolbar = new HorizontalLayout(title, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "20px");

        // 创建主机列表网格
        hostGrid = new Grid<>();
        hostGrid.setSizeFull();
        hostGrid.addComponentColumn(host -> {
            Span nameSpan = new Span(host.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("主机名称").setAutoWidth(true);
        hostGrid.addColumn(InventoryHost::getHost).setHeader("主机地址").setAutoWidth(true);
        hostGrid.addColumn(host -> host.getPort() != null ? host.getPort() : 22).setHeader("端口").setAutoWidth(true);
        hostGrid.addColumn(InventoryHost::getUsername).setHeader("用户名").setAutoWidth(true);
        hostGrid.addComponentColumn(host -> {
            Span statusBadge = new Span();
            statusBadge.getStyle().set("padding", "4px 8px");
            statusBadge.getStyle().set("border-radius", "4px");
            statusBadge.getStyle().set("font-size", "12px");
            statusBadge.getStyle().set("font-weight", "bold");

            if (Boolean.TRUE.equals(host.getConnected())) {
                statusBadge.setText("已连接");
                statusBadge.getStyle().set("background-color", "#10b981");
                statusBadge.getStyle().set("color", "white");
            } else {
                statusBadge.setText("未连接");
                statusBadge.getStyle().set("background-color", "#ef4444");
                statusBadge.getStyle().set("color", "white");
            }

            return statusBadge;
        }).setHeader("状态").setAutoWidth(true);
        hostGrid.addComponentColumn(host -> {
            Span checkedTime = new Span(
                    host.getLastChecked() != null ? host.getLastChecked().toString().substring(0, 16) : "未检测");
            checkedTime.getStyle().set("font-size", "12px");
            checkedTime.getStyle().set("color", "#6c757d");
            return checkedTime;
        }).setHeader("最后检测").setAutoWidth(true);
        hostGrid.addComponentColumn(this::createActionButtons).setHeader("操作").setAutoWidth(true);

        // 添加组件到布局
        add(toolbar, hostGrid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 初始化 WebSocket 连接
     */
    private void initWebSocketConnection() {
        String jsCode = "if (!window.hostsWebSocket) {" +
                "  var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';" +
                "  var host = window.location.host;" +
                "  window.hostsWebSocket = new WebSocket(protocol + '//' + host + '/ws/broadcast/hosts');" +
                "  window.hostsWebSocket.onmessage = function(event) {" +
                "    var data = JSON.parse(event.data);" +
                "    if (data.type === 'refresh') {" +
                "      setTimeout(function() { window.location.reload(); }, 100);" +
                "    }" +
                "  };" +
                "  window.hostsWebSocket.onclose = function() {" +
                "    setTimeout(function() { window.hostsWebSocket = null; }, 3000);" +
                "  };" +
                "}";

        UI.getCurrent().getElement().executeJs(jsCode);

        // 添加页面关闭时的清理
        UI.getCurrent().addDetachListener(e -> {
            UI.getCurrent().getElement().executeJs("if (window.hostsWebSocket) { window.hostsWebSocket.close(); }");
        });
    }

    /**
     * 创建操作按钮
     */
    private HorizontalLayout createActionButtons(InventoryHost host) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "编辑");
        editButton.addClickListener(e -> openEditHostDialog(host));

        Button testButton = new Button(VaadinIcon.REFRESH.create());
        testButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        testButton.getElement().setAttribute("title", "测试连接");
        testButton.addClickListener(e -> testHostConnection(host));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除");
        deleteButton.addClickListener(e -> deleteHost(host));

        HorizontalLayout actions = new HorizontalLayout(editButton, testButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 测试主机连接（异步执行）
     */
    private void testHostConnection(InventoryHost host) {
        if (host == null) {
            showNotification("主机对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        // 显示加载提示（2秒后自动消失）
        Notification.show("正在测试连接...", 2000, Notification.Position.TOP_CENTER);

        // 异步执行连接测试
        new Thread(() -> {
            SSHConnectionService.ConnectionResult result = null;

            try {
                // 在后台线程执行连接测试
                result = sshConnectionService.testConnection(host);
            } catch (Exception e) {
                // 静默处理异常
                System.err.println("Connection test failed: " + e.getMessage());
            }

            // 在后台线程中先更新数据库（有事务上下文）
            final boolean success = (result != null && result.isSuccess());

            try {
                hostRepository.updateConnectionStatus(host.getId(), success);
                // 广播主机列表更新
                UIBroadcaster.broadcastRefresh("hosts");
            } catch (Exception e) {
                // 静默处理事务异常，不影响用户体验
                System.err.println("Failed to update connection status: " + e.getMessage());
            }

            System.out.println("Connection test completed. Success: " + success);
        }).start();
    }

    /**
     * 打开添加主机对话框
     */
    private void openAddHostDialog() {
        openHostDialog(null);
    }

    /**
     * 打开编辑主机对话框
     */
    private void openEditHostDialog(InventoryHost host) {
        if (host == null) {
            showNotification("主机对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }
        openHostDialog(host);
    }

    /**
     * 打开主机对话框（新增或编辑）
     */
    private void openHostDialog(InventoryHost host) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();

        TextField nameField = new TextField("主机名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("例如：web-01");

        TextField hostField = new TextField("主机地址");
        hostField.setRequired(true);
        hostField.setPlaceholder("例如：192.168.1.10");

        IntegerField portField = new IntegerField("端口");
        portField.setRequired(true);
        portField.setValue(22);
        portField.setMin(1);
        portField.setMax(65535);

        TextField usernameField = new TextField("用户名");
        usernameField.setRequired(true);
        usernameField.setPlaceholder("例如：root");

        PasswordField passwordField = new PasswordField("密码");
        passwordField.setRequired(true);
        passwordField.setPlaceholder("输入SSH密码");

        formLayout.add(nameField, hostField, portField, usernameField, passwordField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 如果是编辑模式，填充现有数据
        boolean isEditMode = (host != null);
        if (isEditMode && host != null) {
            nameField.setValue(host.getName());
            nameField.setReadOnly(true);
            hostField.setValue(host.getHost());
            portField.setValue(host.getPort());
            usernameField.setValue(host.getUsername());
            passwordField.setValue(host.getPassword());
        }

        // 使用数组包装 saveButton 以便在 lambda 中使用
        final Button[] saveButtonWrapper = new Button[1];

        saveButtonWrapper[0] = new Button("保存", event -> {
            if (isEditMode && host != null) {
                // 编辑模式
                if (validateFormForEdit(hostField, portField, usernameField, passwordField, host)) {
                    updateHost(
                            host,
                            hostField.getValue(),
                            portField.getValue(),
                            usernameField.getValue(),
                            passwordField.getValue());
                    dialog.close();
                }
            } else if (!isEditMode) {
                // 新增模式 - 先保存，再后台测试连接
                if (validateForm(nameField, hostField, portField, usernameField, passwordField)) {
                    try {
                        // 先保存主机（默认连接状态为 false）
                        saveHost(
                                nameField.getValue(),
                                hostField.getValue(),
                                portField.getValue(),
                                usernameField.getValue(),
                                passwordField.getValue(),
                                false);

                        // 关闭对话框
                        dialog.close();

                        // 显示加载提示（2秒后自动消失）
                        Notification.show("正在测试连接...", 2000, Notification.Position.TOP_CENTER);

                        // 在后台异步测试连接
                        new Thread(() -> {
                            SSHConnectionService.ConnectionResult result = null;

                            try {
                                // 测试SSH连接
                                result = sshConnectionService.testConnection(
                                        hostField.getValue(),
                                        portField.getValue(),
                                        usernameField.getValue(),
                                        passwordField.getValue());
                            } catch (Exception ex) {
                                // 静默处理异常
                                System.err.println("Connection test failed: " + ex.getMessage());
                            }

                            // 在后台线程中先更新数据库（有事务上下文）
                            final boolean success = (result != null && result.isSuccess());

                            try {
                                hostRepository.updateConnectionStatusByName(
                                        nameField.getValue().trim(),
                                        success);
                                // 广播主机列表更新
                                UIBroadcaster.broadcastRefresh("hosts");
                            } catch (Exception ex) {
                                // 静默处理事务异常，不影响用户体验
                                System.err.println("Failed to update connection status: " + ex.getMessage());
                            }

                            System.out.println("Connection test completed. Success: " + success);
                        }).start();
                    } catch (Exception ex) {
                        showNotification("保存主机失败: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    }
                }
            }
        });

        saveButtonWrapper[0].addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButtonWrapper[0].setAutofocus(false);
        saveButtonWrapper[0].getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButtonWrapper[0], cancelButton);
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
            showNotification("请输入主机名称", NotificationVariant.LUMO_ERROR);
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
        if (hostRepository.existsByName(name.getValue().trim())) {
            showNotification("主机名称已存在", NotificationVariant.LUMO_ERROR);
            name.focus();
            return false;
        }

        return true;
    }

    /**
     * 验证表单（编辑）
     */
    private boolean validateFormForEdit(TextField host, IntegerField port,
            TextField username, PasswordField password, InventoryHost existingHost) {
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
     * 保存主机
     */
    @Transactional
    public void saveHost(String name, String hostAddr, int port, String username, String password, boolean connected) {
        InventoryHost newHost = new InventoryHost(
                name.trim(),
                hostAddr.trim(),
                port,
                username.trim(),
                password.trim());

        newHost.setCreatedAt(LocalDateTime.now());
        newHost.setUpdatedAt(LocalDateTime.now());
        newHost.setConnected(connected);
        newHost.setLastChecked(LocalDateTime.now());

        hostRepository.persist(newHost);

        showNotification("主机添加成功", NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
        // 广播刷新事件到所有客户端
        UIBroadcaster.broadcastRefresh("hosts");
    }

    /**
     * 更新主机
     */
    @Transactional
    public void updateHost(InventoryHost host, String hostAddr, int port, String username, String password) {
        if (host == null) {
            showNotification("主机对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        host.setHost(hostAddr.trim());
        host.setPort(port);
        host.setUsername(username.trim());
        host.setPassword(password);
        host.setUpdatedAt(LocalDateTime.now());

        hostRepository.getEntityManager().merge(host);

        showNotification("主机更新成功", NotificationVariant.LUMO_SUCCESS);
        refreshGrid();
        // 广播刷新事件到所有客户端
        UIBroadcaster.broadcastRefresh("hosts");
    }

    /**
     * 删除主机
     */
    private void deleteHost(InventoryHost host) {
        if (host == null) {
            showNotification("主机对象不能为空", NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span message = new Span("确定要删除主机 '" + host.getName() + "' 吗？");
        message.getStyle().set("font-size", "14px");

        Button confirmButton = new Button("确定", e -> {
            deleteHostFromDatabase(host);
            showNotification("主机删除成功", NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            confirmDialog.close();
            // 广播刷新事件到所有客户端
            UIBroadcaster.broadcastRefresh("hosts");
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
     * 从数据库删除主机
     */
    @Transactional
    public void deleteHostFromDatabase(InventoryHost host) {
        if (host == null) {
            return;
        }

        jakarta.persistence.EntityManager em = hostRepository.getEntityManager();

        // 先删除关联的变量
        em.createQuery("DELETE FROM InventoryHostVariable hv WHERE hv.host.id = :hostId")
                .setParameter("hostId", host.getId())
                .executeUpdate();

        // 删除关联的组-主机关系
        em.createQuery("DELETE FROM InventoryGroupHost gh WHERE gh.host.id = :hostId")
                .setParameter("hostId", host.getId())
                .executeUpdate();

        // 删除主机
        em.remove(em.find(InventoryHost.class, host.getId()));
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        try {
            List<InventoryHost> hosts = hostRepository.findAllOrderByCreatedAtDesc();
            hostGrid.setItems(hosts);
        } catch (Exception e) {
            // 静默处理异常，不影响用户体验
            System.err.println("Failed to refresh grid: " + e.getMessage());
        }
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
