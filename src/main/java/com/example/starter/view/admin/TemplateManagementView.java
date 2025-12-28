package com.example.starter.view.admin;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.Project;
import com.example.starter.entity.Template;
import com.example.starter.service.ProjectService;
import com.example.starter.service.TemplateService;
import com.example.starter.service.auth.UserService;
import com.example.starter.service.inventory.InventoryService;
import com.example.starter.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 模板管理视图 - 管理Ansible AWX类似的Job Template
 */
@Route(value = "admin/templates", layout = MainLayout.class)
public class TemplateManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private TemplateService templateService;

    @Inject
    private ProjectService projectService;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private UserService userService;

    private Grid<Template> grid = new Grid<>(Template.class, false);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TemplateManagementView() {
        addClassName("template-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 标题
        H2 title = new H2("模板管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加模板按钮
        Button addButton = new Button("新建模板", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openCreateDialog());

        // 标题栏
        HorizontalLayout headerLayout = new HorizontalLayout(title, addButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        // 配置表格
        configureGrid();

        // 添加到布局
        add(headerLayout, grid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 配置表格
     */
    private void configureGrid() {
        grid.setSizeFull();

        // 模板名称
        grid.addComponentColumn(template -> {
            Span nameSpan = new Span(template.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("模板名称").setAutoWidth(true);

        // 描述
        grid.addColumn(Template::getDescription).setHeader("描述").setAutoWidth(true);

        // 项目名称
        grid.addColumn(template -> template.getProject() != null ? template.getProject().getName() : "N/A")
                .setHeader("项目").setAutoWidth(true);

        // 清单名称
        grid.addColumn(template -> template.getInventory() != null ? template.getInventory().getName() : "N/A")
                .setHeader("清单").setAutoWidth(true);

        // 创建时间
        grid.addComponentColumn(template -> {
            Span createdTime = new Span(template.getCreatedAt().format(DATE_FORMATTER));
            createdTime.getStyle().set("font-size", "12px");
            createdTime.getStyle().set("color", "#6c757d");
            return createdTime;
        }).setHeader("创建时间").setAutoWidth(true);

        // 更新时间
        grid.addComponentColumn(template -> {
            Span updatedTime = new Span(template.getUpdatedAt().format(DATE_FORMATTER));
            updatedTime.getStyle().set("font-size", "12px");
            updatedTime.getStyle().set("color", "#6c757d");
            return updatedTime;
        }).setHeader("更新时间").setAutoWidth(true);

        // 操作列
        grid.addComponentColumn(this::createActionButtons).setHeader("操作").setAutoWidth(true);
    }

    /**
     * 创建操作按钮
     */
    private HorizontalLayout createActionButtons(Template template) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "编辑");
        editButton.addClickListener(e -> openEditDialog(template));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除");
        deleteButton.addClickListener(e -> confirmDelete(template));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        grid.setItems(templateService.getAllTemplates());
    }

    /**
     * 显示通知
     */
    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    /**
     * 打开创建模板对话框
     */
    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();

        TextField nameField = new TextField("模板名称");
        nameField.setPlaceholder("请输入模板名称");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        TextField descriptionField = new TextField("描述");
        descriptionField.setPlaceholder("模板描述");
        descriptionField.setPrefixComponent(VaadinIcon.INFO_CIRCLE.create());

        ComboBox<Project> projectField = new ComboBox<>("项目");
        projectField.setPlaceholder("请选择项目");
        projectField.setRequired(true);
        projectField.setRequiredIndicatorVisible(true);
        projectField.setPrefixComponent(VaadinIcon.FOLDER.create());
        projectField.setItemLabelGenerator(Project::getName);
        List<Project> projects = projectService.getProjectResources();
        projectField.setItems(projects);

        ComboBox<Inventory> inventoryField = new ComboBox<>("清单");
        inventoryField.setPlaceholder("请选择清单");
        inventoryField.setRequired(true);
        inventoryField.setRequiredIndicatorVisible(true);
        inventoryField.setPrefixComponent(VaadinIcon.LIST.create());
        inventoryField.setItemLabelGenerator(Inventory::getName);
        List<Inventory> inventories = inventoryService.getAllInventories();
        inventoryField.setItems(inventories);

        formLayout.add(nameField, descriptionField, projectField, inventoryField);

        Button saveButton = new Button("保存", e -> {
            try {
                String name = nameField.getValue();
                String description = descriptionField.getValue();
                Project project = projectField.getValue();
                Inventory inventory = inventoryField.getValue();

                if (name == null || name.trim().isEmpty()) {
                    showNotification("模板名称不能为空", NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (project == null) {
                    showNotification("请选择项目", NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (inventory == null) {
                    showNotification("请选择清单", NotificationVariant.LUMO_ERROR);
                    return;
                }

                templateService.createTemplate(name, description, project.getId(), inventory.getId());
                refreshGrid();
                dialog.close();
                showNotification("模板创建成功", NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 打开编辑模板对话框
     */
    private void openEditDialog(Template template) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();

        TextField nameField = new TextField("模板名称");
        nameField.setValue(template.getName());
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        TextField descriptionField = new TextField("描述");
        descriptionField.setValue(template.getDescription() != null ? template.getDescription() : "");
        descriptionField.setPrefixComponent(VaadinIcon.INFO_CIRCLE.create());

        ComboBox<Project> projectField = new ComboBox<>("项目");
        projectField.setPlaceholder("请选择项目");
        projectField.setRequired(true);
        projectField.setRequiredIndicatorVisible(true);
        projectField.setPrefixComponent(VaadinIcon.FOLDER.create());
        projectField.setItemLabelGenerator(Project::getName);
        List<Project> projects = projectService.getProjectResources();
        projectField.setItems(projects);
        projectField.setValue(template.getProject());

        ComboBox<Inventory> inventoryField = new ComboBox<>("清单");
        inventoryField.setPlaceholder("请选择清单");
        inventoryField.setRequired(true);
        inventoryField.setRequiredIndicatorVisible(true);
        inventoryField.setPrefixComponent(VaadinIcon.LIST.create());
        inventoryField.setItemLabelGenerator(Inventory::getName);
        List<Inventory> inventories = inventoryService.getAllInventories();
        inventoryField.setItems(inventories);
        inventoryField.setValue(template.getInventory());

        formLayout.add(nameField, descriptionField, projectField, inventoryField);

        Button saveButton = new Button("保存", e -> {
            try {
                String name = nameField.getValue();
                String description = descriptionField.getValue();
                Project project = projectField.getValue();
                Inventory inventory = inventoryField.getValue();

                if (name == null || name.trim().isEmpty()) {
                    showNotification("模板名称不能为空", NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (project == null) {
                    showNotification("请选择项目", NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (inventory == null) {
                    showNotification("请选择清单", NotificationVariant.LUMO_ERROR);
                    return;
                }

                template.setName(name);
                template.setDescription(description);
                template.setProject(project);
                template.setInventory(inventory);

                templateService.updateTemplate(template);
                refreshGrid();
                dialog.close();
                showNotification("模板更新成功", NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 确认删除模板
     */
    private void confirmDelete(Template template) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H2 title = new H2("确认删除");
        title.getStyle().set("margin-top", "0");

        Span message = new Span("确定要删除模板 \"" + template.getName() + "\" 吗？此操作不可恢复。");
        message.getStyle().set("font-size", "14px");

        Button deleteButton = new Button("删除", e -> {
            templateService.deleteTemplate(template.getId());
            refreshGrid();
            dialog.close();
            showNotification("模板已删除", NotificationVariant.LUMO_SUCCESS);
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(deleteButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        layout.add(title, message, buttonLayout);
        dialog.add(layout);
        dialog.open();
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
