package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.service.TcpConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/tcp")
public class TcpController {

    @Autowired
    private TcpConnectionService tcpService;  // 只注入TcpConnectionService

    @PostMapping("/start")
    public Result startTcpServer(@RequestBody Map<String, Object> params) {
        Integer port = (Integer) params.get("port");
        if (port == null) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "端口号不能为空");
        }

        try {
            boolean success = tcpService.startServer(port);
            if (success) {
                return Result.success("TCP服务器启动成功");
            } else {
                return Result.error(ResultCodeEnum.SERVER_ERROR.getCode(), "TCP服务器启动失败");
            }
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SERVER_ERROR.getCode(), "启动过程中发生错误: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public Result stopTcpServer() {
        try {
            tcpService.stopServer();
            return Result.success("TCP服务器已停止");
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SERVER_ERROR.getCode(), "停止过程中发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public Result getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("listening", tcpService.isServerRunning());
        status.put("port", tcpService.getServerPort());
        status.put("startTime", tcpService.getServerStartTime());
        status.put("clientCount", tcpService.getClientCount());
        status.put("lastActivity", tcpService.getLastActivity());

        return Result.success(status);
    }

    @PostMapping("/send")
    public Result sendToClients(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        if (message == null || message.isEmpty()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "消息内容不能为空");
        }

        try {
            tcpService.sendMessageToAllClients(message);
            return Result.success("消息发送成功");
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SERVER_ERROR.getCode(), "发送消息失败: " + e.getMessage());
        }
    }

    @GetMapping("/messages")
    public Result getMessages() {
        try {
            List<String> messages = tcpService.getReceivedMessages();
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SERVER_ERROR.getCode(), "获取消息失败: " + e.getMessage());
        }
    }
}

