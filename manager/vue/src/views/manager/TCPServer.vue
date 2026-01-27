<template>
  <div class="tcp-server-page">
    <h2>TCP服务器管理</h2>
    <div class="tcp-connection-panel">
      <el-card class="tcp-card">
        <div slot="header" class="clearfix">
          <span>AGV通信连接</span>
          <el-button
              style="float: right; padding: 3px 0"
              type="text"
              @click="toggleConnection"
          >
            {{ connectionStatus ? '停止监听' : '开始监听' }}
          </el-button>
        </div>

        <div class="status-info">
          <p><strong>状态:</strong>
            <el-tag :type="statusTagType">{{ connectionStatusText }}</el-tag>
          </p>
          <p><strong>监听端口:</strong> {{ serverPort }}</p>
          <p><strong>连接时间:</strong> {{ connectionTime }}</p>
          <p><strong>客户端数量:</strong> {{ connectedClients }}</p>
          <p><strong>最后活动:</strong> {{ lastActivity }}</p>
        </div>

        <div class="connection-form">
          <el-form :model="connectionForm" label-width="100px" size="small">
            <el-form-item label="监听端口">
              <el-input-number
                  v-model="connectionForm.port"
                  :min="1"
                  :max="65535"
                  :disabled="connectionStatus"
              ></el-input-number>
            </el-form-item>
            <el-form-item>
              <el-button
                  type="primary"
                  @click="saveConnectionConfig"
                  :disabled="connectionStatus"
              >保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 通信日志 -->
        <div class="communication-log">
          <div class="log-header">
            <h4>通信日志</h4>
            <el-button size="mini" @click="clearLogs">清空日志</el-button>
          </div>
          <div class="log-container">
            <ul class="log-list">
              <li v-for="(log, index) in logs" :key="index" class="log-item">
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-type" :class="'log-type-' + log.type">{{ log.type.toUpperCase() }}</span>
                <span class="log-message">{{ log.message }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 发送消息区域 -->
        <div class="send-message-area">
          <h4>发送消息</h4>
          <el-form :model="sendMessageForm" label-width="80px" size="small">
            <el-form-item label="消息内容">
              <el-input
                  v-model="sendMessageForm.content"
                  type="textarea"
                  :rows="3"
                  placeholder="输入要发送的消息内容"
              ></el-input>
            </el-form-item>
            <el-form-item>
              <el-button
                  type="success"
                  @click="sendMessage"
                  :disabled="!connectionStatus || connectedClients === 0"
              >发送给所有客户端</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TCPServer',
  data() {
    return {
      connectionStatus: false,
      connectionStatusText: '未监听',
      statusTagType: 'info',
      serverPort: 4500,
      connectionTime: '-',
      connectedClients: 0,
      lastActivity: '-',
      connectionForm: {
        port: 4500
      },
      logs: [],
      sendMessageForm: {
        content: ''
      },
      statusCheckInterval: null,  // 定时检查状态的定时器
      websocket: null,
      reconnectAttempts: 0,
      maxReconnectAttempts: 5
    }
  },
  mounted() {
    this.loadConnectionStatus();
    // 开始定期检查TCP服务器状态
    this.startStatusPolling();
    // 初始化WebSocket连接
    this.connectWebSocket();
  },
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval);
    }
    // 关闭WebSocket连接
    this.disconnectWebSocket();
  },
  methods: {
    toggleConnection() {
      if (!this.connectionStatus) {
        this.startTcpServer();
      } else {
        this.stopTcpServer();
      }
    },

    async startTcpServer() {
      try {
        const params = {
          port: this.connectionForm.port
        };
        const response = await request.post('/api/tcp/start', params);

        // 添加调试日志
        console.log('TCP启动响应:', response);
        console.log('响应code类型:', typeof response.code);
        console.log('响应code值:', response.code);

        if (response.code === 200 || response.code === '200') {
          this.connectionStatus = true;
          this.connectionStatusText = '正在监听';
          this.statusTagType = 'success';
          this.connectionTime = new Date().toLocaleString();
          this.serverPort = this.connectionForm.port;
          this.addLog('info', `开始在端口 ${this.connectionForm.port} 上监听`);
          this.$message.success('TCP服务器启动成功');
          this.loadConnectionStatus();
        } else {
          this.$message.error(`启动失败: ${response.msg || '未知错误'}`);
          this.addLog('error', `启动失败: ${response.msg || '未知错误'}, Code: ${response.code}`);
        }
      } catch (error) {
        console.error('启动TCP服务器错误:', error);
        console.error('错误详情:', error.response);

        let errorMessage = error.message;
        if (error.response && error.response.data) {
          errorMessage = error.response.data.msg || error.response.data.message || errorMessage;
        }
        this.$message.error('启动失败: ' + errorMessage);
        this.addLog('error', `启动失败: ${errorMessage}`);
      }
    }
,

    async stopTcpServer() {
      try {
        const response = await request.post('/api/tcp/stop');
        if (response.code === '200') {
          this.connectionStatus = false;
          this.connectionStatusText = '未监听';
          this.statusTagType = 'info';
          this.connectionTime = '-';
          this.connectedClients = 0;
          this.lastActivity = '-';
          this.addLog('info', 'TCP服务器已停止监听');
          this.$message.success('TCP服务器已停止');
        } else {
          this.$message.error(response.msg || '停止失败');
          this.addLog('error', `停止失败: ${response.msg}`);
        }
      } catch (error) {
        console.error('停止TCP服务器错误:', error);
        this.$message.error('停止失败: ' + (error.response?.data?.msg || error.message));
        this.addLog('error', `停止失败: ${error.response?.data?.msg || error.message}`);
      }
    },

    loadConnectionStatus() {
      request.get('/api/tcp/status').then((res) => {
        if (res.code === '200') {
          this.connectionStatus = res.data.listening;
          this.connectionStatusText = res.data.listening ? '正在监听' : '未监听';
          this.statusTagType = res.data.listening ? 'success' : 'info';
          this.serverPort = res.data.port;
          this.connectionTime = res.data.startTime;
          this.connectedClients = res.data.clientCount || 0;
          this.lastActivity = res.data.lastActivity || '-';

          // 如果客户端数量发生变化，记录到日志
          if (this.connectedClients !== res.data.clientCount) {
            this.addLog('info', `客户端数量变化: ${res.data.clientCount}`);
          }
        }
      }).catch((error) => {
        console.error('加载连接状态错误:', error);
        this.addLog('error', `加载状态失败: ${error.message}`);
      });
    },

    startStatusPolling() {
      // 每3秒检查一次TCP服务器状态
      this.statusCheckInterval = setInterval(() => {
        if (this.connectionStatus) {
          this.loadConnectionStatus();
        }
      }, 3000);
    },

    saveConnectionConfig() {
      localStorage.setItem('tcpConfig', JSON.stringify(this.connectionForm));
      this.$message.success('配置已保存');
    },

    addLog(type, message) {
      const time = new Date().toLocaleTimeString();
      this.logs.unshift({ time, type, message });

      // 限制日志条数
      if (this.logs.length > 100) {
        this.logs = this.logs.slice(0, 100);
      }
    },

    clearLogs() {
      this.logs = [];
      this.$message.success('日志已清空');
    },

    async sendMessage() {
      if (!this.sendMessageForm.content.trim()) {
        this.$message.warning('请输入要发送的消息内容');
        return;
      }

      try {
        const response = await request.post('/api/tcp/send', {
          message: this.sendMessageForm.content
        });

        if (response.code === '200') {
          this.addLog('out', `发送消息: ${this.sendMessageForm.content}`);
          this.$message.success('消息发送成功');
          this.sendMessageForm.content = ''; // 清空输入框
        } else {
          this.$message.error(response.msg || '发送失败');
          this.addLog('error', `发送失败: ${response.msg}`);
        }
      } catch (error) {
        console.error('发送消息错误:', error);
        this.$message.error('发送失败: ' + (error.response?.data?.msg || error.message));
        this.addLog('error', `发送失败: ${error.response?.data?.msg || error.message}`);
      }
    },

    connectWebSocket() {
      // 方案1：直接使用当前页面的协议和主机，但修改端口为后端端口
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;

      // 这里需要根据您的实际配置来确定端口
      // 如果后端运行在8080端口，且前端是8081，需要直接指定8080
      const wsUrl = `${protocol}//${host}:8080/ws/tcp-status`;

      // 或者更好的方案：在环境变量中配置
      // const wsUrl = process.env.VUE_APP_WS_URL || 'ws://localhost:8080/ws/tcp-status';

      console.log('WebSocket连接URL:', wsUrl);
      try {
        this.websocket = new WebSocket(wsUrl);

        this.websocket.onopen = () => {
          console.log('WebSocket连接已建立');
          this.reconnectAttempts = 0;
          this.addLog('info', 'WebSocket连接已建立，可以接收实时状态更新');
        };

        this.websocket.onmessage = (event) => {
          console.log('收到原始 WebSocket 消息:', event.data);
          try {
            const data = JSON.parse(event.data);
            switch (data.type) {
              case 'status':
                this.updateConnectionStatus(data.payload);
                break;
              case 'message':
                this.addLog('in', `收到消息: ${data.payload.content}`);
                break;
              case 'clientConnect':
                this.addLog('success', `客户端连接: ${data.payload.address}`);
                break;
              case 'clientDisconnect':
                this.addLog('info', `客户端断开: ${data.payload.address}`);
                break;
              default:
                console.log('未知消息类型:', data);
            }
          } catch (err) {
            console.error('处理WebSocket消息错误:', err);
          }
        };

        this.websocket.onclose = (event) => {
          console.log('WebSocket连接已关闭', event);
          this.addLog('error', 'WebSocket连接已关闭');
          this.attemptReconnect();
        };

        this.websocket.onerror = (error) => {
          console.error('WebSocket错误:', error);
          this.addLog('error', 'WebSocket连接错误');
          this.disconnectWebSocket();
          this.attemptReconnect();
        };
      } catch (error) {
        console.error('建立WebSocket连接失败:', error);
        this.addLog('error', 'WebSocket连接建立失败');
        this.attemptReconnect();
      }
    },

    disconnectWebSocket() {
      if (this.websocket) {
        this.websocket.close();
        this.websocket = null;
      }
    },

    attemptReconnect() {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++;
        console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        this.addLog('info', `WebSocket重新连接尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
        setTimeout(() => {
          this.connectWebSocket();
        }, 3000); // 3秒后重试
      } else {
        console.error('达到最大重连尝试次数，停止重连');
        this.addLog('error', '达到最大WebSocket重连次数');
      }
    },

    updateConnectionStatus(statusData) {
      console.log('接收到状态数据:', statusData); // 添加调试日志

      // 确保字段名匹配
      this.connectionStatus = statusData.listening;
      this.connectionStatusText = statusData.listening ? '正在监听' : '未监听';
      this.statusTagType = statusData.listening ? 'success' : 'info';
      this.serverPort = statusData.port;
      this.connectionTime = statusData.startTime || statusData.connectionTime; // 检查字段名
      this.connectedClients = statusData.clientCount || statusData.connectedClients || 0; // 检查字段名
      this.lastActivity = statusData.lastActivity || '-';
    }
  }
}
</script>

<style scoped>
.tcp-server-page {
  padding: 20px;
}

.tcp-connection-panel {
  margin-top: 20px;
}

.tcp-card {
  width: 100%;
  max-width: 800px;
}

.status-info p {
  margin: 8px 0;
  font-size: 14px;
}

.connection-form {
  margin-top: 20px;
}

.communication-log {
  margin-top: 20px;
}

.send-message-area {
  margin-top: 20px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-container {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #ddd;
  padding: 10px;
  background-color: #f9f9f9;
}

.log-list {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.log-item {
  padding: 5px 0;
  border-bottom: 1px solid #eee;
  font-family: monospace;
  font-size: 12px;
  display: flex;
  gap: 8px;
}

.log-time {
  color: #666;
}

.log-type {
  font-weight: bold;
}

.log-type-info {
  color: #2196F3;
}

.log-type-success {
  color: #4CAF50;
}

.log-type-error {
  color: #F44336;
}

.log-type-in {
  color: #9C27B0;
}

.log-type-out {
  color: #FF9800;
}

.log-message {
  flex: 1;
}
</style>
