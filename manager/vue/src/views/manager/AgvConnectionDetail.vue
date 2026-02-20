<template>
  <el-dialog title="AGV 连接详情" :visible.sync="visible" width="50%">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="AGV编号">{{ agv.agvId }}</el-descriptions-item>
      <el-descriptions-item label="IP地址">{{ agv.ipAddress }}</el-descriptions-item>
      <el-descriptions-item label="MAC地址">{{ agv.macAddress }}</el-descriptions-item>
      <el-descriptions-item label="连接状态">
        <el-tag :type="getConnectionStatusType(agv.isOnline)">
          {{ getConnectionStatusText(agv.isOnline) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="最后更新时间">{{ agv.lastUpdateTime }}</el-descriptions-item>
      <el-descriptions-item label="当前任务">{{ agv.assignedTask || '无' }}</el-descriptions-item>
      <el-descriptions-item label="承载物料">{{ agv.carryingMaterial || '无' }}</el-descriptions-item>
    </el-descriptions>

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'AgvConnectionDetail',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    agv: {
      type: Object,
      default: () => ({})
    }
  },
  methods: {
    getConnectionStatusType(isOnline) {
      return isOnline ? 'success' : 'danger';
    },
    getConnectionStatusText(isOnline) {
      return isOnline ? '在线' : '离线';
    }
  }
};
</script>
