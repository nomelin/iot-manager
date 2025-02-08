<template>
  <div class="container">
    <div class="main-content">
      <el-collapse v-model="activeCollapse" class="upload-collapse">
        <el-collapse-item name="upload">
          <template #title>
            <div class="collapse-title">
              <i class="el-icon-upload"></i>
              <span>批量文件上传（点击展开/折叠）</span>
            </div>
          </template>
          <div class="collapse-content">
            <el-form ref="formRef" :model="form" :rules="rules">
              <!-- 多文件上传 -->
              <el-form-item prop="files">
                <el-upload
                    :auto-upload="false"
                    :file-list="fileList"
                    :limit="20"
                    :on-change="handleFileChange"
                    :on-exceed="handleExceed"
                    :on-remove="handleFileRemove"
                    action=null
                    class="upload-demo"
                    drag
                    multiple
                >
                  <!--                  <el-button size="medium" type="primary">选择多个文件</el-button>-->
                  <i class="el-icon-upload"></i>
                  <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                  <div slot="tip" class="el-upload__tip">支持最多20个文件，单个文件不超过100MB</div>
                  <!--                  <template #tip>-->
                  <!--                    <div class="el-upload__tip">-->
                  <!--                      支持最多20个文件，单个文件不超过100MB（自动过滤重复文件）-->
                  <!--                    </div>-->
                  <!--                  </template>-->
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
                    :disabled="fileList.length === 0"
                    :loading="isUploading"
                    type="primary"
                    @click="submitUpload"
                >
                  {{ isUploading ? '批量上传中...' : '开始批量上传' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 任务列表 -->
      <div class="task-list-container">
        <div class="task-list-header">
          任务列表（{{ Object.keys(taskInfos).length }}）
          <el-tag size="mini" type="info">点击上方折叠面板管理上传任务</el-tag>
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
                  :type="statusTagType(task.status)"
                  size="small"
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
          {required: true, message: '请输入设备ID', trigger: 'blur'},
          {type: 'number', min: 0, message: '设备ID不能为负数', trigger: 'blur'}
        ],
        skipRows: [
          {type: 'number', min: 0, message: '跳过的行数不能为负数', trigger: 'blur'}
        ]
      },
      fileList: [],// 上传文件列表
      taskInfos: {},
      pollingMap: new Map(),
      isUploading: false,
      fileSet: new Set() // 用于文件名去重
    }
  },
  beforeUnmount() {
    this.clearAllPolling()
  },
  methods: {
    formatDateTime(datetime) {
      return datetime ? dayjs(datetime).format('YYYY-MM-DD HH:mm') : '-'
    },
    generateFileKey(file) {
      return `${file.name}_${file.size}`;//按文件名和文件大小来去重
    },
    // 处理文件变化，移除重复的文件
    handleFileChange(file, files) {
      //file是当前处理的文件，files是添加这个文件之后的列表
      //几个一起上传时，会调用几次这个钩子函数。
      // console.log("file:" + file.name + "\nfiles:" + files.map(file => file.name))
      const key = this.generateFileKey(file);
      if (this.fileSet.has(key)) {
        this.$message.warning(`文件 "${file.name}" 已存在，自动忽略`);
        //删除重复文件
        this.fileList = this.fileList.filter(f => f.uid !== file.uid)
        return false;
      }
      this.fileSet.add(key);
      this.fileList.push(file);
      // console.log("fileSet:" + Array.from(this.fileSet) + "\nfileList:" + this.fileList.map(file => file.name))
      return true;
    },
    handleFileRemove(file) {
      this.fileSet.delete(this.generateFileKey(file))
      this.fileList = this.fileList.filter(f => f.uid !== file.uid)
      // console.log("fileSet:" + Array.from(this.fileSet) + "\nfileList:" + this.fileList.map(file => file.name))
    },
    handleExceed() {
      this.$message.error("最多同时上传20个文件")
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
              headers: {'Content-Type': 'multipart/form-data'}
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
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止全局滚动条 */
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden; /* 关键样式 */
}

.upload-collapse {
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-height: 60px;

}

::v-deep .el-collapse-item__header {
  padding: 0 24px;
  height: 60px;
  border-bottom: none;
  border-radius: 1rem;
}

::v-deep .el-collapse-item__content {
  padding: 0 24px;
  overflow-y: auto; /* 折叠面板内部滚动 */
  max-height: 50vh; /* 限制最大高度 */
  /*border-radius: 1rem;*/
}

.collapse-content {
  padding-bottom: 24px;
  /*border-radius: 1rem;*/
}

.task-list-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-top: 16px;
  overflow: hidden; /* 关键样式 */
}

.task-list-header {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
}

.task-item {
  padding: 12px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border-radius: 1rem;
}

/*鼠标指针选中时偏移卡片*/
.task-item:hover {
  transform: translateX(4px);
  box-shadow: 2px 2px 8px rgba(0, 0, 0, 0.1);

}

.task-item:last-child {
  margin-bottom: 0;
}

.time-info {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #909399;
  margin: 8px 0;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.filename {
  flex: 1;
  margin-right: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

/* 其他优化样式 */
.el-form-item {
  margin-bottom: 18px;
}

.el-input {
  max-width: 400px;
}

.upload-demo {
  width: 100%;
}

/*/deep/ .el-collapse-item__header{*/
/*  border-radius: 1rem;*/
/*}*/
</style>