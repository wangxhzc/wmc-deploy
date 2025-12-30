package com.example.starter.view;

import com.example.starter.entity.Task;
import com.example.starter.entity.Task.TaskStatus;
import com.example.starter.service.StatisticsService;
import com.example.starter.service.auth.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 资源预览页面 - 登录后的默认主页，显示统计图表和数据
 */
@Route(value = "", layout = MainLayout.class)
public class ResourcePreviewView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    UserService userService;

    @Inject
    StatisticsService statisticsService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 组件引用
    private Div hostCard;
    private Div inventoryCard;
    private Div projectCard;
    private Div templateCard;
    private Div totalTasksCard;
    private Div successTasksCard;
    private Div failedTasksCard;
    private Div successRateCard;
    private Div hostChartContainer;
    private Div taskChartContainer;
    private Div recentTasksContainer;

    public ResourcePreviewView() {
        addClassName("resource-preview-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 初始化 WebSocket 连接
        initWebSocketConnection();

        // 页面标题
        H1 title = new H1("资源预览");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 刷新按钮
        Button refreshButton = new Button("刷新数据", VaadinIcon.REFRESH.create(), e -> refreshData());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout headerLayout = new HorizontalLayout(title, refreshButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        add(headerLayout);

        // 创建统计卡片
        VerticalLayout statsCards = createStatsCards();
        add(statsCards);

        // 创建图表区域
        HorizontalLayout chartsLayout = createCharts();
        chartsLayout.setWidthFull();
        add(chartsLayout);

        // 创建最近任务列表
        VerticalLayout recentTasksLayout = createRecentTasks();
        recentTasksLayout.setWidthFull();
        add(recentTasksLayout);

        // 首次加载数据
        refreshData();
    }

    /**
     * 初始化 WebSocket 连接
     */
    private void initWebSocketConnection() {
        String jsCode = "if (!window.dashboardWebSocket) {" +
                "  var protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';" +
                "  var host = window.location.host;" +
                "  window.dashboardWebSocket = new WebSocket(protocol + '//' + host + '/ws/broadcast/dashboard');" +
                "  window.dashboardWebSocket.onmessage = function(event) {" +
                "    var data = JSON.parse(event.data);" +
                "    if (data.type === 'refresh') {" +
                "      window.location.reload();" +
                "    }" +
                "  };" +
                "  window.dashboardWebSocket.onclose = function() {" +
                "    setTimeout(function() { window.dashboardWebSocket = null; }, 3000);" +
                "  };" +
                "}";

        UI.getCurrent().getElement().executeJs(jsCode);

        UI.getCurrent().addDetachListener(e -> {
            UI.getCurrent().getElement()
                    .executeJs("if (window.dashboardWebSocket) { window.dashboardWebSocket.close(); }");
        });
    }

    /**
     * 创建统计卡片
     */
    private VerticalLayout createStatsCards() {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.getStyle().set("margin-bottom", "20px");

        HorizontalLayout cardsRow1 = new HorizontalLayout();
        cardsRow1.setWidthFull();
        cardsRow1.setSpacing(true);

        // 主机统计卡片
        hostCard = createStatCard("主机总数", "0", VaadinIcon.DESKTOP, "#3498db");

        // 清单统计卡片
        inventoryCard = createStatCard("清单总数", "0", VaadinIcon.SERVER, "#9b59b6");

        // 项目统计卡片
        projectCard = createStatCard("项目总数", "0", VaadinIcon.FOLDER, "#e67e22");

        // 模板统计卡片
        templateCard = createStatCard("模板总数", "0", VaadinIcon.CLIPBOARD_TEXT, "#1abc9c");

        cardsRow1.add(hostCard, inventoryCard, projectCard, templateCard);
        cardsRow1.setFlexGrow(1, hostCard);
        cardsRow1.setFlexGrow(1, inventoryCard);
        cardsRow1.setFlexGrow(1, projectCard);
        cardsRow1.setFlexGrow(1, templateCard);

        HorizontalLayout cardsRow2 = new HorizontalLayout();
        cardsRow2.setWidthFull();
        cardsRow2.setSpacing(true);
        cardsRow2.getStyle().set("margin-top", "15px");

        // 任务总数卡片
        totalTasksCard = createStatCard("任务总数", "0", VaadinIcon.CLOCK, "#34495e");

        // 成功任务卡片
        successTasksCard = createStatCard("成功任务", "0", VaadinIcon.CHECK_CIRCLE, "#2ecc71");

        // 失败任务卡片
        failedTasksCard = createStatCard("失败任务", "0", VaadinIcon.CLOSE_CIRCLE, "#e74c3c");

        // 成功率卡片
        successRateCard = createStatCard("任务成功率", "0%", VaadinIcon.CHART_LINE, "#16a085");

        cardsRow2.add(totalTasksCard, successTasksCard, failedTasksCard, successRateCard);
        cardsRow2.setFlexGrow(1, totalTasksCard);
        cardsRow2.setFlexGrow(1, successTasksCard);
        cardsRow2.setFlexGrow(1, failedTasksCard);
        cardsRow2.setFlexGrow(1, successRateCard);

        layout.add(cardsRow1, cardsRow2);
        return layout;
    }

    /**
     * 创建统计卡片
     */
    private Div createStatCard(String title, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle().set("background-color", "white");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("padding", "20px");
        card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        card.getStyle().set("border-left", "4px solid " + color);

        // 图标
        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("48px");
        iconComponent.getStyle().set("color", color);
        iconComponent.getStyle().set("opacity", "0.8");

        // 标题
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "14px");
        titleSpan.getStyle().set("color", "#7f8c8d");
        titleSpan.getStyle().set("display", "block");
        titleSpan.getStyle().set("margin-bottom", "10px");

        // 数值
        H3 valueH3 = new H3(value);
        valueH3.getStyle().set("margin", "0");
        valueH3.getStyle().set("color", "#2c3e50");
        valueH3.getStyle().set("font-size", "28px");
        valueH3.setId("stat-value");

        HorizontalLayout content = new HorizontalLayout(iconComponent,
                new VerticalLayout(titleSpan, valueH3));
        content.setSpacing(true);
        content.setAlignItems(Alignment.CENTER);

        card.add(content);
        return card;
    }

    /**
     * 创建图表区域
     */
    private HorizontalLayout createCharts() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);
        layout.getStyle().set("margin-bottom", "20px");

        // 主机连接状态图表
        Div hostChart = createSimpleChart("主机连接状态", "host-chart", true);
        hostChart.setWidth("50%");

        // 任务状态分布图表
        Div taskChart = createSimpleChart("任务状态分布", "task-chart", true);
        taskChart.setWidth("50%");

        layout.add(hostChart, taskChart);
        layout.setFlexGrow(1, hostChart);
        layout.setFlexGrow(1, taskChart);

        return layout;
    }

    /**
     * 创建简单的HTML图表
     */
    private Div createSimpleChart(String title, String chartType, boolean isPie) {
        Div chart = new Div();
        chart.getStyle().set("background-color", "white");
        chart.getStyle().set("border-radius", "8px");
        chart.getStyle().set("padding", "20px");
        chart.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        H3 chartTitle = new H3(title);
        chartTitle.getStyle().set("margin-top", "0");
        chartTitle.getStyle().set("margin-bottom", "20px");
        chartTitle.getStyle().set("color", "#2c3e50");

        Div chartContainer = new Div();
        chartContainer.getStyle().set("min-height", "300px");
        chartContainer.getStyle().set("display", "flex");
        chartContainer.getStyle().set("align-items", "center");
        chartContainer.getStyle().set("justify-content", "center");

        Span noData = new Span("暂无数据");
        noData.getStyle().set("color", "#95a5a6");
        noData.getStyle().set("font-size", "16px");

        chartContainer.add(noData);

        if ("host-chart".equals(chartType)) {
            hostChartContainer = chartContainer;
        } else if ("task-chart".equals(chartType)) {
            taskChartContainer = chartContainer;
        }

        chart.add(chartTitle, chartContainer);
        return chart;
    }

    /**
     * 创建最近任务列表
     */
    private VerticalLayout createRecentTasks() {
        VerticalLayout layout = new VerticalLayout();
        layout.getStyle().set("background-color", "white");
        layout.getStyle().set("border-radius", "8px");
        layout.getStyle().set("padding", "20px");
        layout.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        H3 title = new H3("最近任务");
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        recentTasksContainer = new Div();
        recentTasksContainer.setWidthFull();

        Span noData = new Span("暂无任务数据");
        noData.getStyle().set("color", "#95a5a6");
        noData.getStyle().set("font-size", "14px");
        noData.getStyle().set("display", "block");
        noData.getStyle().set("text-align", "center");
        noData.getStyle().set("padding", "40px 0");

        recentTasksContainer.add(noData);

        layout.add(title, recentTasksContainer);
        return layout;
    }

    /**
     * 刷新数据
     */
    @SuppressWarnings("unchecked")
    private void refreshData() {
        try {
            Map<String, Object> stats = statisticsService.getAllStatistics();

            // 更新统计卡片
            updateStatCards(stats);

            // 更新图表
            updateCharts(stats);

            // 更新最近任务列表
            updateRecentTasks((List<Task>) stats.get("recentTasks"));
        } catch (Exception e) {
            System.err.println("刷新数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新统计卡片
     */
    @SuppressWarnings("unchecked")
    private void updateStatCards(Map<String, Object> stats) {
        Map<String, Object> hostStats = (Map<String, Object>) stats.get("hosts");
        Map<String, Object> taskStats = (Map<String, Object>) stats.get("tasks");

        // 更新主机卡片
        updateCardValue(hostCard, String.valueOf(hostStats.get("total")));
        updateCardValue(inventoryCard, String.valueOf(stats.get("inventories")));
        updateCardValue(projectCard, String.valueOf(stats.get("projects")));
        updateCardValue(templateCard, String.valueOf(stats.get("templates")));

        // 更新任务卡片
        updateCardValue(totalTasksCard, String.valueOf(taskStats.get("total")));
        updateCardValue(successTasksCard, String.valueOf(taskStats.get("success")));
        updateCardValue(failedTasksCard, String.valueOf(taskStats.get("failed")));

        double successRate = (Double) taskStats.get("successRate");
        updateCardValue(successRateCard, String.format("%.1f%%", successRate));
    }

    /**
     * 更新卡片数值
     */
    private void updateCardValue(Div card, String value) {
        if (card != null) {
            H3 valueH3 = (H3) card.getChildren().findFirst()
                    .flatMap(c -> c instanceof HorizontalLayout ? ((HorizontalLayout) c).getChildren().findFirst()
                            : java.util.Optional.empty())
                    .flatMap(c -> c instanceof VerticalLayout ? ((VerticalLayout) c).getChildren().skip(1).findFirst()
                            : java.util.Optional.empty())
                    .orElse(null);
            if (valueH3 != null) {
                valueH3.setText(value);
            }
        }
    }

    /**
     * 更新图表
     */
    @SuppressWarnings("unchecked")
    private void updateCharts(Map<String, Object> stats) {
        Map<String, Object> hostStats = (Map<String, Object>) stats.get("hosts");
        Map<String, Object> taskStats = (Map<String, Object>) stats.get("tasks");

        // 更新主机连接状态图表
        if (hostChartContainer != null) {
            hostChartContainer.removeAll();
            if ((Long) hostStats.get("total") > 0) {
                long connected = (Long) hostStats.get("connected");
                long disconnected = (Long) hostStats.get("disconnected");
                hostChartContainer.add(createProgressBar(connected, disconnected, "已连接", "未连接",
                        "#2ecc71", "#e74c3c"));
            } else {
                Span noData = new Span("暂无数据");
                noData.getStyle().set("color", "#95a5a6");
                hostChartContainer.add(noData);
            }
        }

        // 更新任务状态分布图表
        if (taskChartContainer != null) {
            taskChartContainer.removeAll();
            if ((Long) taskStats.get("total") > 0) {
                long success = (Long) taskStats.get("success");
                long failed = (Long) taskStats.get("failed");
                long running = (Long) taskStats.get("running");
                long pending = (Long) taskStats.get("pending");
                long cancelled = (Long) taskStats.get("cancelled");

                VerticalLayout chartLayout = new VerticalLayout();
                chartLayout.setSpacing(true);
                chartLayout.setPadding(false);
                chartLayout.setWidthFull();

                chartLayout.add(createProgressBar(success, failed + running + pending + cancelled, "成功", "其他",
                        "#2ecc71", "#95a5a6"));
                chartLayout.add(createProgressBar(running, pending + cancelled + failed, "运行中", "其他",
                        "#f39c12", "#95a5a6"));
                chartLayout.add(createProgressBar(pending, cancelled, "等待中", "其他",
                        "#3498db", "#95a5a6"));

                taskChartContainer.add(chartLayout);
            } else {
                Span noData = new Span("暂无数据");
                noData.getStyle().set("color", "#95a5a6");
                taskChartContainer.add(noData);
            }
        }
    }

    /**
     * 创建进度条
     */
    private Div createProgressBar(long value1, long value2, String label1, String label2, String color1,
            String color2) {
        long total = value1 + value2;
        double percentage1 = total > 0 ? (value1 * 100.0 / total) : 0;
        double percentage2 = 100 - percentage1;

        Div container = new Div();
        container.setWidthFull();
        container.getStyle().set("margin", "10px 0");

        // 标签行
        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setWidthFull();

        Span label1Span = new Span(label1 + ": " + value1 + " (" + String.format("%.1f%%", percentage1) + ")");
        label1Span.getStyle().set("color", color1);
        label1Span.getStyle().set("font-size", "14px");
        label1Span.getStyle().set("font-weight", "bold");

        Span label2Span = new Span(label2 + ": " + value2 + " (" + String.format("%.1f%%", percentage2) + ")");
        label2Span.getStyle().set("color", color2);
        label2Span.getStyle().set("font-size", "14px");

        labelRow.add(label1Span, label2Span);
        labelRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // 进度条
        Div progressBar = new Div();
        progressBar.setWidthFull();
        progressBar.getStyle().set("height", "30px");
        progressBar.getStyle().set("border-radius", "4px");
        progressBar.getStyle().set("overflow", "hidden");
        progressBar.getStyle().set("background-color", "#ecf0f1");
        progressBar.getStyle().set("display", "flex");

        Div bar1 = new Div();
        bar1.getStyle().set("width", percentage1 + "%");
        bar1.getStyle().set("height", "100%");
        bar1.getStyle().set("background-color", color1);
        bar1.getStyle().set("display", "flex");
        bar1.getStyle().set("align-items", "center");
        bar1.getStyle().set("justify-content", "center");
        bar1.getStyle().set("color", "white");
        bar1.getStyle().set("font-size", "12px");
        bar1.getStyle().set("font-weight", "bold");
        if (percentage1 > 10) {
            bar1.setText(String.format("%.0f%%", percentage1));
        }

        Div bar2 = new Div();
        bar2.getStyle().set("width", percentage2 + "%");
        bar2.getStyle().set("height", "100%");
        bar2.getStyle().set("background-color", color2);
        bar2.getStyle().set("display", "flex");
        bar2.getStyle().set("align-items", "center");
        bar2.getStyle().set("justify-content", "center");
        bar2.getStyle().set("color", "white");
        bar2.getStyle().set("font-size", "12px");
        bar2.getStyle().set("font-weight", "bold");
        if (percentage2 > 10) {
            bar2.setText(String.format("%.0f%%", percentage2));
        }

        progressBar.add(bar1, bar2);

        container.add(labelRow, progressBar);
        return container;
    }

    /**
     * 更新最近任务列表
     */
    private void updateRecentTasks(List<Task> tasks) {
        if (recentTasksContainer != null) {
            recentTasksContainer.removeAll();

            if (tasks.isEmpty()) {
                Span noData = new Span("暂无任务数据");
                noData.getStyle().set("color", "#95a5a6");
                noData.getStyle().set("font-size", "14px");
                noData.getStyle().set("display", "block");
                noData.getStyle().set("text-align", "center");
                noData.getStyle().set("padding", "40px 0");
                recentTasksContainer.add(noData);
                return;
            }

            VerticalLayout tasksList = new VerticalLayout();
            tasksList.setSpacing(false);
            tasksList.setPadding(false);
            tasksList.setWidthFull();

            for (Task task : tasks) {
                tasksList.add(createTaskRow(task));
            }

            recentTasksContainer.add(tasksList);
        }
    }

    /**
     * 创建任务行
     */
    private Div createTaskRow(Task task) {
        Div row = new Div();
        row.getStyle().set("padding", "15px");
        row.getStyle().set("border-bottom", "1px solid #ecf0f1");
        row.getStyle().set("display", "flex");
        row.getStyle().set("justify-content", "space-between");
        row.getStyle().set("align-items", "center");

        // 左侧信息
        VerticalLayout leftInfo = new VerticalLayout();
        leftInfo.setSpacing(false);
        leftInfo.setPadding(false);
        leftInfo.getStyle().set("margin", "0");

        // 任务名称
        Span nameSpan = new Span(task.getName());
        nameSpan.getStyle().set("font-weight", "bold");
        nameSpan.getStyle().set("font-size", "14px");
        nameSpan.getStyle().set("color", "#2c3e50");

        // 模板名称
        String templateName = task.getTemplate() != null ? task.getTemplate().getName() : "N/A";
        Span templateSpan = new Span("模板: " + templateName);
        templateSpan.getStyle().set("font-size", "12px");
        templateSpan.getStyle().set("color", "#7f8c8d");

        // 创建时间
        Span timeSpan = new Span("创建时间: " + task.getCreatedAt().format(DATE_FORMATTER));
        timeSpan.getStyle().set("font-size", "12px");
        timeSpan.getStyle().set("color", "#95a5a6");

        leftInfo.add(nameSpan, templateSpan, timeSpan);

        // 右侧状态
        Span statusSpan = new Span(getStatusText(task.getStatus()));
        statusSpan.getStyle().set("padding", "4px 12px");
        statusSpan.getStyle().set("border-radius", "4px");
        statusSpan.getStyle().set("font-weight", "bold");
        statusSpan.getStyle().set("font-size", "12px");

        switch (task.getStatus()) {
            case PENDING:
                statusSpan.getStyle().set("background-color", "#e3f2fd");
                statusSpan.getStyle().set("color", "#1976d2");
                break;
            case RUNNING:
                statusSpan.getStyle().set("background-color", "#fff3e0");
                statusSpan.getStyle().set("color", "#f57c00");
                break;
            case SUCCESS:
                statusSpan.getStyle().set("background-color", "#e8f5e9");
                statusSpan.getStyle().set("color", "#388e3c");
                break;
            case FAILED:
                statusSpan.getStyle().set("background-color", "#ffebee");
                statusSpan.getStyle().set("color", "#d32f2f");
                break;
            case CANCELLED:
                statusSpan.getStyle().set("background-color", "#f5f5f5");
                statusSpan.getStyle().set("color", "#616161");
                break;
        }

        row.add(leftInfo, statusSpan);
        return row;
    }

    /**
     * 获取状态文本
     */
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
