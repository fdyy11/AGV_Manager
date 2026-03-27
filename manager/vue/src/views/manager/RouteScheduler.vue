<template>
  <div class="route-scheduler">
    <h2>路线调度管理</h2>

    <!-- 地图控制面板 -->
    <el-card style="margin-bottom: 20px;">
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
            <el-button size="small" type="primary">导入.dbh 地图文件</el-button>
          </el-upload>
          <el-button size="small" @click="loadMapNodes">刷新地图</el-button>
        </div>
      </div>
      <p v-if="mapNodes.length === 0" style="text-align: center; color: #909399; padding: 20px;">
        当前没有地图数据，请先导入.dbh 地图文件
      </p>
    </el-card>

    <!-- 工厂地图可视化 -->
    <el-card>
      <div slot="header">
        <span>工厂地图</span>
        <div style="float: right;">
          <el-button size="small" @click="toggleSizeLock">{{ isSizeLocked ? '解锁大小' : '锁定大小' }}</el-button>
          <el-button size="small" @click="zoomIn">放大</el-button>
          <el-button size="small" @click="zoomOut">缩小</el-button>
          <el-button size="small" @click="resetZoom">重置缩放</el-button>
        </div>
      </div>
      <div class="map-container">
        <svg
            width="100%"
            height="600"
            :viewBox="isSizeLocked ? '0 0 640 480' : '0 0 1200 800'"
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
            <!-- 节点类型注释 -->
            <text
                :x="node.x * scale + offsetX + 15"
                :y="node.y * scale + offsetY + 5"
                text-anchor="start"
                font-size="11"
                fill="#666"
                font-weight="bold"
            >{{ getNodeTypeName(node.nodeType) }}</text>
            <!-- 节点 ID -->
            <text
                :x="node.x * scale + offsetX"
                :y="node.y * scale + offsetY + 25"
                text-anchor="middle"
                font-size="10"
                fill="#333"
            >{{ node.nodeId }}</text>
          </g>

          <!-- AGV 位置 -->
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
      <!-- 节点类型图例 -->
      <div class="legend-container">
        <div class="legend-item" v-for="(color, type) in nodeLegend" :key="type">
          <span class="legend-dot" :style="{ backgroundColor: color }"></span>
          <span class="legend-label">{{ getNodeTypeName(type) }}</span>
        </div>
      </div>
    </el-card>

    <!-- AGV 控制对话框 -->
    <el-dialog title="AGV 控制" :visible.sync="controlDialogVisible" width="40%">
      <el-form :model="selectedAgv" label-width="100px">
        <el-form-item label="AGV 编号">
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
import request from '@/utils/request';

export default {
  name: 'RouteScheduler',
  data() {
    return {
      agvs: [],
      mapNodes: [],
      mapEdges: [],
      selectedNode: null,
      controlDialogVisible: false,
      selectedAgv: {},
      targetLocation: '',
      websocket: null,
      scale: 1,
      offsetX: 0,
      offsetY: 0,
      isPanning: false,
      startX: 0,
      startY: 0,
      startOffsetX: 0,
      startOffsetY: 0,
      isSizeLocked: true, // 默认锁定地图大小
      defaultWidth: 640,
      defaultHeight: 480,
      // 节点类型图例（支持中英文）
      nodeLegend: {
        'assembly': '#409EFF',
        'storage': '#67C23A',
        'charging': '#E6A23C',
        'intersection': '#909399',
        'other': '#909399'
      }
    };
  },
  mounted() {
    this.loadAgvs();
    setInterval(() => {
      this.loadAgvs();
    }, 5000); // 每 5 秒刷新一次
    this.loadMapNodes();
    this.loadMapEdges();
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close();
    }
  },
  methods: {
    getHeaders() {
      let token = localStorage.getItem('xm-token');
      if (!token) {
        const user = JSON.parse(localStorage.getItem('xm-user') || '{}');
        token = user.token || '';
      }
      return {
        'token': token
      };
    },
    async loadAgvs() {
      try {
        // 从数据库加载所有AGV（包括已连接和未连接的）
        const response = await request.get('/tcp-client/all-agvs');
        this.agvs = response.data || [];
        console.log('从数据库加载的 AGV 数据:', this.agvs);
      } catch (error) {
        console.error('加载 AGV 数据失败:', error);
      }
    },
    async loadMapNodes() {
      try {
        const response = await request.get('/map/nodes');
        this.mapNodes = response.data || [];
        console.log('地图节点数据:', this.mapNodes);
        if (this.mapNodes.length > 0) {
          console.log('第一个节点的 nodeType:', this.mapNodes[0].nodeType);
        }
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
        this.generateEdgesFromNodes();
      }
    },
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
          if (distance < 100) {
            this.mapEdges.push({
              fromNodeId: node1.nodeId,
              toNodeId: node2.nodeId
            });
          }
        }
      }
    },
    getNodeColor(nodeType) {
      // 如果 nodeType 为空或 undefined，返回默认颜色
      if (!nodeType) {
        console.log('节点类型为空，使用默认颜色');
        return '#909399'; // 灰色
      }
      // 支持中文和英文两种映射
      const colors = {
        // 英文类型
        'assembly': '#409EFF',      // 站点 - 蓝色
        'storage': '#67C23A',       // 普通节点 - 绿色
        'charging': '#E6A23C',      // 充电站 - 橙色
        'intersection': '#909399',  // 交叉路口 - 灰色
        'other': '#909399',         // 其他类型 - 灰色
        // 中文类型
        '站点': '#409EFF',
        '普通节点': '#67C23A',
        '充电站': '#E6A23C',
        '路口': '#909399',
        '其他': '#909399'
      };
      const color = colors[nodeType] || '#909399';
      console.log(`节点类型 ${nodeType} 对应颜色：${color}`);
      return color;
    },
    
    /**
     * 获取节点类型的中文名称
     */
    getNodeTypeName(nodeType) {
      // 如果是英文类型，转换为中文
      const typeNames = {
        'assembly': '站点',
        'storage': '普通节点',
        'charging': '充电站',
        'intersection': '路口',
        'other': '其他'
      };
      // 如果已经是中文，直接返回；否则查表转换
      return typeNames[nodeType] || nodeType || '普通节点';
    },
    getAgvColor(status) {
      const colors = {
        'idle': '#67C23A',
        'working': '#409EFF',
        'charging': '#E6A23C',
        'fault': '#F56C6C'
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
        this.$message.error('发送命令失败：' + (error.response?.data?.msg || error.message));
      }
    },
    initWebSocket() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname;
      const wsUrl = `${protocol}//${host}:8080/ws/tcp-status`; // 使用 8080 端口
      try {
        this.websocket = new WebSocket(wsUrl);
        this.websocket.onopen = () => {
          console.log('WebSocket 连接已建立');
        };
        this.websocket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'agvStatus') {
              this.updateAgvStatus(data.payload);
            }
          } catch (err) {
            console.error('处理 WebSocket 消息错误:', err);
          }
        };
        this.websocket.onclose = () => {
          console.log('WebSocket 连接已关闭');
          setTimeout(() => {
            this.initWebSocket();
          }, 5000);
        };
        this.websocket.onerror = (error) => {
          console.error('WebSocket 错误:', error);
        };
      } catch (error) {
        console.error('建立 WebSocket 连接失败:', error);
      }
    },
    updateAgvStatus(status) {
      const index = this.agvs.findIndex(agv => agv.agvId === status.agvId);
      if (index !== -1) {
        this.$set(this.agvs, index, { ...this.agvs[index], ...status });
      } else {
        this.agvs.push(status);
      }
    },
    toggleSizeLock() {
      this.isSizeLocked = !this.isSizeLocked;
      if (this.isSizeLocked) {
        // 锁定时重置为默认大小和缩放
        this.scale = 1;
        this.offsetX = 0;
        this.offsetY = 0;
      }
    },
    zoomIn() {
      if (this.isSizeLocked) return; // 锁定状态下不允许缩放
      this.scale *= 1.2;
    },
    zoomOut() {
      if (this.isSizeLocked) return; // 锁定状态下不允许缩放
      this.scale /= 1.2;
    },
    resetZoom() {
      if (this.isSizeLocked) {
        this.scale = 1;
        this.offsetX = 0;
        this.offsetY = 0;
      } else {
        this.scale = 1;
        this.offsetX = 0;
        this.offsetY = 0;
      }
    },
    handleWheel(e) {
      if (this.isSizeLocked) return; // 锁定状态下不允许滚轮缩放
      e.preventDefault();
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      this.scale *= delta;
      this.scale = Math.max(0.1, Math.min(5, this.scale));
    },
    startPan(e) {
      if (e.button !== 0) return;
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
    beforeMapUpload(file) {
      const isDbh = file.name.endsWith('.dbh');
      const isLt50M = file.size / 1024 / 1024 < 50;
      if (!isDbh) {
        this.$message.error('只能上传.dbh格式的地图文件!');
      }
      if (!isLt50M) {
        this.$message.error('地图文件大小不能超过 50MB!');
      }
      return isDbh && isLt50M;
    },
    handleMapImportSuccess(response, file, fileList) {
      if (response.code === '200') {
        this.$message.success('地图导入成功！');
        this.loadMapNodes();
        this.loadMapEdges();
      } else {
        this.$message.error(response.msg || '地图导入失败');
      }
    },
    handleMapImportError(err, file, fileList) {
      this.$message.error('地图导入失败：' + (err.message || '网络错误'));
    }
  }
};
</script>

<style scoped>
.map-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  cursor: grab;
}
.map-container:active {
  cursor: grabbing;
}
.legend-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.legend-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #333;
  display: inline-block;
}
.legend-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
</style>
