package com.example.starter.view.admin;

import com.example.starter.entity.Project;
import com.example.starter.service.ProjectService;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;

/**
 * 项目管理视图 - 管理Ansible Playbook项目
 */
@Route(value = "admin/projects", layout = MainLayout.class)
public class ProjectManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private ProjectService projectService;

    @Inject
    private UserService userService;

    private Grid<Project> grid = new Grid<>(Project.class, false);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProjectManagementView() {
        addClassName("project-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 标题
        H2 title = new H2("项目管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加项目按钮
        Button addButton = new Button("新建项目", VaadinIcon.PLUS.create());
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
        grid.addComponentColumn(project -> {
            Span nameSpan = new Span(project.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("项目名称").setAutoWidth(true);
        grid.addColumn(Project::getDescription).setHeader("描述").setAutoWidth(true);
        grid.addComponentColumn(project -> {
            Span createdTime = new Span(project.getCreatedAt().format(DATE_FORMATTER));
            createdTime.getStyle().set("font-size", "12px");
            createdTime.getStyle().set("color", "#6c757d");
            return createdTime;
        }).setHeader("创建时间").setAutoWidth(true);
        grid.addComponentColumn(project -> {
            Span updatedTime = new Span(project.getUpdatedAt().format(DATE_FORMATTER));
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
    private HorizontalLayout createActionButtons(Project project) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "编辑");
        editButton.addClickListener(e -> openEditDialog(project));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除");
        deleteButton.addClickListener(e -> confirmDelete(project));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        grid.setItems(projectService.getProjectResources());
    }

    /**
     * 显示通知
     */
    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    /**
     * 打开创建项目对话框
     */
    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeight("500px");

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();

        TextField nameField = new TextField("项目名称");
        nameField.setPlaceholder("请输入项目名称");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        TextField descriptionField = new TextField("描述");
        descriptionField.setPlaceholder("项目描述");
        descriptionField.setPrefixComponent(VaadinIcon.INFO_CIRCLE.create());

        TextArea yamlField = new TextArea("YAML内容");
        yamlField.setPlaceholder("---\n# 在这里输入Ansible Playbook内容\n");
        yamlField.setSizeFull();
        yamlField.setPrefixComponent(VaadinIcon.FILE_CODE.create());
        yamlField.getStyle().set("min-height", "200px");

        formLayout.add(nameField, descriptionField, yamlField);
        formLayout.setColspan(yamlField, 2);

        Button saveButton = new Button("保存", e -> {
            try {
                String name = nameField.getValue();
                String description = descriptionField.getValue();
                String yamlContent = yamlField.getValue();

                if (name == null || name.trim().isEmpty()) {
                    showNotification("项目名称不能为空", NotificationVariant.LUMO_ERROR);
                    return;
                }

                projectService.createProject(name, description, yamlContent);
                refreshGrid();
                dialog.close();
                showNotification("项目创建成功", NotificationVariant.LUMO_SUCCESS);
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
     * 打开编辑项目对话框
     */
    private void openEditDialog(Project project) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeight("500px");

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();

        TextField nameField = new TextField("项目名称");
        nameField.setValue(project.getName());
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        TextField descriptionField = new TextField("描述");
        descriptionField.setValue(project.getDescription() != null ? project.getDescription() : "");
        descriptionField.setPrefixComponent(VaadinIcon.INFO_CIRCLE.create());

        // 解码YAML内容用于编辑
        String decodedYaml = projectService.getDecodedYamlContent(project);
        TextArea yamlField = new TextArea("YAML内容");
        yamlField.setValue(decodedYaml);
        yamlField.setSizeFull();
        yamlField.setPrefixComponent(VaadinIcon.FILE_CODE.create());
        yamlField.getStyle().set("min-height", "200px");

        formLayout.add(nameField, descriptionField, yamlField);
        formLayout.setColspan(yamlField, 2);

        Button saveButton = new Button("保存", e -> {
            try {
                String name = nameField.getValue();
                String description = descriptionField.getValue();
                String yamlContent = yamlField.getValue();

                if (name == null || name.trim().isEmpty()) {
                    showNotification("项目名称不能为空", NotificationVariant.LUMO_ERROR);
                    return;
                }

                project.setName(name);
                project.setDescription(description);
                project.setYamlContent(yamlContent);

                projectService.updateProject(project);
                refreshGrid();
                dialog.close();
                showNotification("项目更新成功", NotificationVariant.LUMO_SUCCESS);
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
     * 确认删除项目
     */
    private void confirmDelete(Project project) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H2 title = new H2("确认删除");
        title.getStyle().set("margin-top", "0");

        Span message = new Span("确定要删除项目 \"" + project.getName() + "\" 吗？此操作不可恢复。");
        message.getStyle().set("font-size", "14px");

        Button deleteButton = new Button("删除", e -> {
            projectService.deleteProject(project.getId());
            refreshGrid();
            dialog.close();
            showNotification("项目已删除", NotificationVariant.LUMO_SUCCESS);
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
