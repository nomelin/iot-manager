<template>
  <div class="debug-container">
    <!-- 缓存操作区域 -->
    <el-card class="debug-section">
      <div class="section-header">
        <h3>缓存操作</h3>
        <div>
          <el-button :loading="cacheKeysLoading" type="primary" @click="getCacheKeys">刷新缓存键</el-button>
          <el-button type="danger" @click="clearCache">清除全部缓存</el-button>
        </div>
      </div>
      <div class="tags-container">
        <el-tag v-for="key in cacheKeys" :key="key" class="cache-key" type="info">{{ key }}</el-tag>
        <div v-if="cacheKeys.length === 0" class="empty-tip">暂无缓存数据</div>
      </div>
    </el-card>

    <!-- 缓存统计信息 -->
    <el-card class="debug-section">
      <div class="section-header">
        <h3>缓存统计</h3>
        <el-button :loading="statsLoading" type="primary" @click="getCacheStats">刷新统计</el-button>
      </div>
      <el-table :data="[cacheStats]" border style="width: 100%">
        <el-table-column
            v-for="(value, key) in cacheStats"
            :key="key"
            :label="key"
            :prop="key"
            min-width="150"
        />
      </el-table>
    </el-card>

    <!-- 任务管理 -->
    <el-card class="debug-section">
      <div class="section-header">
        <h3>任务列表</h3>
        <el-button :loading="taskIdsLoading" type="primary" @click="getTaskIds">刷新任务ID</el-button>
      </div>
      <div class="tags-container">
        <el-tag
            v-for="id in taskIds"
            :key="id"
            :type="getTaskTagType(id)"
            class="task-id"
            @click="showTaskDetail(id)"
        >
          {{ id }}
        </el-tag>
        <div v-if="taskIds.length === 0" class="empty-tip">暂无任务数据</div>
      </div>
    </el-card>

    <!-- 任务详情查询 -->
    <el-card class="debug-section">
      <div class="section-header">
        <h3>任务详情查询</h3>
        <div style="display: flex; gap: 10px">
          <el-input
              v-model="taskIdInput"
              placeholder="请输入任务ID"
              style="width: 200px"
          />
          <el-button
              :disabled="!taskIdInput"
              :loading="taskDetailLoading"
              type="primary"
              @click="getTaskDetail"
          >
            查询
          </el-button>
        </div>
      </div>
      <div v-if="currentTask" class="task-detail">
        <el-descriptions border column="2" title="任务详情">
          <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatTaskStatus(currentTask.status) }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentTask.fileName }}</el-descriptions-item>
          <el-descriptions-item label="总行数">{{ currentTask.totalRows }}</el-descriptions-item>
          <el-descriptions-item label="已处理行数">{{ currentTask.processedRows }}</el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress
                :percentage="Math.round((currentTask.processedRows / currentTask.totalRows) * 100)"
                :status="getProgressStatus(currentTask.status)"
            />
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatTime(currentTask.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatTime(currentTask.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" span="2">
            <pre class="error-message">{{ currentTask.errorMessage }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 临时文件管理 -->
    <el-card class="debug-section">
      <div class="section-header">
        <h3>临时文件</h3>
        <div>
          <el-button :loading="tempFilesLoading" type="primary" @click="getTempFiles">刷新文件列表</el-button>
          <el-button type="danger" @click="deleteAllTempFiles">清空临时文件</el-button>
        </div>
      </div>
      <div class="tags-container">
        <el-tag v-for="file in tempFiles" :key="file" class="cache-key" type="info">{{ file }}</el-tag>
        <div v-if="tempFiles.length === 0" class="empty-tip">暂无临时文件</div>
      </div>
    </el-card>


  </div>
</template>

<script>
import dayjs from 'dayjs'

export default {
  data() {
    return {
      // 缓存相关
      cacheKeys: [],
      cacheKeysLoading: false,
      cacheStats: {},
      statsLoading: false,

      // 任务相关
      taskIds: [],
      taskIdsLoading: false,
      taskIdInput: '',
      currentTask: null,
      taskDetailLoading: false,

      // 临时文件相关
      tempFiles: [],
      tempFilesLoading: false,
    }
  },

  mounted() {
    this.getCacheKeys()
    this.getCacheStats()
    this.getTaskIds()
    this.getTempFiles()
  },

  methods: {
    // 缓存操作
    async getCacheKeys() {
      this.cacheKeysLoading = true
      try {
        const res = await this.$request.get('/debug/cache/listAllKeys')
        if (res.code === '200') {
          this.cacheKeys = res.data || []
        }
      } finally {
        this.cacheKeysLoading = false
      }
    },

    async clearCache() {
      try {
        await this.$confirm('确定要清除全部缓存吗？此操作不可恢复！', '警告', {
          type: 'warning'
        })
        const res = await this.$request.get('/debug/cache/clearAll')
        if (res.code === '200') {
          this.$message.success('缓存已清除')
          this.getCacheKeys()
          this.getCacheStats()
        }
      } catch (error) {
        // 取消操作不提示
      }
    },

    async getCacheStats() {
      this.statsLoading = true
      try {
        const res = await this.$request.get('/debug/cache/getStats')
        if (res.code === '200') {
          this.cacheStats = res.data || {}
        }
      } finally {
        this.statsLoading = false
      }
    },

    // 任务操作
    async getTaskIds() {
      this.taskIdsLoading = true
      try {
        const res = await this.$request.get('/debug/task/getAllTaskIds')
        if (res.code === '200') {
          this.taskIds = res.data || []
        }
      } finally {
        this.taskIdsLoading = false
      }
    },

    async getTaskDetail() {
      if (!this.taskIdInput) return
      this.taskDetailLoading = true
      try {
        const res = await this.$request.get(`/debug/task/getTask/${this.taskIdInput}`)
        if (res.code === '200') {
          this.currentTask = res.data
        }
      } finally {
        this.taskDetailLoading = false
      }
    },

    showTaskDetail(id) {
      this.taskIdInput = id
      this.getTaskDetail()
    },

    // 工具方法
    formatTaskStatus(status) {
      const map = {
        QUEUED: '排队中',
        PROCESSING: '处理中',
        PAUSED: '已暂停',
        COMPLETED: '已完成',
        FAILED: '失败',
        CANCELLED: '已取消'
      }
      return map[status] || '未知状态'
    },

    getTaskTagType(id) {
      if (!this.currentTask || this.currentTask.id !== id) return 'info'
      const statusMap = {
        COMPLETED: 'success',
        FAILED: 'danger',
        CANCELLED: 'warning',
        PROCESSING: '',
        QUEUED: 'info',
        PAUSED: 'warning'
      }
      return statusMap[this.currentTask.status] || 'info'
    },

    getProgressStatus(status) {
      const map = {
        COMPLETED: 'success',
        FAILED: 'exception',
        CANCELLED: 'warning',
        PROCESSING: null,
        QUEUED: null,
        PAUSED: null
      }
      return map[status] || null
    },

    formatTime(time) {
      return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
    },

    // 临时文件操作
    async getTempFiles() {
      this.tempFilesLoading = true;
      try {
        const res = await this.$request.get('/debug/file/allTempFiles');
        if (res.code === '200') {
          this.tempFiles = res.data || [];
        }
      } finally {
        this.tempFilesLoading = false;
      }
    },

    async deleteAllTempFiles() {
      try {
        await this.$confirm('确定要清空所有临时文件吗？', '警告', {
          type: 'warning'
        });
        const res = await this.$request.get('/debug/file/deleteAllTempFiles');
        if (res.code === '200') {
          this.$message.success('临时文件已清空');
          await this.getTempFiles();
        }
      } catch (error) {
        // 取消操作不提示
      }
    },

  }
}
</script>

<style scoped>
.debug-container {
  height: 100vh;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.debug-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 5px 0;
}

.cache-key, .task-id {
  cursor: default;
  user-select: none;
}

.task-id {
  cursor: pointer;
  transition: all 0.3s;
}

.task-id:hover {
  transform: scale(1.05);
}

.empty-tip {
  color: #999;
  font-size: 14px;
}

.error-message {
  color: #f56c6c;
  white-space: pre-wrap;
  margin: 0;
}

.task-detail {
  margin-top: 15px;
}
</style>