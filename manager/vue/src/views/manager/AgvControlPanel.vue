<template>
  <div class="agv-control-panel">
    <h2>AGV 控制面板</h2>

    <!-- AGV 选择 -->
    <el-card style="margin-bottom: 20px;">
      <div slot="header">
        <span>选择 AGV</span>
        <el-button size="mini" style="float: right;" @click="loadConnectedAgvs">刷新列表</el-button>
      </div>
      <el-form :inline="true" label-width="100px">
        <el-form-item label="AGV 编号">
          <el-select v-model="selectedAgvId" placeholder="请选择 AGV" @change="onAgvSelected">
            <el-option
              v-for="agv in connectedAgvs"
              :key="agv.agvId"
              :label="agv.agvId + ' (' + agv.ipAddress + ':' + agv.port + ')'",
              :value="agv.agvId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="testApiConnection">测试 API 连接</el-button>
        </el-form-item>
      </el-form>
          
      <!-- 连接状态提示 -->
      <div v-if="connectedAgvs.length === 0" class="connection-tip">
        <el-alert
          title="暂无连接的 AGV"
          type="warning"
          description="请先前往【通信管理】->【AGV 连接】页面建立 TCP 连接，然后点击“刷新列表”按钮。"
          show-icon
          :closable="false"
        />
      </div>
      <div v-else class="connection-tip-success">
        <el-alert
          :title="`当前已连接 ${connectedAgvs.length} 台 AGV`"
          type="success"
          :closable="false"
        />
      </div>
    </el-card>

    <!-- 快捷命令区域 -->
    <el-card style="margin-bottom: 20px;">
      <div slot="header">
        <span>快捷命令</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="success" @click="sendCommand('robotStatus')" style="width: 100%;">
            查询状态
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" @click="sendCommand('getPosition')" style="width: 100%;">
            获取位置
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="danger" @click="sendCommand('emergencyStop')" style="width: 100%;">
            紧急停止
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" @click="sendCommand('reset')" style="width: 100%;">
            系统复位
          </el-button>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 10px;">
        <el-col :span="6">
          <el-button type="primary" @click="sendCommand('charge')" style="width: 100%;">
            充电
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" @click="sendCommand('cancelTask')" style="width: 100%;">
            取消任务
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 移动控制区域 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <!-- 坐标移动 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>坐标移动</span>
          </div>
          <el-form label-width="80px">
            <el-form-item label="X 坐标">
              <el-input-number v-model="moveParams.x" :precision="4" :step="0.1" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="Y 坐标">
              <el-input-number v-model="moveParams.y" :precision="4" :step="0.1" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="角度">
              <el-input-number v-model="moveParams.theta" :precision="4" :step="0.1" style="width: 100%;" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="sendMoveToCoord" style="width: 100%;">
                发送移动命令
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 标记点移动 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>标记点移动</span>
          </div>
          <el-form label-width="80px">
            <el-form-item label="标记点">
              <el-input v-model="moveParams.marker" placeholder="输入标记点名称" />
            </el-form-item>
            <el-form-item label="UUID">
              <el-input v-model="moveParams.uuid" placeholder="可选，自动生成" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="sendMoveToMarker" style="width: 100%;">
                发送到标记点
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自定义命令 -->
    <el-card style="margin-bottom: 20px;">
      <div slot="header">
        <span>自定义 API 命令</span>
      </div>
      <el-form label-width="100px">
        <el-form-item label="API 路径">
          <el-input v-model="customCommand.apiPath" placeholder="/api/xxx" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input
            v-model="customCommand.paramsText"
            type="textarea"
            :rows="3"
            placeholder='格式：key1=value1&key2=value2'
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="sendCustomCommand">
            发送自定义命令
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 响应日志 -->
    <el-card>
      <div slot="header">
        <span>AGV 响应日志</span>
        <el-button size="mini" style="float: right;" @click="clearLogs">清空日志</el-button>
      </div>
      <div class="log-container">
        <div v-for="(log, index) in logs" :key="index" class="log-item" :class="'log-' + log.type">
          <div class="log-time">{{ log.time }}</div>
          <div class="log-content">
            <span v-if="log.command" class="log-command">[{{ log.command }}]</span>
            <span v-if="log.status" class="log-status">状态：{{ log.status }}</span>
            <pre>{{ log.data }}</pre>
          </div>
        </div>
        <div v-if="logs.length === 0" class="no-logs">暂无响应数据</div>
      </div>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'AgvControlPanel',
  data() {
    return {
      selectedAgvId: '',
      connectedAgvs: [],
      moveParams: {
        x: 0.0,
        y: 0.0,
        theta: 0.0,
        marker: '',
        uuid: ''
      },
      customCommand: {
        apiPath: '/api/',
        paramsText: ''
      },
      logs: [],
      websocket: null
    };
  },
  mounted() {
    this.loadConnectedAgvs();
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close();
    }
  },
  methods: {
    async loadConnectedAgvs() {
      try {
        // 先从数据库加载所有 AGV
        const response = await request.get('/tcp-client/all-agvs');
        const allAgvs = response.data || [];
        
        // 然后检查实际的 TCP 连接状态
        const connectedResponse = await request.get('/tcp-client/connected-agvs');
        const connectedAgvIds = Object.keys(connectedResponse.data || {});
        
        console.log('数据库中的 AGV:', allAgvs);
        console.log('实际 TCP 连接的 AGV:', connectedAgvIds);
        
        // 只显示真正连接到 TCP 的 AGV
        this.connectedAgvs = allAgvs.filter(agv => 
          connectedAgvIds.includes(agv.agvId)
        );
        
        if (this.connectedAgvs.length > 0 && !this.selectedAgvId) {
          this.selectedAgvId = this.connectedAgvs[0].agvId;
          this.$message.success(`已自动选择 AGV: ${this.selectedAgvId}`);
        } else if (connectedAgvIds.length === 0) {
          this.$message.warning('当前没有已连接的 AGV，请先在 AGV 连接页面建立连接');
        }
      } catch (error) {
        console.error('加载 AGV 列表失败:', error);
        this.$message.error('加载 AGV 列表失败：' + (error.response?.data?.msg || error.message));
      }
    },

    onAgvSelected() {
      // 验证选中的 AGV 是否真的连接
      if (this.selectedAgvId) {
        this.$message.success(`已选择 AGV: ${this.selectedAgvId}`);
      }
    },

    async testApiConnection() {
      try {
        // 测试几个关键 API
        const tests = [
          { name: '所有 AGV 列表', url: '/tcp-client/all-agvs' },
          { name: '已连接 AGV', url: '/tcp-client/connected-agvs' },
          { name: '自定义命令接口', url: '/tcp-client/api/custom', method: 'POST', params: { agvId: 'TEST', apiPath: '/test' } }
        ];

        for (const test of tests) {
          try {
            let response;
            if (test.method === 'POST') {
              response = await request.post(test.url, null, { params: test.params });
            } else {
              response = await request.get(test.url);
            }
            console.log(`✓ ${test.name} - 成功`, response.data);
          } catch (error) {
            console.error(`✗ ${test.name} - 失败`, error.response?.status, error.response?.data);
          }
        }

        this.$message.success('API 连接测试完成，请查看浏览器控制台');
      } catch (error) {
        this.$message.error('API 测试失败：' + error.message);
      }
    },

    async sendCommand(commandType) {
      if (!this.selectedAgvId) {
        this.$message.warning('请先选择 AGV');
        return;
      }

      // 验证 AGV 是否真的连接
      try {
        const checkResponse = await request.get(`/tcp-client/check-connection/${this.selectedAgvId}`);
        if (!checkResponse.data.connected) {
          this.$message.error(`AGV ${this.selectedAgvId} 未连接，请先建立连接`);
          this.loadConnectedAgvs(); // 重新加载 AGV 列表
          return;
        }
      } catch (error) {
        console.error('检查 AGV 连接状态失败:', error);
        this.$message.error('检查 AGV 连接状态失败');
        return;
      }

      const commandMap = {
        'robotStatus': { url: '/tcp-client/api/robot-status', params: {} },
        'getPosition': { url: '/tcp-client/api/get-position', params: {} },
        'emergencyStop': { url: '/tcp-client/api/emergency-stop', params: {} },
        'reset': { url: '/tcp-client/api/reset', params: {} },
        'charge': { url: '/tcp-client/api/charge', params: {} },
        'cancelTask': { url: '/tcp-client/api/cancel-task', params: {} }
      };

      const command = commandMap[commandType];
      if (!command) return;

      try {
        this.addLog({
          type: 'send',
          command: commandType,
          data: `发送命令到 AGV ${this.selectedAgvId}`
        });

        const response = await request.post(command.url, null, {
          params: { agvId: this.selectedAgvId }
        });

        this.addLog({
          type: 'receive',
          command: commandType,
          status: 'success',
          data: response.data
        });

        this.$message.success('命令发送成功');
      } catch (error) {
        this.addLog({
          type: 'error',
          command: commandType,
          status: 'failed',
          data: error.response?.data?.msg || error.message
        });
        this.$message.error('命令发送失败');
      }
    },

    async sendMoveToCoord() {
      if (!this.selectedAgvId) {
        this.$message.warning('请先选择 AGV');
        return;
      }

      // 验证 AGV 是否真的连接
      try {
        const checkResponse = await request.get(`/tcp-client/check-connection/${this.selectedAgvId}`);
        if (!checkResponse.data.connected) {
          this.$message.error(`AGV ${this.selectedAgvId} 未连接，请先建立连接`);
          this.loadConnectedAgvs();
          return;
        }
      } catch (error) {
        console.error('检查 AGV 连接状态失败:', error);
        this.$message.error('检查 AGV 连接状态失败');
        return;
      }

      try {
        this.addLog({
          type: 'send',
          command: 'moveToCoord',
          data: `移动到坐标 (${this.moveParams.x}, ${this.moveParams.y}, ${this.moveParams.theta})`
        });

        const response = await request.post('/tcp-client/api/move-to-coord', null, {
          params: {
            agvId: this.selectedAgvId,
            x: this.moveParams.x,
            y: this.moveParams.y,
            theta: this.moveParams.theta
          }
        });

        this.addLog({
          type: 'receive',
          command: 'moveToCoord',
          status: 'success',
          data: response.data
        });

        this.$message.success('移动命令已发送');
      } catch (error) {
        this.addLog({
          type: 'error',
          command: 'moveToCoord',
          status: 'failed',
          data: error.response?.data?.msg || error.message
        });
        this.$message.error('移动命令发送失败');
      }
    },

    async sendMoveToMarker() {
      if (!this.selectedAgvId) {
        this.$message.warning('请先选择 AGV');
        return;
      }

      if (!this.moveParams.marker) {
        this.$message.warning('请输入标记点名称');
        return;
      }

      // 验证 AGV 是否真的连接
      try {
        const checkResponse = await request.get(`/tcp-client/check-connection/${this.selectedAgvId}`);
        if (!checkResponse.data.connected) {
          this.$message.error(`AGV ${this.selectedAgvId} 未连接，请先建立连接`);
          this.loadConnectedAgvs();
          return;
        }
      } catch (error) {
        console.error('检查 AGV 连接状态失败:', error);
        this.$message.error('检查 AGV 连接状态失败');
        return;
      }

      try {
        this.addLog({
          type: 'send',
          command: 'moveToMarker',
          data: `移动到标记点 ${this.moveParams.marker}`
        });

        const response = await request.post('/tcp-client/api/move-to-marker', null, {
          params: {
            agvId: this.selectedAgvId,
            marker: this.moveParams.marker,
            uuid: this.moveParams.uuid || undefined
          }
        });

        this.addLog({
          type: 'receive',
          command: 'moveToMarker',
          status: 'success',
          data: response.data
        });

        this.$message.success('移动命令已发送');
      } catch (error) {
        this.addLog({
          type: 'error',
          command: 'moveToMarker',
          status: 'failed',
          data: error.response?.data?.msg || error.message
        });
        this.$message.error('移动命令发送失败');
      }
    },

    async sendCustomCommand() {
      if (!this.selectedAgvId) {
        this.$message.warning('请先选择 AGV');
        return;
      }

      if (!this.customCommand.apiPath) {
        this.$message.warning('请输入 API 路径');
        return;
      }

      // 验证 AGV 是否真的连接
      try {
        const checkResponse = await request.get(`/tcp-client/check-connection/${this.selectedAgvId}`);
        if (!checkResponse.data.connected) {
          this.$message.error(`AGV ${this.selectedAgvId} 未连接，请先建立连接`);
          this.loadConnectedAgvs();
          return;
        }
      } catch (error) {
        console.error('检查 AGV 连接状态失败:', error);
        this.$message.error('检查 AGV 连接状态失败');
        return;
      }

      // 解析参数
      let params = {};
      if (this.customCommand.paramsText) {
        const pairs = this.customCommand.paramsText.split('&');
        pairs.forEach(pair => {
          const [key, value] = pair.split('=');
          if (key && value) {
            params[key.trim()] = value.trim();
          }
        });
      }

      try {
        this.addLog({
          type: 'send',
          command: 'customCommand',
          data: `${this.customCommand.apiPath} ${JSON.stringify(params)}`
        });

        const response = await request.post('/tcp-client/api/custom', null, {
          params: {
            agvId: this.selectedAgvId,
            apiPath: this.customCommand.apiPath,
            params: Object.keys(params).length > 0 ? params : undefined
          }
        });

        this.addLog({
          type: 'receive',
          command: 'customCommand',
          status: 'success',
          data: response.data
        });

        this.$message.success('自定义命令已发送');
      } catch (error) {
        this.addLog({
          type: 'error',
          command: 'customCommand',
          status: 'failed',
          data: error.response?.data?.msg || error.message
        });
        this.$message.error('自定义命令发送失败');
      }
    },

    initWebSocket() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;
      // 使用配置文件中的端口 9090
      const wsUrl = `${protocol}//${host}:9090/ws/tcp-status`;

      try {
        this.websocket = new WebSocket(wsUrl);

        this.websocket.onopen = () => {
          console.log('WebSocket 连接已建立');
          this.addLog({
            type: 'system',
            data: 'WebSocket 连接已建立'
          });
        };

        this.websocket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            console.log('收到 WebSocket 消息:', data);

            // 如果是 AGV 响应数据，添加到日志
            if (data.agvId || data.command || data.responseType) {
              this.addLog({
                type: 'receive',
                command: data.command || data.responseType || 'AGV 响应',
                status: data.parseSuccess ? 'success' : 'failed',
                data: data
              });
            }
          } catch (err) {
            console.error('处理 WebSocket 消息错误:', err);
          }
        };

        this.websocket.onclose = () => {
          console.log('WebSocket 连接已关闭');
          this.addLog({
            type: 'system',
            data: 'WebSocket 连接已关闭，5 秒后重连...'
          });
          setTimeout(() => {
            this.initWebSocket();
          }, 5000);
        };

        this.websocket.onerror = (error) => {
          console.error('WebSocket 错误:', error);
          this.addLog({
            type: 'error',
            data: 'WebSocket 连接错误'
          });
        };
      } catch (error) {
        console.error('建立 WebSocket 连接失败:', error);
        this.addLog({
          type: 'error',
          data: 'WebSocket 连接失败：' + error.message
        });
      }
    },

    addLog(log) {
      const now = new Date();
      const timeStr = now.toLocaleTimeString() + '.' + now.getMilliseconds();
      this.logs.unshift({
        ...log,
        time: timeStr
      });

      // 限制日志数量
      if (this.logs.length > 100) {
        this.logs.pop();
      }
    },

    clearLogs() {
      this.logs = [];
      this.$message.success('日志已清空');
    }
  }
};
</script>

<style scoped>
.agv-control-panel {
  padding: 20px;
}

.connection-tip,
.connection-tip-success {
  margin-top: 10px;
}

.log-container {
  max-height: 500px;
  overflow-y: auto;
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 10px;
}

.log-item {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #fff;
  border-left: 4px solid #409EFF;
  border-radius: 4px;
}

.log-send {
  border-left-color: #409EFF;
}

.log-receive {
  border-left-color: #67C23A;
}

.log-error {
  border-left-color: #F56C6C;
}

.log-system {
  border-left-color: #909399;
}

.log-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.log-content {
  font-size: 13px;
  color: #606266;
}

.log-command {
  font-weight: bold;
  color: #409EFF;
  margin-right: 10px;
}

.log-status {
  color: #67C23A;
  margin-right: 10px;
}

.log-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin-top: 5px;
  background-color: #f8f9fa;
  padding: 8px;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.no-logs {
  text-align: center;
  color: #909399;
  padding: 20px;
}
</style>
