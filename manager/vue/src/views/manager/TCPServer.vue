<template>
  <div class="tcp-client-page">
    <h2>AGV 客户端连接管理</h2>
    <el-alert
      title="提示"
      type="info"
      description="已连接的AGV列表会在页面间切换时保持显示，断开连接的AGV会标记为'未连接'状态"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    >
    </el-alert>

    <!-- AGV 连接配置 -->
    <el-card style="margin-bottom: 20px;">
      <div slot="header">
        <span>AGV 连接配置</span>
        <el-button style="float: right;" type="primary" @click="addAgvConnection">添加 AGV</el-button>
      </div>
      <!-- 修改 AGV 连接配置部分 -->
      <el-table :data="agvConnections" style="width: 100%">
        <el-table-column prop="ip" label="IP 地址" width="150">
          <template v-slot="scope">
            <el-input v-model="scope.row.ip" size="mini" />
          </template>
        </el-table-column>
        <el-table-column prop="port" label="端口" width="100">
          <template v-slot="scope">
            <el-input v-model="scope.row.port" size="mini" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template v-slot="scope">
            <el-tag :type="getAgvStatusType(scope.row.status)">
              {{ getAgvStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="420">
          <template v-slot="scope">
            <div class="button-group">
              <el-button size="mini" @click="connectToAgv(scope.row)">连接</el-button>
              <el-button size="mini" type="danger" @click="disconnectFromAgvConfig(scope.row)">断开</el-button>
              <el-button size="mini" type="primary" @click="saveConnection(scope.row)">保存</el-button>
              <el-button size="mini" type="primary" @click="sendTestCommand(scope.row)">测试</el-button>
              <el-button size="mini" @click="viewAgvDetails(scope.row)">详情</el-button>
              <el-button size="mini" type="danger" @click="deleteAgv(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

    </el-card>

    <!-- 已连接的 AGV 列表 -->
    <el-card>
      <div slot="header">
        <span>已连接的 AGV</span>
        <el-button 
          style="float: right; margin-left: 10px;" 
          size="small" 
          @click="loadConnectedAgvs"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>
      
      <!-- 加载状态提示 -->
      <div v-if="loading" style="text-align: center; padding: 20px;">
        <i class="el-icon-loading" style="font-size: 24px; color: #409EFF;"></i>
        <p style="margin-top: 10px; color: #666;">正在加载AGV数据...</p>
      </div>
      
      <!-- 空状态提示 -->
      <div v-else-if="connectedAgvs.length === 0" style="text-align: center; padding: 40px;">
        <i class="el-icon-info" style="font-size: 48px; color: #909399;"></i>
        <p style="margin-top: 10px; color: #909399;">暂无AGV数据</p>
        <p style="color: #C0C4CC; font-size: 14px;">请确保后端服务正常运行，并且数据库中有AGV记录</p>
      </div>
      <el-table :data="connectedAgvs" style="width: 100%">
        <el-table-column prop="agvId" label="AGV 编号" width="120" />
        <el-table-column prop="ipAddress" label="IP 地址" width="150" />
        <el-table-column prop="port" label="端口" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template v-slot="scope">
            <el-tag :type="getAgvStatusType(scope.row.status)">
              {{ getAgvStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template v-slot="scope">
            <el-button size="mini" @click="viewAgvDetails(scope.row)">详情</el-button>
            <el-button size="mini" type="danger" @click="disconnectAgv(scope.row)">断开</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TCPServer',
  data() {
    return {
      agvConnections: [
        { ip: '127.0.0.1', port: 5555, status: 'disconnected', agvId: '' },
        { ip: '127.0.0.1', port: 2233, status: 'disconnected', agvId: '' },
        { ip: '127.0.0.1', port: 7777, status: 'disconnected', agvId: '' }
      ],
      connectedAgvs: [],
      refreshInterval: null,
      loading: false
    }
  },
  methods: {
    // 保存连接信息
    async saveConnection(agv) {
      try {
        const response = await request.post('/tcp-client/save-connection', {
          agvId: agv.agvId,
          ip: agv.ip,
          port: agv.port
        });
        if (response.code === '200') {
          this.$message.success('连接信息已保存');
        } else {
          this.$message.error(response.msg || '保存失败');
        }
      } catch (error) {
        this.$message.error('保存失败: ' + (error.response?.data?.msg || error.message));
      }
    },

    addAgvConnection() {
      this.agvConnections.push({ ip: '127.0.0.1', port: 1111, status: 'disconnected', agvId: '' })
    },
    async connectToAgv(agv) {
      try {
        // 确保使用配置的端口号，而不是默认的5555
        const portToUse = agv.port || 5555;
        console.log(`准备连接AGV: IP=${agv.ip}, 配置端口=${agv.port}, 实际使用端口=${portToUse}`);
        
        // 使用查询参数而不是请求体来传递参数
        const requestData = {
          agvId: agv.agvId || undefined,
          ip: agv.ip || '127.0.0.1',
          port: portToUse
        };
        console.log('发送连接请求，参数:', requestData);
        
        const response = await request.post('/tcp-client/connect', null, {
          params: requestData
        });

        if (response.code === '200') {
          this.$message.success(`成功连接到 AGV ${agv.ip}:${portToUse}`);
          
          // 更新配置列表中的状态
          agv.status = 'connected';
          
          // 重新加载已连接的AGV列表
          await this.loadConnectedAgvs();
        } else {
          this.$message.error(response.msg || '连接失败');
        }
      } catch (error) {
        this.$message.error('连接失败: ' + (error.response?.data?.msg || error.message));
      }
    },
    async loadAllAgvs() {
      try {
        const response = await request.get('/tcp-client/all-agvs');
        if (response.code === '200') {
          // 显示所有AGV，包括已连接和未连接的
          this.connectedAgvs = response.data.map(agv => ({
            ...agv,
            status: agv.status || 'disconnected',
            ip: agv.ipAddress || '',
            port: agv.port || 0
          }));
        } else {
          this.$message.error(response.msg || '加载失败');
        }
      } catch (error) {
        this.$message.error('加载 AGV 数据失败: ' + (error.response?.data?.msg || error.message));
      }
    },
    
    // 新增：专门加载已连接的AGV列表
    async loadConnectedAgvs() {
      this.loading = true;
      try {
        console.log('=== 开始加载AGV数据 ===');
        
        // 先执行批量连接状态检查
        await this.checkAllConnections();
        
        // 先获取所有AGV
        const allAgvsResponse = await request.get('/tcp-client/all-agvs');
        console.log('所有AGV响应:', allAgvsResponse);
        
        if (allAgvsResponse.code === '200') {
          console.log('获取到的AGV数据:', allAgvsResponse.data);
          
          // 获取当前已连接的AGV信息
          const connectedResponse = await request.get('/tcp-client/connected-agvs');
          console.log('已连接AGV响应:', connectedResponse);
          
          let connectedData = {};
          if (connectedResponse.code === '200') {
            connectedData = connectedResponse.data;
          }
          
          // 获取已连接的AGV ID列表
          const connectedIds = Object.keys(connectedData);
          console.log('已连接的AGV IDs:', connectedIds);
          
          // 更新所有AGV的状态
          this.connectedAgvs = allAgvsResponse.data.map(agv => {
            const isConnected = connectedIds.includes(agv.agvId);
            const finalStatus = isConnected ? 'connected' : (agv.status || 'disconnected');
            console.log(`AGV ${agv.agvId}: 数据库状态=${agv.status}, 最终状态=${finalStatus}`);
            return {
              ...agv,
              status: finalStatus,
              ip: agv.ipAddress || '',
              port: agv.port || 0
            };
          });
          
          console.log('最终显示的AGV列表:', this.connectedAgvs);
          console.log('=== AGV数据加载完成 ===');
        } else {
          console.error('获取所有AGV失败:', allAgvsResponse.msg);
          this.$message.error('获取AGV数据失败: ' + allAgvsResponse.msg);
        }
      } catch (error) {
        console.error('加载已连接AGV失败:', error);
        this.$message.error('加载AGV数据失败: ' + error.message);
        // 如果获取连接状态失败，至少显示所有AGV
        await this.loadAllAgvs();
      } finally {
        this.loading = false;
      }
    },

    async sendTestCommand(agv) {
      try {
        const response = await request.post('/tcp-client/send-test-command', {
          agvId: agv.agvId
        });

        if (response.code === '200') {
          this.$message.success('测试命令已发送');
        } else {
          this.$message.error(response.msg || '发送失败');
        }
      } catch (error) {
        this.$message.error('发送测试命令失败: ' + (error.response?.data?.msg || error.message));
      }
    },



    async disconnectFromAgv(agv) {
      try {
        console.log('=== 准备断开连接 ===');
        console.log('传入的AGV对象:', agv);
        console.log('AGV ID:', agv.agvId);
        console.log('AGV ID类型:', typeof agv.agvId);
        console.log('AGV ID长度:', agv.agvId ? agv.agvId.length : 'undefined');
        
        // 验证AGV ID是否存在
        if (!agv.agvId) {
          console.error('AGV ID为空或未定义');
          this.$message.error('AGV ID不能为空');
          return;
        }
        
        if (typeof agv.agvId !== 'string') {
          console.error('AGV ID不是字符串类型:', typeof agv.agvId);
          this.$message.error('AGV ID格式错误');
          return;
        }
        
        console.log('发送断开请求，参数:', { agvId: agv.agvId });
        
        const response = await request.post('/tcp-client/disconnect', null, {
          params: { agvId: agv.agvId }
        });
        
        console.log('=== 断开连接响应 ===');
        console.log('响应数据:', response);
        console.log('响应代码:', response.code);
        
        if (response.code === '200') {
          this.$message.success(`已断开与 AGV ${agv.agvId} 的连接`);
          
          // 更新配置列表中的状态
          const configAgv = this.agvConnections.find(item => item.ip === agv.ip && item.port === agv.port);
          if (configAgv) {
            configAgv.status = 'disconnected';
          }
          
          // 重新加载已连接的AGV列表
          await this.loadConnectedAgvs();
        } else {
          console.error('服务器返回错误:', response.msg);
          this.$message.error(response.msg || '断开连接失败');
        }
      } catch (error) {
        console.error('=== 断开连接发生异常 ===');
        console.error('错误对象:', error);
        console.error('错误详情:', {
          message: error.message,
          response: error.response,
          config: error.config,
          request: error.request
        });
        
        let errorMsg = '断开连接失败';
        if (error.response) {
          console.error('服务器响应:', error.response);
          errorMsg += ': ' + (error.response.data?.msg || error.response.statusText || '服务器错误');
        } else if (error.request) {
          console.error('网络请求失败:', error.request);
          errorMsg += ': 网络请求失败';
        } else {
          errorMsg += ': ' + error.message;
        }
        
        this.$message.error(errorMsg);
      }
    },
    getAgvStatusType(status) {
      const types = {
        connected: 'success',
        disconnected: 'info',
        working: 'primary',
        charging: 'warning'
      };
      return types[status] || 'info';
    },

    getAgvStatusText(status) {
      const texts = {
        connected: '已连接',
        disconnected: '未连接',
        working: '工作中',
        charging: '充电中'
      };
      return texts[status] || status || '未知';
    },
    viewAgvDetails(agv) {
      this.$alert(`
        <div>
          <p><strong>AGV编号:</strong> ${agv.agvId}</p>
          <p><strong>IP地址:</strong> ${agv.ip}</p>
          <p><strong>端口:</strong> ${agv.port}</p>
          <p><strong>状态:</strong> ${this.getAgvStatusText(agv.status)}</p>
        </div>
      `, 'AGV详情', {
        dangerouslyUseHTMLString: true
      })
    },
    disconnectAgv(agv) {
      this.disconnectFromAgv(agv)
    },
    
    // 新增：批量检查所有连接状态
    async checkAllConnections() {
      try {
        console.log('执行批量连接状态检查...');
        const response = await request.get('/tcp-client/check-all-connections');
        if (response.code === '200') {
          console.log('批量检查结果:', response.data);
          // 可以在这里处理检查结果，比如显示通知
          const disconnectedCount = response.data.filter(item => !item.connected).length;
          if (disconnectedCount > 0) {
            console.log(`${disconnectedCount} 个AGV连接已断开`);
            // 可选：显示通知
            // this.$message.warning(`${disconnectedCount} 个AGV连接已断开`);
          }
        }
      } catch (error) {
        console.warn('批量检查连接状态失败:', error);
      }
    },
    
    // 新增：检查单个AGV连接状态
    async checkAgvConnection(agvId) {
      try {
        const response = await request.get(`/tcp-client/check-connection/${agvId}`);
        if (response.code === '200') {
          console.log(`AGV ${agvId} 连接状态:`, response.data);
          return response.data;
        }
      } catch (error) {
        console.warn(`检查AGV ${agvId} 连接状态失败:`, error);
      }
      return null;
    },
    
    // 新增：获取连接统计信息
    async getConnectionStats() {
      try {
        const response = await request.get('/tcp-client/connection-stats');
        if (response.code === '200') {
          console.log('连接统计信息:', response.data);
          return response.data;
        }
      } catch (error) {
        console.warn('获取连接统计信息失败:', error);
      }
      return null;
    },
    
    // 专门处理配置区域的断开连接
    async disconnectFromAgvConfig(agv) {
      try {
        console.log('配置区域断开连接，原始数据:', agv);
        
        // 查找对应的已连接AGV记录
        const connectedAgv = this.connectedAgvs.find(item => 
          item.ipAddress === agv.ip && item.port === agv.port
        );
        
        if (connectedAgv && connectedAgv.agvId) {
          console.log('找到对应的已连接AGV:', connectedAgv);
          await this.disconnectFromAgv(connectedAgv);
        } else {
          this.$message.warning('未找到对应的已连接AGV记录');
        }
      } catch (error) {
        console.error('配置区域断开连接错误:', error);
        this.$message.error('断开连接失败: ' + error.message);
      }
    },
    async mounted() {
      console.log('=== TCPServer组件挂载 ===');
      try {
        // 立即加载数据
        await this.loadConnectedAgvs();
        console.log('初始数据加载完成');
      } catch (error) {
        console.error('初始数据加载失败:', error);
        // 即使失败也设置定时器
      }
      
      // 设置定时刷新已连接AGV列表
      this.refreshInterval = setInterval(() => {
        this.loadConnectedAgvs();
      }, 3000); // 每3秒刷新一次
      console.log('定时器已设置');
    },
    
    activated() {
      // 当组件被激活时重新开始定时刷新
      if (!this.refreshInterval) {
        this.loadConnectedAgvs();
        this.refreshInterval = setInterval(() => {
          this.loadConnectedAgvs();
        }, 3000);
      } else {
        // 如果定时器已存在，立即刷新一次数据
        this.loadConnectedAgvs();
      }
    },
    
    deactivated() {
      // 当组件被停用时清除定时器但不清空数据
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
        this.refreshInterval = null;
      }
    },
    
    beforeDestroy() {
      // 清除定时器
      if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
      }
    },
    
    async deleteAgv(agv) {
      try {
        // 先确认是否真的要删除
        await this.$confirm(`确定要删除 AGV ${agv.agvId} 的记录吗？这将永久删除该AGV的所有信息。`, '删除确认', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        const response = await request.delete(`/tcp-client/delete/${agv.agvId}`);
        if (response.code === '200') {
          this.$message.success('删除成功');
          // 重新加载所有列表
          await this.loadConnectedAgvs();
        } else {
          this.$message.error(response.msg || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败: ' + (error.response?.data?.msg || error.message));
        }
      }
    }


  }
}
</script>

<style scoped>
.tcp-client-page {
  padding: 20px;
}

/* 使用 flex 布局优化按钮排列 */
.button-group {
  display: flex;
  justify-content: space-between; /* 按钮之间均匀分布 */
  align-items: center;
}
</style>