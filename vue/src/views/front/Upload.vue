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
            <el-form ref="formRef" :model="form" :rules="rules" label-position="left" label-width="200px">
              <!-- 多文件上传 -->
              <el-form-item label="数据文件" prop="files">
                <el-upload
                    :auto-upload="false"
                    :file-list="fileList"
                    :limit="fileLimit"
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
                  <div slot="tip" class="el-upload__tip">支持同时上传{{
                      this.fileLimit
                    }}个文件
                    <!--                    ，单个文件不超过100MB-->
                  </div>
                  <!--                  <template #tip>-->
                  <!--                    <div class="el-upload__tip">-->
                  <!--                      支持最多20个文件，单个文件不超过100MB（自动过滤重复文件）-->
                  <!--                    </div>-->
                  <!--                  </template>-->
                </el-upload>
                <el-button
                    :disabled="fileList.length === 0"
                    type="warning"
                    @click="clearAllFiles"
                >
                  清空所有文件
                </el-button>
              </el-form-item>

              <!-- 设备ID -->
              <el-form-item label="设备">
                <el-select
                    v-model="form.deviceId"
                    placeholder="请选择设备"
                >
                  <el-option
                      v-for="device in allDevices"
                      :key="device.id"
                      :label="device.name"
                      :value="device.id"
                  />
                </el-select>
                <!-- 显示设备缩略卡片 -->
                <div v-if="form.deviceId" class="device_preview">
                  <device-card-mini :device="selectedDevice"
                                    :show-data-types="true"/>
                </div>

              </el-form-item>

              <el-form-item label="是否使用文件名作为数据标签">
                <el-switch
                    v-model="usingFileNameAsTag"
                    active-text="是"
                    inactive-text="否"
                >
                </el-switch>
              </el-form-item>
              <!-- 跳过行数 -->
              <el-form-item label="跳过行数" prop="skipRows">
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
              <!--批次大小-->
              <el-form-item label="批次大小" prop="batchSize">
                <el-input
                    v-model.number="form.batchSize"
                    min="1"
                    placeholder="请输入批次大小"
                    type="number"
                >
                  <template #prefix>
                    <i class="el-icon-skip"></i>
                  </template>
                </el-input>
              </el-form-item>
              <!--合并时间戳数量-->
              <el-form-item label="每批次合并旧时间戳数量(-1为全量)" prop="mergeTimeStampNum">
                <el-input
                    v-model.number="form.mergeTimeStampNum"
                    min="-1"
                    placeholder="请输入合并时间戳数量"
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
          <el-button
              size="mini"
              style="margin: 5px"
              type="info"
              @click="clearCompleted"
          >
            清除已成功完成任务
          </el-button>
          <el-button
              size="mini"
              style="margin: 5px"
              type="warning"
              @click="clearAllTask"
          >
            清除所有任务
          </el-button>
        </div>
        <div class="task-list">
          <div
              v-for="[taskId, task] in sortedTasks"
              :key="taskId"
              class="task-item"
          >
            <div class="task-meta">
              <span class="filename">{{ task.fileName }}</span>
              <div class="task-actions">
                <el-tag :type="statusTagType(task.status)" size="small">
                  {{ statusText(task.status) }}
                </el-tag>
                <el-button-group v-if="showActions(task.status)" class="action-buttons">
                  <el-button
                      v-if="task.status === 'PROCESSING'"
                      :loading="task.pausing"
                      size="mini"
                      type="warning"
                      @click="handlePause(taskId)"
                  >
                    <i class="el-icon-video-pause"></i>暂停
                  </el-button>
                  <el-button
                      v-if="task.status === 'PAUSED'"
                      :loading="task.resuming"
                      size="mini"
                      type="success"
                      @click="handleResume(taskId)"
                  >
                    <i class="el-icon-video-play"></i>继续
                  </el-button>
                  <el-button
                      :loading="task.cancelling"
                      size="mini"
                      type="danger"
                      @click="handleCancel(taskId)"
                  >
                    <i class="el-icon-close"></i>取消
                  </el-button>
                </el-button-group>
              </div>
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
                <pre v-html="extractErrorMessages(task.errorMessage)"></pre>
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
import DeviceCardMini from "@/views/front/module/DeviceCardMini";


export default {
  name: 'BatchUpload',
  components: {
    DeviceCardMini
  },
  data() {
    return {
      allDevices: [],                      // 全部设备信息
      activeCollapse: ['upload'], // 默认展开上传面板
      form: {
        deviceId: null,
        skipRows: 1,
        mergeTimeStampNum: -1,
        batchSize: 500,
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
      fileLimit: 1000,
      taskInfos: {},
      pollingMap: new Map(),
      isUploading: false,
      fileSet: new Set(), // 用于文件名去重
      usingFileNameAsTag: true,// 是否使用文件名作为标签
    }
  },
  created() {
    this.fetchAllDevices()
    this.restoreFailedTasks();
  },
  beforeUnmount() {
    this.clearAllPolling()
  },
  computed: {
    selectedDevice() {
      return this.allDevices.find(device => device.id === this.form.deviceId);
    },
    sortedTasks() {
      return Object.entries(this.taskInfos).sort((a, b) => {
        const taskA = a[1];
        const taskB = b[1];

        // FAILED 状态置顶
        if (taskA.status === 'FAILED' && taskB.status !== 'FAILED') return -1;
        if (taskB.status === 'FAILED' && taskA.status !== 'FAILED') return 1;

        // 其他状态按开始时间倒序排列
        return new Date(taskB.startTime) - new Date(taskA.startTime);
      });
    },
  },
  methods: {
    extractErrorMessages(errorMessage) {
      let errorMessages = '';
      const regex = /msg='(.*?)'.*?extraMessage='(.*?)'/g;
      let match;

      // 使用正则表达式匹配所有 msg 和 extraMessage
      while ((match = regex.exec(errorMessage)) !== null) {
        const msg = match[1]; // msg
        const extraMessage = match[2]; // extraMessage

        // 格式化输出
        errorMessages += `错误: ${msg}——细节: ${extraMessage}\n`;
      }

      return errorMessages
    },
    formatDateTime(datetime) {
      return datetime ? dayjs(datetime).format('YYYY-MM-DD HH:mm:ss.SSS') : '-'
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
      this.$message.error("最多同时上传" + this.fileLimit + "个文件")
    },

    async submitUpload() {
      if (this.fileList.length === 0) {
        this.$message.warning('请先选择文件')
        return
      }
      console.log(this.form.deviceId)
      if (!this.form.deviceId) {
        this.$message.warning('请选择设备')
        return
      }
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
          formData.append('mergeTimestampNum', this.form.mergeTimeStampNum)
          formData.append('batchSize', this.form.batchSize)
          console.log("fileName", file.name)
          if (this.usingFileNameAsTag) {
            formData.append('tag', file.name.split('.')[0])
          }
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
        console.log(`轮询任务${taskId}...`)
        try {
          if (this.taskInfos[taskId].status === 'PAUSED') {
            return // 任务已暂停, 不再轮询
          }
          const res = await this.$request.get(`/task/get/${taskId}`)
          if (res.code === '200') {
            // 合并后端返回的最新状态
            this.$set(this.taskInfos, taskId, {
              ...this.taskInfos[taskId], // 保留前端信息
              ...res.data,               // 覆盖后端数据
              fileName: this.taskInfos[taskId].fileName // 保持文件名不变
            })

            // 处理状态变化
            if (res.data.status === 'FAILED') {
              this.saveFailedTask(taskId); // 新增存储逻辑
            } else if (this.taskInfos[taskId]?.status === 'FAILED') {
              this.removeFailedTask(taskId); // 状态不再是失败时移除
            }


            // 终止轮询条件
            if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(res.data.status)) {
              console.log(`轮询任务${taskId}已完成`)
              clearInterval(interval)
              this.pollingMap.delete(taskId)
            }
          }
        } catch (error) {
          console.error(`轮询任务${taskId}失败:`, error)
          clearInterval(interval)
          this.pollingMap.delete(taskId)
        }
      }, 500)

      this.pollingMap.set(taskId, interval)
    },

    clearAllPolling() {
      this.pollingMap.forEach(interval => clearInterval(interval))
      this.pollingMap.clear()
    },

    showActions(status) {
      return ['PROCESSING', 'PAUSED'].includes(status)
    },
    async handlePause(taskId) {
      try {
        this.$set(this.taskInfos[taskId], 'pausing', true)//这些是原来显示前端的加载的
        await this.$request.post(`/task/pause/${taskId}`)
        this.$message.success('已发送暂停请求')
      } catch (e) {
        this.$message.error('暂停失败: ' + e.message)
      } finally {
        this.$set(this.taskInfos[taskId], 'pausing', false)
      }
    },
    async handleResume(taskId) {
      try {
        this.$set(this.taskInfos[taskId], 'resuming', true)
        const res = await this.$request.post(`/task/resume/${taskId}`)
        this.$message.success('已发送继续请求')
        if (res.code === '200') {
          this.$set(this.taskInfos[taskId], 'status', 'PROCESSING')//为了让前端恢复轮询
        }
      } catch (e) {
        this.$message.error('继续失败: ' + e.message)
      } finally {
        this.$set(this.taskInfos[taskId], 'resuming', false)
      }
    },
    async handleCancel(taskId) {
      try {
        this.$set(this.taskInfos[taskId], 'cancelling', true)
        const res = await this.$request.post(`/task/cancel/${taskId}`)
        this.$message.success('已发送取消请求')
        if (res.code === '200') {
          this.$set(this.taskInfos[taskId], 'status', 'CANCELLED')//为了让前端恢复轮询
        }
      } catch (e) {
        this.$message.error('取消失败: ' + e.message)
      } finally {
        this.$set(this.taskInfos[taskId], 'cancelling', false)
      }
    },

    statusTagType(status) {
      const map = {
        PROCESSING: 'info',
        COMPLETED: 'success',
        FAILED: 'danger',
        QUEUE: 'warning',
        PAUSED: 'warning',
        CANCELLED: 'warning'
      }
      return map[status] || 'info'
    },

    statusText(status) {
      const map = {
        PROCESSING: '处理中',
        COMPLETED: '已完成',
        FAILED: '失败',
        QUEUE: '等待中',
        PAUSED: '已暂停',
        CANCELLED: '已取消'
      }
      return map[status] || '未知状态'
    },

    progressStatus(status) {
      return status === 'FAILED' ? 'exception' :
          status === 'PAUSED' ? 'warning' :
              status === 'CANCELLED' ? 'warning' :
                  status === 'COMPLETED' ? 'success' : null
    },
    async fetchAllDevices() {
      try {
        const res = await this.$request.get('/device/all');
        if (res.code === '200') {
          this.allDevices = res.data;
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error('获取设备列表失败');
      }
    },
    clearCompleted() {
      // 过滤掉 COMPLETED 状态的任务
      const filtered = Object.entries(this.taskInfos).reduce((acc, [taskId, task]) => {
        if (task.status !== 'COMPLETED') {
          acc[taskId] = task;
        }
        return acc;
      }, {});

      this.taskInfos = {...filtered};

      // 同时清理本地存储中的已完成任务
      const stored = localStorage.getItem('failedTasks') || '[]';
      const failedTasks = JSON.parse(stored).filter(item =>
          this.taskInfos[item.taskId]?.status === 'FAILED'
      );
      localStorage.setItem('failedTasks', JSON.stringify(failedTasks));
    },
    clearAllTask() {
      this.taskInfos = {};
      // 同时清理本地存储中的所有任务
      localStorage.removeItem('failedTasks');
    },
    // 从存储恢复失败任务
    async restoreFailedTasks() {
      const stored = localStorage.getItem('failedTasks');
      if (!stored) return;

      try {
        const failedTasks = JSON.parse(stored).filter(item => {
          // 过滤超过1天的记录
          return Date.now() - item.timestamp < 24 * 60 * 60 * 1000;
        });

        // 更新存储（移除过期记录）
        localStorage.setItem('failedTasks', JSON.stringify(failedTasks));

        console.log('恢复失败任务:', JSON.stringify(failedTasks));
        // 获取任务详情
        for (const {taskId} of failedTasks) {
          if (!this.taskInfos[taskId]) { // 避免重复显示
            const res = await this.$request.get(`/task/get/${taskId}`);
            if (res.code === '200') {
              this.$set(this.taskInfos, taskId, res.data);
              this.startPolling(taskId); // 重启轮询
            }
          }
        }
      } catch (error) {
        console.error('恢复失败任务出错:', error);
      }
    },

    //保存失败任务到 localStorage
    saveFailedTask(taskId) {
      const stored = localStorage.getItem('failedTasks') || '[]';
      const failedTasks = JSON.parse(stored);

      // 避免重复添加
      if (!failedTasks.some(item => item.taskId === taskId)) {
        failedTasks.push({
          taskId,
          timestamp: Date.now()
        });
        localStorage.setItem('failedTasks', JSON.stringify(failedTasks));
      }
    },

    //从存储移除任务
    removeFailedTask(taskId) {
      const stored = localStorage.getItem('failedTasks') || '[]';
      const failedTasks = JSON.parse(stored).filter(item => item.taskId !== taskId);
      localStorage.setItem('failedTasks', JSON.stringify(failedTasks));
    },

    clearAllFiles() {
      this.fileList = [];
      this.fileSet.clear();
      this.$message.success("已清空所有文件");
    },
  }
}
</script>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止全局滚动条 */
  font-weight: bold;
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
  padding-bottom: 1rem;

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
  transition: transform 0.1s ease, box-shadow 0.1s ease; /* 添加平滑过渡效果 */
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

::v-deep .el-button {
  font-weight: bold !important;
}

::v-deep .el-collapse-item__header {
  font-weight: bold !important;
}
</style>