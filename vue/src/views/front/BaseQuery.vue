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
            multiple
            clearable
            placeholder="请选择传感器"
            style="width: 100%"
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

      <!-- 阈值设置 -->
      <el-form-item
          v-if="form.selectMeasurements && form.selectMeasurements.length > 0"
          label="阈值设置"
      >
        <div v-for="(measurement, index) in form.selectMeasurements" :key="measurement" class="threshold-item">
          <span class="threshold-label">{{ measurement }}</span>
          <el-input-number
              v-model="thresholds[index][0]"
              :precision="3"
              class="threshold-input"
              placeholder="最小值"
          ></el-input-number>
          <span class="threshold-separator">-</span>
          <el-input-number
              v-model="thresholds[index][1]"
              :precision="3"
              class="threshold-input"
              placeholder="最大值"
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
      timeRange: [1719072000000,1719158400000],
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
      },
      deep: true
    }
  },
  methods: {
    async handleDeviceChange(deviceId) {
      if (!deviceId) return
      try {
        const res = await this.$request.get(`/device/getMeasurements/${deviceId}`)
        if (res.code === '200') {
          this.deviceMeasurements = res.data
          console.log("属性值：" + this.deviceMeasurements)
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取设备信息失败')
      }
    },

    async submitQuery() {
      // 处理时间范围
      if (this.timeRange && this.timeRange.length === 2) {
        this.form.startTime = this.timeRange[0]
        this.form.endTime = this.timeRange[1]
      }

      // 构建请求参数
      const params = {
        ...this.form,
        thresholds: this.form.selectMeasurements.length > 0
            ? this.thresholds.map(t => t.map(v => v !== null ? Number(v) : null))
            : null
      }

      try {
        const res = await this.$request.post('/data/query', params)
        if (res.code === '200') {
          this.transformData(res.data)
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('请求失败')
      }
    },

    transformData(deviceTable) {
      this.tableData = []
      this.measurementsColumns = []

      // 提取测量项列
      if (deviceTable.types) {
        this.measurementsColumns = [...deviceTable.types]
      }

      // 处理records数据
      const records = deviceTable.records || {}
      for (const [timestamp, recordList] of Object.entries(records)) {
        recordList.forEach(record => {
          const rowData = {
            timestamp: Number(timestamp),
            ...record.fields
          }
          this.tableData.push(rowData)
        })
      }

      // 按时间排序
      this.tableData.sort((a, b) => a.timestamp - b.timestamp)
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
      this.timeRange = []
      this.thresholds = []
      this.tableData = []
    },
    selectAll() {
      this.form.selectMeasurements = this.deviceMeasurements
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}

.query-form {
  max-width: 800px;
  margin: 0 auto;
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
  margin-top: 30px;
}

.empty-tip {
  text-align: center;
  color: #999;
  margin-top: 20px;
}
</style>