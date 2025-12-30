package com.example.starter.util;

/**
 * UI 广播器 - 使用 WebSocket 实时更新前端
 * 支持 nginx 和 haproxy 代理
 */
public class UIBroadcaster {

    /**
     * 广播刷新事件到指定视图类型的所有客户端
     */
    public static void broadcastRefresh(String viewType) {
        BroadcastWebSocket.broadcastRefresh(viewType);
    }

    /**
     * 广播自定义消息到指定视图类型的所有客户端
     */
    public static void broadcast(String viewType, String message) {
        BroadcastWebSocket.broadcast(viewType, message);
    }

    /**
     * 获取指定视图类型的活跃连接数
     */
    public static int getActiveConnections(String viewType) {
        return BroadcastWebSocket.getActiveConnections(viewType);
    }
}
