package com.example.starter.util;

import com.vaadin.flow.component.UI;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UI 广播器 - 用于在后台线程中更新 UI
 * 支持 nginx 7层代理环境
 */
public class UIBroadcaster {

    // 注册的 UI 集合，按视图类型分类
    private static final ConcurrentHashMap<String, Set<UI>> registeredUIs = new ConcurrentHashMap<>();

    /**
     * 注册 UI 到指定视图类型
     */
    public static void register(UI ui, String viewType) {
        registeredUIs.computeIfAbsent(viewType, k -> ConcurrentHashMap.newKeySet()).add(ui);
        ui.getElement().addEventListener("disconnect", e -> unregister(ui, viewType));
    }

    /**
     * 注销 UI
     */
    public static void unregister(UI ui, String viewType) {
        Set<UI> uis = registeredUIs.get(viewType);
        if (uis != null) {
            uis.remove(ui);
        }
    }

    /**
     * 广播刷新事件到指定视图类型的所有 UI
     */
    public static void broadcastRefresh(String viewType) {
        Set<UI> uis = registeredUIs.get(viewType);
        if (uis != null && !uis.isEmpty()) {
            // 复制一份以避免并发修改异常
            Set<UI> uisCopy = new HashSet<>(uis);

            for (UI ui : uisCopy) {
                if (ui != null && ui.isAttached()) {
                    // 在 UI 线程中直接执行所有操作
                    try {
                        ui.access(() -> {
                            // 使用一个简单的 DOM 操作来强制触发 UI 更新
                            ui.getElement().executeJs(
                                    "document.body.style.opacity = '0.99'; document.body.style.opacity = '1';");
                            // 立即触发自定义事件
                            ui.getElement().executeJs(
                                    "setTimeout(() => window.dispatchEvent(new CustomEvent('ui-refresh', { detail: { viewType: '"
                                            + viewType + "' }}), 0);");
                        });
                    } catch (Exception e) {
                        // 静默处理异常
                        System.err.println("Failed to broadcast refresh: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 清理所有注册的 UI（用于测试或特殊场景）
     */
    public static void clearAll() {
        registeredUIs.clear();
    }
}
