# AGV API 信号发送与响应处理模块说明

## 一、后端部分

### 1. 核心服务类：`AgvApiService.java`

**位置**: `manager/springboot/src/main/java/com/example/service/AgvApiService.java`

**功能**: 统一管理所有 AGV API 信号的发送和响应解析

#### 提供的主要方法：

| 方法名 | 功能 | 参数 | 返回 |
|--------|------|------|------|
| `sendMoveCommand` | 发送移动命令到指定坐标 | agvId, x, y, theta | Map<String, Object> |
| `sendMoveToMarkerCommand` | 发送移动命令到标记点 | agvId, markerName, uuid | Map<String, Object> |
| `sendRobotStatusCommand` | 查询机器人状态 | agvId | Map<String, Object> |
| `sendEmergencyStopCommand` | 紧急停止 | agvId | Map<String, Object> |
| `sendResetCommand` | 系统复位 | agvId | Map<String, Object> |
| `sendChargeCommand` | 充电命令 | agvId | Map<String, Object> |
| `sendCancelTaskCommand` | 取消当前任务 | agvId | Map<String, Object> |
| `sendGetPositionCommand` | 获取位置信息 | agvId | Map<String, Object> |
| `sendCustomCommand` | 发送自定义 API 命令 | agvId, apiPath, params | Map<String, Object> |

#### 响应解析功能：

- **自动解析 JSON 响应**: 解析 AGV 服务端返回的 JSON 数据
- **字段提取**: 提取常见字段如 code, status, message, data 等
- **类型识别**: 根据响应类型 (move_result, robot_status, position_info) 进行专门处理
- **数据格式化**: 将解析后的数据格式化为前端友好的格式

### 2. TCP 服务增强：`TcpClientService.java`

**新增功能**:
- 注入 `AgvApiService` 用于处理响应
- 添加 WebSocket 会话管理，用于推送 AGV 响应到前端
- 增强 `handleAgvResponse` 方法，自动调用解析服务并广播

### 3. 控制器接口：`TcpClientController.java`

**新增 RESTful API 接口**:

```
POST /tcp-client/api/move-to-coord
参数：agvId, x, y, theta
功能：发送移动命令到指定坐标

POST /tcp-client/api/move-to-marker
参数：agvId, marker, uuid(可选)
功能：发送移动命令到标记点

POST /tcp-client/api/robot-status
参数：agvId
功能：查询机器人状态

POST /tcp-client/api/emergency-stop
参数：agvId
功能：紧急停止

POST /tcp-client/api/reset
参数：agvId
功能：系统复位

POST /tcp-client/api/charge
参数：agvId
功能：充电命令

POST /tcp-client/api/cancel-task
参数：agvId
功能：取消当前任务

POST /tcp-client/api/get-position
参数：agvId
功能：获取位置信息

POST /tcp-client/api/custom
参数：agvId, apiPath, params(可选)
功能：发送自定义 API 命令
```

## 二、前端部分

### 1. AGV 控制面板组件：`AgvControlPanel.vue`

**位置**: `manager/vue/src/views/manager/AgvControlPanel.vue`

**功能特性**:

#### (1) AGV 选择
- 下拉选择已连接的 AGV
- 显示 AGV 的 IP 和端口信息
- 实时刷新 AGV 列表

#### (2) 快捷命令区域
提供一键发送常用命令的按钮：
- 🔍 查询状态
- 📍 获取位置
- 🛑 紧急停止
- 🔄 系统复位
- 🔌 充电
- ❌ 取消任务

#### (3) 移动控制区域

**坐标移动**:
- 输入 X, Y, Theta（角度）坐标
- 支持小数，可调节精度和步长
- 一键发送到 AGV

**标记点移动**:
- 输入标记点名称
- UUID 自动生成（也可手动输入）
- 发送到指定标记点

#### (4) 自定义命令
- 自由输入 API 路径（如 `/api/test`）
- 输入参数，格式：`key1=value1&key2=value2`
- 发送任意自定义 API 命令

#### (5) 响应日志
- **实时显示**: 通过 WebSocket 接收 AGV 响应
- **分类显示**: 
  - 蓝色：发送的命令
  - 绿色：接收到的成功响应
  - 红色：错误信息
  - 灰色：系统消息
- **详细信息**: 显示时间戳、命令类型、响应数据
- **清空功能**: 一键清空所有日志

### 2. WebSocket 实时更新

**连接地址**: `ws://localhost:8080/ws/tcp-status`

**工作流程**:
1. 前端建立 WebSocket 连接
2. 后端 AGV 返回 JSON 响应
3. `AgvApiService` 解析响应数据
4. 通过 WebSocket 推送给前端
5. 前端自动添加到日志显示

## 三、使用示例

### 后端使用（Java）

```java
@Resource
private AgvApiService agvApiService;

// 发送移动命令
try {
    Map<String, Object> result = agvApiService.sendMoveCommand(
        "AGV_001", 15.0, 4.0, 1.5707963
    );
    System.out.println("命令发送成功：" + result);
} catch (IOException e) {
    e.printStackTrace();
}

// 发送自定义命令
try {
    Map<String, String> params = new HashMap<>();
    params.put("location", "15.0,4.0,1.5707963");
    Map<String, Object> result = agvApiService.sendCustomCommand(
        "AGV_001", "/api/move", params
    );
} catch (IOException e) {
    e.printStackTrace();
}
```

### 前端使用（Vue）

```javascript
// 发送移动命令到坐标
async sendMoveToCoord() {
  try {
    const response = await request.post('/tcp-client/api/move-to-coord', null, {
      params: {
        agvId: 'AGV_001',
        x: 15.0,
        y: 4.0,
        theta: 1.5707963
      }
    });
    this.$message.success('移动命令已发送');
  } catch (error) {
    this.$message.error('发送失败');
  }
}

// 发送查询状态命令
async sendRobotStatus() {
  try {
    const response = await request.post('/tcp-client/api/robot-status', null, {
      params: { agvId: 'AGV_001' }
    });
    console.log('AGV 状态:', response.data);
  } catch (error) {
    console.error('查询失败:', error);
  }
}
```

## 四、数据格式

### 发送的命令格式

```
/api/move?location=15.0,4.0,1.5707963
/api/move?marker=target_name&uuid=123456
/api/robot_status
/api/emergency_stop
```

### AGV 响应示例

```json
{
  "code": 200,
  "status": "success",
  "message": "命令执行成功",
  "type": "robot_status",
  "data": {
    "battery": 85.5,
    "speed": 1.2,
    "position_x": 10.5,
    "position_y": 5.3,
    "theta": 0.785,
    "status": "working",
    "error_code": 0
  }
}
```

### 解析后的数据

```json
{
  "agvId": "AGV_001",
  "rawResponse": "{...}",
  "timestamp": 1234567890,
  "code": 200,
  "status": "success",
  "message": "命令执行成功",
  "responseType": "robot_status",
  "battery": 85.5,
  "speed": 1.2,
  "positionX": 10.5,
  "positionY": 5.3,
  "theta": 0.785,
  "parseSuccess": true
}
```

## 五、访问方式

1. 启动后端服务（Spring Boot）
2. 启动前端服务（Vue）
3. 访问路由：`/agv-control-panel`
4. 在左侧菜单中点击"AGV 控制面板"

## 六、优势特点

1. **统一管理**: 所有 API 信号集中在一个服务类中，便于维护和扩展
2. **自动解析**: 自动解析 JSON 响应，提取关键信息
3. **实时推送**: 通过 WebSocket 实时推送 AGV 响应到前端
4. **友好界面**: 提供直观的控制面板，无需记忆 API 格式
5. **日志追踪**: 完整的命令发送和响应日志，便于调试
6. **灵活扩展**: 支持自定义 API 命令，适应不同场景

## 七、注意事项

1. 确保 AGV 已连接后再发送命令
2. WebSocket 断线后会自动重连（5 秒间隔）
3. 日志数量限制为 100 条，避免内存占用过大
4. 紧急停止按钮请谨慎使用
5. 自定义命令需要确保 API 路径正确
