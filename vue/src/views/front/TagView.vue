<template>
  <div
      v-loading="isLoading||isLoadingTag"
      class="device-view"
      element-loading-text="数据加载中..."
  >
    <!-- 上侧控制区域 -->
    <el-row :gutter="20" align="middle" class="control-panel">
      <el-col :span="4">
        <el-form label-position="left" label-width="auto">
          <el-form-item label="组选择">
            <el-select v-model="selectedGroup" placeholder="选择组" @change="fetchDevices">
              <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id"/>
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>
      <!-- 多设备选择 -->
      <el-col :span="10">
        <el-form label-position="left" label-width="auto">
          <el-form-item label="选择设备">
            <el-select
                :value="selectedDeviceIds"
                multiple
                placeholder="选择设备（可多选）"
                @change="handleDeviceChange"
            >
              <el-option
                  v-for="device in devices"
                  :key="device.id"
                  :label="device.name"
                  :value="device.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>

      <el-col :span="2">
        <el-button type="primary" @click="showTagDialog = true">选择标签</el-button>
      </el-col>

      <!-- 传感器筛选输入框 -->
      <el-col :span="6">
        <el-tooltip
            content="支持查询语法：直接输入关键词模糊匹配, 使用{sensor1|sensor2}精确匹配多个字段"
            placement="top"
        >
          <el-input
              v-model="tempFilterText"
              clearable
              placeholder="输入传感器名称关键字"
              @change="handleFilterChange"
              @clear="handleClear"
          >
            <template #suffix>
              <i class="el-icon-info" style="cursor: help"/>
            </template>
          </el-input>
        </el-tooltip>
      </el-col>

      <!-- 操作按钮组 -->
      <el-col :span="2">
        <el-col :span="2">
          <el-button
              type="primary"
              @click="exportDialogVisible = true"
          >
            数据导出
          </el-button>
        </el-col>

        <export-dialog
            v-model="exportDialogVisible"
            :selected-device-ids="selectedDeviceIds"
            :devices="devices"
            :selected-tags="selectedTags"
            :preprocessed-data="preprocessedData"
            :filter-text="filterText"
        />
      </el-col>
    </el-row>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 图表展示区域 -->
      <div class="chart-area">
        <TagLineChart
            v-if="processedData"
            :chart-data="processedData"
        />
      </div>
    </div>

    <!-- 标签选择对话框 -->
    <!-- 标签选择抽屉 -->
    <el-drawer
        :visible.sync="showTagDialog"
        custom-class="tag-drawer"
        label="rtl"
        size="35%"
        title="选择标签"
        @open="handleDrawerOpen"
    >
      <div class="tag-drawer-content">
        <div class="mode-switch">
          <el-switch
              v-model="immediateApply"
              active-text="立即应用更改"
              style="margin-bottom: 20px"
          />
        </div>
        <el-scrollbar v-if="immediateApply">
          <div class="device-tags-container">
            <div
                v-for="device in selectedDevices"
                :key="device.id"
                class="device-tag-section"
            >
              <h3>{{ device.name }} 标签</h3>
              <div class="tag-selector">
                <el-checkbox
                    v-model="allTagsSelected[device.id]"
                    :indeterminate="isIndeterminate[device.id]"
                    @change="handleSelectAll(device.id, $event)"
                >全选
                </el-checkbox>
                <el-button
                    type="text"
                    @click="handleUnselectAll(device.id)"
                    style="margin-left: 10px;"
                >全不选</el-button>
                <el-scrollbar>
                  <el-checkbox-group
                      v-model="selectedTags[device.id]"
                      class="tag-group"
                  >
                    <el-checkbox
                        v-for="tag in sortedDeviceTags[device.id]"
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
          </div>
        </el-scrollbar>

        <!-- 懒应用模式 -->
        <el-scrollbar v-else>
          <div class="device-tags-container">
            <div
                v-for="device in selectedDevices"
                :key="device.id"
                class="device-tag-section"
            >
              <h3>{{ device.name }} 标签</h3>
              <div class="tag-selector">
                <el-checkbox
                    v-model="pendingAllSelected[device.id]"
                    :indeterminate="pendingIndeterminate[device.id]"
                    @change="handlePendingSelectAll(device.id, $event)"
                >全选
                </el-checkbox>
                <el-button
                    type="text"
                    @click="handlePendingUnselectAll(device.id)"
                    style="margin-left: 10px;"
                >全不选</el-button>
                <el-scrollbar>
                  <el-checkbox-group
                      v-model="pendingSelectedTags[device.id]"
                      class="tag-group"
                  >
                    <el-checkbox
                        v-for="tag in sortedDeviceTags[device.id]"
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
          </div>
        </el-scrollbar>


      </div>

      <div class="drawer-footer">
        <el-button @click="showTagDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">应用更改</el-button>
      </div>
    </el-drawer>


  </div>
</template>

<script>
import TagLineChart from './DeviceView/TagLineChart.vue'
import ExportDialog from './DeviceView/ExportDialog.vue'

export default {
  name: 'DeviceView',
  components: {TagLineChart,ExportDialog},
  data() {
    return {
      // allDevices: [],
      selectedDeviceIds: [],
      deviceTags: {}, // {deviceId: [...]}
      selectedTags: {}, // {deviceId: [...]}
      preprocessedData: {}, // {deviceId: preprocessedData}
      filterText: '',
      tempFilterText: '',
      showTagDialog: false,
      isLoading: false,
      isLoadingTag: false,

      selectedGroup: null, // 当前选择的组ID
      groups: [], // 全部组信息
      devices: [], // 当前组设备

      immediateApply: true,
      pendingSelectedTags: {},
      pendingAllSelected: {},
      pendingIndeterminate: {},

      exportDialogVisible: false,
    }
  },
  computed: {
    selectedDevices() {
      return this.selectedDeviceIds
          .map(id => this.devices.find(d => d.id === id))
          .filter(Boolean)
    },
    sortedDeviceTags() {
      const result = {}
      this.selectedDeviceIds.forEach(deviceId => {
        result[deviceId] = this.sortTags([...this.deviceTags[deviceId] || []])
      })
      return result
    },
    allTagsSelected() {
      const result = {}
      this.selectedDeviceIds.forEach(deviceId => {
        const tags = this.deviceTags[deviceId] || []
        result[deviceId] = tags.length > 0 &&
            this.selectedTags[deviceId]?.length === tags.length
      })
      return result
    },
    isIndeterminate() {
      const result = {}
      this.selectedDeviceIds.forEach(deviceId => {
        const selected = this.selectedTags[deviceId]?.length || 0
        const total = this.deviceTags[deviceId]?.length || 0
        result[deviceId] = selected > 0 && selected < total
      })
      return result
    },
    processedData() {
      const processStart = Date.now()
      this.isLoadingTag = true
      // 处理所有选中的设备数据
      const allData = this.selectedDeviceIds.map(deviceId => {
        const device = this.devices.find(d => d.id === deviceId)
        if (!device || !this.preprocessedData[deviceId]) return []

        return this.processDeviceData(
            this.preprocessedData[deviceId],
            this.selectedTags[deviceId] || [],
            device.name
        )
      }).flat()

      // 合并相同field的数据
      const merged = {}
      allData.forEach(({field, series}) => {
        if (!merged[field]) merged[field] = {field, series: []}
        merged[field].series.push(...series)
      })

      // 过滤逻辑
      const filter = this.parseFilterSyntax(this.filterText)
      const filtered = Object.values(merged).filter(item => {
        return filter.regex ? filter.regex.test(item.field) : true
      })

      const processEnd = Date.now()
      this.isLoadingTag = false
      console.log('切换tag处理时间:' + (processEnd - processStart) + 'ms')
      // console.log('最终展示图表数量:', filtered.length)
      return filtered
    }
  },

  watch: {
    immediateApply(val) {
      console.log('切换模式:', val)
      if (!val) {
        this.handleDrawerOpen()//因为在不关闭抽屉时也需要更新状态，所以这里需要调用。
      }
    }
  },


  methods: {
    handleDrawerOpen() {
      // 初始化懒应用模式的临时数据
      if (!this.immediateApply) {
        this.pendingSelectedTags = JSON.parse(JSON.stringify(this.selectedTags))
        this.updatePendingSelectionState()
      }
    },

    handleConfirm() {
      if (!this.immediateApply) {
        // 应用懒模式的修改
        this.selectedTags = JSON.parse(JSON.stringify(this.pendingSelectedTags))
      }
      this.showTagDialog = false
    },

    handlePendingSelectAll(deviceId, val) {
      this.$set(this.pendingSelectedTags, deviceId,
          val ? [...this.deviceTags[deviceId]] : []
      )
      this.updatePendingSelectionState()
    },

    updatePendingSelectionState() {
      this.selectedDeviceIds.forEach(deviceId => {
        const tags = this.deviceTags[deviceId] || []
        const selected = this.pendingSelectedTags[deviceId]?.length || 0

        this.$set(this.pendingAllSelected, deviceId,
            tags.length > 0 && selected === tags.length
        )
        this.$set(this.pendingIndeterminate, deviceId,
            selected > 0 && selected < tags.length
        )
      })
    },
    // 立即应用模式的全不选
    handleUnselectAll(deviceId) {
      this.$set(this.selectedTags, deviceId, []);
    },

    // 懒应用模式的全不选
    handlePendingUnselectAll(deviceId) {
      this.$set(this.pendingSelectedTags, deviceId, []);
      this.updatePendingSelectionState();
    },
    fetchGroups() {
      this.$request
          .get("/group/all")
          .then((res) => {
            if (res.code === "200") {
              console.log("加载组信息成功：" + JSON.stringify(res.data));
              this.groups = res.data;
            } else {
              this.$message.error("加载组信息失败：" + res.msg);
            }
          })
          .catch((error) => {
            this.$message.error("请求组信息失败：" + error.message);
          });
    },
    fetchDevices() {
      if (!this.selectedGroup) {
        this.devices = [];
        this.selectedDeviceIds = [];
        // this.selectedTags = {};
        // this.preprocessedData = {};
        return;
      }
      this.$request
          .get(`/group/getDevices/${this.selectedGroup}`)
          .then((res) => {
            if (res.code === "200") {
              this.devices = res.data;
              this.selectedDeviceIds = []; // 重置设备选择
              // this.selectedTags = {}; // 重置设备标签选择
              // this.preprocessedData = {}; // 重置设备数据
            } else {
              this.$message.error("加载设备信息失败：" + res.msg);
            }
          })
          .catch((error) => {
            this.$message.error("请求设备信息失败：" + error.message);
          });
    },


    // 处理筛选条件变化
    handleFilterChange(value) {
      this.filterText = value;
    },

    // 处理清除操作
    handleClear() {
      this.tempFilterText = '';
      this.filterText = '';
    },
    processDeviceData(data, tags, deviceName) {
      const selected = new Set(tags)
      return Object.entries(data).map(([field, tagsData]) => ({
        field,
        series: Object.entries(tagsData)
            .filter(([tag]) => selected.has(tag))
            .map(([tag, {name, data}]) => ({
              name: `${deviceName}-${name}`,
              data,
              lineStyle: {type: 'solid'}
            }))
      }))
    },
    // async fetchAllDevices() {
    //   try {
    //     const res = await this.$request.get('/device/all')
    //     if (res.code === '200') {
    //       this.allDevices = res.data
    //     }
    //   } catch (error) {
    //     this.$message.error('设备加载失败')
    //   }
    // },
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
    // 新增方法：解析高级查询语法
    parseFilterSyntax(input) {
      // 辅助方法：转义正则特殊字符
      function escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      }

      // 处理空值情况
      if (!input) return {regex: null, isExact: false}

      // 精确匹配模式 {条件1|条件2}
      const exactMatch = input.match(/^{(.*)}$/)
      if (exactMatch) {
        const conditions = exactMatch[1].split('|').map(s => s.trim())
        // 创建不区分大小写的正则表达式，匹配完整字段名
        return {
          regex: new RegExp(`^(${conditions.map(c => escapeRegExp(c)).join('|')})$`, 'i'),
          isExact: true
        }
      }

      // 普通模糊匹配（保留原有逻辑）
      return {
        regex: new RegExp(escapeRegExp(input), 'i'),
        isExact: false
      }
    },

    async handleDeviceChange(deviceIds) {
      // console.log("deviceIds", JSON.stringify(deviceIds))
      // console.log("进入selectedDeviceIds", JSON.stringify(this.selectedDeviceIds))
      // 移除已取消选择的设备数据
      const removed = this.selectedDeviceIds.filter(id => !deviceIds.includes(id))
      console.log("device removed", JSON.stringify(removed))
      removed.forEach(id => {
        delete this.preprocessedData[id]
        delete this.selectedTags[id]
        delete this.deviceTags[id]
      })

      // 加载新设备的标签和数据
      const added = deviceIds.filter(id => !this.selectedDeviceIds.includes(id))
      console.log("device added", JSON.stringify(added))
      for (const deviceId of added) {
        if (!this.deviceTags[deviceId]) {
          const device = this.devices.find(d => d.id === deviceId)
          // console.log("device", device)
          this.deviceTags[deviceId] = device?.allTags || []
          this.$set(this.selectedTags, deviceId, [])
          await this.fetchDeviceData(deviceId)
        }
      }

      this.selectedDeviceIds = deviceIds
      console.log("selectedDeviceIds", JSON.stringify(this.selectedDeviceIds))
    },

    async fetchDeviceData(deviceId) {
      if (!deviceId) return
      // this.$message.info('请稍候...')
      this.isLoading = true
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
          const records = res.data.records;
          const totalRecords = Object.values(records).reduce((sum, arr) => sum + arr.length, 0);
          this.$notify({
            title: `数据获取成功，共 ${totalRecords} 条数据`,
            message: `服务器耗时：${queryEnd - queryStart}ms`,
            type: "success",
            position: "bottom-right",
            duration: 3000
          })
          console.log(`device ${deviceId} ,数据获取成功，共 ${totalRecords} 条数据, 服务器耗时：${queryEnd - queryStart}ms`)

          // 存储预处理数据
          this.$set(this.preprocessedData, deviceId, this.preprocessData(res.data))
          const processEnd = Date.now()

          this.$notify({
            title: `数据处理完成，共 ${totalRecords} 条数据`,
            message: `处理耗时：${processEnd - queryEnd}ms`,
            type: "success",
            position: "bottom-right",
            duration: 3000,
            offset: 80
          })
          console.log(`device ${deviceId} ,数据处理完成，共 ${totalRecords} 条数据, 前端处理耗时：${processEnd - queryEnd}ms`)
        }
      } catch (error) {
        this.$message.error('数据加载失败')
      } finally {
        this.isLoading = false
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

    handleSelectAll(deviceId, val) {
      this.$set(this.selectedTags, deviceId,
          val ? [...this.deviceTags[deviceId]] : []
      )
    },

    naturalSort(arr) {
      return arr.slice().sort((a, b) =>
          a.localeCompare(b, undefined, {numeric: true, sensitivity: 'base'})
      )
    }


  },
  created() {
    // this.fetchAllDevices()
    this.fetchGroups(); // 初始化加载组信息
  },
}
</script>

<style scoped>
.device-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  font-weight: bold;
}

.el-select, .el-input {
  width: 100%;
}

.control-panel {
  /*margin-bottom: 20px;*/
}

.mode-switch {
  padding: 0 20px 10px;
  border-bottom: 1px solid #eee;
  margin-bottom: 10px;
}

.main-content {
  flex: 1;
  overflow: hidden;
  width: 100%;
}


.chart-area {
  height: 100%;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
}

::v-deep .el-dialog__body {
  padding: 10px 20px;
}

::v-deep .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-drawer-content {
  height: calc(100% - 55px);
  padding: 20px;
}

.device-tags-container {
  display: flex;
  flex-wrap: nowrap;
  gap: 20px;
}

.device-tag-section {
  /*flex: 0 0 300px;  !* 固定每个设备的宽度 *!*/
  background: #f8f9fa;
  border-radius: 4px;
  padding: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: auto;
  /*min-width: 50px;*/
}

.tag-selector {
  height: calc(75vh - 100px); /* 计算合适的高度 */
  display: flex;
  flex-direction: column;
}

.tag-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}

.tag-item {
  margin: 0;
  white-space: nowrap; /* 防止标签换行 */
}

::v-deep .el-scrollbar__wrap {
  overflow-x: auto; /* 确保横向滚动生效 */
}

/* 调整竖向滚动条样式 */
::v-deep .el-scrollbar__bar.is-vertical {
  right: 2px;
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  border-top: 1px solid #e8e8e8;
  padding: 10px 20px;
  text-align: left;
  background: #fff;
}

.el-button--text {
  padding: 0;
  height: auto;
}
::v-deep .el-button {
  font-weight: bold !important;
}
::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}
</style>