package com.example.starter.view.admin;

import com.example.starter.service.auth.UserService;
import jakarta.inject.Inject;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * 管理员主视图 - 包含所有管理功能的导航
 */
@Route("admin")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    UserService userService;

    public AdminView() {
        addClassName("admin-dashboard");
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("管理员控制台");
        title.getStyle().set("margin-bottom", "30px");

        H2 resourcesTitle = new H2("资源管理");
        resourcesTitle.getStyle().set("margin-bottom", "15px");

        // 资源管理卡片布局
        HorizontalLayout resourcesLayout = new HorizontalLayout();
        resourcesLayout.setWidthFull();
        resourcesLayout.setSpacing(true);
        resourcesLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        // 主机管理卡片
        VerticalLayout hostCard = createMenuCard(
                VaadinIcon.SERVER,
                "主机管理",
                "管理所有主机的连接信息和配置",
                "admin/hosts");
        hostCard.getStyle().set("min-width", "250px");

        // 清单管理卡片
        VerticalLayout inventoryCard = createMenuCard(
                VaadinIcon.LIST,
                "清单管理",
                "创建和管理Ansible清单及主机组",
                "admin/inventories");
        inventoryCard.getStyle().set("min-width", "250px");

        // 项目管理卡片
        VerticalLayout projectCard = createMenuCard(
                VaadinIcon.FOLDER,
                "项目管理",
                "管理Ansible Playbook项目",
                "admin/projects");
        projectCard.getStyle().set("min-width", "250px");

        resourcesLayout.add(hostCard, inventoryCard, projectCard);

        H2 systemTitle = new H2("系统管理");
        systemTitle.getStyle().set("margin-top", "40px");
        systemTitle.getStyle().set("margin-bottom", "15px");

        // 系统管理卡片布局
        HorizontalLayout systemLayout = new HorizontalLayout();
        systemLayout.setWidthFull();
        systemLayout.setSpacing(true);
        systemLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        // 用户管理卡片
        VerticalLayout userCard = createMenuCard(
                VaadinIcon.USER,
                "用户管理",
                "管理系统用户和权限",
                "admin/users");
        userCard.getStyle().set("min-width", "250px");

        systemLayout.add(userCard);

        add(title, resourcesTitle, resourcesLayout, systemTitle, systemLayout);
    }

    /**
     * 创建菜单卡片
     */
    private VerticalLayout createMenuCard(VaadinIcon icon, String title, String description, String targetRoute) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("menu-card");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background-color", "#f5f5f5")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("transition", "all 0.3s");

        // 添加悬停效果
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("box-shadow", "0 4px 8px rgba(0,0,0,0.1)")
                    .set("transform", "translateY(-2px)");
        });

        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("box-shadow", "none")
                    .set("transform", "translateY(0)");
        });

        // 点击卡片跳转
        card.getElement().addEventListener("click", e -> {
            getUI().ifPresent(ui -> ui.navigate(targetRoute));
        });

        H3 cardTitle = new H3(title);
        cardTitle.getStyle().set("margin", "0 0 10px 0");
        cardTitle.getStyle().set("display", "flex");
        cardTitle.getStyle().set("align-items", "center");
        cardTitle.getStyle().set("gap", "10px");

        // 创建图标按钮组件
        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("24px");

        // 添加图标和标题到H3
        cardTitle.add(iconComponent);
        cardTitle.add(title);

        NativeLabel descLabel = new NativeLabel(description);
        descLabel.getStyle().set("color", "#666");
        descLabel.getStyle().set("font-size", "14px");

        card.add(cardTitle, descLabel);

        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 检查用户是否已登录且具有管理员权限
        if (!userService.isLoggedIn() || !userService.isAdmin()) {
            event.rerouteTo("login");
        }
    }
}
