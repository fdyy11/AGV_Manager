// src/main/java/com/example/service/TcpClientService.java
package com.example.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TcpClientService {
    
    @PostConstruct
    public void init() {
        // 启动连接状态监控任务
        scheduler.scheduleAtFixedRate(this::checkConnectionStatus, 5, 5, TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 检查所有连接状态
     */
    private void checkConnectionStatus() {
        long currentTime = System.currentTimeMillis();
        
        for (String agvId : connectedAgvs.keySet()) {
            Socket socket = connectedAgvs.get(agvId);
            Long lastHeartbeat = lastHeartbeatTime.get(agvId);
            
            // 检查连接是否有效
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                System.out.println("检测到AGV " + agvId + " 连接已断开");
                handleConnectionLost(agvId);
                continue;
            }
            
            // 检查心跳超时
            if (lastHeartbeat != null && (currentTime - lastHeartbeat) > HEARTBEAT_TIMEOUT) {
                System.out.println("AGV " + agvId + " 心跳超时，断开连接");
                handleConnectionLost(agvId);
            }
        }
    }
    
    /**
     * 处理连接丢失
     */
    private void handleConnectionLost(String agvId) {
        try {
            disconnectFromAgv(agvId);
            System.out.println("已处理AGV " + agvId + " 的连接断开");
        } catch (Exception e) {
            System.err.println("处理连接断开失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新心跳时间
     */
    private void updateHeartbeat(String agvId) {
        lastHeartbeatTime.put(agvId, System.currentTimeMillis());
    }

    private final ConcurrentHashMap<String, Socket> connectedAgvs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // WebSocket 会话管理（用于推送 AGV 响应数据到前端）
    private final List<WebSocketSession> websocketSessions = new CopyOnWriteArrayList<>();
    
    @Resource
    private AgvApiService agvApiService; // 注入 AgvApiService
    
    private static final long HEARTBEAT_TIMEOUT = 30000; // 30秒超时
    private static final long HEARTBEAT_INTERVAL = 10000; // 10秒心跳间隔

    /**
     * 连接到指定的AGV服务端
     */
    public void connectToAgv(String agvId, String ip, int port) throws IOException {
        if (connectedAgvs.containsKey(agvId)) {
            throw new RuntimeException("AGV " + agvId + " 已经连接");
        }

        try {
            Socket socket = new Socket(ip, port);
            connectedAgvs.put(agvId, socket);
            updateHeartbeat(agvId); // 初始化心跳时间
            System.out.println("成功连接到 AGV: " + ip + ":" + port);

            // 启动一个线程来监听来自 AGV 的消息
            new Thread(() -> listenForMessages(agvId, socket)).start();
        } catch (IOException e) {
            System.err.println("连接失败: " + e.getMessage());
            throw e; // 抛出异常供上层处理
        }
    }


    /**
     * 断开与指定AGV的连接
     */
    public void disconnectFromAgv(String agvId) throws IOException {
        System.out.println("准备断开AGV: " + agvId + ", 当前连接数: " + connectedAgvs.size());
        
        Socket socket = connectedAgvs.remove(agvId);
        if (socket != null) {
            System.out.println("找到对应的socket连接");
            if (!socket.isClosed()) {
                System.out.println("关闭socket连接");
                socket.close();
            } else {
                System.out.println("socket已经关闭");
            }
        } else {
            System.out.println("未找到对应的socket连接，可能是已经断开或从未连接");
        }
        
        System.out.println("断开操作完成，剩余连接数: " + connectedAgvs.size());
    }

    /**
     * 向指定AGV发送消息
     */
    public void sendMessageToAgv(String agvId, String message) throws IOException {
        System.out.println("准备向 AGV " + agvId + " 发送消息: " + message);
        
        Socket socket = connectedAgvs.get(agvId);
        if (socket == null) {
            System.err.println("错误：找不到AGV " + agvId + " 的socket连接");
            throw new RuntimeException("AGV " + agvId + " 未连接");
        }
        
        if (socket.isClosed()) {
            System.err.println("错误：AGV " + agvId + " 的socket连接已关闭");
            throw new RuntimeException("AGV " + agvId + " 连接已断开");
        }
        
        System.out.println("Socket状态正常，开始发送数据...");
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
        System.out.println("消息发送完成");
    }


    /**
     * 处理 AGV 返回的 JSON 响应
     */
    private void handleAgvResponse(String agvId, String jsonResponse) {
        try {
            // 使用 AgvApiService 解析响应
            Map<String, Object> parsedData = agvApiService.parseAgvResponse(agvId, jsonResponse);
            
            // 通过 WebSocket 推送给前端
            broadcastToWebsocket(parsedData);
            
        } catch (Exception e) {
            System.err.println("处理 AGV 响应失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册 WebSocket 会话
     */
    public void registerWebSocketSession(WebSocketSession session) {
        if (!websocketSessions.contains(session)) {
            websocketSessions.add(session);
            System.out.println("WebSocket 会话已注册，当前会话数：" + websocketSessions.size());
        }
    }
    
    /**
     * 移除 WebSocket 会话
     */
    public void removeWebSocketSession(WebSocketSession session) {
        websocketSessions.remove(session);
        System.out.println("WebSocket 会话已移除，当前会话数：" + websocketSessions.size());
    }
    
    /**
     * 广播消息到所有 WebSocket 会话
     */
    private void broadcastToWebsocket(Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(data);
            TextMessage message = new TextMessage(jsonMessage);
            
            for (WebSocketSession session : websocketSessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            System.err.println("广播 WebSocket 消息失败：" + e.getMessage());
        }
    }
    
    /**
     * 监听来自AGV的消息
     */
    private void listenForMessages(String agvId, Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("收到来自 AGV " + agvId + " 的消息: " + message);
                updateHeartbeat(agvId); // 更新心跳时间
                
                // 尝试解析为JSON响应
                if (message.trim().startsWith("{")) {
                    handleAgvResponse(agvId, message);
                }
            }
        } catch (IOException e) {
            System.err.println("监听 AGV " + agvId + " 消息时出错: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("关闭 AGV " + agvId + " 连接时出错: " + e.getMessage());
            }
            connectedAgvs.remove(agvId);
            lastHeartbeatTime.remove(agvId);
            System.out.println("AGV " + agvId + " 连接已完全断开");
        }
    }

    /**
     * 获取所有已连接的AGV
     */
    public ConcurrentHashMap<String, Socket> getConnectedAgvs() {
        return connectedAgvs;
    }
    
    /**
     * 检查指定AGV是否连接
     */
    public boolean isAgvConnected(String agvId) {
        Socket socket = connectedAgvs.get(agvId);
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    /**
     * 获取连接统计信息
     */
    public String getConnectionStats() {
        return String.format("当前连接数: %d, 监控的心跳数: %d", 
                           connectedAgvs.size(), lastHeartbeatTime.size());
    }
}
