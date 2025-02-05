<template>
  <div class="container">
    <el-form :model="form" class="query-form" label-position="left" label-width="120px">
      <!-- 设备ID -->
      <el-form-item label="设备ID">
        <el-input-number
            v-model="form.deviceId"
            :min="1"
            placeholder="请输入设备ID"
            @change="handleDeviceChange"
        ></el-input-number>
      </el-form-item>

      <!-- 时间范围 -->
      <el-form-item label="时间范围">
        <el-date-picker
            v-model="timeRange"
            :default-time="['00:00:00', '23:59:59']"
            end-placeholder="结束时间"
            range-separator="至"
            start-placeholder="开始时间"
            type="datetimerange"
            value-format="timestamp"
        ></el-date-picker>
      </el-form-item>

      <!-- 传感器选择 -->
      <el-form-item label="选择传感器">
        <el-select
            v-model="form.selectMeasurements"
            clearable
            multiple
            placeholder="请选择传感器"
            style="width: 80%"
            @change="handleMeasurementsChange"
        >
          <el-option
              v-for="item in deviceMeasurements"
              :key="item"
              :label="item"
              :value="item"
          ></el-option>
        </el-select>
        <el-button @click="selectAll">全选</el-button>
      </el-form-item>

      <!-- 聚合设置 -->
      <el-form-item label="聚合时间(ms)">
        <el-input-number
            v-model="form.aggregationTime"
            :min="0"
            placeholder="输入聚合时间"
        ></el-input-number>
      </el-form-item>

      <el-form-item label="聚合函数">
        <el-select v-model="form.queryAggregateFunc" placeholder="请选择聚合函数">
          <el-option
              v-for="func in aggregateFuncOptions"
              :key="func"
              :label="func"
              :value="func"
          ></el-option>
        </el-select>
      </el-form-item>

      <!-- 阈值过滤开关 -->
      <el-form-item label="阈值过滤">
        <el-switch v-model="thresholdFilterEnabled"/>
      </el-form-item>

      <!-- 阈值设置 -->
      <el-form-item
          v-if="form.selectMeasurements && form.selectMeasurements.length > 0 && thresholdFilterEnabled"
          label="阈值设置"
      >
        <div v-for="(measurement, index) in form.selectMeasurements" :key="measurement" class="threshold-item">
          <span class="threshold-label">{{ measurement }}</span>
          <el-input-number
              :controls="true"
              :placeholder="thresholds[index][0] === null ? '-∞' : '最小值'"
              :precision="3"
              :value="thresholds[index][0]"
              class="threshold-input"
              @change="val => handleThresholdChange(index, 0, val)"
          ></el-input-number>
          <span class="threshold-separator">-</span>
          <el-input-number
              :controls="true"
              :placeholder="thresholds[index][1] === null ? '+∞' : '最大值'"
              :precision="3"
              :value="thresholds[index][1]"
              class="threshold-input"
              @change="val => handleThresholdChange(index, 1, val)"
          ></el-input-number>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitQuery">查询</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 结果展示 -->
    <div v-if="tableData.length > 0" class="result-container">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column label="时间戳" prop="timestamp" width="180">
          <template #default="{row}">
            {{ new Date(row.timestamp).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column
            v-for="col in measurementsColumns"
            :key="col"
            :label="col"
            :prop="col"
        ></el-table-column>
      </el-table>
    </div>
    <div v-else class="empty-tip">
      暂无数据
    </div>
  </div>
</template>

<script>
export default {
  name: 'BaseQuery',
  data() {
    return {
      form: {
        deviceId: null,
        startTime: null,
        endTime: null,
        selectMeasurements: [],
        aggregationTime: null,
        queryAggregateFunc: null,
      },
      thresholdFilterEnabled: false,
      timeRange: [1719072000000, 1719158400000],
      deviceMeasurements: [],
      thresholds: [],
      aggregateFuncOptions: ['AVG', 'MAX', 'MIN', 'SUM', 'COUNT', 'FIRST', 'LAST'],
      tableData: [],
      measurementsColumns: []
    }
  },
  watch: {
    'form.selectMeasurements': {
      handler(newVal) {
        // 初始化阈值数组
        this.thresholds = newVal.map(() => [null, null])
        console.log("thresholds: " + JSON.stringify(this.thresholds))
      },
      deep: true
    }
  },
  methods: {
    handleThresholdChange(index, position, value) {
      const newValue = value === undefined || value === null ? null : Number(value)
      this.$set(this.thresholds[index], position, newValue)
    },
    async handleDeviceChange(deviceId) {
      if (!deviceId) return
      try {
        const res = await this.$request.get(`/device/getMeasurements/${deviceId}`)
        if (res.code === '200') {
          this.deviceMeasurements = res.data
          this.form.selectMeasurements = []
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取设备信息失败')
      }
    },

    handleMeasurementsChange(selected) {
      //保持选择的传感器顺序
      this.form.selectMeasurements = this.deviceMeasurements.filter(m => selected.includes(m))
    },

    async submitQuery() {
      if (this.timeRange?.length === 2) {
        this.form.startTime = this.timeRange[0]
        this.form.endTime = this.timeRange[1]
      }
      if (this.form.selectMeasurements === null || this.form.selectMeasurements.length === 0) {
        this.form.selectMeasurements = this.deviceMeasurements // 如果没有选择属性，则选择全部属性.
      }
      // 构建请求参数
      const params = {
        ...this.form,
        thresholds: this.thresholdFilterEnabled
            ? this.thresholds.map(t => [
              t[0] !== null ? Number(t[0]) : null,
              t[1] !== null ? Number(t[1]) : null
            ])
            : null
      }
      console.log("params: " + JSON.stringify(params))

      try {
        const res = await this.$request.post('/data/query', params)
        if (res.code === '200') {
          this.transformData(res.data)
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('请求失败: ' + error)
      }
    },

    transformData(deviceTable) {
      // console.log("deviceTable: " + JSON.stringify(deviceTable))
      this.tableData = []

      this.measurementsColumns = this.form.selectMeasurements;
      console.log("measurementsColumns: " + JSON.stringify(this.measurementsColumns))

      // 处理records数据
      const records = deviceTable.records || {}
      console.log("records: " + JSON.stringify(Object.entries(records).slice(0, 5)))
      for (const [timestamp, recordList] of Object.entries(records)) {
        recordList.forEach(record => {
          this.tableData.push({
            timestamp: Number(timestamp),
            ...record.fields
          })
        })
      }

      // 按时间排序
      this.tableData.sort((a, b) => a.timestamp - b.timestamp)
      console.log("tableData: " + JSON.stringify(this.tableData.slice(0, 5)))
    },

    resetForm() {
      this.form = {
        deviceId: null,
        startTime: null,
        endTime: null,
        selectMeasurements: [],
        aggregationTime: null,
        queryAggregateFunc: null,
      }
      this.thresholdFilterEnabled = false
      this.timeRange = []
      this.thresholds = []
      this.tableData = []
    },
    selectAll() {
      this.form.selectMeasurements = [...this.deviceMeasurements]
    }
  }
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100%; /* 填满父容器 */
  box-sizing: border-box;
  font-weight: bold;
}

.query-form {
  max-width: 90%;
  margin: 20px auto;
}

.threshold-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.threshold-label {
  width: 120px;
  margin-right: 10px;
}

.threshold-input {
  width: 150px;
}

.threshold-separator {
  margin: 0 10px;
}

.result-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.empty-tip {
  text-align: center;
  color: #999;
  margin-top: 20px;
}
</style>