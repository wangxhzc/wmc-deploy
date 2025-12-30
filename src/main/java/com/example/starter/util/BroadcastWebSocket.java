package com.example.starter.util;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 广播器 - 用于在后台线程中实时更新前端 UI
 * 支持 nginx 和 haproxy 代理
 */
@ServerEndpoint(value = "/ws/broadcast/{viewType}")
public class BroadcastWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastWebSocket.class);

    // 按视图类型存储所有活跃的 session
    private static final ConcurrentHashMap<String, Set<Session>> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("viewType") String viewType) {
        sessions.computeIfAbsent(viewType, k -> new CopyOnWriteArraySet<>()).add(session);
        logger.info("WebSocket connected: {} for view type: {}", session.getId(), viewType);
    }

    @OnClose
    public void onClose(Session session, @PathParam("viewType") String viewType) {
        Set<Session> viewSessions = sessions.get(viewType);
        if (viewSessions != null) {
            viewSessions.remove(session);
            if (viewSessions.isEmpty()) {
                sessions.remove(viewType);
            }
        }
        logger.info("WebSocket disconnected: {} for view type: {}", session.getId(), viewType);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("viewType") String viewType) {
        logger.error("WebSocket error for session {} and view type {}: {}",
                session.getId(), viewType, error.getMessage());
    }

    /**
     * 广播消息到指定视图类型的所有客户端
     */
    public static void broadcast(String viewType, String message) {
        Set<Session> viewSessions = sessions.get(viewType);
        if (viewSessions != null && !viewSessions.isEmpty()) {
            for (Session session : viewSessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        logger.error("Failed to send message to session {}: {}",
                                session.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 广播刷新事件到指定视图类型的所有客户端
     */
    public static void broadcastRefresh(String viewType) {
        String message = String.format("{\"type\":\"refresh\",\"viewType\":\"%s\"}", viewType);
        broadcast(viewType, message);
    }

    /**
     * 获取指定视图类型的活跃连接数
     */
    public static int getActiveConnections(String viewType) {
        Set<Session> viewSessions = sessions.get(viewType);
        return viewSessions != null ? viewSessions.size() : 0;
    }
}
