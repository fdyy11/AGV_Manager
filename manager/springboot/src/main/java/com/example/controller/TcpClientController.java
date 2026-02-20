// src/main/java/com/example/controller/TcpClientController.java
package com.example.controller;

import cn.hutool.core.date.DateUtil;
import com.example.common.Result;
import com.example.entity.Agv;
import com.example.mapper.AgvMapper; // 确保导入正确的 Mapper 类
import com.example.service.TcpClientService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tcp-client")
public class TcpClientController {

    @Resource
    private TcpClientService tcpClientService;

    @Resource
    private AgvMapper agvMapper; // 添加 AgvMapper 的依赖注入

    /**
     * 连接到指定的AGV服务端
     */
    @PostMapping("/connect")
    public Result connectToAgv(@RequestParam(required = false) String agvId,
                               @RequestParam(defaultValue = "127.0.0.1") String ip,
                               @RequestParam(defaultValue = "5555") int port) {

        System.out.println("接收到的连接参数 - IP: " + ip + ", Port: " + port);
        try {
            // 查询是否已存在相同 IP 和 Port 的 AGV 记录
            Agv existingAgv = agvMapper.selectByIpAndPort(ip, port);
            if (existingAgv == null) {
                // 不存在则创建新记录
                if (agvId == null || agvId.isEmpty()) {
                    agvId = generateUniqueAgvId();
                }
                Agv newAgv = new Agv();
                newAgv.setAgvId(agvId);
                newAgv.setIpAddress(ip);
                newAgv.setPort(port);
                newAgv.setStatus("connected");
                newAgv.setLastUpdateTime(new Date());
                agvMapper.insert(newAgv);
            } else {
                // 存在则复用已有记录
                agvId = existingAgv.getAgvId();
                existingAgv.setStatus("connected");
                existingAgv.setLastUpdateTime(new Date());
                agvMapper.updateByAgvId(existingAgv);
            }

            // 建立 TCP 连接
            tcpClientService.connectToAgv(agvId, ip, port);

            // 更新数据库中的状态为 connected
            agvMapper.updateStatusByAgvId(agvId, "connected");

            return Result.success("成功连接到 AGV " + agvId);
        } catch (Exception e) {
            return Result.error("201", "连接失败: " + e.getMessage());
        }
    }




    // 自动生成唯一的 AGV ID
    private String generateUniqueAgvId() {
        String prefix = "AGV"; // 前缀
        int nextId = getNextAgvId(); // 获取下一个可用的编号
        return String.format("%s_%03d", prefix, nextId); // 格式化为 AGV_001, AGV_002 等
    }

    // 获取下一个可用的 AGV ID 编号
    private int getNextAgvId() {
        // 查询数据库中最大的 AGV ID 编号
        List<Agv> allAgvs = agvMapper.selectAll(null);
        int maxId = 0;

        for (Agv agv : allAgvs) {
            if (agv.getAgvId().startsWith("AGV_")) {
                try {
                    int id = Integer.parseInt(agv.getAgvId().substring(4)); // 提取编号部分
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }

        return maxId + 1; // 返回下一个编号
    }



    @PostMapping("/send-test-command")
    public Result sendTestCommand(@RequestParam String agvId) {
        try {
            String testCommand = "/api/move?marker=target_name&uuid=123456";
            tcpClientService.sendMessageToAgv(agvId, testCommand); // 发送 UTF-8 编码的测试命令
            return Result.success("测试命令已发送");
        } catch (Exception e) {
            return Result.error("201", "发送测试命令失败: " + e.getMessage());
        }
    }



    /**
     * 断开与指定AGV的连接
     */

    @PostMapping("/disconnect")
    public Result disconnectFromAgv(@RequestParam String agvId) {
        try {
            System.out.println("=== 开始断开AGV连接 ===");
            System.out.println("AGV ID: " + agvId);
            System.out.println("AGV ID长度: " + (agvId != null ? agvId.length() : "null"));
            
            // 验证参数
            if (agvId == null || agvId.trim().isEmpty()) {
                System.err.println("AGV ID不能为空");
                return Result.error("201", "AGV ID不能为空");
            }
            
            // 先查询当前AGV状态
            Agv currentAgv = agvMapper.selectByAgvId(agvId);
            if (currentAgv != null) {
                System.out.println("当前AGV状态: " + currentAgv.getStatus());
                System.out.println("当前AGV所有信息: " + currentAgv.toString());
            } else {
                System.out.println("未找到AGV记录: " + agvId);
            }
            
            // 先更新数据库状态
            System.out.println("准备更新数据库状态为disconnected");
            agvMapper.updateStatusByAgvId(agvId, "disconnected");
            System.out.println("数据库状态已更新为disconnected");
            
            // 再断开TCP连接
            System.out.println("准备断开TCP连接");
            tcpClientService.disconnectFromAgv(agvId);
            System.out.println("TCP连接已断开");
            
            System.out.println("=== 断开连接完成 ===");

            return Result.success("成功断开与 AGV " + agvId + " 的连接");
        } catch (Exception e) {
            System.err.println("=== 断开连接失败 ===");
            System.err.println("AGV ID: " + agvId);
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return Result.error("201", "断开连接失败: " + e.getMessage());
        }
    }


    /**
     * 向指定AGV发送消息
     */
    @PostMapping("/send")
    public Result sendMessageToAgv(@RequestParam String agvId, @RequestParam String message) {
        try {
            tcpClientService.sendMessageToAgv(agvId, message);
            return Result.success("消息发送成功");
        } catch (Exception e) {
            return Result.error("201", "发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有已连接的AGV
     */
    @GetMapping("/connected-agvs")
    public Result getConnectedAgvs() {
        Map<String, Object> connectedAgvs = tcpClientService.getConnectedAgvs().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of("ip", entry.getValue().getInetAddress().getHostAddress(),
                                "port", entry.getValue().getPort())
                ));
        return Result.success(connectedAgvs);
    }

    @PostMapping("/save-connection")
    public Result saveConnection(@RequestBody Map<String, Object> payload) {
        try {
            String agvId = (String) payload.get("agvId");
            String ip = (String) payload.get("ip");
            Integer port = (Integer) payload.get("port");

            // 更新 AGV 的连接信息（假设存储在数据库中）
            Agv agv = agvMapper.selectByAgvId(agvId);
            if (agv != null) {
                agv.setIpAddress(ip);
                agv.setPort(port);
                agvMapper.updateByAgvId(agv);
                return Result.success("连接信息已保存");
            } else {
                return Result.error("404", "未找到 AGV: " + agvId);
            }
        } catch (Exception e) {
            return Result.error("500", "保存失败: " + e.getMessage());
        }
    }
    @DeleteMapping("/delete/{agvId}")
    public Result deleteAgv(@PathVariable String agvId) {
        try {
            Agv agv = agvMapper.selectByAgvId(agvId);
            if (agv == null) {
                return Result.error("404", "未找到 AGV: " + agvId);
            }

            // 断开连接（如果已连接）
            tcpClientService.disconnectFromAgv(agvId);

            // 删除数据库记录
            agvMapper.deleteById(agv.getId());

            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("500", "删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/all-agvs")
    public Result getAllAgvs() {
        try {
            System.out.println("=== 开始查询所有AGV ===");
            List<Agv> allAgvs = agvMapper.selectAll(null);
            System.out.println("查询到的所有AGV: " + allAgvs.size() + " 个");
            for (Agv agv : allAgvs) {
                System.out.println("AGV: " + agv.getAgvId() + ", 状态: " + agv.getStatus() + ", IP: " + agv.getIpAddress() + ", Port: " + agv.getPort());
            }
            System.out.println("=== AGV查询完成 ===");
            return Result.success(allAgvs);
        } catch (Exception e) {
            System.err.println("查询所有AGV失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("500", "查询失败: " + e.getMessage());
        }
    }
    
    // 测试接口：手动更新AGV状态
    @PostMapping("/test-update-status")
    public Result testUpdateStatus(@RequestParam String agvId, @RequestParam String status) {
        try {
            System.out.println("测试更新状态 - AGV ID: " + agvId + ", 状态: " + status);
            
            // 验证状态值是否在枚举范围内
            String[] validStatus = {"connected", "disconnected", "working", "charging"};
            boolean isValid = false;
            for (String valid : validStatus) {
                if (valid.equals(status)) {
                    isValid = true;
                    break;
                }
            }
            
            if (!isValid) {
                System.err.println("无效的状态值: " + status);
                return Result.error("400", "无效的状态值: " + status + "，有效值为: " + String.join(",", validStatus));
            }
            
            agvMapper.updateStatusByAgvId(agvId, status);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            System.err.println("测试更新状态失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("500", "更新失败: " + e.getMessage());
        }
    }

}
