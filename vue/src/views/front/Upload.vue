<template>
  <div class="container">
    <div class="header">
      <!-- 可添加统一头部内容 -->
    </div>
    <div class="main-content">
      <div class="upload-box">
        <div class="upload-form">
          <div class="title">文 件 上 传</div>
          <el-form ref="formRef" :model="form" :rules="rules">
            <!-- 文件上传 -->
            <el-form-item prop="file">
              <el-upload
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :show-file-list="false"
                  class="upload-demo"
              >
                <el-button size="medium" type="primary">选择文件</el-button>
                <span v-if="form.file" class="file-name">{{ form.file.name }}</span>
              </el-upload>
            </el-form-item>

            <!-- 设备ID -->
            <el-form-item prop="deviceId">
              <div class="custom-input">
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
              </div>
            </el-form-item>

            <!-- 跳过行数 -->
            <el-form-item prop="skipRows">
              <div class="custom-input">
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
              </div>
            </el-form-item>

            <!-- 提交按钮 -->
            <el-form-item>
              <el-button
                  :loading="isUploading"
                  type="primary"
                  @click="submitUpload"
              >
                {{ isUploading ? '上传中...' : '开始上传' }}
              </el-button>
            </el-form-item>

            <!-- 进度展示 -->
            <div v-if="taskInfo" class="progress-container">
              <div class="progress-info">
                <span>状态：{{ taskInfo.status }}</span>
                <span>进度：{{ taskInfo.processedRows }}/{{ taskInfo.totalRows }}</span>
                <span>{{ progressPercentage }}%</span>
              </div>
              <el-progress
                  :percentage="progressPercentage"
                  :status="progressStatus"
                  :stroke-width="16"
              />
              <div v-if="taskInfo.errorMessage" class="error-message">
                错误信息：{{ taskInfo.errorMessage }}
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Upload',
  data() {
    return {
      form: {
        file: null,
        deviceId: null,
        skipRows: 0
      },
      rules: {
        file: [
          {required: true, message: '请选择文件', trigger: 'change'}
        ],
        deviceId: [
          {required: true, message: '请输入设备ID', trigger: 'blur'},
          {type: 'number', min: 0, message: '设备ID不能为负数', trigger: 'blur'}
        ],
        skipRows: [
          {type: 'number', min: 0, message: '跳过的行数不能为负数', trigger: 'blur'}
        ]
      },
      taskId: null,
      taskInfo: null,
      pollingInterval: null,
      isUploading: false
    }
  },
  computed: {
    progressPercentage() {
      return this.taskInfo ? Math.round(this.taskInfo.progressPercentage || 0) : 0
    },
    progressStatus() {
      if (!this.taskInfo) return 'success'
      switch (this.taskInfo.status) {
        case 'FAILED':
          return 'exception'
        case 'COMPLETED':
          return 'success'
        default:
          return null
      }
    }
  },
  beforeUnmount() {
    this.clearPolling()
  },
  methods: {
    handleFileChange(file) {
      if (file.size > 100 * 1024 * 1024) {
        this.$message.error('文件大小不能超过100MB')
        return false
      }
      this.form.file = file.raw
      this.$refs.formRef.validateField('file')
    },

    async submitUpload() {
      try {
        await this.$refs.formRef.validate()
        this.isUploading = true

        // 构建FormData
        const formData = new FormData()
        formData.append('file', this.form.file)
        formData.append('deviceId', this.form.deviceId)
        formData.append('skipRows', this.form.skipRows)

        // 调用上传接口
        const res = await this.$request.post('/files/upload', formData, {
          headers: {'Content-Type': 'multipart/form-data'}
        })

        if (res.code === '200') {
          this.taskId = res.data
          this.startPolling()
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('上传失败: ' + (error.message || error))
      } finally {
        this.isUploading = false
      }
    },

    startPolling() {
      this.clearPolling()
      this.pollingInterval = setInterval(async () => {
        try {
          const res = await this.$request.get(`/task/get/${this.taskId}`)
          if (res.code === '200') {
            this.taskInfo = res.data
            console.log("taskInfo:" + this.taskInfo)
            // 处理结束状态
            if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(this.taskInfo.status)) {
              this.clearPolling()
              if (this.taskInfo.status === 'FAILED') {
                this.$message.error('处理失败: ' + (this.taskInfo.errorMessage || ''))
              }
            }
          }
        } catch (error) {
          this.$message.error('获取任务状态失败')
          this.clearPolling()
        }
      }, 1000)
    },

    clearPolling() {
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval)
        this.pollingInterval = null
      }
    }
  }
}
</script>

<style scoped>
/* 复用注册页面的样式基础 */
.container {
  min-height: 100vh;
  background: #f0f2f5;
}

.upload-box {
  max-width: 600px;
  margin: 0 auto;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.title {
  font-size: 24px;
  color: #303133;
  text-align: center;
  margin-bottom: 30px;
}

.custom-input {
  margin: 10px 0;
}

.file-name {
  margin-left: 10px;
  color: #606266;
}

.progress-container {
  margin-top: 20px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
}

.error-message {
  margin-top: 10px;
  color: #f56c6c;
  font-size: 14px;
}

.el-button {
  width: 100%;
  margin-top: 20px;
}
</style>