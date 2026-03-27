<template>
  <div class="task-scheduler">
    <h2>任务调度管理</h2>

    <!-- 任务统计 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.totalTasks }}</div>
            <div class="stat-label">总任务数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.pendingTasks }}</div>
            <div class="stat-label">待分配</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.executingTasks }}</div>
            <div class="stat-label">执行中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-number">{{ stats.completedTasks }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和操作 -->
    <div class="search-operation">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索任务编号、物料类型"
          style="width: 200px; margin-right: 10px;"
          @keyup.enter.native="loadTasks(1)"
      />
      <el-button type="info" plain @click="loadTasks(1)">查询</el-button>
      <el-button type="warning" plain @click="resetSearch">重置</el-button>
      <el-button type="primary" plain style="float: right;" @click="createTaskDialog = true">
        创建任务
      </el-button>
    </div>

    <!-- 任务列表 -->
    <el-card style="margin-top: 20px;">
      <el-table :data="tasks" style="width: 100%">
        <el-table-column prop="taskId" label="任务编号" width="150" />
        <el-table-column prop="taskType" label="任务类型" width="100">
          <template v-slot="scope">
            <el-tag :type="getTaskTypeTag(scope.row.taskType)">
              {{ formatTaskType(scope.row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialType" label="物料类型" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="startPoint" label="起点" width="100">
          <template v-slot="scope">
            <span>{{ getNodeDescription(scope.row.startPoint) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endPoint" label="终点" width="100">
          <template v-slot="scope">
            <span>{{ getNodeDescription(scope.row.endPoint) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="agvId" label="分配AGV" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template v-slot="scope">
            <el-tag :type="getTaskStatusTag(scope.row.status)">
              {{ formatTaskStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template v-slot="scope">
            <el-tag :type="getPriorityTag(scope.row.priority)">
              {{ formatPriority(scope.row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <!-- 修改操作列的宽度为220，确保三个按钮在同一行 -->
        <el-table-column label="操作" width="220">
          <template v-slot="scope">
            <el-button size="mini" @click="viewTaskDetails(scope.row)">详情</el-button>
            <el-button
                size="mini"
                type="primary"
                :disabled="scope.row.status !== 'pending'"
                @click="assignTask(scope.row)"
            >分配</el-button>
            <el-button
                size="mini"
                type="danger"
                :disabled="scope.row.status === 'completed' || scope.row.status === 'cancelled'"
                @click="cancelTask(scope.row.id)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            @current-change="handleCurrentChange"
            :current-page="pageNum"
            :page-sizes="[5, 10, 20]"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 创建任务对话框 -->
    <el-dialog title="创建配送任务" :visible.sync="createTaskDialog" width="50%" :close-on-click-modal="false">
      <el-form :model="newTask" label-width="100px" :rules="taskRules" ref="taskForm">
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="newTask.taskType" placeholder="选择任务类型">
            <el-option label="配送" value="delivery"></el-option>
            <el-option label="回收" value="pickup"></el-option>
            <el-option label="充电" value="charge"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="物料类型" prop="materialType">
          <el-input v-model="newTask.materialType" placeholder="输入物料类型"></el-input>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="newTask.quantity" :min="1" :max="1000"></el-input-number>
        </el-form-item>
        <el-form-item label="起点位置" prop="startPoint">
          <el-select v-model="newTask.startPoint" placeholder="选择起点位置" filterable>
            <el-option
                v-for="node in allStorageNodes"
                :key="node.nodeId"
                :label="`${node.nodeId} - ${node.description || node.nodeType}`"
                :value="node.nodeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="终点位置" prop="endPoint">
          <el-select v-model="newTask.endPoint" placeholder="选择终点位置" filterable>
            <el-option
                v-for="node in allAssemblyNodes"
                :key="node.nodeId"
                :label="`${node.nodeId} - ${node.description || node.nodeType}`"
                :value="node.nodeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="newTask.priority">
            <el-radio label="low">低</el-radio>
            <el-radio label="medium">中</el-radio>
            <el-radio label="high">高</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="需求节拍">
          <el-date-picker
              v-model="newTask.needTime"
              type="datetime"
              placeholder="选择需求时间"
              format="yyyy-MM-dd HH:mm:ss"
              value-format="yyyy-MM-dd HH:mm:ss">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="createTaskDialog = false">取消</el-button>
        <el-button type="primary" @click="submitTask">创建</el-button>
      </div>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog title="任务详情" :visible.sync="detailDialogVisible" width="50%">
      <div v-if="selectedTask">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务编号">{{ selectedTask.taskId }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">
            <el-tag :type="getTaskTypeTag(selectedTask.taskType)">
              {{ formatTaskType(selectedTask.taskType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="物料类型">{{ selectedTask.materialType }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ selectedTask.quantity }}</el-descriptions-item>
          <el-descriptions-item label="起点">
            {{ getNodeDescription(selectedTask.startPoint) }}
          </el-descriptions-item>
          <el-descriptions-item label="终点">
            {{ getNodeDescription(selectedTask.endPoint) }}
          </el-descriptions-item>
          <el-descriptions-item label="分配AGV">{{ selectedTask.agvId || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getTaskStatusTag(selectedTask.status)">
              {{ formatTaskStatus(selectedTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityTag(selectedTask.priority)">
              {{ formatPriority(selectedTask.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedTask.createTime }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ selectedTask.startTime || '未开始' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ selectedTask.endTime || '未结束' }}</el-descriptions-item>
          <el-descriptions-item label="需求时间">{{ selectedTask.needTime || '无要求' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'TaskScheduler',
  data() {
    return {
      tasks: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      searchKeyword: '',
      stats: {
        totalTasks: 0,
        pendingTasks: 0,
        executingTasks: 0,
        completedTasks: 0
      },
      createTaskDialog: false,
      detailDialogVisible: false,
      selectedTask: {},
      newTask: {
        taskType: 'delivery',
        materialType: '',
        quantity: 1,
        startPoint: '',
        endPoint: '',
        priority: 'medium',
        needTime: null
      },
      allStorageNodes: [],
      allAssemblyNodes: [],
      allMapNodes: [], // 存储所有地图节点
      refreshInterval: null, // 定时刷新定时器
      taskRules: {
        taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
        materialType: [{ required: true, message: '请输入物料类型', trigger: 'blur' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
        startPoint: [{ required: true, message: '请选择起点位置', trigger: 'change' }],
        endPoint: [{ required: true, message: '请选择终点位置', trigger: 'change' }]
      }
    }
  },

  mounted() {
    this.loadTasks(1);
    this.loadAllMapNodes(); // 加载所有地图节点
    // 设置定时刷新，每 3 秒更新一次任务和统计数据
    this.refreshInterval = setInterval(() => {
      this.loadTasks(this.pageNum);
    }, 3000);
  },
  
  beforeDestroy() {
    // 清除定时器
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  },

  methods: {
    async loadTasks(pageNum) {
      if (pageNum) this.pageNum = pageNum;
    
      try {
        const response = await request.get('/task/selectPage', {
          params: {
            pageNum: this.pageNum,
            pageSize: this.pageSize,
            taskId: this.searchKeyword || undefined,
            materialType: this.searchKeyword || undefined
          }
        });
    
        this.tasks = response.data?.list || [];
        this.total = response.data?.total || 0;
            
        // 每次加载任务后更新统计数据
        this.updateStats();
      } catch (error) {
        console.error('加载任务失败:', error);
        this.$message.error('加载任务失败：' + (error.response?.data?.msg || error.message));
      }
    },

    // 加载所有地图节点
    async loadAllMapNodes() {
      try {
        const response = await request.get('/map/nodes');
        this.allMapNodes = response.data || [];

        // 按类型分类节点
        this.allStorageNodes = this.allMapNodes.filter(node => node.nodeType === 'storage' || node.nodeType === 'warehouse');
        this.allAssemblyNodes = this.allMapNodes.filter(node => node.nodeType === 'assembly' || node.nodeType === 'workstation' || node.nodeType === 'station');

        // 如果没有按类型分类的节点，使用所有节点
        if (this.allStorageNodes.length === 0) {
          this.allStorageNodes = this.allMapNodes;
        }
        if (this.allAssemblyNodes.length === 0) {
          this.allAssemblyNodes = this.allMapNodes;
        }
      } catch (error) {
        console.error('加载地图节点失败:', error);
      }
    },

    // 根据节点ID获取节点描述
    getNodeDescription(nodeId) {
      const node = this.allMapNodes.find(n => n.nodeId === nodeId);
      if (node) {
        return `${node.nodeId}${node.description ? ' - ' + node.description : ''}`;
      }
      return nodeId || '未设置';
    },

    updateStats() {
      try {
        // 根据当前任务列表计算统计数据
        this.stats.totalTasks = this.total;
        this.stats.pendingTasks = this.tasks.filter(t => t.status === 'pending').length;
        this.stats.executingTasks = this.tasks.filter(t => t.status === 'executing' || t.status === 'assigned').length;
        this.stats.completedTasks = this.tasks.filter(t => t.status === 'completed').length;
        console.log('统计数据已更新:', this.stats);
      } catch (error) {
        console.error('更新统计失败:', error);
      }
    },
    
    async loadStats() {
      // 保留此方法以兼容，但现在使用 updateStats()
      this.updateStats();
    },

    handleCurrentChange(pageNum) {
      this.loadTasks(pageNum);
    },

    resetSearch() {
      this.searchKeyword = '';
      this.loadTasks(1);
    },

    getTaskTypeTag(taskType) {
      const tags = {
        'delivery': 'success',
        'pickup': 'warning',
        'charge': 'primary'
      };
      return tags[taskType] || 'info';
    },

    formatTaskType(taskType) {
      const formats = {
        'delivery': '配送',
        'pickup': '回收',
        'charge': '充电'
      };
      return formats[taskType] || taskType;
    },

    getTaskStatusTag(status) {
      const tags = {
        'pending': 'info',
        'assigned': 'primary',
        'executing': 'warning',
        'completed': 'success',
        'cancelled': 'danger'
      };
      return tags[status] || 'info';
    },

    formatTaskStatus(status) {
      const formats = {
        'pending': '待分配',
        'assigned': '已分配',
        'executing': '执行中',
        'completed': '已完成',
        'cancelled': '已取消'
      };
      return formats[status] || status;
    },

    getPriorityTag(priority) {
      const tags = {
        'low': 'info',
        'medium': 'warning',
        'high': 'danger'
      };
      return tags[priority] || 'info';
    },

    formatPriority(priority) {
      const formats = {
        'low': '低',
        'medium': '中',
        'high': '高'
      };
      return formats[priority] || priority;
    },

    viewTaskDetails(task) {
      this.selectedTask = {...task};
      this.detailDialogVisible = true;
    },

    async assignTask(task) {
      try {
        await request.post('/task/assign', { id: task.id });
        this.$message.success('任务分配成功');
        this.loadTasks(this.pageNum);
      } catch (error) {
        console.error('分配任务失败:', error);
        this.$message.error('分配任务失败: ' + (error.response?.data?.msg || error.message));
      }
    },

    async cancelTask(taskId) {
      this.$confirm('确定要取消这个任务吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const task = { id: taskId, status: 'cancelled' };
          await request.put('/task/updateStatus', task);
          this.$message.success('任务已取消');
          this.loadTasks(this.pageNum);
        } catch (error) {
          console.error('取消任务失败:', error);
          this.$message.error('取消任务失败: ' + (error.response?.data?.msg || error.message));
        }
      }).catch(() => {
        // 用户取消操作
      });
    },

// 在 TaskScheduler.vue 中优化请求处理
// 在 TaskScheduler.vue 中优化请求处理
    async submitTask() {
      this.$refs.taskForm.validate(async (valid) => {
        if (valid) {
          try {
            // 确保时间格式正确
            const taskData = { ...this.newTask };

            // 如果needTime为空，设为null而不是undefined
            if (!taskData.needTime) {
              taskData.needTime = null;
            } else {
              // 确保日期格式为字符串格式
              if (taskData.needTime instanceof Date) {
                taskData.needTime = this.formatDate(taskData.needTime);
              }
            }

            console.log('发送任务数据:', taskData);

            const response = await request.post('/task/create', taskData);
            console.log('任务创建响应:', response);

            if (response.code === '200') {
              this.$message.success('任务创建成功');
              this.createTaskDialog = false;
              this.resetForm();
              this.loadTasks(1);
            } else {
              this.$message.error(response.msg || '任务创建失败');
            }
          } catch (error) {
            console.error('创建任务失败:', error);
            // 显示更详细的错误信息
            if (error.response && error.response.data) {
              this.$message.error('创建任务失败: ' + (error.response.data.msg || error.response.data));
            } else {
              this.$message.error('创建任务失败: ' + (error.message || '网络错误'));
            }
          }
        }
      });
    },

// 添加日期格式化方法
    formatDate(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');

      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
    ,

    resetForm() {
      this.newTask = {
        taskType: 'delivery',
        materialType: '',
        quantity: 1,
        startPoint: '',
        endPoint: '',
        priority: 'medium',
        needTime: null
      };
      this.$refs.taskForm?.clearValidate();
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
.search-operation {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>
