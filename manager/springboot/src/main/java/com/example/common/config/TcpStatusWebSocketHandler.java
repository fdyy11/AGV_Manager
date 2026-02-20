// java
package com.example.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpStatusWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket连接已建立: " + session.getId() + ", sessions=" + sessions.size()
                + ", remote=" + session.getRemoteAddress());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            System.out.println("收到WebSocket文本消息 from " + session.getId() + ": " + payload);

            // 解析 JSON 数据
            try {
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                // 处理业务逻辑
            } catch (Exception e) {
                System.err.println("解析JSON失败: " + e.getMessage());
            }
        } else if (message instanceof BinaryMessage) {
            System.out.println("收到WebSocket二进制消息 from " + session.getId());
            // 忽略二进制数据或特殊处理
        }
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误, session=" + (session != null ? session.getId() : "null") + ", err=" + exception.getMessage());
        if (session != null) {
            sessions.remove(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessions.remove(session.getId());
        System.out.println("WebSocket连接已关闭: " + session.getId() + ", 状态: " + closeStatus + ", sessions=" + sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastTcpStatus(Map<String, Object> status) {
        String jsonMessage = convertToJson(createMessage("status", status));
        sendToAll(jsonMessage);
    }

    public void broadcastReceivedMessage(String message) {
        Map<String, String> payload = Collections.singletonMap("content", message);
        String jsonMessage = convertToJson(createMessage("message", payload));
        sendToAll(jsonMessage);
    }

    public void broadcastClientConnect(String clientAddress) {
        Map<String, String> payload = Collections.singletonMap("address", clientAddress);
        String jsonMessage = convertToJson(createMessage("clientConnect", payload));
        sendToAll(jsonMessage);
    }

    public void broadcastClientDisconnect(String clientAddress) {
        Map<String, String> payload = Collections.singletonMap("address", clientAddress);
        String jsonMessage = convertToJson(createMessage("clientDisconnect", payload));
        sendToAll(jsonMessage);
    }

    private void sendToAll(String jsonMessage) {
        System.out.println("准备发送 WebSocket 消息给所有会话, sessionsCount=" + sessions.size() + ", payload=" + jsonMessage);
        for (Map.Entry<String, WebSocketSession> e : sessions.entrySet()) {
            WebSocketSession session = e.getValue();
            String sid = e.getKey();
            if (session == null) {
                System.out.println("跳过 null session id=" + sid);
                sessions.remove(sid);
                continue;
            }
            System.out.println("尝试发送到 session=" + sid + ", open=" + session.isOpen() + ", remote=" + session.getRemoteAddress());
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                    System.out.println("发送成功到 session=" + sid);
                } catch (IOException ex) {
                    System.err.println("发送WebSocket消息失败 session=" + sid + ", err=" + ex.getMessage());
                    sessions.remove(sid);
                }
            } else {
                System.out.println("会话未打开，移除 session=" + sid);
                sessions.remove(sid);
            }
        }
        System.out.println("sendToAll 完成, 当前 sessions=" + sessions.size());
    }

    private Map<String, Object> createMessage(String type, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        return message;
    }

    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.println("转换JSON失败: " + e.getMessage());
            return "{}";
        }
    }

    // 调试用：返回当前会话数
    public int getSessionCount() {
        return sessions.size();
    }

    // 调试用：返回当前会话详情（id, open, remoteAddress）
    public List<Map<String, Object>> getSessionDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> e : sessions.entrySet()) {
            WebSocketSession s = e.getValue();
            Map<String, Object> info = new HashMap<>();
            info.put("id", e.getKey());
            info.put("open", s != null && s.isOpen());
            try {
                info.put("remote", s != null ? s.getRemoteAddress() : null);
            } catch (Exception ex) {
                info.put("remote", null);
            }
            list.add(info);
        }
        return list;
    }


}
