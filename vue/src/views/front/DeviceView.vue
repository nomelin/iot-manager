<template>
  <div class="device-view">
    <!-- 上侧控制区域 -->
    <el-row :gutter="20" class="control-panel">
      <!-- 设备选择 -->
      <el-col :span="6">
        <el-form>
          <el-form-item label="设备选择">
            <el-select
                v-model="selectedDeviceId"
                placeholder="选择设备"
                @change="handleDeviceChange"
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
      <!-- 标签选择侧边栏 -->
      <div class="tag-sidebar">
        <div class="tag-header">
          <el-checkbox
              v-model="allTagsSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
          >
            全选
          </el-checkbox>
        </div>
        <el-scrollbar class="tag-list">
          <el-checkbox-group v-model="selectedTags">
            <el-checkbox
                v-for="tag in sortedDeviceTags"
                :key="tag"
                :label="tag"
                class="tag-item"
            >
              {{ formatTagDisplay(tag) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-scrollbar>
      </div>

      <!-- 图表展示区域 -->
      <div class="chart-area">
        <TagLineChart
            v-if="processedData"
            :chart-data="processedData"
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
      selectedDeviceId: null,
      deviceTags: [],
      selectedTags: [],
      rawData: null,
      preprocessedData: null
    }
  },
  computed: {
    allTagsSelected: {
      get() {
        return this.deviceTags.length > 0 &&
            this.selectedTags.length === this.deviceTags.length
      },
      set(value) {
        this.selectedTags = value ? [...this.deviceTags] : []
      }
    },
    isIndeterminate() {
      return this.selectedTags.length > 0 &&
          this.selectedTags.length < this.deviceTags.length
    },
    sortedDeviceTags() {
      //NO_TAG放第一个，其次是整数类字符串从小到大排序，最后是其他字符串，按字母顺序排序
      return [...this.deviceTags].sort((a, b) => {
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
    processedData() {
      if (!this.preprocessedData) return null

      const processStart = Date.now()
      const selectedTagsSet = new Set(this.selectedTags)
      const result = {}

      for (const [field, tags] of Object.entries(this.preprocessedData)) {
        const series = []
        for (const [tag, tagData] of Object.entries(tags)) {
          if (selectedTagsSet.has(tag)) {
            series.push({
              name: tagData.name,
              data: tagData.data
            })
          }
        }
        result[field] = {series}
      }

      const res = Object.entries(result).map(([field, data]) => ({
        field,
        ...data
      }))
      const processEnd = Date.now()
      console.log('切换tag处理时间:' + (processEnd - processStart) + 'ms')
      return res
    },
  },
  watch: {},
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

    async handleDeviceChange(deviceId) {
      const device = this.allDevices.find(d => d.id === deviceId)
      this.deviceTags = device.allTags || []
      this.selectedTags = []
      await this.fetchDeviceData(deviceId)
    },

    async fetchDeviceData(deviceId) {
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
          this.preprocessedData = this.preprocessData(res.data)
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

    handleSelectAll(val) {
      this.selectedTags = val ? [...this.deviceTags] : []
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