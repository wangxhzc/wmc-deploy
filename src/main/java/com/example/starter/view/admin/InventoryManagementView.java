package com.example.starter.view.admin;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.InventoryGroup;
import com.example.starter.entity.InventoryHost;
import com.example.starter.entity.InventoryVariable;
import com.example.starter.service.auth.UserService;
import com.example.starter.service.inventory.InventoryService;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory清单管理视图
 */
@Route(value = "inventories", layout = MainLayout.class)
public class InventoryManagementView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    InventoryService inventoryService;

    @Inject
    UserService userService;

    private Grid<Inventory> inventoryGrid;
    private H2 title;

    public InventoryManagementView() {
        addClassName("inventory-management-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @PostConstruct
    public void init() {
        // 标题
        title = new H2("清单管理");
        title.getStyle().set("margin-bottom", "20px");
        title.getStyle().set("color", "#2c3e50");

        // 添加清单按钮
        Button addButton = new Button("添加清单", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddInventoryDialog());

        // 工具栏
        HorizontalLayout toolbar = new HorizontalLayout(title, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle().set("margin-bottom", "20px");

        // 创建清单列表网格
        inventoryGrid = new Grid<>();
        inventoryGrid.setSizeFull();
        inventoryGrid.addColumn(Inventory::getName).setHeader("清单名称").setAutoWidth(true);
        inventoryGrid.addColumn(Inventory::getDescription).setHeader("描述").setAutoWidth(true);
        inventoryGrid.addComponentColumn(this::createActionButtons).setHeader("操作").setAutoWidth(true);

        // 添加组件到布局
        add(toolbar, inventoryGrid);

        // 加载数据
        refreshGrid();
    }

    /**
     * 创建操作按钮
     */
    private HorizontalLayout createActionButtons(Inventory inventory) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("title", "管理清单");
        editButton.addClickListener(e -> openInventoryDetailDialog(inventory));

        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("title", "删除清单");
        deleteButton.addClickListener(e -> deleteInventory(inventory));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);
        return actions;
    }

    /**
     * 打开添加清单对话框
     */
    private void openAddInventoryDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        TextField nameField = new TextField("清单名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("输入清单名称，例如：production");

        TextField descriptionField = new TextField("描述");
        descriptionField.setPlaceholder("输入清单描述（可选）");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 保存按钮
        Button saveButton = new Button("保存", e -> {
            try {
                inventoryService.createInventory(nameField.getValue().trim(), descriptionField.getValue());
                showNotification("清单添加成功", NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
                dialog.close();
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

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
     * 打开编辑清单对话框
     */
    private void openEditInventoryDialog(Inventory inventory) {
        Dialog editDialog = new Dialog();
        editDialog.setWidth("500px");

        TextField nameField = new TextField("清单名称");
        nameField.setRequired(true);
        nameField.setValue(inventory.getName());
        nameField.setPlaceholder("输入清单名称，例如：production");

        TextField descriptionField = new TextField("描述");
        descriptionField.setValue(inventory.getDescription() != null ? inventory.getDescription() : "");
        descriptionField.setPlaceholder("输入清单描述（可选）");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, descriptionField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // 保存按钮
        Button saveButton = new Button("保存", e -> {
            try {
                // 检查名称是否修改
                String newName = nameField.getValue().trim();
                if (!newName.equals(inventory.getName())) {
                    // 检查新名称是否已存在
                    if (inventoryService.existsByName(newName)) {
                        showNotification("清单名称已存在", NotificationVariant.LUMO_ERROR);
                        return;
                    }
                }

                // 更新清单
                inventory.setName(newName);
                inventory.setDescription(descriptionField.getValue());
                inventoryService.updateInventory(inventory);

                showNotification("清单更新成功", NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
                editDialog.close();
                // 重新打开详情对话框
                openInventoryDetailDialog(inventory);
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> editDialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        editDialog.add(dialogLayout);
        editDialog.open();

        nameField.focus();
    }

    /**
     * 打开清单详情对话框
     */
    private void openInventoryDetailDialog(Inventory inventory) {
        openInventoryDetailDialog(inventory, null);
    }

    /**
     * 打开清单详情对话框（带刷新的Inventory对象）
     */
    private void openInventoryDetailDialog(Inventory inventory, Inventory refreshedInventory) {
        // 从数据库重新加载清单，包含所有关联数据
        Inventory fullInventory = refreshedInventory != null
                ? refreshedInventory
                : inventoryService.getInventoryByIdWithAssociations(inventory.getId());

        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setHeight("700px");

        // 标题和编辑按钮
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "10px");

        H2 title = new H2("清单详情: " + fullInventory.getName());
        title.getStyle().set("margin", "0");

        Button editInfoButton = new Button("编辑信息", VaadinIcon.EDIT.create());
        editInfoButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editInfoButton.addClickListener(e -> {
            dialog.close();
            openEditInventoryDialog(fullInventory);
        });

        headerLayout.add(title, editInfoButton);

        // 描述
        Span description = new Span(fullInventory.getDescription() != null ? fullInventory.getDescription() : "无描述");
        description.getStyle().set("color", "#666");
        description.getStyle().set("margin-bottom", "20px");

        // 创建选项卡
        Tabs tabs = new Tabs();
        Tab hostsTab = new Tab("全局主机");
        Tab groupsTab = new Tab("组");
        Tab variablesTab = new Tab("全局变量");
        tabs.add(hostsTab, groupsTab, variablesTab);

        // 内容区域
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);

        // 默认显示主机面板
        contentLayout
                .add(createHostsPanel(fullInventory, dialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab));

        // 选项卡切换事件
        tabs.addSelectedChangeListener(e -> {
            contentLayout.removeAll();
            if (e.getSelectedTab() == hostsTab) {
                contentLayout.add(createHostsPanel(fullInventory, dialog, contentLayout, tabs, hostsTab, groupsTab,
                        variablesTab));
            } else if (e.getSelectedTab() == groupsTab) {
                contentLayout.add(createGroupsPanel(fullInventory, dialog, contentLayout, tabs, hostsTab, groupsTab,
                        variablesTab));
            } else if (e.getSelectedTab() == variablesTab) {
                contentLayout.add(createVariablesPanel(fullInventory, dialog, contentLayout, tabs, hostsTab, groupsTab,
                        variablesTab));
            }
        });

        // 关闭按钮
        Button closeButton = new Button("关闭", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.getElement().setAttribute("type", "button");

        VerticalLayout dialogLayout = new VerticalLayout(headerLayout, description, tabs, contentLayout, closeButton);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        dialogLayout.setFlexGrow(1, contentLayout);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 创建全局主机面板
     */
    private VerticalLayout createHostsPanel(Inventory inventory, Dialog parentDialog, VerticalLayout contentLayout,
            Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);

        // 添加主机按钮
        Button addButton = new Button("添加主机", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddHostToInventoryDialog(inventory, parentDialog, contentLayout, tabs,
                hostsTab, groupsTab, variablesTab));
        addButton.getElement().setAttribute("type", "button");

        // 主机列表
        Grid<InventoryHost> hostGrid = new Grid<>();
        hostGrid.setSizeFull();
        hostGrid.addColumn(InventoryHost::getName).setHeader("主机名称").setAutoWidth(true);
        hostGrid.addColumn(InventoryHost::getHost).setHeader("主机地址").setAutoWidth(true);
        hostGrid.addComponentColumn(host -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> {
                inventoryService.removeHostFromInventory(inventory.getId(), host.getId());
                // 刷新当前面板
                refreshHostsPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
            });
            return deleteButton;
        }).setHeader("操作").setAutoWidth(true);

        hostGrid.setItems(inventory.getHosts());

        panel.add(addButton, hostGrid);
        return panel;
    }

    /**
     * 创建组管理面板
     */
    private VerticalLayout createGroupsPanel(Inventory inventory, Dialog parentDialog, VerticalLayout contentLayout,
            Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);

        // 添加组按钮
        Button addButton = new Button("添加组", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddGroupDialog(inventory, parentDialog, contentLayout, tabs, hostsTab,
                groupsTab, variablesTab));
        addButton.getElement().setAttribute("type", "button");

        // 组列表
        Grid<InventoryGroup> groupGrid = new Grid<>();
        groupGrid.setSizeFull();
        groupGrid.addComponentColumn(group -> {
            Span nameSpan = new Span(group.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("组名称").setAutoWidth(true);
        groupGrid.addComponentColumn(group -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button viewButton = new Button(VaadinIcon.EYE.create());
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewButton.addClickListener(e -> openGroupDetailDialog(group));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> {
                inventoryService.removeGroupFromInventory(inventory.getId(), group);
                refreshGroupsPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
            });

            actions.add(viewButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        }).setHeader("操作").setAutoWidth(true);

        groupGrid.setItems(inventory.getGroups());

        panel.add(addButton, groupGrid);
        return panel;
    }

    /**
     * 创建全局变量面板
     */
    private VerticalLayout createVariablesPanel(Inventory inventory, Dialog parentDialog, VerticalLayout contentLayout,
            Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);

        // 添加变量按钮
        Button addButton = new Button("添加变量", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddVariableDialog(inventory, parentDialog, contentLayout, tabs, hostsTab,
                groupsTab, variablesTab));
        addButton.getElement().setAttribute("type", "button");

        // 变量列表
        Grid<InventoryVariable> variableGrid = new Grid<>();
        variableGrid.setSizeFull();
        variableGrid.addComponentColumn(variable -> {
            Span nameSpan = new Span(variable.getVariableName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("变量名").setAutoWidth(true);
        variableGrid.addComponentColumn(variable -> {
            Span valueSpan = new Span(variable.getVariableValue());
            return valueSpan;
        }).setHeader("变量值").setAutoWidth(true);
        variableGrid.addComponentColumn(variable -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> {
                inventoryService.removeVariableFromInventory(inventory.getId(), variable);
                refreshVariablesPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
            });
            return deleteButton;
        }).setHeader("操作").setAutoWidth(true);

        variableGrid.setItems(inventory.getVariables());

        panel.add(addButton, variableGrid);
        return panel;
    }

    /**
     * 打开添加主机到清单对话框
     */
    private void openAddHostToInventoryDialog(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        Dialog dialog = new Dialog();
        dialog.setWidth("700px");

        Span title = new Span("选择要添加到清单的主机");
        title.getStyle().set("font-size", "16px");
        title.getStyle().set("font-weight", "bold");
        title.getStyle().set("margin-bottom", "15px");

        // 获取所有未分配的主机
        List<InventoryHost> availableHosts = inventoryService.getAvailableHosts();

        // 主机选择网格
        Grid<InventoryHost> hostGrid = new Grid<>();
        hostGrid.setSizeFull();
        hostGrid.setHeight("400px");
        hostGrid.addComponentColumn(host -> {
            Span nameSpan = new Span(host.getName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("主机名称").setAutoWidth(true);
        hostGrid.addColumn(InventoryHost::getHost).setHeader("主机地址").setAutoWidth(true);
        hostGrid.addColumn(host -> host.getPort() != null ? host.getPort() : 22).setHeader("端口").setAutoWidth(true);
        hostGrid.setItems(availableHosts);

        if (availableHosts.isEmpty()) {
            Span message = new Span("没有可添加的主机，请先在主机管理中添加主机");
            message.getStyle().set("color", "#999");
            dialog.add(title, message);
        } else {
            Button addButton = new Button("添加选中的主机", e -> {
                InventoryHost selectedHost = hostGrid.asSingleSelect().getValue();
                if (selectedHost == null) {
                    showNotification("请选择要添加的主机", NotificationVariant.LUMO_ERROR);
                    return;
                }

                try {
                    inventoryService.addHostToInventory(selectedHost.getId(), inventory.getId());
                    showNotification("主机添加成功", NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                    // 刷新当前面板，不重新打开对话框
                    refreshHostsPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
                } catch (IllegalArgumentException ex) {
                    showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                }
            });
            addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addButton.getElement().setAttribute("type", "button");

            Button cancelButton = new Button("取消", e -> dialog.close());
            cancelButton.getElement().setAttribute("type", "button");

            HorizontalLayout buttonLayout = new HorizontalLayout(addButton, cancelButton);
            buttonLayout.setJustifyContentMode(JustifyContentMode.END);
            buttonLayout.setWidthFull();

            VerticalLayout dialogLayout = new VerticalLayout(title, hostGrid, buttonLayout);
            dialogLayout.setPadding(true);
            dialogLayout.setSizeFull();

            dialog.add(dialogLayout);
        }

        dialog.open();
    }

    /**
     * 打开添加组对话框
     */
    private void openAddGroupDialog(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        TextField nameField = new TextField("组名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("例如：webservers");

        TextField descriptionField = new TextField("描述");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, descriptionField);

        Button saveButton = new Button("保存", e -> {
            try {
                inventoryService.addGroupToInventory(inventory.getId(), nameField.getValue().trim(),
                        descriptionField.getValue());
                showNotification("组添加成功", NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGroupsPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 打开添加变量对话框
     */
    private void openAddVariableDialog(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        TextField nameField = new TextField("变量名称");
        nameField.setRequired(true);
        nameField.setPlaceholder("例如：ansible_user");

        TextArea valueField = new TextArea("变量值");
        valueField.setPlaceholder("例如：ubuntu");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, valueField);

        Button saveButton = new Button("保存", e -> {
            try {
                inventoryService.addVariableToInventory(inventory.getId(), nameField.getValue().trim(),
                        valueField.getValue());
                showNotification("变量添加成功", NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshVariablesPanel(inventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab, variablesTab);
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 打开组详情对话框
     */
    private void openGroupDetailDialog(InventoryGroup group) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        // 标题
        H2 title = new H2("组详情: " + group.getName());
        title.getStyle().set("margin-bottom", "10px");

        // 描述
        Span description = new Span(group.getDescription() != null ? group.getDescription() : "无描述");
        description.getStyle().set("color", "#666");
        description.getStyle().set("margin-bottom", "20px");

        // 创建选项卡
        Tabs tabs = new Tabs();
        Tab hostsTab = new Tab("组内主机");
        Tab variablesTab = new Tab("组变量");
        tabs.add(hostsTab, variablesTab);

        // 内容区域
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);

        // 默认显示主机面板
        contentLayout.add(createGroupHostsPanel(group));

        // 选项卡切换事件
        tabs.addSelectedChangeListener(e -> {
            contentLayout.removeAll();
            if (e.getSelectedTab() == hostsTab) {
                contentLayout.add(createGroupHostsPanel(group));
            } else if (e.getSelectedTab() == variablesTab) {
                contentLayout.add(createGroupVariablesPanel(group));
            }
        });

        // 关闭按钮
        Button closeButton = new Button("关闭", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.getElement().setAttribute("type", "button");

        VerticalLayout dialogLayout = new VerticalLayout(title, description, tabs, contentLayout, closeButton);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        dialogLayout.setFlexGrow(1, contentLayout);

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 创建组内主机面板
     */
    private VerticalLayout createGroupHostsPanel(InventoryGroup group) {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);

        // 添加主机按钮
        Button addButton = new Button("添加主机到组", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAddHostToGroupDialog(group));
        addButton.getElement().setAttribute("type", "button");

        // 主机列表
        Grid<InventoryHost> hostGrid = new Grid<>();
        hostGrid.setSizeFull();
        hostGrid.addComponentColumn(host -> {
            Span hostSpan = new Span(host.getName() + " (" + host.getHost() + ")");
            hostSpan.getStyle().set("font-weight", "bold");
            return hostSpan;
        }).setHeader("主机").setAutoWidth(true);
        hostGrid.addComponentColumn(host -> {
            Button removeButton = new Button(VaadinIcon.TRASH.create());
            removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            removeButton.addClickListener(e -> {
                showNotification("请使用数据库管理工具删除组内主机", NotificationVariant.LUMO_PRIMARY);
            });
            return removeButton;
        }).setHeader("操作").setAutoWidth(true);

        hostGrid.setItems(new ArrayList<>());

        panel.add(addButton, hostGrid);
        return panel;
    }

    /**
     * 创建组变量面板
     */
    private VerticalLayout createGroupVariablesPanel(InventoryGroup group) {
        VerticalLayout panel = new VerticalLayout();
        panel.setSizeFull();
        panel.setPadding(false);

        // 添加变量按钮
        Button addButton = new Button("添加变量", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> {
            showNotification("组变量功能暂未实现", NotificationVariant.LUMO_PRIMARY);
        });
        addButton.getElement().setAttribute("type", "button");

        // 变量列表
        Grid<InventoryVariable> variableGrid = new Grid<>();
        variableGrid.setSizeFull();
        variableGrid.addComponentColumn(variable -> {
            Span nameSpan = new Span(variable.getVariableName());
            nameSpan.getStyle().set("font-weight", "bold");
            return nameSpan;
        }).setHeader("变量名").setAutoWidth(true);
        variableGrid.addComponentColumn(variable -> {
            Span valueSpan = new Span(variable.getVariableValue());
            return valueSpan;
        }).setHeader("变量值").setAutoWidth(true);
        variableGrid.addComponentColumn(variable -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> {
                showNotification("请使用数据库管理工具删除组变量", NotificationVariant.LUMO_PRIMARY);
            });
            return deleteButton;
        }).setHeader("操作").setAutoWidth(true);

        variableGrid.setItems(new ArrayList<>());

        panel.add(addButton, variableGrid);
        return panel;
    }

    /**
     * 打开添加主机到组对话框
     */
    private void openAddHostToGroupDialog(InventoryGroup group) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        // 从主机管理加载所有主机
        List<InventoryHost> allHosts = inventoryService.getAvailableHosts();

        // 主机选择网格
        Grid<InventoryHost> hostGrid = new Grid<>();
        hostGrid.setSizeFull();
        hostGrid.setHeight("300px");
        hostGrid.addComponentColumn(host -> {
            Span hostSpan = new Span(host.getName() + " (" + host.getHost() + ")");
            hostSpan.getStyle().set("font-weight", "bold");
            return hostSpan;
        }).setHeader("主机").setAutoWidth(true);
        hostGrid.setItems(allHosts);

        Button addButton = new Button("添加选中的主机", e -> {
            InventoryHost selectedHost = hostGrid.asSingleSelect().getValue();
            if (selectedHost == null) {
                showNotification("请选择要添加的主机", NotificationVariant.LUMO_ERROR);
                return;
            }

            showNotification("组内主机功能暂未实现，请使用数据库管理工具", NotificationVariant.LUMO_PRIMARY);
            dialog.close();
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> dialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(addButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(hostGrid, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSizeFull();

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * 删除清单
     */
    private void deleteInventory(Inventory inventory) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        Span message = new Span("确定要删除清单 '" + inventory.getName() + "' 吗？");
        message.getStyle().set("font-size", "14px");

        Button confirmButton = new Button("确定", e -> {
            try {
                inventoryService.deleteInventory(inventory);
                showNotification("清单删除成功", NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
                confirmDialog.close();
            } catch (IllegalArgumentException ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.getElement().setAttribute("type", "button");

        Button cancelButton = new Button("取消", e -> confirmDialog.close());
        cancelButton.getElement().setAttribute("type", "button");

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();

        layout.add(message, buttonLayout);
        confirmDialog.add(layout);
        confirmDialog.open();
    }

    /**
     * 刷新网格数据
     */
    private void refreshGrid() {
        List<Inventory> inventories = inventoryService.getAllInventories();
        inventoryGrid.setItems(inventories);
    }

    /**
     * 刷新主机面板
     */
    private void refreshHostsPanel(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        // 从数据库重新加载清单
        Inventory refreshedInventory = inventoryService.getInventoryByIdWithAssociations(inventory.getId());
        // 移除当前面板并重新创建
        contentLayout.removeAll();
        contentLayout.add(createHostsPanel(refreshedInventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab,
                variablesTab));
    }

    /**
     * 刷新组面板
     */
    private void refreshGroupsPanel(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        // 从数据库重新加载清单
        Inventory refreshedInventory = inventoryService.getInventoryByIdWithAssociations(inventory.getId());
        // 移除当前面板并重新创建
        contentLayout.removeAll();
        contentLayout.add(createGroupsPanel(refreshedInventory, parentDialog, contentLayout, tabs, hostsTab, groupsTab,
                variablesTab));
    }

    /**
     * 刷新变量面板
     */
    private void refreshVariablesPanel(Inventory inventory, Dialog parentDialog,
            VerticalLayout contentLayout, Tabs tabs, Tab hostsTab, Tab groupsTab, Tab variablesTab) {
        // 从数据库重新加载清单
        Inventory refreshedInventory = inventoryService.getInventoryByIdWithAssociations(inventory.getId());
        // 移除当前面板并重新创建
        contentLayout.removeAll();
        contentLayout.add(createVariablesPanel(refreshedInventory, parentDialog, contentLayout, tabs, hostsTab,
                groupsTab, variablesTab));
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
