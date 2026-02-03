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

    <!-- 地图控制面板 -->

    <!-- 地图控制面板 -->
    <el-card style="margin-top: 20px;">
      <div slot="header">
        <span>地图管理</span>
        <div style="float: right;">
          <el-upload
              class="upload-demo"
              :action="$baseUrl + '/map/import'"
              :on-success="handleMapImportSuccess"
              :on-error="handleMapImportError"
              :before-upload="beforeMapUpload"
              :headers="getHeaders()"
              :show-file-list="false"
              accept=".dbh"
          >
            <el-button size="small" type="primary">导入.dbh地图文件</el-button>
          </el-upload>
          <el-button size="small" @click="loadMapNodes">刷新地图</el-button>
        </div>
      </div>
      <p v-if="mapNodes.length === 0" style="text-align: center; color: #909399; padding: 20px;">
        当前没有地图数据，请先导入.dbh地图文件
      </p>
    </el-card>

    <!-- 工厂地图可视化 -->
    <el-card style="margin-top: 20px;">
      <div slot="header">
        <span>工厂地图</span>
        <div style="float: right;">
          <el-button size="small" @click="zoomIn">放大</el-button>
          <el-button size="small" @click="zoomOut">缩小</el-button>
          <el-button size="small" @click="resetZoom">重置缩放</el-button>
        </div>
      </div>
      <div class="map-container">
        <svg
            width="100%"
            height="600"
            viewBox="0 0 1200 800"
            @wheel="handleWheel"
            @mousedown="startPan"
            @mousemove="panning"
            @mouseup="endPan"
            @mouseleave="endPan"
        >
          <!-- 背景网格 -->
          <defs>
            <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
              <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#f0f0f0" stroke-width="1"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />

          <!-- 路径连线 -->
          <g v-for="(edge, index) in mapEdges" :key="'edge-' + index">
            <line
                :x1="getNodeXWithTransform(edge.fromNodeId)"
                :y1="getNodeYWithTransform(edge.fromNodeId)"
                :x2="getNodeXWithTransform(edge.toNodeId)"
                :y2="getNodeYWithTransform(edge.toNodeId)"
                stroke="#ccc"
                stroke-width="2"
            />
          </g>


          <!-- 地图节点 -->
          <g v-for="node in mapNodes" :key="node.nodeId">
            <circle
                :cx="node.x * scale + offsetX"
                :cy="node.y * scale + offsetY"
                r="10"
                :fill="getNodeColor(node.nodeType)"
                :stroke="selectedNode === node.nodeId ? 'red' : 'black'"
                :stroke-width="selectedNode === node.nodeId ? 2 : 1"
                @click="selectNode(node)"
            />
            <text
                :x="node.x * scale + offsetX"
                :y="node.y * scale + offsetY + 20"
                text-anchor="middle"
                font-size="12"
                fill="#333"
            >{{ node.nodeId }}</text>
          </g>

          <!-- AGV位置 -->
          <g v-for="agv in agvs.filter(a => a.isOnline)" :key="agv.agvId">
            <circle
                :cx="getNodeXWithTransform(agv.currentLocation)"
                :cy="getNodeYWithTransform(agv.currentLocation)"
                r="15"
                :fill="getAgvColor(agv.status)"
                stroke="black"
                stroke-width="2"
            >
              <title>{{ agv.agvId }} - {{ agv.status }}</title>
            </circle>
            <text
                :x="getNodeXWithTransform(agv.currentLocation)"
                :y="getNodeYWithTransform(agv.currentLocation) - 20"
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
      mapEdges: [], // 添加地图路径数据
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
      websocket: null,
      // 添加地图变换参数
      scale: 1,
      offsetX: 0,
      offsetY: 0,
      isPanning: false,
      startX: 0,
      startY: 0,
      startOffsetX: 0,
      startOffsetY: 0
    }
  },

  mounted() {
    this.loadAgvs();
    this.loadMapNodes();
    this.loadMapEdges(); // 加载地图路径
    this.calculateStats();
    this.initWebSocket();
  },

  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close();
    }
  },

  methods: {
    // 获取请求头
    getHeaders() {
      // 检查多种可能的token存储方式
      let token = localStorage.getItem('xm-token');
      if (!token) {
        // 尝试从用户信息中获取
        const user = JSON.parse(localStorage.getItem('xm-user') || '{}');
        token = user.token || '';
      }
      return {
        'token': token
      };
    },

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

    async loadMapEdges() {
      try {
        const response = await request.get('/map/edges');
        this.mapEdges = response.data || [];
      } catch (error) {
        console.error('加载地图路径失败:', error);
        // 如果没有路径API，则创建简单的邻近节点连接
        this.generateEdgesFromNodes();
      }
    },

    // 根据节点位置生成简单邻近连接
    generateEdgesFromNodes() {
      this.mapEdges = [];
      for (let i = 0; i < this.mapNodes.length; i++) {
        for (let j = i + 1; j < this.mapNodes.length; j++) {
          const node1 = this.mapNodes[i];
          const node2 = this.mapNodes[j];
          const distance = Math.sqrt(
              Math.pow(node1.x - node2.x, 2) +
              Math.pow(node1.y - node2.y, 2)
          );

          // 如果两个节点距离较近，则认为它们之间有路径
          if (distance < 100) { // 距离阈值可根据实际情况调整
            this.mapEdges.push({
              fromNodeId: node1.nodeId,
              toNodeId: node2.nodeId
            });
          }
        }
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

    // 带变换的坐标计算
    getNodeXWithTransform(locationId) {
      const node = this.mapNodes.find(n => n.nodeId === locationId);
      return node ? node.x * this.scale + this.offsetX : 0;
    },

    getNodeYWithTransform(locationId) {
      const node = this.mapNodes.find(n => n.nodeId === locationId);
      return node ? node.y * this.scale + this.offsetY : 0;
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
    },

    // 地图缩放功能
    zoomIn() {
      this.scale *= 1.2;
    },

    zoomOut() {
      this.scale /= 1.2;
    },

    resetZoom() {
      this.scale = 1;
      this.offsetX = 0;
      this.offsetY = 0;
    },

    handleWheel(e) {
      e.preventDefault();
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      this.scale *= delta;

      // 限制缩放范围
      this.scale = Math.max(0.1, Math.min(5, this.scale));
    },

    startPan(e) {
      if (e.button !== 0) return; // 只响应左键
      this.isPanning = true;
      this.startX = e.clientX - this.offsetX;
      this.startY = e.clientY - this.offsetY;
    },

    panning(e) {
      if (!this.isPanning) return;
      this.offsetX = e.clientX - this.startX;
      this.offsetY = e.clientY - this.startY;
    },

    endPan() {
      this.isPanning = false;
    },

    // 地图文件上传相关方法
    beforeMapUpload(file) {
      const isDbh = file.name.endsWith('.dbh');
      const isLt50M = file.size / 1024 / 1024 < 50;

      if (!isDbh) {
        this.$message.error('只能上传.dbh格式的地图文件!');
      }
      if (!isLt50M) {
        this.$message.error('地图文件大小不能超过50MB!');
      }
      return isDbh && isLt50M;
    },

    handleMapImportSuccess(response, file, fileList) {
      if (response.code === '200') {
        this.$message.success('地图导入成功！');
        // 重新加载地图数据
        this.loadMapNodes();
        this.loadMapEdges();
      } else {
        this.$message.error(response.msg || '地图导入失败');
      }
    },

    handleMapImportError(err, file, fileList) {
      this.$message.error('地图导入失败: ' + (err.message || '网络错误'));
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
  cursor: grab;
}
.map-container:active {
  cursor: grabbing;
}
</style>
