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
            
            // 特定命令的响应处理
            if (jsonNode.has("type")) {
                String type = jsonNode.get("type").asText();
                result.put("responseType", type);
                
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
        if (jsonNode.has("data")) {
            JsonNode dataNode = jsonNode.get("data");
            if (dataNode.has("success")) {
                result.put("moveSuccess", dataNode.get("success").asBoolean());
            }
            if (dataNode.has("target_location")) {
                result.put("targetLocation", dataNode.get("target_location").asText());
            }
        }
    }

    /**
     * 处理机器人状态响应
     */
    private void handleRobotStatus(Map<String, Object> result, JsonNode jsonNode) {
        System.out.println("处理机器人状态响应");
        if (jsonNode.has("data")) {
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
