// java
package com.example.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TcpStatusWebSocketHandler tcpStatusWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("=== WebSocket 配置初始化 ===");
        System.out.println("注册 WebSocket 处理器：TcpStatusWebSocketHandler");
        System.out.println("路径：/ws/tcp-status");
        registry.addHandler(tcpStatusWebSocketHandler, "/ws/tcp-status")
                .setAllowedOrigins("*");
        System.out.println("WebSocket 配置完成");
    }
}
