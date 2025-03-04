<template>
  <div class="device-view">
    <!-- 上侧控制区域 -->
    <el-row :gutter="20" class="control-panel">
      <!-- 设备选择 -->
      <el-col :span="5">
        <el-form>
          <el-form-item label="设备一">
            <el-select
                v-model="selectedDeviceId1"
                placeholder="选择设备"
                @change="handleDevice1Change"
            >
              <el-option
                  v-for="device in allDevices"
                  :key="device.id"
                  :label="device.name"
                  :value="device.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :span="5">
        <el-form>
          <el-form-item label="设备二">
            <el-select
                v-model="selectedDeviceId2"
                placeholder="选择设备"
                @change="handleDevice2Change"
            >
              <el-option
                  v-for="device in allDevices"
                  :key="device.id"
                  :label="device.name"
                  :value="device.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 双列标签选择 -->
      <div class="tag-sidebar">
        <!-- 设备一标签 -->
        <div class="tag-column">
          <div class="tag-header">
            <el-checkbox
                v-model="allTags1Selected"
                :indeterminate="isIndeterminate1"
                @change="handleSelectAll1"
            >
              全选<br>({{ device1Name || '设备一' }})
            </el-checkbox>
          </div>
          <el-scrollbar class="tag-list">
            <el-checkbox-group v-model="selectedTags1">
              <el-checkbox
                  v-for="tag in sortedDevice1Tags"
                  :key="tag"
                  :label="tag"
                  class="tag-item"
              >
                {{ formatTagDisplay(tag) }}
              </el-checkbox>
            </el-checkbox-group>
          </el-scrollbar>
        </div>

        <!-- 设备二标签 -->
        <div class="tag-column">
          <div class="tag-header">
            <el-checkbox
                v-model="allTags2Selected"
                :indeterminate="isIndeterminate2"
                @change="handleSelectAll2"
            >
              全选<br>({{ device2Name || '设备二' }})
            </el-checkbox>
          </div>
          <el-scrollbar class="tag-list">
            <el-checkbox-group v-model="selectedTags2">
              <el-checkbox
                  v-for="tag in sortedDevice2Tags"
                  :key="tag"
                  :label="tag"
                  class="tag-item"
              >
                {{ formatTagDisplay(tag) }}
              </el-checkbox>
            </el-checkbox-group>
          </el-scrollbar>
        </div>
      </div>

      <!-- 图表展示区域 -->
      <div class="chart-area">
        <TagLineChart
            v-if="processedData"
            :chart-data="processedData"
            :device1-name="device1Name"
            :device2-name="device2Name"
        />
      </div>
    </div>
  </div>
</template>

<script>
import TagLineChart from './DeviceView/TagLineChart.vue'

export default {
  name: 'DeviceView',
  components: {TagLineChart},
  data() {
    return {
      allDevices: [],
      selectedDeviceId1: null,
      selectedDeviceId2: null,
      device1Tags: [],
      device2Tags: [],
      selectedTags1: [],
      selectedTags2: [],
      preprocessedData1: null,
      preprocessedData2: null,
    }
  },
  computed: {
    device1Name() {
      return this.allDevices.find(d => d.id === this.selectedDeviceId1)?.name
    },
    device2Name() {
      return this.allDevices.find(d => d.id === this.selectedDeviceId2)?.name
    },
    // 设备一相关计算属性
    allTags1Selected: {
      get() { return this.device1Tags.length > 0 && this.selectedTags1.length === this.device1Tags.length },
      set(val) { this.selectedTags1 = val ? [...this.device1Tags] : [] }
    },
    isIndeterminate1() {
      return this.selectedTags1.length > 0 && this.selectedTags1.length < this.device1Tags.length
    },
    sortedDevice1Tags() {
      return this.sortTags([...this.device1Tags])
    },
    // 设备二相关计算属性
    allTags2Selected: {
      get() { return this.device2Tags.length > 0 && this.selectedTags2.length === this.device2Tags.length },
      set(val) { this.selectedTags2 = val ? [...this.device2Tags] : [] }
    },
    isIndeterminate2() {
      return this.selectedTags2.length > 0 && this.selectedTags2.length < this.device2Tags.length
    },
    sortedDevice2Tags() {
      return this.sortTags([...this.device2Tags])
    },
    processedData() {
      const processStart = Date.now()
      const processDevice = (data, tags, deviceName, lineStyle) => {
        if (!data) return []
        const selected = new Set(tags)
        return Object.entries(data).map(([field, tagsData]) => ({
          field,
          series: Object.entries(tagsData)
              .filter(([tag]) => selected.has(tag))
              .map(([tag, { name, data }]) => ({
                name: `${deviceName}-${name}`,
                data,
                lineStyle: { type: lineStyle }
              }))
        }))
      }

      const data1 = processDevice(this.preprocessedData1, this.selectedTags1, this.device1Name, 'solid')
      const data2 = processDevice(this.preprocessedData2, this.selectedTags2, this.device2Name, 'dashed')

      // 合并相同field的数据
      const merged = {}
      ;[...data1, ...data2].forEach(({ field, series }) => {
        if (!merged[field]) merged[field] = { field, series: [] }
        merged[field].series.push(...series)
      })
      const processEnd = Date.now()
      console.log('切换tag处理时间:' + (processEnd - processStart) + 'ms')


      return Object.values(merged)
    }
  },
  methods: {
    async fetchAllDevices() {
      try {
        const res = await this.$request.get('/device/all')
        if (res.code === '200') {
          this.allDevices = res.data
        }
      } catch (error) {
        this.$message.error('设备加载失败')
      }
    },
    sortTags(tags) {
      //NO_TAG放第一个，其次是整数类字符串从小到大排序，最后是其他字符串，按字母顺序排序
      return [...tags].sort((a, b) => {
        // 优先处理NO_TAG
        if (a === 'NO_TAG') return -1
        if (b === 'NO_TAG') return 1

        // 判断是否为纯数字字符串
        const aIsNumeric = /^\d+$/.test(a)
        const bIsNumeric = /^\d+$/.test(b)

        // 数字类型比较
        if (aIsNumeric && bIsNumeric) {
          return parseInt(a, 10) - parseInt(b, 10)
        }

        // 数字类型优先于非数字类型
        if (aIsNumeric) return -1
        if (bIsNumeric) return 1

        // 普通字符串按字母顺序排序
        return a.localeCompare(b)
      })
    },
    async handleDevice1Change(deviceId) {
      const device = this.allDevices.find(d => d.id === deviceId)
      this.device1Tags = device?.allTags || []
      this.selectedTags1 = []
      await this.fetchDeviceData(deviceId, 'preprocessedData1')
    },
    async handleDevice2Change(deviceId) {
      const device = this.allDevices.find(d => d.id === deviceId)
      this.device2Tags = device?.allTags || []
      this.selectedTags2 = []
      await this.fetchDeviceData(deviceId, 'preprocessedData2')
    },
    async fetchDeviceData(deviceId, target) {
      if (!deviceId) return
      this.$message.info('请稍候...')
      try {
        const queryStart = Date.now()
        const res = await this.$request.post('/data/query', {
          deviceId,
          startTime: null,
          endTime: null,
          tagQuery: null
        })
        if (res.code === '200') {
          const queryEnd = Date.now()
          this.$notify({
            title: "数据获取成功",
            message: `服务器耗时：${queryEnd - queryStart}ms`,
            type: "success",
            position: "bottom-right",
            duration: 2000
          })

          // 直接存储预处理数据，计算属性会自动更新
          this[target] = this.preprocessData(res.data)
          const processEnd = Date.now()

          this.$notify({
            title: "数据处理完成",
            message: `处理耗时：${processEnd - queryEnd}ms`,
            type: "success",
            position: "bottom-right",
            duration: 2000,
            offset: 80
          })
        }
      } catch (error) {
        this.$message.error('数据加载失败')
      }
    },

    preprocessData(rawData) {
      const sortedRecords = Object.entries(rawData.records)
          .map(([timestamp, records]) => ({
            timestamp: parseInt(timestamp),
            records: records.map(record => ({
              ...record,
              // 从fields获取tag
              tag: record.fields.tag || 'NO_TAG'
            }))
          }))
          .sort((a, b) => a.timestamp - b.timestamp)

      const fieldMap = {}

      // 优化后的预处理逻辑
      sortedRecords.forEach(({records}) => {
        records.forEach(record => {
          const tag = record.tag
          const formattedTag = tag === 'NO_TAG' ? '无标签' : tag

          Object.entries(record.fields).forEach(([field, value]) => {
            if (field === 'tag') return

            if (!fieldMap[field]) fieldMap[field] = {}
            if (!fieldMap[field][tag]) {
              fieldMap[field][tag] = {
                name: formattedTag, // 预处理显示名称
                data: [],
                sequence: 1
              }
            }

            fieldMap[field][tag].data.push([
              fieldMap[field][tag].sequence++,
              value
            ])
          })
        })
      })

      return fieldMap
    },

    formatTagDisplay(tag) {
      return tag === 'NO_TAG' ? '无标签' : tag
    },
    handleSelectAll1(val) {
      this.selectedTags1 = val ? [...this.device1Tags] : []
    },
    handleSelectAll2(val) {
      this.selectedTags2 = val ? [...this.device2Tags] : []
    }
  },
  created() {
    this.fetchAllDevices()
  }
}
</script>

<style scoped>
.device-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%; /* 填满父容器 */
  overflow: hidden;
}

.control-panel {
  padding: 15px;
  border-bottom: 1px solid #e8e8e8;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  width: 100%; /* 填满父容器 */
}

.tag-sidebar {
  display: flex;
  width: auto;
}
.tag-column {
  width: auto;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.tag-header {
  padding: 15px;
  border-bottom: 1px solid #e8e8e8;
}

.tag-list {
  flex: 1;
  padding: 10px;
}

.tag-item {
  display: block;
  margin: 8px 0;
}

.chart-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>