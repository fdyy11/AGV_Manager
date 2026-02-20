// src/main/java/com/example/service/TcpClientService.java
package com.example.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TcpClientService {

    private final ConcurrentHashMap<String, Socket> connectedAgvs = new ConcurrentHashMap<>();

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
            System.out.println("成功连接到 AGV: " + ip + ":" + port);

            // 启动一个线程来监听来自AGV的消息
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
        Socket socket = connectedAgvs.get(agvId);
        if (socket == null || socket.isClosed()) {
            throw new RuntimeException("AGV " + agvId + " 未连接或已断开");
        }

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
    }


    /**
     * 监听来自AGV的消息
     */
    private void listenForMessages(String agvId, Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("收到来自 AGV " + agvId + " 的消息: " + message);
                // 这里可以处理接收到的消息，例如更新AGV状态等
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
        }
    }

    /**
     * 获取所有已连接的AGV
     */
    public ConcurrentHashMap<String, Socket> getConnectedAgvs() {
        return connectedAgvs;
    }
}
