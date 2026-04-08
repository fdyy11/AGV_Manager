// src/main/java/com/example/service/TcpClientService.java
package com.example.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
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
        System.out.println("=== TcpClientService 初始化 ===");
        System.out.println("Scheduler 线程池状态：已创建");
        System.out.println("启动连接状态监控任务...");
        // 启动连接状态监控任务
        scheduler.scheduleAtFixedRate(this::checkConnectionStatus, 5, 5, TimeUnit.SECONDS);
        System.out.println("初始化完成");
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
            Long lastActivity = lastActivityTime.get(agvId);
                
            // 检查连接是否有效
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                System.out.println("检测到 AGV " + agvId + " 连接已断开");
                handleConnectionLost(agvId);
                continue;
            }
                
            // 检查心跳超时（但如果最近有活动，则放宽限制）
            if (lastHeartbeat != null && (currentTime - lastHeartbeat) > HEARTBEAT_TIMEOUT) {
                // 如果最近 30 秒内有活动（发送过命令），则不立即断开
                if (lastActivity != null && (currentTime - lastActivity) < 30000) {
                    System.out.println("AGV " + agvId + " 心跳超时，但最近有命令活动，暂不断开");
                    continue;
                }
                    
                long timeoutSeconds = (currentTime - lastHeartbeat) / 1000;
                System.out.println("AGV " + agvId + " 心跳超时，断开连接");
                System.out.println("最后心跳时间：" + new Date(lastHeartbeat));
                System.out.println("当前时间：" + new Date(currentTime));
                System.out.println("超时时长：" + timeoutSeconds + "秒");
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
        long currentTime = System.currentTimeMillis();
        lastHeartbeatTime.put(agvId, currentTime);
        lastActivityTime.put(agvId, currentTime); // 同时更新活动时间
    }
    
    /**
     * 更新活动时间（发送消息时调用）
     */
    private void updateActivityTime(String agvId) {
        lastActivityTime.put(agvId, System.currentTimeMillis());
    }

    private final ConcurrentHashMap<String, Socket> connectedAgvs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // WebSocket 会话管理（用于推送 AGV 响应数据到前端）
    private final List<WebSocketSession> websocketSessions = new CopyOnWriteArrayList<>();
    
    @Resource
    private AgvApiService agvApiService; // 注入 AgvApiService
    
    @Resource
    private com.example.mapper.AgvMapper agvMapper; // 注入 AgvMapper 用于更新坐标信息
    
    private static final long HEARTBEAT_TIMEOUT = 120000; // 120 秒超时（给 AGV 更多响应时间）
    private static final long HEARTBEAT_INTERVAL = 15000; // 15 秒心跳间隔
    
    // 记录每个 AGV 最后的活动时间（包括发送和接收）
    private final ConcurrentHashMap<String, Long> lastActivityTime = new ConcurrentHashMap<>();

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
            System.err.println("连接失败：" + e.getMessage());
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
        
        System.out.println("Socket 状态正常，开始发送数据...");
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
        out.flush(); // 确保数据被发送
        
        // 更新活动时间（重要！防止心跳超时）
        updateActivityTime(agvId);
        
        System.out.println("消息发送完成，等待 AGV 响应...");
        System.out.println("已更新 AGV " + agvId + " 的活动时间：" + new Date(lastActivityTime.get(agvId)));
        
        // 添加调试信息：检查 socket 输入流是否就绪
        try {
            System.out.println("[调试] 检查 Socket 输入流状态...");
            System.out.println("[调试] Socket InputStream: " + socket.getInputStream());
            System.out.println("[调试] Socket available bytes: " + socket.getInputStream().available());
            if (socket.getInputStream().available() > 0) {
                System.out.println("[调试] ⚠️ 发现有待读取的数据！");
            } else {
                System.out.println("[调试] 当前没有待读取的数据，监听线程正在等待中...");
            }
        } catch (Exception e) {
            System.err.println("[调试] 检查输入流失败：" + e.getMessage());
        }
    }


    /**
     * 处理 AGV 返回的 JSON 响应
     */
    private void handleAgvResponse(String agvId, String jsonResponse) {
        System.out.println("[处理响应] ========== 开始处理 AGV " + agvId + " 的 JSON 响应 ==========");
        System.out.println("[处理响应] JSON 内容：" + jsonResponse);
        
        try {
            // 使用 AgvApiService 解析响应
            System.out.println("[处理响应] 调用 AgvApiService.parseAgvResponse...");
            Map<String, Object> parsedData = agvApiService.parseAgvResponse(agvId, jsonResponse);
            System.out.println("[处理响应] 解析完成，得到 " + parsedData.size() + " 个字段");
            System.out.println("[处理响应] 解析后的数据：" + parsedData);
            
            // ✅ 更新电量信息到数据库（如果有）
            if (parsedData.containsKey("power_percent")) {
                Double powerPercent = (Double) parsedData.get("power_percent");
                System.out.println("[处理响应] ✓ 检测到电量信息：" + powerPercent + "%");
                updateAgvBatteryLevel(agvId, powerPercent.intValue());
            }
            
            // ✅ 检查是否是移动命令的响应，如果是且成功，则延迟查询新位置
            if (parsedData.containsKey("moveStatus") && "OK".equals(parsedData.get("moveStatus"))) {
                System.out.println("[处理响应] ✓ 移动命令执行成功，准备查询新位置...");
                // 延迟 3 秒后查询位置（给 AGV 时间移动）
                scheduler.schedule(() -> {
                    try {
                        System.out.println("[自动查询] 开始查询 AGV " + agvId + " 的新位置...");
                        String positionCommand = "/api/robot_status";
                        sendMessageToAgv(agvId, positionCommand);
                        System.out.println("[自动查询] 已发送位置查询命令");
                    } catch (Exception e) {
                        System.err.println("[自动查询] 查询位置失败: " + e.getMessage());
                    }
                }, 3, TimeUnit.SECONDS);
            }
            
            // 检查是否包含坐标信息
            if (parsedData.containsKey("positionX") && parsedData.containsKey("positionY")) {
                System.out.println("[处理响应] ✓ 检测到坐标信息");
                Double x = (Double) parsedData.get("positionX");
                Double y = (Double) parsedData.get("positionY");
                Double theta = (Double) parsedData.get("theta");
                
                System.out.println("[处理响应] 坐标值：X=" + x + ", Y=" + y + ", Theta=" + theta);
                
                // 更新数据库中的 AGV 坐标信息
                System.out.println("[处理响应] 准备更新数据库...");
                updateAgvPosition(agvId, x, y, theta);
                System.out.println("[处理响应] 数据库更新完成");
                
                // 将坐标信息添加到推送数据中
                parsedData.put("agvId", agvId);
                parsedData.put("type", "agvPosition");
                System.out.println("[处理响应] 设置为位置更新类型");
            } else {
                System.out.println("[处理响应] ✗ 未检测到坐标信息，作为状态更新处理");
                // 如果不包含坐标信息，则作为状态更新推送
                parsedData.put("agvId", agvId);
                parsedData.put("type", "agvStatus");
            }
            
            // 添加 payload 字段，方便前端直接使用
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", parsedData.get("type"));
            pushData.put("payload", parsedData);
            
            System.out.println("[处理响应] 准备推送到 WebSocket，数据类型：" + parsedData.get("type"));
            System.out.println("[处理响应] 推送数据：" + pushData);
            
            // 通过 WebSocket 推送给前端
            broadcastToWebsocket(pushData);
            
            System.out.println("[WebSocket] ✓ 推送数据到前端完成：type=" + parsedData.get("type"));
            System.out.println("[处理响应] ========== AGV 响应处理完成 ==========");
            
        } catch (Exception e) {
            System.err.println("[处理响应] ✗ 处理 AGV 响应失败：" + e.getMessage());
            System.err.println("[处理响应] 错误类型：" + e.getClass().getName());
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
        System.out.println("[WebSocket] ========== 开始推送数据到前端 ==========");
        System.out.println("[WebSocket] 准备推送数据：" + data);
        System.out.println("[WebSocket] 当前会话数：" + websocketSessions.size());
        
        if (websocketSessions.isEmpty()) {
            System.out.println("[WebSocket] ⚠️ 警告：当前没有任何 WebSocket 会话连接！");
            System.out.println("[WebSocket] 请检查前端是否正确连接到 WebSocket");
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(data);
            TextMessage message = new TextMessage(jsonMessage);
            
            int successCount = 0;
            int failCount = 0;
            int notOpenCount = 0;
            
            System.out.println("[WebSocket] JSON 消息长度：" + jsonMessage.length() + " 字符");
            
            for (WebSocketSession session : websocketSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                        successCount++;
                        System.out.println("[WebSocket] ✓ 发送成功到会话 " + session.getId());
                    } catch (Exception ex) {
                        failCount++;
                        System.err.println("[WebSocket] ✗ 发送失败到会话 " + session.getId() + ": " + ex.getMessage());
                        System.err.println("[WebSocket] 错误类型：" + ex.getClass().getName());
                    }
                } else {
                    notOpenCount++;
                    System.out.println("[WebSocket] ⚠️ 会话未打开：" + session.getId());
                }
            }
            
            System.out.println("[WebSocket] ========== 推送完成 ==========");
            System.out.println("[WebSocket] 总计：" + websocketSessions.size() + " 个会话");
            System.out.println("[WebSocket] 成功：" + successCount + ", 失败：" + failCount + ", 未打开：" + notOpenCount);
        } catch (Exception e) {
            System.err.println("[WebSocket] ✗ 广播 WebSocket 消息失败：" + e.getMessage());
            System.err.println("[WebSocket] 错误类型：" + e.getClass().getName());
            e.printStackTrace();
        }
    }
    
    /**
     * 监听来自 AGV 的消息
     */
    private void listenForMessages(String agvId, Socket socket) {
        try {
            System.out.println("[监听线程] 开始监听 AGV " + agvId + " 的消息...");
            System.out.println("[监听线程] Socket 输入流可用：" + (socket.getInputStream() != null));
                
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                int messageCount = 0;
                    
                System.out.println("[监听线程] 准备读取消息，等待 AGV 响应...");
                System.out.println("[监听线程] === 注意：如果长时间没有收到消息，可能是 AGV 没有响应 ===");
                    
                while ((message = in.readLine()) != null) {
                    messageCount++;
                    System.out.println("========================================");
                    System.out.println("[监听线程] 收到第 " + messageCount + " 条来自 AGV " + agvId + " 的消息：");
                    System.out.println("[原始消息] " + message);
                    System.out.println("[消息长度] " + message.length() + " 字符");
                    System.out.println("[消息类型] " + (message.trim().startsWith("{") ? "JSON" : "文本"));
                    updateHeartbeat(agvId); // 更新心跳时间
                                    
                    // 尝试解析为 JSON 响应
                    if (message.trim().startsWith("{")) {
                        System.out.println("[JSON] 开始处理 AGV JSON 响应...");
                        handleAgvResponse(agvId, message);
                        System.out.println("[JSON] AGV 响应处理完成");
                    } else {
                        System.out.println("[TEXT] AGV 返回的是文本消息，非 JSON 格式");
                        System.out.println("[TEXT] 将推送到前端显示");
                        // 如果是文本消息，也推送到前端
                        Map<String, Object> textData = new java.util.HashMap<>();
                        textData.put("agvId", agvId);
                        textData.put("type", "textMessage");
                        textData.put("message", message);
                        System.out.println("[推送] 准备推送文本消息到前端");
                        broadcastToWebsocket(textData);
                        System.out.println("[推送] 文本消息推送完成");
                    }
                    System.out.println("========================================");
                }
                    
                System.out.println("[监听线程] AGV " + agvId + " 连接关闭，共收到 " + messageCount + " 条消息");
                if (messageCount == 0) {
                    System.out.println("[监听线程] ⚠️ 警告：直到连接断开都没有收到任何消息！");
                    System.out.println("[监听线程] 可能原因：");
                    System.out.println("[监听线程]   1. AGV 服务端没有发送响应");
                    System.out.println("[监听线程]   2. 网络问题导致消息丢失");
                    System.out.println("[监听线程]   3. AGV 服务端处理超时");
                }
                    
            } catch (IOException e) {
                String errMsg = e.getMessage();
                // Socket closed 是正常现象，不需要打印完整堆栈
                if ("Socket closed".equals(errMsg) || "Connection reset".equals(errMsg)) {
                    System.out.println("[监听线程] AGV " + agvId + " 连接正常关闭：" + errMsg);
                } else {
                    System.err.println("[监听线程] 监听 AGV " + agvId + " 消息时出错：" + errMsg);
                    System.err.println("[监听线程] 错误类型：" + e.getClass().getName());
                    e.printStackTrace();
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("[监听线程] 关闭 AGV " + agvId + " 连接时出错：" + e.getMessage());
                }
                connectedAgvs.remove(agvId);
                lastHeartbeatTime.remove(agvId);
                lastActivityTime.remove(agvId);
                System.out.println("[监听线程] AGV " + agvId + " 连接已完全断开");
            }
        } catch (Exception e) {
            System.err.println("[监听线程] 初始化失败：" + e.getMessage());
            e.printStackTrace();
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
        return String.format("当前连接数：%d, 监控的心跳数：%d", 
                           connectedAgvs.size(), lastHeartbeatTime.size());
    }
    
    /**
     * 更新 AGV 的位置信息到数据库
     */
    private void updateAgvPosition(String agvId, Double x, Double y, Double theta) {
        try {
            // 查询 AGV 记录
            com.example.entity.Agv agv = agvMapper.selectByAgvId(agvId);
            if (agv != null) {
                // 更新坐标信息
                agv.setCurrentX(x);
                agv.setCurrentY(y);
                agv.setCurrentTheta(theta);
                agv.setLastUpdateTime(new Date());
                agvMapper.updateByAgvId(agv);
                System.out.println("已更新 AGV " + agvId + " 坐标：X=" + x + ", Y=" + y);
            } else {
                System.err.println("未找到 AGV 记录：" + agvId);
            }
        } catch (Exception e) {
            System.err.println("更新 AGV 位置失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新 AGV 的电量信息到数据库
     */
    private void updateAgvBatteryLevel(String agvId, Integer batteryLevel) {
        try {
            // 查询 AGV 记录
            com.example.entity.Agv agv = agvMapper.selectByAgvId(agvId);
            if (agv != null) {
                // 更新电量信息
                agv.setBatteryLevel(batteryLevel);
                agv.setLastUpdateTime(new Date());
                agvMapper.updateByAgvId(agv);
                System.out.println("✅ 已更新 AGV " + agvId + " 电量：" + batteryLevel + "%");
            } else {
                System.err.println("⚠️ 未找到 AGV 记录：" + agvId);
            }
        } catch (Exception e) {
            System.err.println("❌ 更新 AGV 电量失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
