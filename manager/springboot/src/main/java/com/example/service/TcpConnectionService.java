package com.example.service;

import com.example.common.config.TcpStatusWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TcpConnectionService {

    @Autowired
    private TcpStatusWebSocketHandler webSocketHandler;

    private ServerSocket serverSocket;
    private volatile boolean serverRunning = false;
    private int serverPort;
    private String serverStartTime;
    private final List<Socket> clientSockets = new CopyOnWriteArrayList<>();
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
    private final AtomicInteger clientCount = new AtomicInteger(0);
    // 添加客户端连接映射表
    private final Map<String, Socket> clientConnections = new ConcurrentHashMap<>();

    public boolean startServer(int port) {
        try {
            if (serverRunning) {
                stopServer();
            }

            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));

            serverRunning = true;
            this.serverPort = port;
            this.serverStartTime = new java.util.Date(System.currentTimeMillis()).toString();

            Thread serverThread = new Thread(this::acceptConnections, "TcpServer-AcceptThread");
            serverThread.setDaemon(true);
            serverThread.start();

            System.out.println("TCP 服务器已启动并监听 0.0.0.0:" + port);
            broadcastStatus();
            return true;
        } catch (IOException e) {
            serverRunning = false;
            System.err.println("启动服务器失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void acceptConnections() {
        System.out.println("Accept 线程已启动");
        while (serverRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setKeepAlive(true);

                clientSockets.add(clientSocket);

                // 生成客户端ID并添加到映射表
                String clientId = generateClientId(clientSocket);
                clientConnections.put(clientId, clientSocket);

                int cur = clientCount.incrementAndGet();
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress() + ", clientCount=" + cur);

                broadcastStatus();

                if (webSocketHandler != null) {
                    try {
                        System.out.println("尝试通过 webSocketHandler 广播 clientConnect");
                        webSocketHandler.broadcastClientConnect(clientSocket.getRemoteSocketAddress().toString());
                        System.out.println("广播 clientConnect 完成");
                    } catch (Exception ex) {
                        System.err.println("广播 clientConnect 时异常: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("webSocketHandler 为 null，无法广播 clientConnect");
                }

                Thread clientThread = new Thread(() -> handleClient(clientSocket), "TcpServer-ClientHandler-" + clientSocket.getPort());
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (SocketException se) {
                if (serverRunning) {
                    System.err.println("接受连接时 SocketException: " + se.getMessage());
                    se.printStackTrace();
                } else {
                    System.out.println("Accept 线程因服务器停止而退出");
                }
                break;
            } catch (IOException e) {
                System.err.println("接受连接时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Accept 线程结束");
    }

    private void handleClient(Socket socket) {
        System.out.println("Client handler 启动 for " + socket.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = reader.readLine()) != null && serverRunning && !socket.isClosed()) {
                String fullMessage = "来自 " + socket.getRemoteSocketAddress() + ": " + message;
                receivedMessages.add(fullMessage);
                System.out.println("收到消息: " + fullMessage);

                if (webSocketHandler != null) {
                    try {
                        System.out.println("尝试通过 webSocketHandler 广播 receivedMessage");
                        webSocketHandler.broadcastReceivedMessage(fullMessage);
                        System.out.println("广播 receivedMessage 完成");
                    } catch (Exception ex) {
                        System.err.println("广播 receivedMessage 时异常: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("webSocketHandler 为 null，无法广播 receivedMessage");
                }
            }
            System.out.println("客户端读到 EOF 或关闭: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("处理客户端消息时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanupClient(socket);
        }
    }

    private void cleanupClient(Socket socket) {
        boolean removed = clientSockets.remove(socket);

        // 从客户端连接映射表中移除
        String clientId = findClientIdBySocket(socket);
        if (clientId != null) {
            clientConnections.remove(clientId);
        }

        if (removed) {
            int current = clientCount.decrementAndGet();
            System.out.println("客户端断开: " + socket.getRemoteSocketAddress() + ", clientCount=" + current);
        } else {
            System.out.println("断开但未在列表中找到 socket: " + socket.getRemoteSocketAddress());
        }

        if (webSocketHandler != null) {
            try {
                System.out.println("尝试通过 webSocketHandler 广播 clientDisconnect");
                webSocketHandler.broadcastClientDisconnect(socket.getRemoteSocketAddress().toString());
                System.out.println("广播 clientDisconnect 完成");
            } catch (Exception ex) {
                System.err.println("广播 clientDisconnect 时异常: " + ex.getMessage());
                ex.printStackTrace();
            }
            broadcastStatus();
        } else {
            System.err.println("webSocketHandler 为 null，无法广播 clientDisconnect");
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭客户端连接时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastStatus() {
        if (webSocketHandler != null) {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("listening", serverRunning);
            status.put("port", serverPort);
            status.put("startTime", serverStartTime);
            status.put("clientCount", clientCount.get());
            status.put("lastActivity", getLastActivity());
            try {
                System.out.println("尝试广播 tcp 状态: " + status);
                webSocketHandler.broadcastTcpStatus(status);
                System.out.println("广播 tcp 状态 完成");
            } catch (Exception ex) {
                System.err.println("广播状态失败: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            System.err.println("webSocketHandler 为 null，无法广播状态");
        }
    }

    public void stopServer() {
        serverRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            for (Socket socket : clientSockets) {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("关闭客户端时出错: " + e.getMessage());
                }
            }

            clientSockets.clear();
            clientConnections.clear(); // 清空客户端连接映射表
            clientCount.set(0);
            broadcastStatus();
            System.out.println("TCP 服务器已停止");
        } catch (IOException e) {
            System.err.println("停止服务器时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isServerRunning() {
        return serverRunning;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerStartTime() {
        return serverStartTime;
    }

    public int getClientCount() {
        return clientCount.get();
    }

    public String getLastActivity() {
        if (!receivedMessages.isEmpty()) {
            return receivedMessages.get(receivedMessages.size() - 1);
        }
        return "无活动记录";
    }

    public void sendMessageToAllClients(String message) {
        for (Socket socket : clientSockets) {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                clientSockets.remove(socket);
                clientCount.set(clientSockets.size());
                broadcastStatus();
                continue;
            }
            try {
                OutputStream out = socket.getOutputStream();
                synchronized (out) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                    writer.write(message);
                    writer.write("\r\n");
                    writer.flush();
                }
            } catch (IOException e) {
                System.err.println("发送消息到客户端失败: " + e.getMessage());
                try {
                    socket.close();
                } catch (IOException ex) {
                    // ignore
                }
                clientSockets.remove(socket);
                clientCount.set(clientSockets.size());
                broadcastStatus();
            }
        }
    }

    public List<String> getReceivedMessages() {
        return new CopyOnWriteArrayList<>(receivedMessages);
    }

    @PreDestroy
    public void destroy() {
        stopServer();
    }

    // 生成客户端ID的方法
    private String generateClientId(Socket socket) {
        return socket.getRemoteSocketAddress().toString() + "_" + System.currentTimeMillis();
    }

    // 根据Socket查找客户端ID
    private String findClientIdBySocket(Socket socket) {
        for (Map.Entry<String, Socket> entry : clientConnections.entrySet()) {
            if (entry.getValue().equals(socket)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // 发送消息给特定客户端
    public void sendMessageToSpecificClient(String clientId, String message) {
        if (clientConnections.containsKey(clientId)) {
            Socket clientSocket = clientConnections.get(clientId);
            try {
                if (!clientSocket.isClosed() && clientSocket.isConnected()) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println(message);
                    out.flush();
                } else {
                    // 如果连接已断开，从映射表中移除
                    clientConnections.remove(clientId);
                }
            } catch (IOException e) {
                System.err.println("发送消息到特定客户端失败: " + e.getMessage());
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    // ignore
                }
                clientConnections.remove(clientId);
            }
        } else {
            System.err.println("客户端ID不存在: " + clientId);
        }
    }
}
