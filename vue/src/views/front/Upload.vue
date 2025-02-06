<template>
  <div class="container">
    <div class="main-content">
      <el-collapse v-model="activeCollapse" class="upload-collapse" accordion>
        <el-collapse-item name="upload">
          <template #title>
            <div class="collapse-title">
              <i class="el-icon-upload"></i>
              <span>批量文件上传（点击展开/折叠）</span>
            </div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules">
            <!-- 多文件上传 -->
            <el-form-item prop="files">
              <el-upload
                  multiple
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :on-remove="handleFileRemove"
                  :file-list="fileList"
                  :limit="20"
                  class="upload-demo"
              >
                <el-button size="medium" type="primary">选择多个文件</el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    支持最多20个文件，单个文件不超过100MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <!-- 设备ID -->
            <el-form-item prop="deviceId">
              <el-input
                  v-model.number="form.deviceId"
                  min="0"
                  placeholder="请输入设备ID"
                  type="number"
              >
                <template #prefix>
                  <i class="el-icon-cpu"></i>
                </template>
              </el-input>
            </el-form-item>

            <!-- 跳过行数 -->
            <el-form-item prop="skipRows">
              <el-input
                  v-model.number="form.skipRows"
                  min="0"
                  placeholder="请输入跳过的行数"
                  type="number"
              >
                <template #prefix>
                  <i class="el-icon-skip"></i>
                </template>
              </el-input>
            </el-form-item>

            <!-- 提交按钮 -->
            <el-form-item>
              <el-button
                  :loading="isUploading"
                  type="primary"
                  @click="submitUpload"
                  :disabled="fileList.length === 0"
              >
                {{ isUploading ? '批量上传中...' : '开始批量上传' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>

      <!-- 任务列表 -->
      <div class="task-list-container">
        <div class="task-list-header">
          任务列表（{{ Object.keys(taskInfos).length }}）
          <el-tag type="info" size="mini">点击上方折叠面板管理上传任务</el-tag>
        </div>
        <div class="task-list">
          <div
              v-for="[taskId, task] in Object.entries(taskInfos)"
              :key="taskId"
              class="task-item"
          >
            <div class="task-meta">
              <span class="filename">{{ task.fileName }}</span>
              <el-tag
                  size="small"
                  :type="statusTagType(task.status)"
              >
                {{ statusText(task.status) }}
              </el-tag>
            </div>

            <div class="time-info">
              <span>开始：{{ formatDateTime(task.startTime) }}</span>
              <span>结束：{{ formatDateTime(task.endTime) }}</span>
            </div>

            <div class="progress-container">
              <el-progress
                  :percentage="Math.round(task.progressPercentage || 0)"
                  :status="progressStatus(task.status)"
                  :stroke-width="14"
              />
              <div class="progress-detail">
                <span>已处理：{{ task.processedRows || 0 }}/{{ task.totalRows || '?' }}行</span>
                <span v-if="task.speed">速度：{{ task.speed }}/s</span>
              </div>
              <div v-if="task.errorMessage" class="error-message">
                {{ task.errorMessage }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'

export default {
  name: 'BatchUpload',
  data() {
    return {
      activeCollapse: ['upload'], // 默认展开上传面板
      form: {
        deviceId: null,
        skipRows: 1
      },
      rules: {
        deviceId: [
          { required: true, message: '请输入设备ID', trigger: 'blur' },
          { type: 'number', min: 0, message: '设备ID不能为负数', trigger: 'blur' }
        ],
        skipRows: [
          { type: 'number', min: 0, message: '跳过的行数不能为负数', trigger: 'blur' }
        ]
      },
      fileList: [],
      taskInfos: {},
      pollingMap: new Map(),
      isUploading: false
    }
  },
  beforeUnmount() {
    this.clearAllPolling()
  },
  methods: {
    formatDateTime(datetime) {
      return datetime ? dayjs(datetime).format('YYYY-MM-DD HH:mm') : '-'
    },

    handleFileChange(file, files) {
      if (file.size > 100 * 1024 * 1024) {
        this.$message.error(`${file.name} 超过100MB限制`)
        return false
      }
      this.fileList = files
    },
    handleFileRemove(file, fileList) {
      this.fileList = fileList
    },

    async submitUpload() {
      try {
        await this.$refs.formRef.validate()
        this.isUploading = true
        console.log('开始上传')
        console.log(this.activeCollapse)
        this.activeCollapse = [] // 提交后自动折叠面板

        for (const file of this.fileList) {
          const formData = new FormData()
          formData.append('file', file.raw)
          formData.append('deviceId', this.form.deviceId)
          formData.append('skipRows', this.form.skipRows)

          try {
            const res = await this.$request.post('/files/upload', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (res.code === '200') {
              const taskId = res.data
              this.$set(this.taskInfos, taskId, {
                fileName: file.name,
                status: 'PENDING',
                progressPercentage: 0,
                startTime: null,
                endTime: null
              })
              this.startPolling(taskId)
            }
          } catch (error) {
            this.$message.error(`${file.name} 上传失败: ${error.message}`)
          }
        }
      } catch (error) {
        this.$message.error('表单验证失败')
      } finally {
        this.isUploading = false
      }
    },

    startPolling(taskId) {
      // 先停止已有轮询
      if (this.pollingMap.has(taskId)) {
        clearInterval(this.pollingMap.get(taskId))
      }

      const interval = setInterval(async () => {
        try {
          const res = await this.$request.get(`/task/get/${taskId}`)
          if (res.code === '200') {
            // 合并后端返回的最新状态
            this.$set(this.taskInfos, taskId, {
              ...this.taskInfos[taskId], // 保留前端信息
              ...res.data,               // 覆盖后端数据
              fileName: this.taskInfos[taskId].fileName // 保持文件名不变
            })

            // 终止轮询条件
            if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(res.data.status)) {
              clearInterval(interval)
              this.pollingMap.delete(taskId)
            }
          }
        } catch (error) {
          console.error(`轮询任务${taskId}失败:`, error)
          clearInterval(interval)
          this.pollingMap.delete(taskId)
        }
      }, 100)

      this.pollingMap.set(taskId, interval)
    },

    clearAllPolling() {
      this.pollingMap.forEach(interval => clearInterval(interval))
      this.pollingMap.clear()
    },

    statusTagType(status) {
      const map = {
        PROCESSING: 'info',
        COMPLETED: 'success',
        FAILED: 'danger',
        PENDING: 'warning'
      }
      return map[status] || 'info'
    },

    statusText(status) {
      const map = {
        PROCESSING: '处理中',
        COMPLETED: '已完成',
        FAILED: '失败',
        PENDING: '等待中'
      }
      return map[status] || '未知状态'
    },

    progressStatus(status) {
      return status === 'FAILED' ? 'exception' :
          status === 'COMPLETED' ? 'success' : null
    }
  }
}
</script>

<style scoped>
.container {
  height: 100%;
  width: 99%;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.upload-collapse {
  margin-bottom: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

::v-deep .el-collapse-item__header {
  padding: 0 24px;
  font-weight: 500;
  height: 60px;
}

::v-deep .el-collapse-item__content {
  padding: 0 24px 24px;
}
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-list-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
  max-height: 100%;
}

.task-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  max-height: 100%;
}

.time-info {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #909399;
  margin: 8px 0;
}

.task-item {
  max-width: 90%;
  padding: 16px;
  margin-bottom: 12px;
  background: #f8f9fa;
  border-radius: 1rem;
  transition: all 0.3s;

&:hover {
   transform: translateX(4px);
   box-shadow: 2px 2px 8px rgba(0, 0, 0, 0.1);
 }
}

.error-message{
  color: red;
}

/* 其他样式保持原有实现不变 */
</style>