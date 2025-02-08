<template>
  <div class="container">
    <el-collapse v-model="settingsVisible" accordion class="query-collapse">
      <el-collapse-item name="1">
        <template slot="title">
          <div style="font-weight: bold;font-size: 18px; text-align: center;color: #555;width: 100%">
            查询设置
          </div>
        </template>
        <div class="scrollable-container">
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
                  style="width: 70%"
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
            <el-form-item label="聚合时间">
              <el-select
                  v-model="form.aggregationTime"
                  placeholder="请选择聚合时间"
              >
                <el-option
                    v-for="item in aggregationTimeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>


            <el-form-item v-if="form.aggregationTime!== 0" label="聚合函数">
              <el-select v-model="form.queryAggregateFunc" placeholder="请选择聚合函数">
                <el-option
                    v-for="func in aggregateFuncOptions"
                    :key="func.code"
                    :value="func.code"
                >
                  <el-tooltip :content="func.desc" effect="dark" placement="top">
                    <span>{{ func.code }} ({{ func.name }})</span>
                  </el-tooltip>
                </el-option>
              </el-select>
            </el-form-item>

            <!--            &lt;!&ndash; 阈值过滤开关 &ndash;&gt;-->
            <!--            <el-form-item label="阈值过滤">-->
            <!--              <el-switch v-model="thresholdFilterEnabled"/>-->
            <!--            </el-form-item>-->

            <!--            &lt;!&ndash; 高亮开关 &ndash;&gt;-->
            <!--            <el-form-item label="阈值高亮">-->
            <!--              <el-switch v-model="thresholdHighlightEnabled"/>-->
            <!--            </el-form-item>-->

            <!-- 阈值设置区域 -->
            <el-form-item
                v-if="form.selectMeasurements && form.selectMeasurements.length > 0"
                class="threshold-settings"
                label="阈值设置"
            >
              <el-row :gutter="20" class="threshold-container">
                <!-- 阈值过滤 -->
                <el-col :span="12">
                  <!-- 阈值过滤开关 -->
                  <el-form-item label="阈值过滤">
                    <el-switch v-model="thresholdFilterEnabled"/>
                  </el-form-item>
                  <div v-if="thresholdFilterEnabled" class="threshold-panel">
                    <div class="threshold-title">过滤阈值(min, max)</div>
                    <div v-for="(measurement, index) in form.selectMeasurements" :key="measurement+'-filter'"
                         class="threshold-item">
                      <span class="threshold-label">{{ measurement }}</span>
                      <el-input-number
                          :controls="true"
                          :placeholder="thresholds[index][0] === null || thresholds[index][0] === undefined? '不限制' : '最小值'"
                          :precision="2"
                          :value="thresholds[index][0]"
                          class="threshold-input"
                          @change="val => handleThresholdChange(index, 0, val)"
                      ></el-input-number>
                      <span class="threshold-separator">-</span>
                      <el-input-number
                          :controls="true"
                          :placeholder="thresholds[index][1] === null || thresholds[index][1] === undefined? '不限制' : '最大值'"
                          :precision="2"
                          :value="thresholds[index][1]"
                          class="threshold-input"
                          @change="val => handleThresholdChange(index, 1, val)"
                      ></el-input-number>
                    </div>
                  </div>
                </el-col>

                <!-- 阈值高亮 -->
                <el-col :span="12">
                  <!-- 高亮开关 -->
                  <el-form-item label="阈值高亮">
                    <el-switch v-model="thresholdHighlightEnabled"/>
                  </el-form-item>
                  <div v-if="thresholdHighlightEnabled" class="threshold-panel">
                    <div class="threshold-title">高亮阈值(min, max)</div>
                    <div v-for="(measurement, index) in form.selectMeasurements" :key="measurement+'-highlight'"
                         class="threshold-item">
                      <span class="threshold-label">{{ measurement }}</span>
                      <el-input-number
                          :controls="true"
                          :placeholder="highlightThresholds[index][0] === null || highlightThresholds[index][0] === undefined? '不限制' : '最小值'"
                          :precision="2"
                          :value="highlightThresholds[index][0]"
                          class="threshold-input"
                          @change="val => handleHighlightChange(index, 0, val)"
                      ></el-input-number>
                      <span class="threshold-separator">-</span>
                      <el-input-number
                          :controls="true"
                          :placeholder="highlightThresholds[index][1] === null || highlightThresholds[index][1] === undefined? '不限制' : '最大值'"
                          :precision="2"
                          :value="highlightThresholds[index][1]"
                          class="threshold-input"
                          @change="val => handleHighlightChange(index, 1, val)"
                      ></el-input-number>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitQuery">查询</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 结果展示 -->
    <div v-if="tableData.length > 0" class="result-container">
      <el-table
          :cell-style="getCellStyle"
          :data="tableData" border
          style="width: 100%"
          height="100%"
      >
        <!-- 新增序号列 -->
        <el-table-column
            :index="indexMethod"
            fixed="left"
            label="序号"
            type="index"
            width="60"
        ></el-table-column>
        <el-table-column fixed="left" label="时间戳" prop="timestamp" width="180">
          <template #default="{row}">
            {{ new Date(row.timestamp).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column
            v-for="col in measurementsColumns"
            :key="col"
            :label="col"
            :prop="col"
        >
          <template #default="{ row }">
            {{
              typeof row[col] === 'number' ? (Number.isInteger(row[col]) ? row[col] : row[col].toFixed(4)) : row[col]
            }}
          </template>
        </el-table-column>

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
        aggregationTime: 0,
        queryAggregateFunc: null,
      },
      thresholdFilterEnabled: false,// 阈值过滤开关
      thresholdHighlightEnabled: false, // 阈值高亮开关
      highlightThresholds: [], // 高亮阈值配置
      timeRange: [1719072000000, 1719158400000],
      deviceMeasurements: [],
      thresholds: [],//阈值过滤配置
      aggregateFuncOptions: [], // 聚合函数动态获取
      tableData: [],
      measurementsColumns: [],
      aggregationTimeOptions: [
        {label: '不聚合', value: 0},
        {label: '1ms', value: 1},
        {label: '1s', value: 1000},
        {label: '10s', value: 10000},
        {label: '1m', value: 60000},
        {label: '1h', value: 3600000},
        {label: '1d', value: 86400000}
      ],
      settingsVisible: ['1'], // 设置默认展开状态
    }
  },
  watch: {
    'form.selectMeasurements': {
      handler(newVal) {
        // 初始化阈值数组
        this.thresholds = newVal.map(() => [undefined, undefined])
        this.highlightThresholds = newVal.map(() => [undefined, undefined])
        console.log("thresholds: " + JSON.stringify(this.thresholds))
        console.log("highlightThresholds: " + JSON.stringify(this.highlightThresholds))
      },
      deep: true
    }
  },
  created() {
    this.loadAggregateFunctions();
  },
  methods: {
    indexMethod(index) {
      return index + 1; // 从1开始编号
    },
    handleThresholdChange(index, position, value) {
      const newValue = value === undefined || value === null ? null : Number(value)
      this.$set(this.thresholds[index], position, newValue)
    },
    handleHighlightChange(index, position, value) {
      const newVal = value === undefined ? null : Number(value)
      this.$set(this.highlightThresholds[index], position, newVal)
    },
    getCellStyle({row, column, rowIndex, columnIndex}) {
      // console.log(`row: ${JSON.stringify(row)}, column: ${JSON.stringify(column)}, rowIndex: ${rowIndex}, columnIndex: ${columnIndex}`)
      if (!this.thresholdHighlightEnabled) return {}
      if (columnIndex === 0 || columnIndex === 1) return {} // 序号列和时间戳列不高亮
      const value = row[column.label]
      // console.log(`value: ${value}`)
      if (typeof value !== 'number') return {}

      const labelIndex = this.form.selectMeasurements.indexOf(column.label)
      const [min, max] = this.highlightThresholds[labelIndex] || []
      // console.log(` labelIndex: ${labelIndex}, min: ${min}, max: ${max}`)

      const exceedMin = min !== null && value < min
      const exceedMax = max !== null && value > max
      // console.log(`exceedMin: ${exceedMin}, exceedMax: ${exceedMax}`)
      return exceedMin || exceedMax
          ? {backgroundColor: '#ffcccc'}
          : {}

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

    async loadAggregateFunctions() {
      try {
        const res = await this.$request.get('/data/aggregateFuncs');
        if (res.code === '200') {
          this.aggregateFuncOptions = res.data.map(item => ({
            code: item.code,
            name: item.name,
            desc: item.desc,
          }));
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error('获取聚合函数失败');
      }
    },

    async submitQuery() {
      if (this.form.aggregationTime !== 0 && this.form.queryAggregateFunc === null) {
        this.$message.error('请选择聚合函数')
        return
      }
      if (this.timeRange?.length === 2) {
        this.form.startTime = this.timeRange[0]
        this.form.endTime = this.timeRange[1]
      }
      if (this.form.selectMeasurements === null || this.form.selectMeasurements.length === 0) {
        console.log("selectMeasurements is null or empty，自动选择全部属性")
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
        const startTime = new Date().getTime()
        this.$message.info('请稍候...')
        const res = await this.$request.post('/data/query', params)
        if (res.code === '200') {
          const queryEndTime = new Date().getTime()
          this.$message.success('查询成功, 共' + res.data.total + '条数据, 准备展示...')
          console.log('查询成功, 共' + res.data.total + '条数据,耗时' + (queryEndTime - startTime) + '毫秒')
          this.transformData(res.data)
          const endTime = new Date().getTime()
          console.log('处理数据成功, 共耗时' + (endTime - queryEndTime) + '毫秒')
          document.activeElement.blur();//关闭面板前让按钮不是焦点，避免控制台报错
          this.settingsVisible = [] // 关闭设置面板
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
  padding: 20px;
}

.query-collapse{
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-height: 60px;
}

.scrollable-container {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
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
  overflow-x: auto;
  /*max-height: 400px;*/
  padding: 10px;
  margin-top: 16px;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.empty-tip {
  text-align: center;
  color: #999;
  margin-top: 20px;
}

.threshold-settings {
  width: 100%;
}

.threshold-container {
  width: 100%;
  display: flex;
}

.threshold-panel {
  border: 1px solid #ebeef5;
  border-radius: 1rem;
  padding: 10px;
  margin-bottom: 10px;
}

.threshold-title {
  font-weight: bold;
  margin-bottom: 10px;
  color: #666;
}
</style>