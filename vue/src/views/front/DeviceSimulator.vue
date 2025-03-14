<template>
  <div class="device-container">
    <!-- 设备创建 -->
    <el-card class="device-section">
      <div class="section-header">
        <h3>设备管理</h3>
        <div>
          <el-button type="primary" @click="showCreateDialog">新建设备</el-button>
          <el-button :loading="loading" type="info" @click="refreshDevices">刷新列表</el-button>
        </div>
      </div>
    </el-card>

    <!-- 设备列表 -->
    <el-card class="device-section">
      <div class="section-header">
        <h3>设备列表</h3>
        <div class="stats-text">共 {{ devices.length }} 台设备</div>
      </div>
      <el-table :data="devices" border style="width: 100%"  @row-click="selectDevice">
        <el-table-column label="设备ID" prop="deviceId" width="200"/>
        <el-table-column label="用户ID" prop="userId" width="150"/>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.isRunning ? 'success' : 'danger'">
              {{ row.isRunning ? '运行中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缓冲区大小" prop="bufferSize" width="100"/>
        <el-table-column label="产生间隔(ms)" prop="interval" width="120"/>
        <el-table-column label="操作" width="280">
          <template slot-scope="{ row }">
            <el-button-group>
              <el-button size="small" @click="toggleDeviceStatus(row)">
                {{ row.isRunning ? '停止' : '启动' }}
              </el-button>
              <el-button size="small" type="warning" @click="editDevice(row)">
                编辑
              </el-button>
              <el-button size="small" type="danger" @click="deleteDevice(row.deviceId)">
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 传感器管理 -->
    <el-card v-if="currentDevice" class="device-section">
      <div class="section-header">
        <h3>传感器管理 - {{ currentDevice.deviceId }}</h3>
        <div>
          <el-button type="primary" @click="showSensorDialog">添加传感器</el-button>
        </div>
      </div>

      <el-table :data="currentDevice.sensors" border style="width: 100%">
        <el-table-column label="传感器ID" prop="sensorId" width="200"/>
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            {{ getGeneratorType(row.dataGenerator) }}
          </template>
        </el-table-column>
        <el-table-column label="参数" min-width="300">
          <template slot-scope="{ row }">
            <pre class="sensor-params">{{ formatGeneratorParams(row.dataGenerator) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="editSensor(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 设备对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="deviceDialogVisible" width="500px">
      <el-form :model="deviceForm" label-width="100px">
        <el-form-item label="设备ID" required>
          <el-input v-model="deviceForm.deviceId" :disabled="isEditMode"/>
        </el-form-item>
        <el-form-item label="用户ID" required>
          <el-input v-model="deviceForm.userId"/>
        </el-form-item>
        <el-form-item label="缓冲区大小" required>
          <el-input-number v-model="deviceForm.bufferSize" :min="1"/>
        </el-form-item>
        <el-form-item label="间隔时间(ms)" required>
          <el-input-number v-model="deviceForm.interval" :min="100" :step="100"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDevice">确认</el-button>
      </template>
    </el-dialog>

    <!-- 传感器对话框 -->
    <el-dialog :visible.sync="sensorDialogVisible" title="传感器配置" width="600px">
      <el-form :model="sensorForm" label-width="120px">
        <el-form-item label="传感器ID" required>
          <el-input v-model="sensorForm.sensorId"/>
        </el-form-item>
        <el-form-item label="生成类型" required>
          <el-select v-model="sensorForm.generationType" @change="handleTypeChange">
            <el-option label="随机数" value="随机数"/>
            <el-option label="正弦波(带噪声)" value="正弦波(带噪声)"/>
          </el-select>
        </el-form-item>

        <!-- 通用参数 -->
        <el-form-item label="数值类型" required>
          <el-radio-group v-model="sensorForm.isInteger">
            <el-radio :label="true">整数</el-radio>
            <el-radio :label="false">浮点数</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="最小值" required>
          <el-input-number v-model="sensorForm.minValue"/>
        </el-form-item>
        <el-form-item label="最大值" required>
          <el-input-number v-model="sensorForm.maxValue"/>
        </el-form-item>

        <!-- 正弦波专用参数 -->
        <template v-if="sensorForm.generationType === '正弦波(带噪声)'">
          <el-form-item label="频率" required>
            <el-input-number v-model="sensorForm.frequency" :min="0.1" :step="0.1"/>
          </el-form-item>
          <el-form-item label="相位" required>
            <el-input-number v-model="sensorForm.phase"/>
          </el-form-item>
          <el-form-item label="噪声幅度" required>
            <el-input-number v-model="sensorForm.smallRandomDisturbance" :min="0"/>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="sensorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSensor">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

// 创建专用的axios实例
const deviceApi = axios.create({
  baseURL: 'http://localhost:23456', // 设备模拟器专用接口地址
  timeout: 10000
})

// 请求拦截器（可选）
deviceApi.interceptors.request.use(config => {
  config.headers['Content-Type'] = 'application/json'
  return config
})

// 响应拦截器（可选）
deviceApi.interceptors.response.use(
    response => {
      if (response.data.code !== '200') {
        this.$message({
          message: response.data.msg || '请求错误',
          type: 'error',
          duration: 3000
        });
        return Promise.reject(response.data);
      }
      return response.data;
    },
    error => {
      this.$message({
        message: error.message || '网络错误',
        type: 'error',
        duration: 3000
      });
      return Promise.reject(error);
    }
);

export default {
  data() {
    return {
      loading: false,
      devices: [],
      currentDevice: null,

      // 设备对话框相关
      deviceDialogVisible: false,
      isEditMode: false,
      deviceForm: {
        deviceId: '',
        userId: '',
        bufferSize: 100,
        interval: 1000
      },

      // 传感器对话框相关
      sensorDialogVisible: false,
      sensorForm: {
        sensorId: '',
        generationType: '随机数',
        isInteger: false,
        minValue: 0,
        maxValue: 100,
        frequency: 1,
        phase: 0,
        smallRandomDisturbance: 0.1
      }
    }
  },

  computed: {
    dialogTitle() {
      return this.isEditMode ? '编辑设备' : '新建设备'
    }
  },

  mounted() {
    this.refreshDevices()
  },

  methods: {

    // 修正后的设备刷新方法
    async refreshDevices() {
      this.loading = true;
      try {
        const {data} = await deviceApi.get('/api/devices');
        console.log("设备列表加载成功:", JSON.stringify(data));
        this.devices = data || [];
        this.currentDevice = null
      } catch (error) {
        console.error('设备列表加载失败:', error);
      } finally {
        this.loading = false;
      }
    },

    selectDevice(device) {
      this.currentDevice = device;
    },

    showCreateDialog() {
      // console.log('show create dialog')
      this.deviceForm = {
        deviceId: '',
        userId: '',
        bufferSize: 1,
        interval: 1000
      }
      this.isEditMode = false
      this.deviceDialogVisible = true
      // console.log("当前弹窗状态:", this.deviceDialogVisible);
    },

    async submitDevice() {
      const url = this.isEditMode
          ? `/api/devices/${this.deviceForm.deviceId}`
          : '/api/devices'

      const method = this.isEditMode ? 'put' : 'post'

      try {
        await deviceApi[method](url, this.deviceForm)
        this.$message.success('操作成功')
        this.deviceDialogVisible = false
        await this.refreshDevices()
      } catch (error) {
        this.$message.error(error.message)
      }
    },

    editDevice(device) {
      this.deviceForm = {...device}
      this.isEditMode = true
      this.deviceDialogVisible = true
      this.currentDevice = device
    },

    async deleteDevice(deviceId) {
      try {
        await this.$confirm('确认删除该设备？', '警告', {type: 'warning'})
        await deviceApi.delete(`/api/devices/${deviceId}`)
        this.$message.success('删除成功')
        await this.refreshDevices()
      } catch {
      }
    },

    editSensor(row) {
      console.log("未实现")
    },

// 修正后的设备状态切换方法
    async toggleDeviceStatus(device) {
      const action = device.isRunning ? 'stop' : 'start';
      try {
        await deviceApi.post(`/api/devices/${device.deviceId}/${action}`);
        this.$message({
          message: `${action === 'start' ? '启动' : '停止'}成功`,
          type: 'success',
          duration: 1500
        });
        // 局部更新设备状态
        device.isRunning = !device.isRunning;
        console.log("设备状态切换成功:", device.isRunning);
      } catch (error) {
        console.error('设备状态切换失败:', error);
      }
    },

    showSensorDialog() {
      this.sensorForm = {
        sensorId: '',
        generationType: '随机数',
        isInteger: false,
        minValue: 0,
        maxValue: 100,
        frequency: 1,
        phase: 0,
        smallRandomDisturbance: 0.1
      }
      this.sensorDialogVisible = true
    },

    async submitSensor() {
      try {
        const url = `/api/devices/${this.currentDevice.deviceId}/sensors`
        await deviceApi.post(url, this.sensorForm)
        this.$message.success('添加成功')
        this.sensorDialogVisible = false
        await this.refreshDevices()
      } catch (error) {
        this.$message.error(error.message)
      }
    },

    getGeneratorType(generator) {
      return generator.name.includes('Sine') ? '正弦波(带噪声)' : '随机数'
    },

    formatGeneratorParams(generator) {
      const params = {...generator}
      delete params.name
      return JSON.stringify(params, null, 2)
    },

    handleTypeChange(type) {
      // 重置相关参数
      if (type === '随机数') {
        this.sensorForm.frequency = 1
        this.sensorForm.phase = 0
        this.sensorForm.smallRandomDisturbance = 0.1
      }
    }
  }
}
</script>

<style scoped>
.device-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.device-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.stats-text {
  color: #666;
  font-size: 14px;
}

.sensor-params {
  margin: 0;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>