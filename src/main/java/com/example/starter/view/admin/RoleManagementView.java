package com.example.starter.view.admin;

import com.example.starter.entity.Role;
import com.example.starter.entity.RoleVariable;
import com.example.starter.repository.RoleRepository;
import com.example.starter.service.auth.UserService;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理视图 - 用于管理和编辑服务器角色及其变量
 */
@Route(value = "roles", layout = MainLayout.class)
public class RoleManagementView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOGGER = Logger.getLogger(RoleManagementView.class);

    @Inject
    RoleRepository roleRepository;

    @Inject
    UserService userService;

    private Grid<Role> roleGrid;
    private H2 title;

    public RoleManagementView() {
        addClassName("role-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 标题
        title = new H2("角色管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加角色按钮
        Button addButton = new Button("添加角色", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddRoleDialog());

        // 工具栏
        HorizontalLayout toolbar = new HorizontalLayout(title, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "20px");

        // 创建角色列表网格
        roleGrid = new Grid<>();
        roleGrid.setSizeFull();
        roleGrid.addColumn(Role::getName).setHeader("角色名称").setAutoWidth(true);
        roleGrid.addColumn(role -> String.valueOf(role.getVariables().size())).setHeader("变量数量").setAutoWidth(true);
        roleGrid.addColumn(new ComponentRenderer<>(role -> {
            Span span = new Span(
                    role.getCreatedAt() != null ? role.getCreatedAt().toString().substring(0, 19) : "");
            span.getStyle().set("font-size", "12px");
            return span;
        })).setHeader("创建时间").setAutoWidth(true);
        roleGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
                .setHeader("操作").setAutoWidth(true);

        // 添加组件到布局
        add(toolbar, roleGrid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 创建操作按钮
     */
    private HorizontalLayout createActionButtons(Role role) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "编辑角色");
        editButton.addClickListener(e -> openEditRoleDialog(role));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除角色");
        deleteButton.addClickListener(e -> deleteRole(role));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 打开添加角色对话框
     */
    private void openAddRoleDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        TextField nameField = new TextField("角色名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("输入角色名称，例如：redis");

        // 变量列表
        VerticalLayout variableListLayout = new VerticalLayout();
        variableListLayout.setSpacing(true);
        variableListLayout.setPadding(false);

        // 添加变量按钮
        Button addVariableButton = new Button("添加变量", VaadinIcon.PLUS.create());
        addVariableButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addVariableButton.addClickListener(e -> {
            addVariableRow(variableListLayout);
        });

        HorizontalLayout headerLayout = new HorizontalLayout(
                new Span("变量列表（可选）"), addVariableButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 保存按钮
        Button saveButton = new Button("保存", e -> {
            if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
                showNotification("请输入角色名称", NotificationVariant.LUMO_ERROR);
                return;
            }

            if (roleRepository.existsByName(nameField.getValue().trim())) {
                showNotification("角色名称已存在", NotificationVariant.LUMO_ERROR);
                return;
            }

            // 收集变量
            List<RoleVariable> variables = new ArrayList<>();
            for (int i = 0; i < variableListLayout.getComponentCount(); i++) {
                HorizontalLayout row = (HorizontalLayout) variableListLayout.getComponentAt(i);
                TextField varNameField = (TextField) row.getComponentAt(0);
                TextField varValueField = (TextField) row.getComponentAt(1);

                if (varNameField.getValue() != null && !varNameField.getValue().trim().isEmpty()) {
                    variables.add(new RoleVariable(varNameField.getValue().trim(),
                            varValueField.getValue() != null ? varValueField.getValue().trim() : ""));
                }
            }

            // 保存角色和变量
            Role newRole = new Role(nameField.getValue().trim());
            for (RoleVariable variable : variables) {
                variable.role = newRole;
                newRole.getVariables().add(variable);
            }
            saveRoleWithVariables(newRole);

            showNotification("角色添加成功", NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("取消", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, headerLayout, variableListLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();

        nameField.focus();
    }

    /**
     * 添加变量行
     */
    private void addVariableRow(VerticalLayout variableListLayout) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);

        TextField varNameField = new TextField();
        varNameField.setPlaceholder("变量名称");
        varNameField.setWidth("40%");
        varNameField.setRequired(true);

        TextField varValueField = new TextField();
        varValueField.setPlaceholder("变量值");
        varValueField.setWidth("40%");

        Button removeButton = new Button(VaadinIcon.TRASH.create());
        removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        removeButton.addClickListener(e -> {
            variableListLayout.remove(row);
            // 移除行后，保存时不会再遍历到这个变量
        });

        row.add(varNameField, varValueField, removeButton);
        row.setFlexGrow(1, varNameField);
        row.setFlexGrow(1, varValueField);

        variableListLayout.add(row);
    }

    /**
     * 打开编辑角色对话框
     */
    private void openEditRoleDialog(Role role) {
        // 从数据库重新获取角色，确保加载所有变量
        Role currentRole = roleRepository.findById(role.getId());

        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        TextField nameField = new TextField("角色名称");
        nameField.setRequired(true);
        nameField.setValue(currentRole.getName());
        nameField.setReadOnly(true);

        // 变量列表
        VerticalLayout variableListLayout = new VerticalLayout();
        variableListLayout.setSpacing(true);
        variableListLayout.setPadding(false);

        // 加载现有变量
        for (RoleVariable variable : currentRole.getVariables()) {
            addVariableRowWithData(variableListLayout, variable);
        }

        // 添加变量按钮
        Button addVariableButton = new Button("添加变量", VaadinIcon.PLUS.create());
        addVariableButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addVariableButton.addClickListener(e -> {
            addVariableRow(variableListLayout);
        });

        HorizontalLayout headerLayout = new HorizontalLayout(
                new Span("变量列表（可选）"), addVariableButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 保存按钮
        Button saveButton = new Button("保存", e -> {
            // 先删除原有变量
            currentRole.getVariables().clear();

            // 收集新变量
            for (int i = 0; i < variableListLayout.getComponentCount(); i++) {
                HorizontalLayout row = (HorizontalLayout) variableListLayout.getComponentAt(i);
                TextField varNameField = (TextField) row.getComponentAt(0);
                TextField varValueField = (TextField) row.getComponentAt(1);

                if (varNameField.getValue() != null && !varNameField.getValue().trim().isEmpty()) {
                    RoleVariable variable = new RoleVariable(
                            varNameField.getValue().trim(),
                            varValueField.getValue() != null ? varValueField.getValue().trim() : "");
                    variable.role = currentRole;
                    currentRole.getVariables().add(variable);
                }
            }

            // 更新角色和变量
            updateRoleWithVariables(currentRole);

            showNotification("角色更新成功", NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("取消", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, headerLayout, variableListLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 添加变量行（带数据）
     */
    private void addVariableRowWithData(VerticalLayout variableListLayout, RoleVariable variable) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);

        TextField varNameField = new TextField();
        varNameField.setPlaceholder("变量名称");
        varNameField.setWidth("40%");
        varNameField.setValue(variable.getVariableName());

        TextField varValueField = new TextField();
        varValueField.setPlaceholder("变量值");
        varValueField.setWidth("40%");
        varValueField.setValue(variable.getVariableValue());

        Button removeButton = new Button(VaadinIcon.TRASH.create());
        removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        removeButton.addClickListener(e -> {
            variableListLayout.remove(row);
            removeButton.setEnabled(false);
        });

        row.add(varNameField, varValueField, removeButton);
        row.setFlexGrow(1, varNameField);
        row.setFlexGrow(1, varValueField);

        variableListLayout.add(row);
    }

    /**
     * 保存角色和变量
     */
    @Transactional
    public void saveRoleWithVariables(Role role) {
        jakarta.persistence.EntityManager em = roleRepository.getEntityManager();

        LOGGER.debug("=== 开始保存角色 ===");
        LOGGER.debug("角色名称: " + role.getName());
        LOGGER.debug("变量数量: " + role.getVariables().size());

        // 保存角色
        em.persist(role);
        em.flush();

        // 手动保存变量
        for (int i = 0; i < role.getVariables().size(); i++) {
            RoleVariable variable = role.getVariables().get(i);
            if (variable.role == null) {
                variable.role = role;
            }
            LOGGER.debug("保存变量 " + i + ": " + variable.getVariableName() + "=" + variable.getVariableValue());
            em.persist(variable);
        }

        em.flush();
        LOGGER.debug("=== 保存完成 ===");
    }

    /**
     * 更新角色和变量
     */
    @Transactional
    public void updateRoleWithVariables(Role role) {
        jakarta.persistence.EntityManager em = roleRepository.getEntityManager();

        // 删除旧变量
        em.createQuery("DELETE FROM RoleVariable rv WHERE rv.role.id = :roleId")
                .setParameter("roleId", role.getId())
                .executeUpdate();

        // 从数据库重新获取角色实体
        Role managedRole = em.find(Role.class, role.getId());

        // 更新角色信息
        managedRole.setName(role.getName());
        managedRole.setUpdatedAt(java.time.LocalDateTime.now());

        // 保存新变量
        for (RoleVariable variable : role.getVariables()) {
            if (variable.role == null) {
                variable.role = managedRole;
            }
            em.persist(variable);
        }
    }

    /**
     * 删除角色
     */
    private void deleteRole(Role role) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span message = new Span("确定要删除角色 '" + role.getName() + "' 吗？");
        message.getStyle().set("font-size", "14px");

        Button confirmButton = new Button("确定", e -> {
            deleteRoleFromDatabase(role);
            showNotification("角色删除成功", NotificationVariant.LUMO_SUCCESS);
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
     * 从数据库删除角色
     */
    @Transactional
    public void deleteRoleFromDatabase(Role role) {
        roleRepository.delete(role);
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        List<Role> roles = roleRepository.findAllActive();
        roleGrid.setItems(roles);
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
