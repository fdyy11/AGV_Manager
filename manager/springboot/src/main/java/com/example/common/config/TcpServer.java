package com.example.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TcpServer {
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private List<Socket> clients = Collections.synchronizedList(new ArrayList<>());
    private int port;
    private String startTime;
    private List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());
    private volatile long lastActivityTimestamp = 0;

    @Autowired(required = false) // 设置为非必需，避免循环依赖
    private TcpStatusWebSocketHandler webSocketHandler;

    public void start(int port) throws IOException {
        if (isRunning) {
            // 如果服务器已经在运行，先停止之前的服务器
            stop();
        }

        this.port = port;
        serverSocket = new ServerSocket(port);
        isRunning = true;
        this.startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("TCP服务器开始在端口 " + port + " 上监听");

        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clients.add(clientSocket);
                    System.out.println("新的客户端连接: " + clientSocket.getRemoteSocketAddress());

                    // 启动处理客户端连接的线程
                    handleClient(clientSocket);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("接受客户端连接时出错: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try {
                // 使用缓冲读取器和打印流
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                String clientAddress = socket.getRemoteSocketAddress().toString();
                System.out.println("客户端连接: " + clientAddress);

                // 通知WebSocket客户端已连接 - 使用实例方法调用
                if (webSocketHandler != null) {
                    webSocketHandler.broadcastClientConnect(clientAddress);
                    // 广播更新后的状态
                    broadcastStatus();
                }

                String inputLine;
                while (isRunning && !socket.isClosed()) {
                    try {
                        // 检查是否有可用数据
                        if (reader.ready()) {
                            inputLine = reader.readLine();
                            if (inputLine != null) {
                                // 记录收到的消息
                                String message = "来自 " + clientAddress + ": " + inputLine;
                                receivedMessages.add(message);
                                System.out.println(message);

                                // 通知WebSocket接收到新消息 - 使用实例方法调用
                                if (webSocketHandler != null) {
                                    webSocketHandler.broadcastReceivedMessage(message);
                                }

                                // 更新最后活动时间
                                lastActivityTimestamp = System.currentTimeMillis();

                                // 可以在这里添加对消息的处理逻辑
                                // 例如：解析AGV指令、更新数据库等
                            } else {
                                // 客户端断开连接
                                break;
                            }
                        } else {
                            // 没有数据时短暂休眠，避免过度占用CPU
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("读取客户端数据时出错: " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("处理客户端连接时出错: " + e.getMessage());
            } finally {
                // 确保从客户端列表中移除
                try {
                    clients.remove(socket);
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("关闭客户端连接时出错: " + e.getMessage());
                }

                String clientAddress = socket.getRemoteSocketAddress().toString();
                System.out.println("客户端断开连接: " + clientAddress);

                // 通知WebSocket客户端已断开连接 - 使用实例方法调用
                if (webSocketHandler != null) {
                    webSocketHandler.broadcastClientDisconnect(clientAddress);
                    // 广播更新后的状态
                    broadcastStatus();
                }
            }
        }).start();
    }

    private void broadcastStatus() {
        if (webSocketHandler != null) {
            // 创建状态对象并广播
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("listening", isRunning);
            status.put("port", port);
            status.put("startTime", startTime);
            status.put("clientCount", getClientCount());
            status.put("lastActivity", getLastActivity());

            webSocketHandler.broadcastTcpStatus(status);
        }
    }

    public void stop() {
        isRunning = false;
        try {
            System.out.println("正在停止TCP服务器...");

            // 关闭所有客户端连接
            synchronized (clients) {
                for (Socket client : new ArrayList<>(clients)) { // 创建副本避免并发修改异常
                    try {
                        if (!client.isClosed()) {
                            client.close();
                        }
                    } catch (IOException e) {
                        System.err.println("关闭客户端连接时出错: " + e.getMessage());
                    }
                }
                clients.clear();
            }

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println("TCP服务器已停止");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    public int getPort() {
        return port;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getClientCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    public void sendMessageToAllClients(String message) {
        synchronized (clients) {
            List<Socket> validClients = new ArrayList<>();
            for (Socket client : clients) {
                if (!client.isClosed() && client.isConnected()) {
                    validClients.add(client);
                }
            }

            for (Socket client : validClients) {
                try {
                    PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                    writer.println(message);
                } catch (IOException e) {
                    System.err.println("发送消息给客户端失败: " + e.getMessage());
                    // 移除无效连接
                    clients.remove(client);
                    // 广播更新后的状态
                    broadcastStatus();
                }
            }
        }
    }

    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }

    public String getLastActivity() {
        if (lastActivityTimestamp > 0) {
            return new Date(lastActivityTimestamp).toString();
        }
        return "-";
    }

    private void addLog(String message) {
        // 这里可以添加日志记录逻辑
        System.out.println("[TCP Server] " + message);
    }
}
