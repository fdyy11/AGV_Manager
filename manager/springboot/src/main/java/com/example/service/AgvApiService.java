// src/main/java/com/example/service/AgvApiService.java
package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AGV API 服务类
 * 统一管理所有 AGV API 信号的发送和响应处理
 */
@Service
public class AgvApiService {

    @Resource
    private TcpClientService tcpClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送移动命令到指定坐标
     * @param agvId AGV 编号
     * @param x X 坐标
     * @param y Y 坐标
     * @param theta 角度
     * @return 响应结果
     */
    public Map<String, Object> sendMoveCommand(String agvId, double x, double y, double theta) throws IOException {
        String command = String.format("/api/move?location=%.7f,%.7f,%.7f", x, y, theta);
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送移动命令到标记点
     * @param agvId AGV 编号
     * @param markerName 标记点名称
     * @param uuid 唯一标识
     * @return 响应结果
     */
    public Map<String, Object> sendMoveToMarkerCommand(String agvId, String markerName, String uuid) throws IOException {
        String command = String.format("/api/move?marker=%s&uuid=%s", markerName, uuid);
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送机器人状态查询命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendRobotStatusCommand(String agvId) throws IOException {
        String command = "/api/robot_status";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送紧急停止命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendEmergencyStopCommand(String agvId) throws IOException {
        String command = "/api/emergency_stop";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送复位命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendResetCommand(String agvId) throws IOException {
        String command = "/api/reset";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送充电命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendChargeCommand(String agvId) throws IOException {
        String command = "/api/charge";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送取消当前任务命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendCancelTaskCommand(String agvId) throws IOException {
        String command = "/api/cancel_task";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送获取位置信息命令
     * @param agvId AGV 编号
     * @return 响应结果
     */
    public Map<String, Object> sendGetPositionCommand(String agvId) throws IOException {
        String command = "/api/get_position";
        return sendAndParseResponse(agvId, command);
    }

    /**
     * 发送自定义 API 命令
     * @param agvId AGV 编号
     * @param apiPath API 路径（如 /api/move）
     * @param params 参数 Map
     * @return 响应结果
     */
    public Map<String, Object> sendCustomCommand(String agvId, String apiPath, Map<String, String> params) throws IOException {
        StringBuilder command = new StringBuilder(apiPath);
        
        if (params != null && !params.isEmpty()) {
            command.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    command.append("&");
                }
                command.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        
        return sendAndParseResponse(agvId, command.toString());
    }

    /**
     * 发送命令并解析响应
     * @param agvId AGV 编号
     * @param command 命令字符串
     * @return 解析后的响应数据
     */
    private Map<String, Object> sendAndParseResponse(String agvId, String command) throws IOException {
        System.out.println("========== 发送 AGV 命令 ==========");
        System.out.println("AGV ID: " + agvId);
        System.out.println("命令内容：" + command);
        System.out.println("发送时间：" + new java.util.Date());
        System.out.println("==================================");
        
        // 发送命令
        tcpClientService.sendMessageToAgv(agvId, command);
        
        // 注意：实际响应是通过 listenForMessages 异步接收的
        // 这里返回一个确认信息，实际数据需要前端通过 WebSocket 或轮询获取
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("command", command);
        result.put("agvId", agvId);
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "命令已发送，等待 AGV 响应");
        
        return result;
    }

    /**
     * 处理 AGV 返回的 JSON 响应
     * @param agvId AGV 编号
     * @param jsonResponse JSON 响应字符串
     * @return 解析后的数据
     */
    public Map<String, Object> parseAgvResponse(String agvId, String jsonResponse) {
        System.out.println("解析 AGV 响应 [" + agvId + "]: " + jsonResponse);
        
        Map<String, Object> result = new HashMap<>();
        result.put("agvId", agvId);
        result.put("rawResponse", jsonResponse);
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            // 提取常见字段
            if (jsonNode.has("code")) {
                result.put("code", jsonNode.get("code").asInt());
            }
            if (jsonNode.has("status")) {
                result.put("status", jsonNode.get("status").asText());
            }
            if (jsonNode.has("message")) {
                result.put("message", jsonNode.get("message").asText());
            }
            if (jsonNode.has("data")) {
                result.put("data", parseDataNode(jsonNode.get("data")));
            }
            
            // 特定命令的响应处理 - 根据 type 和 command 判断
            if (jsonNode.has("type")) {
                String type = jsonNode.get("type").asText();
                result.put("responseType", type);
                
                // 如果是 response 类型，根据 command 字段判断具体类型
                if ("response".equals(type) && jsonNode.has("command")) {
                    String command = jsonNode.get("command").asText();
                    System.out.println("收到响应，command: " + command);
                    
                    // ✅ 使用 startsWith 匹配，支持带参数的命令
                    if (command.startsWith("/api/robot_status")) {
                        handleRobotStatus(result, jsonNode);
                    } else if (command.startsWith("/api/move")) {
                        handleMoveResult(result, jsonNode);
                    } else if (command.startsWith("/api/get_position")) {
                        handlePositionInfo(result, jsonNode);
                    } else {
                        System.out.println("未知 command 类型：" + command);
                    }
                } else {
                    // 旧的响应格式，直接根据 type 判断
                    switch (type) {
                        case "move_result":
                            handleMoveResult(result, jsonNode);
                            break;
                        case "robot_status":
                            handleRobotStatus(result, jsonNode);
                            break;
                        case "position_info":
                            handlePositionInfo(result, jsonNode);
                            break;
                        default:
                            System.out.println("未知响应类型：" + type);
                    }
                }
            }
            
            result.put("parseSuccess", true);
            
        } catch (Exception e) {
            System.err.println("解析 JSON 响应失败：" + e.getMessage());
            result.put("parseSuccess", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 解析 data 节点
     */
    private Map<String, Object> parseDataNode(JsonNode dataNode) {
        Map<String, Object> dataMap = new HashMap<>();
        
        if (dataNode.isObject()) {
            dataNode.fields().forEachRemaining(field -> {
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                if (fieldValue.isNumber()) {
                    dataMap.put(fieldName, fieldValue.asDouble());
                } else if (fieldValue.isBoolean()) {
                    dataMap.put(fieldName, fieldValue.asBoolean());
                } else if (fieldValue.isTextual()) {
                    dataMap.put(fieldName, fieldValue.asText());
                } else if (fieldValue.isArray() || fieldValue.isObject()) {
                    dataMap.put(fieldName, fieldValue.toString());
                }
            });
        }
        
        return dataMap;
    }

    /**
     * 处理移动结果响应
     */
    private void handleMoveResult(Map<String, Object> result, JsonNode jsonNode) {
        System.out.println("处理移动结果响应");
        
        // ✅ 提取 results 中的 current_pose 坐标信息（如果存在）
        if (jsonNode.has("results")) {
            JsonNode resultsNode = jsonNode.get("results");
            
            if (resultsNode.has("current_pose")) {
                JsonNode currentPoseNode = resultsNode.get("current_pose");
                
                if (currentPoseNode.has("x")) {
                    double x = currentPoseNode.get("x").asDouble();
                    result.put("positionX", x);
                    System.out.println("✅ 移动响应中包含 X 坐标：" + x);
                }
                if (currentPoseNode.has("y")) {
                    double y = currentPoseNode.get("y").asDouble();
                    result.put("positionY", y);
                    System.out.println("✅ 移动响应中包含 Y 坐标：" + y);
                }
                if (currentPoseNode.has("theta")) {
                    double theta = currentPoseNode.get("theta").asDouble();
                    result.put("theta", theta);
                    System.out.println("✅ 移动响应中包含角度：" + theta);
                }
            }
            
            // 提取电量信息
            if (resultsNode.has("power_percent")) {
                result.put("power_percent", resultsNode.get("power_percent").asDouble());
            }
        }
        
        // 提取移动任务 ID
        if (jsonNode.has("task_id")) {
            String taskId = jsonNode.get("task_id").asText();
            result.put("taskId", taskId);
            System.out.println("移动任务 ID: " + taskId);
        }
        
        // 检查移动状态
        if (jsonNode.has("status")) {
            String status = jsonNode.get("status").asText();
            result.put("moveStatus", status);
            System.out.println("移动状态: " + status);
            
            // 如果移动成功，延迟查询新位置
            if ("OK".equals(status)) {
                System.out.println("✅ 移动命令执行成功，将在 3 秒后查询新位置...");
                // 注意：这里不直接调用，因为需要异步执行
                // 实际的位置查询会通过前端或定时任务触发
            }
        }
        
        // 提取错误信息（如果有）
        if (jsonNode.has("error_message") && !jsonNode.get("error_message").asText().isEmpty()) {
            String errorMsg = jsonNode.get("error_message").asText();
            result.put("errorMessage", errorMsg);
            System.err.println("⚠️ 移动错误: " + errorMsg);
        }
    }

    /**
     * 处理机器人状态响应
     */
    private void handleRobotStatus(Map<String, Object> result, JsonNode jsonNode) {
        System.out.println("处理机器人状态响应");
        
        // 根据你的描述，响应格式应该是：
        // {"type": "response", "command": "/api/robot_status", ..., "results": {...}}
        if (jsonNode.has("results")) {
            JsonNode resultsNode = jsonNode.get("results");
            
            // 提取 current_pose 中的坐标信息
            if (resultsNode.has("current_pose")) {
                JsonNode currentPoseNode = resultsNode.get("current_pose");
                
                if (currentPoseNode.has("x")) {
                    double x = currentPoseNode.get("x").asDouble();
                    result.put("positionX", x);
                    System.out.println("X 坐标：" + x);
                }
                if (currentPoseNode.has("y")) {
                    double y = currentPoseNode.get("y").asDouble();
                    result.put("positionY", y);
                    System.out.println("Y 坐标：" + y);
                }
                if (currentPoseNode.has("theta")) {
                    double theta = currentPoseNode.get("theta").asDouble();
                    result.put("theta", theta);
                    System.out.println("角度：" + theta);
                }
            }
            
            // 提取其他状态信息
            extractStatusField(result, resultsNode, "move_target", "目标点位");
            extractStatusField(result, resultsNode, "move_status", "移动状态");
            extractStatusField(result, resultsNode, "running_status", "运行状态");
            extractStatusField(result, resultsNode, "move_retry_times", "重试次数");
            extractStatusField(result, resultsNode, "charge_state", "充电状态");
            extractStatusField(result, resultsNode, "soft_estop_state", "软急停状态");
            extractStatusField(result, resultsNode, "hard_estop_state", "硬急停状态");
            extractStatusField(result, resultsNode, "estop_state", "急停状态");
            extractStatusField(result, resultsNode, "power_percent", "电量百分比");
            extractStatusField(result, resultsNode, "current_floor", "当前楼层");
            extractStatusField(result, resultsNode, "chargepile_id", "充电桩 ID");
            extractStatusField(result, resultsNode, "error_code", "错误码");
            
        } else if (jsonNode.has("data")) {
            // 兼容旧的响应格式
            JsonNode dataNode = jsonNode.get("data");
            
            // 提取常见的状态字段
            extractStatusField(result, dataNode, "battery", "电量");
            extractStatusField(result, dataNode, "speed", "速度");
            extractStatusField(result, dataNode, "position_x", "X 坐标");
            extractStatusField(result, dataNode, "position_y", "Y 坐标");
            extractStatusField(result, dataNode, "theta", "角度");
            extractStatusField(result, dataNode, "status", "运行状态");
            extractStatusField(result, dataNode, "error_code", "错误代码");
        }
    }

    /**
     * 处理位置信息响应
     */
    private void handlePositionInfo(Map<String, Object> result, JsonNode jsonNode) {
        System.out.println("处理位置信息响应");
        if (jsonNode.has("data")) {
            JsonNode dataNode = jsonNode.get("data");
            
            if (dataNode.has("x")) {
                result.put("positionX", dataNode.get("x").asDouble());
            }
            if (dataNode.has("y")) {
                result.put("positionY", dataNode.get("y").asDouble());
            }
            if (dataNode.has("theta")) {
                result.put("theta", dataNode.get("theta").asDouble());
            }
            if (dataNode.has("map_id")) {
                result.put("mapId", dataNode.get("map_id").asText());
            }
        }
    }

    /**
     * 提取状态字段（支持数字和文本）
     */
    private void extractStatusField(Map<String, Object> result, JsonNode dataNode, String fieldName, String description) {
        if (dataNode.has(fieldName)) {
            JsonNode fieldNode = dataNode.get(fieldName);
            if (fieldNode.isNumber()) {
                result.put(fieldName, fieldNode.asDouble());
            } else if (fieldNode.isTextual()) {
                result.put(fieldName, fieldNode.asText());
            }
            System.out.println(description + ": " + fieldNode.asText());
        }
    }
}
