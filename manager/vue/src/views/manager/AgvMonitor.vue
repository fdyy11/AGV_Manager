<template>
  <div class="agv-monitor">
    <h2>AGV实时监控</h2>
    
    <!-- AGV状态概览 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.totalAgvs }}</div>
            <div class="stat-label">总AGV数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.onlineAgvs }}</div>
            <div class="stat-label">在线AGV</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.workingAgvs }}</div>
            <div class="stat-label">工作中AGV</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.lowBatteryAgvs }}</div>
            <div class="stat-label">低电量AGV</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AGV列表 -->
    <el-card>
      <div slot="header">
        <span>AGV状态列表</span>
        <el-button style="float: right;" type="primary" @click="refreshAgvs">刷新</el-button>
      </div>
      <el-table :data="agvs" style="width: 100%">
        <el-table-column prop="agvId" label="AGV编号" width="120" />
        <el-table-column prop="currentLocation" label="当前位置" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template v-slot="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batteryLevel" label="电量" width="100">
          <template v-slot="scope">
            <el-progress 
              :percentage="scope.row.batteryLevel" 
              :color="getBatteryColor(scope.row.batteryLevel)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column prop="carryingMaterial" label="承载物料" width="120" />
        <el-table-column prop="speed" label="速度(m/s)" width="100" />
        <el-table-column prop="lastUpdateTime" label="最后更新" width="160" />
        <el-table-column label="操作" width="150">
          <template v-slot="scope">
            <el-button size="mini" @click="viewDetails(scope.row)">详情</el-button>
            <el-button size="mini" type="primary" @click="controlAgv(scope.row)">控制</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工厂地图可视化 -->
    <el-card style="margin-top: 20px;">
      <div slot="header">
        <span>工厂地图</span>
      </div>
      <div class="map-container">
        <svg width="100%" height="600" viewBox="0 0 1200 800">
          <!-- 地图节点 -->
          <g v-for="node in mapNodes" :key="node.nodeId">
            <circle 
              :cx="node.x" 
              :cy="node.y" 
              r="10" 
              :fill="getNodeColor(node.nodeType)"
              :stroke="selectedNode === node.nodeId ? 'red' : 'black'"
              :stroke-width="selectedNode === node.nodeId ? 2 : 1"
              @click="selectNode(node)"
            />
            <text 
              :x="node.x" 
              :y="node.y + 20" 
              text-anchor="middle"
              font-size="12"
            >{{ node.nodeId }}</text>
          </g>
          
          <!-- AGV位置 -->
          <g v-for="agv in agvs.filter(a => a.isOnline)" :key="agv.agvId">
            <circle 
              :cx="getNodeX(agv.currentLocation)" 
              :cy="getNodeY(agv.currentLocation)" 
              r="15" 
              :fill="getAgvColor(agv.status)"
              stroke="black"
              stroke-width="2"
            >
              <title>{{ agv.agvId }} - {{ agv.status }}</title>
            </circle>
            <text 
              :x="getNodeX(agv.currentLocation)" 
              :y="getNodeY(agv.currentLocation) - 20" 
              text-anchor="middle"
              font-size="10"
              fill="white"
            >{{ agv.agvId }}</text>
          </g>
        </svg>
      </div>
    </el-card>

    <!-- AGV控制对话框 -->
    <el-dialog title="AGV控制" :visible.sync="controlDialogVisible" width="40%">
      <el-form :model="selectedAgv" label-width="100px">
        <el-form-item label="AGV编号">
          <el-input v-model="selectedAgv.agvId" disabled></el-input>
        </el-form-item>
        <el-form-item label="目标位置">
          <el-select v-model="targetLocation" placeholder="选择目标位置">
            <el-option 
              v-for="node in mapNodes" 
              :key="node.nodeId" 
              :label="node.nodeId" 
              :value="node.nodeId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="controlDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendMoveCommand">发送移动命令</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AgvMonitor',
  data() {
    return {
      agvs: [],
      mapNodes: [],
      stats: {
        totalAgvs: 0,
        onlineAgvs: 0,
        workingAgvs: 0,
        lowBatteryAgvs: 0
      },
      selectedNode: null,
      controlDialogVisible: false,
      selectedAgv: {},
      targetLocation: '',
      websocket: null
    }
  },
  
  mounted() {
    this.loadAgvs();
    this.loadMapNodes();
    this.calculateStats();
    this.initWebSocket();
  },
  
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close();
    }
  },
  
  methods: {
    async loadAgvs() {
      try {
        const response = await request.get('/agv/online');
        this.agvs = response.data || [];
        this.calculateStats();
      } catch (error) {
        console.error('加载AGV数据失败:', error);
      }
    },
    
    async loadMapNodes() {
      try {
        const response = await request.get('/map/nodes');
        this.mapNodes = response.data || [];
      } catch (error) {
        console.error('加载地图节点失败:', error);
      }
    },
    
    calculateStats() {
      this.stats.totalAgvs = this.agvs.length;
      this.stats.onlineAgvs = this.agvs.filter(agv => agv.isOnline).length;
      this.stats.workingAgvs = this.agvs.filter(agv => agv.status === 'working').length;
      this.stats.lowBatteryAgvs = this.agvs.filter(agv => agv.batteryLevel < 20).length;
    },
    
    refreshAgvs() {
      this.loadAgvs();
    },
    
    getStatusType(status) {
      const types = {
        'idle': 'success',
        'working': 'primary',
        'charging': 'warning',
        'fault': 'danger'
      };
      return types[status] || 'info';
    },
    
    getBatteryColor(batteryLevel) {
      if (batteryLevel < 20) return '#F56C6C'; // 红色
      if (batteryLevel < 50) return '#E6A23C'; // 橙色
      return '#67C23A'; // 绿色
    },
    
    getNodeColor(nodeType) {
      const colors = {
        'assembly': '#409EFF',    // 装配位 - 蓝色
        'storage': '#67C23A',     // 仓库 - 绿色
        'charging': '#E6A23C',    // 充电桩 - 橙色
        'intersection': '#909399', // 交叉口 - 灰色
        'other': '#909399'        // 其他 - 灰色
      };
      return colors[nodeType] || '#909399';
    },
    
    getAgvColor(status) {
      const colors = {
        'idle': '#67C23A',      // 绿色 - 空闲
        'working': '#409EFF',   // 蓝色 - 工作中
        'charging': '#E6A23C',  // 橙色 - 充电
        'fault': '#F56C6C'      // 红色 - 故障
      };
      return colors[status] || '#909399';
    },
    
    getNodeX(locationId) {
      const node = this.mapNodes.find(n => n.nodeId === locationId);
      return node ? node.x : 0;
    },
    
    getNodeY(locationId) {
      const node = this.mapNodes.find(n => n.nodeId === locationId);
      return node ? node.y : 0;
    },
    
    selectNode(node) {
      this.selectedNode = node.nodeId;
    },
    
    viewDetails(agv) {
      this.$alert(`
        <div>
          <p><strong>AGV编号:</strong> ${agv.agvId}</p>
          <p><strong>当前位置:</strong> ${agv.currentLocation}</p>
          <p><strong>状态:</strong> ${agv.status}</p>
          <p><strong>电量:</strong> ${agv.batteryLevel}%</p>
          <p><strong>承载物料:</strong> ${agv.carryingMaterial || '无'}</p>
          <p><strong>IP地址:</strong> ${agv.ipAddress || '未连接'}</p>
          <p><strong>最后更新:</strong> ${agv.lastUpdateTime}</p>
        </div>
      `, 'AGV详情', {
        dangerouslyUseHTMLString: true
      });
    },
    
    controlAgv(agv) {
      this.selectedAgv = {...agv};
      this.controlDialogVisible = true;
    },
    
    async sendMoveCommand() {
      try {
        const command = {
          agvId: this.selectedAgv.agvId,
          targetLocation: this.targetLocation,
          command: 'MOVE'
        };
        
        await request.post('/agv/move', command);
        this.$message.success('移动命令已发送');
        this.controlDialogVisible = false;
      } catch (error) {
        console.error('发送移动命令失败:', error);
        this.$message.error('发送命令失败: ' + (error.response?.data?.msg || error.message));
      }
    },
    
    initWebSocket() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;
      const wsUrl = `${protocol}//${host}:8080/ws/tcp-status`;
      
      try {
        this.websocket = new WebSocket(wsUrl);
        
        this.websocket.onopen = () => {
          console.log('WebSocket连接已建立');
        };
        
        this.websocket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'agvStatus') {
              this.updateAgvStatus(data.payload);
            }
          } catch (err) {
            console.error('处理WebSocket消息错误:', err);
          }
        };
        
        this.websocket.onclose = () => {
          console.log('WebSocket连接已关闭');
          // 尝试重连
          setTimeout(() => {
            this.initWebSocket();
          }, 5000);
        };
        
        this.websocket.onerror = (error) => {
          console.error('WebSocket错误:', error);
        };
      } catch (error) {
        console.error('建立WebSocket连接失败:', error);
      }
    },
    
    updateAgvStatus(status) {
      const index = this.agvs.findIndex(agv => agv.agvId === status.agvId);
      if (index !== -1) {
        this.$set(this.agvs, index, { ...this.agvs[index], ...status });
      } else {
        this.agvs.push(status);
      }
      this.calculateStats();
    }
  }
}
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-item {
  padding: 10px;
}
.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}
.stat-label {
  font-size: 14px;
  color: #606266;
  margin-top: 5px;
}
.map-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
</style>
