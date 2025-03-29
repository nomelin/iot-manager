<template>
  <div class="container">
    <el-collapse v-model="settingsVisible" accordion class="query-collapse">
      <el-collapse-item name="1">
        <template slot="title">
          <div style="font-weight: bold;font-size: 18px; text-align: left;color: #555;width: 100%">
            查询设置
            <el-button type="primary" @click="submitQuery">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button :disabled="tableData.length === 0" type="success" @click="exportData">导出</el-button>
          </div>
        </template>
        <div class="scrollable-container">
          <el-form :model="form" class="query-form" label-position="left" label-width="120px">
            <!-- 组选择 -->
            <el-form-item label="选择组">
              <el-select
                  v-model="selectedGroup"
                  clearable
                  placeholder="选择组(不选则为全部设备)"
                  @change="handleGroupChange"
              >
                <el-option
                    v-for="group in allGroups"
                    :key="group.id"
                    :label="group.name"
                    :value="group.id"
                />
              </el-select>
            </el-form-item>

            <!-- 设备ID -->
            <el-form-item label="设备">
              <el-select
                  v-model="form.deviceId"
                  placeholder="请选择设备"
                  @change="handleDeviceChange"
              >
                <el-option
                    v-for="device in filteredDevices"
                    :key="device.id"
                    :label="device.name"
                    :value="device.id"
                />
              </el-select>
              <!-- 显示设备缩略卡片 -->
              <div v-if="form.deviceId" class="device_preview">
                <device-card-mini :all-groups="allGroups"
                                  :device="selectedDevice"
                                  :show-data-types="true"/>
              </div>
            </el-form-item>

            <!-- 时间范围 -->
            <el-form-item label="时间范围">
              <div class="time-range-container">
                <el-date-picker
                    v-model="form.startTime"
                    :default-time="['00:00:00']"
                    clearable
                    format="yyyy-MM-dd HH:mm:ss"
                    placeholder="开始时间（不限制）"
                    style="width: 220px;"
                    type="datetime"
                    value-format="yyyy-MM-dd HH:mm:ss"
                />
                <span class="time-separator">至</span>
                <el-date-picker
                    v-model="form.endTime"
                    :default-time="['23:59:59']"
                    clearable
                    format="yyyy-MM-dd HH:mm:ss"
                    placeholder="结束时间（不限制）"
                    style="width: 220px;"
                    type="datetime"
                    value-format="yyyy-MM-dd HH:mm:ss"
                />
              </div>
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
              <el-select v-model="form.aggregationTime" placeholder="请选择聚合时间"
                         @change="handleAggregationTimeChange"
              >
                <el-option
                    v-for="item in aggregationTimeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>

            <el-form-item v-if="form.aggregationTime !== 0" label="聚合函数">
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

            <!-- 标签筛选 -->
            <el-form-item label="标识筛选">
              <el-select
                  v-model="form.tagQuery"
                  clearable
                  multiple
                  placeholder="请选择标识"
                  style="width: 70%"
              >
                <el-option
                    v-for="tag in deviceTags"
                    :key="tag"
                    :label="tag === 'NO_TAG' ? '无标识' : tag"
                    :value="tag"
                />
              </el-select>
              <el-button @click="selectAllTags">全选</el-button>
            </el-form-item>

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
                    <div
                        v-for="(measurement, index) in form.selectMeasurements"
                        :key="measurement+'-filter'"
                        class="threshold-item"
                    >
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
                    <div
                        v-for="(measurement, index) in form.selectMeasurements"
                        :key="measurement+'-highlight'"
                        class="threshold-item"
                    >
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


          </el-form>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 结果展示区域，使用 el-table 前端分页 -->
    <div v-if="tableData.length > 0" class="result-container">
      <div class="table-scroll">
        <el-table
            :cell-style="getCellStyle"
            :data="currentPageData"
            border
            class="table"
            height="tableHeight"
            style="width: 100%"
        >
          <!-- 序号列 -->
          <el-table-column
              :index="indexMethod"
              fixed="left"
              label="序号"
              type="index"
              width="60"
          ></el-table-column>
          <!-- 标签列 -->
          <el-table-column
              fixed="left"
              label="标识"
              prop="tag"
              width="120"
          >
            <template #default="{ row }">
              {{ row.tag === "NO_TAG" ? "无" : row.tag }}
            </template>
          </el-table-column>
          <!-- 时间戳列 -->
          <el-table-column
              fixed="left"
              label="时间戳"
              prop="timestamp"
              width="180"
          >
            <template #default="{ row }">
              {{ new Date(row.timestamp).toLocaleString() }}
            </template>
          </el-table-column>
          <!-- 根据选中的传感器生成列 -->
          <el-table-column
              v-for="col in measurementsColumns"
              :key="col"
              :label="col"
              :prop="col"
          >
            <template #default="{ row }">
              {{
                typeof row[col] === 'number'
                    ? (Number.isInteger(row[col]) ? row[col] : row[col].toFixed(4))
                    : row[col]
              }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <!-- 分页控制 -->
      <div class="pagination-container">
        <el-pagination
            :current-page.sync="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 50, 100]"
            :total="tableData.length"
            background
            layout="total, prev, pager, next, jumper, sizes"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
        ></el-pagination>

      </div>
    </div>
    <div v-else class="empty-tip">
      暂无数据
    </div>
  </div>
</template>

<script>
import DeviceCardMini from "@/views/front/module/DeviceCardMini";
import aggregateMixin from "@/mixins/aggregation";
import deviceMixin from "@/mixins/device";
import groupMixin from "@/mixins/group";

export default {
  name: 'BaseQuery',
  mixins: [aggregateMixin,deviceMixin,groupMixin],
  components: {
    DeviceCardMini
  },
  data() {
    return {
      form: {
        deviceId: null,
        startTime: null,  // 初始化为 null
        endTime: null,    // 初始化为 null
        selectMeasurements: [],
        aggregationTime: 0,
        queryAggregateFunc: null,
        tagQuery: [],
      },
      thresholdFilterEnabled: false,      // 阈值过滤开关
      thresholdHighlightEnabled: false,     // 阈值高亮开关
      highlightThresholds: [],              // 高亮阈值配置
      // timeRange: [1719072000000, 1719158400000],
      deviceMeasurements: [],
      thresholds: [],                       // 阈值过滤配置
      tableData: [],                        // 完整查询数据
      measurementsColumns: [],              // 传感器列
      settingsVisible: ['1'],               // 设置默认展开状态

      // 分页相关数据
      currentPage: 1,                       // 当前页码，默认第1页
      pageSize: 100,                         // 每页显示数据数量，可根据需要调整

      selectedGroup: null,
      groupDevices: [],

    };
  },
  computed: {
    // 根据当前页码和每页条数计算当前页显示数据
    currentPageData() {
      const start = (this.currentPage - 1) * this.pageSize;
      return this.tableData.slice(start, start + this.pageSize);
    },
    // 动态计算表格高度：假设分页区域高度为 50px
    tableHeight() {
      return 'calc(100% - 50px)';
    },
    selectedDevice() {
      return this.allDevices.find(device => device.id === this.form.deviceId);
    },
    deviceTags() {
      if (!this.selectedDevice || !this.selectedDevice.allTags) return [];
      return this.sortTags(this.selectedDevice.allTags)
    },
    filteredDevices() {
      return this.selectedGroup ? this.groupDevices : this.allDevices;
    },
  },
  watch: {
    'form.selectMeasurements': {
      handler(newVal) {
        // 初始化阈值数组
        this.thresholds = newVal.map(() => [undefined, undefined]);
        this.highlightThresholds = newVal.map(() => [undefined, undefined]);
        // console.log("thresholds: " + JSON.stringify(this.thresholds))
        // console.log("highlightThresholds: " + JSON.stringify(this.highlightThresholds))
      },
      deep: true
    }
  },
  created() {
  },
  methods: {

    async fetchDevicesByGroup(groupId) {
      try {
        const res = await this.$request.get(`/group/getDevices/${groupId}`);
        if (res.code === '200') {
          this.groupDevices = res.data;
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error('获取组设备失败');
      }
    },

    handleGroupChange(groupId) {
      if (groupId) {
        this.fetchDevicesByGroup(groupId);
      } else {
        this.groupDevices = [];
      }
      this.form.deviceId = null; // 清空已选设备
    },

    selectAllTags() {
      this.form.tagQuery = this.deviceTags;
    },
    // 序号从当前页数据基数开始计算
    indexMethod(index) {
      return index + 1 + (this.currentPage - 1) * this.pageSize;
    },
    handleSizeChange(newSize) {
      this.pageSize = newSize;
      // 若页码不再合适，可重置为第一页
      this.currentPage = 1;
    },
    handleThresholdChange(index, position, value) {
      const newValue = value === undefined || value === null ? null : Number(value)
      this.$set(this.thresholds[index], position, newValue)
    },
    handleHighlightChange(index, position, value) {
      const newVal = value === undefined ? null : Number(value)
      this.$set(this.highlightThresholds[index], position, newVal)
      // 当在“阈值高亮”配置中修改数字时，触发了 el-input-number 的 @change 事件，进而调用了 handleHighlightChange 方法，
      // 该方法通过 this.$set 更新了 highlightThresholds 数组中的值。
      // 由于这些数据是 Vue 实例中的响应式数据，一旦更新，所有依赖这些数据的视图都会自动重新渲染，所以高亮单元格也会更新。
    },
    // 高亮单元格方法（保持原有逻辑）
    getCellStyle({row, column, rowIndex, columnIndex}) {
      // console.log(`row: ${JSON.stringify(row)}, column: ${JSON.stringify(column)}, rowIndex: ${rowIndex}, columnIndex: ${columnIndex}`)
      if (!this.thresholdHighlightEnabled) return {};
      // 序号列和时间戳列不高亮
      if (columnIndex === 0 || columnIndex === 1) return {};
      // 注意：el‑table 中获取单元格对应数据时，使用 column.property
      const value = row[column.label];
      if (typeof value !== 'number') return {};
      const labelIndex = this.form.selectMeasurements.indexOf(column.label);
      const [min, max] = this.highlightThresholds[labelIndex] || [];
      const exceedMin = min != null && value < min;
      const exceedMax = max != null && value > max;
      return (exceedMin || exceedMax) ? {backgroundColor: '#ffcccc'} : {};
    },
    async handleDeviceChange(deviceId) {
      if (!deviceId) return;
      try {
        const res = await this.$request.get(`/device/getMeasurements/${deviceId}`);
        if (res.code === '200') {
          this.deviceMeasurements = res.data;
          this.form.selectMeasurements = [];
          this.form.tagQuery = [];
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error('获取设备信息失败');
      }
    },
    handleMeasurementsChange(selected) {
      // 保持选择的传感器顺序
      this.form.selectMeasurements = this.deviceMeasurements.filter(m => selected.includes(m));
    },
    async submitQuery() {
      if (this.form.deviceId == null) {
        this.$message.error('请选择设备');
        return;
      }
      if (this.form.aggregationTime !== 0 && this.form.queryAggregateFunc == null) {
        this.$message.error('请选择聚合函数');
        return;
      }
      // if (this.timeRange && this.timeRange.length === 2) {
      //   this.form.startTime = this.timeRange[0];
      //   this.form.endTime = this.timeRange[1];
      // }
      if (!this.form.selectMeasurements || this.form.selectMeasurements.length === 0) {
        // 如果没有选择属性，则自动选择全部属性
        this.form.selectMeasurements = this.deviceMeasurements;
        console.log("selectMeasurements is null or empty，自动选择全部属性")
      }
      // 构建请求参数
      const params = {
        ...this.form,
        startTime: this.form.startTime ? new Date(this.form.startTime).getTime() : null,
        endTime: this.form.endTime ? new Date(this.form.endTime).getTime() : null,
        tagQuery: this.form.tagQuery.join('||'), // 拼接数组为字符串
        thresholds: this.thresholdFilterEnabled
            ? this.thresholds.map(t => [
              t[0] !== null ? Number(t[0]) : null,
              t[1] !== null ? Number(t[1]) : null
            ])
            : null
      }
      console.log("params: " + JSON.stringify(params))

      try {
        const startTime = new Date().getTime();
        this.$message.info('等待服务器响应, 请稍候...');
        const res = await this.$request.post('/data/query', params);
        if (res.code === '200') {
          const queryEndTime = new Date().getTime();
          this.$message.success(`查询成功, 耗时${queryEndTime - startTime}毫秒`);
          console.log('查询成功, 耗时' + (queryEndTime - startTime) + '毫秒')

          this.transformData(res.data);

          const endTime = new Date().getTime()
          console.log('处理数据成功, 共耗时' + (endTime - queryEndTime) + '毫秒')
          setTimeout(() => {
            this.$message.success(`处理${this.tableData.length}条数据成功, 共耗时${endTime - queryEndTime}毫秒`)
          }, 1);//只是为了消息不重叠

          // 重置分页为第一页
          this.currentPage = 1;
          //实际上点击按钮时就点击了上面这一栏,所以即便没有下面的代码，控制面板状态也会变化。
          document.activeElement.blur();//关闭面板前让按钮不是焦点，避免控制台报错
          this.settingsVisible = [] // 关闭设置面板

        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error('请求失败: ' + error);
      }
    },
    transformData(deviceTable) {
      // console.log("deviceTable: " + JSON.stringify(deviceTable))
      this.tableData = []

      this.measurementsColumns = this.form.selectMeasurements;
      const records = deviceTable.records || {};
      // let uniqueId = 0; // 自增 id，用于内部标识
      for (const [timestamp, recordList] of Object.entries(records)) {
        recordList.forEach(record => {
          this.tableData.push({
            // id: uniqueId++,
            timestamp: Number(timestamp),
            ...record.fields
          });
        });
      }
      // 按时间戳排序
      this.tableData.sort((a, b) => a.timestamp - b.timestamp);
    },
    resetForm() {
      this.form = {
        deviceId: null,
        startTime: null,
        endTime: null,
        selectMeasurements: [],
        aggregationTime: 0,
        queryAggregateFunc: null,
        tagQuery: [],
      };
      this.thresholdFilterEnabled = false;
      // this.timeRange = [];
      this.thresholds = [];
      this.tableData = [];
      this.currentPage = 1;
    },
    selectAll() {
      this.form.selectMeasurements = [...this.deviceMeasurements];
    },
    handlePageChange(page) {
      this.currentPage = page;
    },

    // 导出文件
    exportData() {
      if (this.tableData.length === 0) {
        this.$message.warning('没有数据可导出');
        return;
      }

      const csvContent = this.generateCSV();
      // 添加BOM标记，否则excel打开时中文会乱码。
      const BOM = '\ufeff';
      const csvContentWithBOM = BOM + csvContent;
      const blob = new Blob([csvContentWithBOM], {type: 'text/csv;charset=utf-8;'});
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', this.generateFileName());
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },

    // 生成CSV内容
    generateCSV() {
      // 第一行：元信息
      const device = this.selectedDevice ? this.selectedDevice.name : '未知设备';
      const start = this.form.startTime
          ? new Date(this.form.startTime).toLocaleString()
          : '无限制';
      const end = this.form.endTime
          ? new Date(this.form.endTime).toLocaleString()
          : '无限制';
      const selectMeasurements = this.form.selectMeasurements.join('/');
      const tagQuery = this.form.tagQuery ? `标识筛选: ${this.form.tagQuery}` : '所有标签';
      const aggregation = this.form.aggregationTime === 0
          ? '不聚合'
          : `查询聚合时间粒度: ${this.aggregationTimeOptions.find(o => o.value === this.form.aggregationTime)?.label}, 查询聚合函数: ${this.form.queryAggregateFunc}`;

      const infoLine = `# 设备: ${device}; 选择传感器:${selectMeasurements}; ${tagQuery}; 查询时间范围: ${start} 至 ${end}; ${aggregation}; 生成于 ${new Date().toLocaleString()}`;

      // 第二行：列头
      const headers = ['#time', ...this.form.selectMeasurements, '标签'].join(',')

      // 数据行
      const dataRows = this.tableData.map(row => {
        const time = this.formatTimestamp(row.timestamp);
        const values = this.form.selectMeasurements.map(m => {
          const value = row[m];
          return typeof value === 'number' ? value.toString() : '';
        });
        const tag = row.tag ?? ''; // 确保标签存在，避免 undefined
        return [time, ...values, tag].join(',');
      }).join('\n');

      return [infoLine, headers, dataRows].join('\n');
    },

    // 时间格式化方法
    formatTimestamp(timestamp) {
      const date = new Date(timestamp);
      return [
        date.getFullYear(),
        date.getMonth() + 1,
        date.getDate(),
        date.getHours(),
        date.getMinutes(),
        date.getSeconds()
      ].join('-');
    },

    // 生成文件名
    generateFileName() {
      const device = this.selectedDevice ? this.selectedDevice.name.replace(/\s+/g, '_') : 'unknown';
      const now = new Date();
      const timeStr = [
        now.getFullYear(),
        (now.getMonth() + 1).toString().padStart(2, '0'),
        now.getDate().toString().padStart(2, '0'),
        now.getHours().toString().padStart(2, '0'),
        now.getMinutes().toString().padStart(2, '0'),
        now.getSeconds().toString().padStart(2, '0')
      ].join('-');
      return `${device}_${timeStr}.csv`;
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
  }
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  font-weight: bold;
  padding: 15px;
  min-width: 0; /* 允许子元素收缩 */
}

.query-collapse {
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-height: 60px;
  padding-bottom: 1rem;
}

.scrollable-container {
  max-height: 500px;
  /*overflow-y: auto;*/
  padding: 10px;
}

::v-deep .el-collapse-item__header {
  padding: 0 24px;
  height: 60px;
  border-bottom: none;
  border-radius: 1rem;
}

::v-deep .el-collapse-item__content {
  padding: 24px;
  overflow-y: auto; /* 折叠面板内部滚动 */
  max-height: 50vh; /* 限制最大高度 */
  /*border-radius: 1rem;*/
}

.query-form {
  max-width: 90%;
  margin: 20px auto;
}

.time-range-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-separator {
  color: #666;
  font-size: 14px;
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
  display: flex;
  flex-direction: column;
  margin-top: 16px;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 10px;
  min-height: 0; /* 关键：允许子元素压缩 */
}

.table-scroll {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden; /* 禁止纵向滚动 */
  min-width: 0;
  min-height: 0; /* 允许在flex布局中压缩 */
}

/* 调整表格高度计算 */
.el-table {
  height: calc(100%) !important;
}

.pagination-container {
  flex-shrink: 0; /* 防止分页被压缩 */
  padding: 10px 0;
  text-align: center;
}

.device_preview {
  /*margin-top: 10px;*/
  /*padding: 10px;*/
  /*border: 1px solid #ebeef5;*/
  /*border-radius: 4px;*/
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

/* 表格区域，自动填满剩余空间 */
.table-wrapper {
  flex: 1;
  overflow: hidden; /* 防止内部 el‑table 超出容器 */
}

/*!* 分页区域，固定高度 *!*/
/*.pagination-container {*/
/*  flex: 0 0 50px; !* 分页区域高度 50px，可根据需要调整 *!*/
/*  padding: 10px 0;*/
/*  text-align: center;*/
/*}*/
::v-deep .el-button {
  font-weight: bold !important;
}
</style>
