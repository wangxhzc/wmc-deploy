package com.example.starter.view.admin;

import com.example.starter.entity.Task;
import com.example.starter.entity.Task.TaskStatus;
import com.example.starter.service.TaskService;
import com.example.starter.service.TemplateService;
import com.example.starter.service.auth.UserService;
import com.example.starter.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * 任务管理视图 - 管理Ansible Job
 */
@Route(value = "admin/tasks", layout = MainLayout.class)
public class TaskManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private TaskService taskService;

    @Inject
    private TemplateService templateService;

    @Inject
    private UserService userService;

    private Grid<Task> grid = new Grid<>(Task.class, false);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TaskManagementView() {
        addClassName("task-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        initWebSocketConnection();

        H2 title = new H2("任务管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        Button addButton = new Button("新建任务", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openCreateTaskDialog());

        HorizontalLayout headerLayout = new HorizontalLayout(title, addButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        configureGrid();
        add(headerLayout, grid);
        refreshGrid();
    }

    private void initWebSocketConnection() {
        String jsCode = "if (!window.tasksWebSocket) {" +
                "  var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';" +
                "  var host = window.location.host;" +
                "  window.tasksWebSocket = new WebSocket(protocol + '//' + host + '/ws/broadcast/tasks');" +
                "  window.tasksWebSocket.onmessage = function(event) {" +
                "    var data = JSON.parse(event.data);" +
                "    if (data.type === 'refresh') {" +
                "      setTimeout(function() { window.location.reload(); }, 100);" +
                "    }" +
                "  };" +
                "  window.tasksWebSocket.onclose = function() {" +
                "    setTimeout(function() { window.tasksWebSocket = null; }, 3000);" +
                "  };" +
                "}";

        UI.getCurrent().getElement().executeJs(jsCode);

        UI.getCurrent().addDetachListener(e -> {
            UI.getCurrent().getElement().executeJs("if (window.tasksWebSocket) { window.tasksWebSocket.close(); }");
        });
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addComponentColumn(task -> {
            Span nameSpan = new Span(task.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("任务名称").setAutoWidth(true);

        grid.addColumn(task -> task.getTemplate() != null ? task.getTemplate().getName() : "N/A")
                .setHeader("模板").setAutoWidth(true);

        grid.addComponentColumn(this::createStatusBadge).setHeader("状态").setAutoWidth(true);

        grid.addComponentColumn(task -> {
            Span createdTime = new Span(task.getCreatedAt().format(DATE_FORMATTER));
            createdTime.getStyle().set("font-size", "12px");
            createdTime.getStyle().set("color", "#6c757d");
            return createdTime;
        }).setHeader("创建时间").setAutoWidth(true);

        grid.addComponentColumn(task -> {
            if (task.getStartedAt() == null) {
                return new Span("-");
            }
            Span startedTime = new Span(task.getStartedAt().format(DATE_FORMATTER));
            startedTime.getStyle().set("font-size", "12px");
            return startedTime;
        }).setHeader("开始时间").setAutoWidth(true);

        grid.addComponentColumn(task -> {
            if (task.getFinishedAt() == null) {
                return new Span("-");
            }
            Span finishedTime = new Span(task.getFinishedAt().format(DATE_FORMATTER));
            finishedTime.getStyle().set("font-size", "12px");
            return finishedTime;
        }).setHeader("完成时间").setAutoWidth(true);

        grid.addComponentColumn(this::createDurationSpan).setHeader("持续时间").setAutoWidth(true);

        grid.addComponentColumn(this::createActionButtons).setHeader("操作").setAutoWidth(true);
    }

    private Span createStatusBadge(Task task) {
        Span badge = new Span(getStatusText(task.getStatus()));
        badge.getStyle().set("padding", "4px 8px");
        badge.getStyle().set("border-radius", "4px");
        badge.getStyle().set("font-weight", "bold");
        badge.getStyle().set("font-size", "12px");

        switch (task.getStatus()) {
            case PENDING:
                badge.getStyle().set("background-color", "#e3f2fd");
                badge.getStyle().set("color", "#1976d2");
                break;
            case RUNNING:
                badge.getStyle().set("background-color", "#fff3e0");
                badge.getStyle().set("color", "#f57c00");
                break;
            case SUCCESS:
                badge.getStyle().set("background-color", "#e8f5e9");
                badge.getStyle().set("color", "#388e3c");
                break;
            case FAILED:
                badge.getStyle().set("background-color", "#ffebee");
                badge.getStyle().set("color", "#d32f2f");
                break;
            case CANCELLED:
                badge.getStyle().set("background-color", "#f5f5f5");
                badge.getStyle().set("color", "#616161");
                break;
        }

        return badge;
    }

    private String getStatusText(TaskStatus status) {
        switch (status) {
            case PENDING:
                return "等待中";
            case RUNNING:
                return "执行中";
            case SUCCESS:
                return "成功";
            case FAILED:
                return "失败";
            case CANCELLED:
                return "已取消";
            default:
                return status.toString();
        }
    }

    private Span createDurationSpan(Task task) {
        if (task.getStartedAt() == null) {
            return new Span("-");
        }

        long duration = task.getDurationInSeconds();
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        if (hours == 0 && minutes == 0) {
            sb.append(seconds).append("秒");
        }

        Span span = new Span(sb.toString());
        span.getStyle().set("font-size", "12px");
        span.getStyle().set("color", "#6c757d");
        return span;
    }

    private HorizontalLayout createActionButtons(Task task) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button logButton = new Button(VaadinIcon.FILE_TEXT.create());
        logButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        logButton.getElement().setAttribute("title", "查看日志");
        logButton.addClickListener(e -> openLogDialog(task));
        actions.add(logButton);

        if (task.getStatus() == TaskStatus.SUCCESS || task.getStatus() == TaskStatus.FAILED ||
                task.getStatus() == TaskStatus.CANCELLED) {
            Button restartButton = new Button(VaadinIcon.REFRESH.create());
            restartButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            restartButton.getElement().setAttribute("title", "重新启动");
            restartButton.addClickListener(e -> {
                taskService.restartTask(task.getId());
                refreshGrid();
                showNotification("任务已重新启动", NotificationVariant.LUMO_SUCCESS);
            });
            actions.add(restartButton);
        }

        if (task.getStatus() == TaskStatus.RUNNING) {
            Button cancelButton = new Button(VaadinIcon.STOP.create());
            cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY);
            cancelButton.getElement().setAttribute("title", "取消任务");
            cancelButton.addClickListener(e -> {
                taskService.cancelTask(task.getId());
                refreshGrid();
                showNotification("任务已取消", NotificationVariant.LUMO_SUCCESS);
            });
            actions.add(cancelButton);
        }

        if (task.getStatus() != TaskStatus.RUNNING) {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY);
            deleteButton.getElement().setAttribute("title", "删除任务");
            deleteButton.addClickListener(e -> openDeleteConfirmationDialog(task));
            actions.add(deleteButton);
        }

        return actions;
    }

    private void refreshGrid() {
        grid.setItems(taskService.getAllTasks());
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }

    private void openCreateTaskDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();

        TextField nameField = new TextField("任务名称");
        nameField.setPlaceholder("请输入任务名称");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPrefixComponent(VaadinIcon.TAG.create());

        ComboBox<com.example.starter.entity.Template> templateField = new ComboBox<>("模板");
        templateField.setPlaceholder("请选择模板");
        templateField.setRequired(true);
        templateField.setRequiredIndicatorVisible(true);
        templateField.setPrefixComponent(VaadinIcon.LIST.create());
        templateField.setItemLabelGenerator(com.example.starter.entity.Template::getName);
        templateField.setItems(templateService.getAllTemplates());

        formLayout.add(nameField, templateField);

        Button saveButton = new Button("创建并启动", e -> {
            try {
                String name = nameField.getValue();
                com.example.starter.entity.Template template = templateField.getValue();

                if (name == null || name.trim().isEmpty()) {
                    showNotification("任务名称不能为空", NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (template == null) {
                    showNotification("请选择模板", NotificationVariant.LUMO_ERROR);
                    return;
                }

                taskService.createAndStartTask(name, template.getId());
                refreshGrid();
                dialog.close();
                showNotification("任务创建成功，正在后台执行", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                showNotification("创建任务失败: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
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

    private void openLogDialog(Task task) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setHeight("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("任务日志: " + task.getName());
        title.getStyle().set("margin-top", "0");

        Paragraph info = new Paragraph("模板: " + (task.getTemplate() != null ? task.getTemplate().getName() : "N/A") +
                " | 状态: " + getStatusText(task.getStatus()));
        info.getStyle().set("color", "#6c757d");
        info.getStyle().set("font-size", "14px");

        TextArea logArea = new TextArea();
        logArea.setSizeFull();
        logArea.setReadOnly(true);
        logArea.getStyle().set("font-family", "monospace");
        logArea.getStyle().set("font-size", "12px");
        logArea.getStyle().set("background-color", "#f5f5f5");

        loadLogContent(task, logArea);

        final Timer[] logRefreshTimer = new Timer[] { null };
        if (task.getStatus() == TaskStatus.RUNNING) {
            logRefreshTimer[0] = new Timer("LogRefreshTimer", true);
            logRefreshTimer[0].schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        UI ui = UI.getCurrent();
                        if (ui != null) {
                            ui.access(() -> {
                                if (dialog.isOpened()) {
                                    Task currentTask = taskService.getTaskById(task.getId());
                                    if (currentTask != null && currentTask.getStatus() == TaskStatus.RUNNING) {
                                        loadLogContent(currentTask, logArea);
                                    } else {
                                        this.cancel();
                                        loadLogContent(taskService.getTaskById(task.getId()), logArea);
                                    }
                                } else {
                                    this.cancel();
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("刷新日志时出错: " + e.getMessage());
                    }
                }
            }, 2000, 2000);
        }

        Button closeButton = new Button("关闭", e -> {
            if (logRefreshTimer[0] != null) {
                logRefreshTimer[0].cancel();
            }
            dialog.close();
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.getElement().setAttribute("type", "button");

        Button refreshButton = new Button("刷新", e -> {
            Task currentTask = taskService.getTaskById(task.getId());
            if (currentTask != null) {
                loadLogContent(currentTask, logArea);
            }
        });
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(refreshButton, closeButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        layout.add(title, info, logArea, buttonLayout);
        layout.setFlexGrow(1, logArea);

        dialog.add(layout);
        dialog.open();
    }

    private void loadLogContent(Task task, TextArea logArea) {
        try {
            String log = taskService.getTaskLog(task.getId());
            logArea.setValue(log);
        } catch (Exception e) {
            logArea.setValue("加载日志失败: " + e.getMessage());
        }
    }

    private void openDeleteConfirmationDialog(Task task) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("确认删除");
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("color", "#2c3e50");

        Paragraph message = new Paragraph("确定要删除任务 \"" + task.getName() + "\" 吗？");
        message.getStyle().set("color", "#6c757d");
        message.getStyle().set("margin-bottom", "20px");

        Paragraph warning = new Paragraph("此操作将同时删除任务的所有日志文件和临时目录，且无法恢复。");
        warning.getStyle().set("color", "#d32f2f");
        warning.getStyle().set("font-size", "14px");
        warning.getStyle().set("margin-bottom", "20px");

        Button confirmButton = new Button("确认删除", e -> {
            try {
                taskService.deleteTask(task.getId());
                refreshGrid();
                dialog.close();
                showNotification("任务已删除", NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                showNotification("删除任务失败: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, confirmButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        layout.add(title, message, warning, buttonLayout);

        dialog.add(layout);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (userService == null || !userService.isLoggedIn()) {
            if (userService != null && userService.isSessionExpired()) {
                UI.getCurrent().getPage().setLocation("/login?expired=true");
            } else {
                event.forwardTo("login");
            }
        }
    }
}
