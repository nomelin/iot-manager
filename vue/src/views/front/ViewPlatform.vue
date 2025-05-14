<template>
  <div class="view-platform">
    <!-- 上侧控制区域 -->
    <div class="control-panel">
      <el-row :gutter="20">
        <!-- 左上：组选择 -->
        <el-col :span="4">
          <el-form label-position="left" label-width="auto">
            <el-form-item label="组选择">
              <el-select v-model="selectedGroup" filterable placeholder="选择组" @change="fetchDevices">
                <el-option v-for="group in allGroups" :key="group.id" :label="group.name" :value="group.id"/>
              </el-select>
            </el-form-item>
          </el-form>
        </el-col>

        <!-- 左下：设备选择 -->
        <el-col :span="8">
          <el-form label-position="left" label-width="auto">
            <el-form-item label="设备选择">
              <el-select
                  v-model="selectedDeviceIds"
                  :clearable="true"
                  filterable
                  multiple
                  placeholder="选择设备"
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
        <!-- 聚合时间 -->
        <el-col :span="4">
          <el-form label-position="left" label-width="auto">
            <el-form-item label="聚合时间">
              <el-select
                  v-model="aggregationTime"
                  placeholder="选择聚合时间"
                  @change="handleAggregationTimeChange"
              >
                <el-option
                    v-for="item in aggregationTimeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </el-col>

        <!-- 聚合函数 -->
        <el-col :span="4">
          <el-form label-position="left" label-width="auto">
            <el-form-item label="聚合函数">
              <el-select
                  v-model="queryAggregateFunc"
                  :disabled="aggregationTime === 0"
                  placeholder="选择聚合函数"
              >
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
          </el-form>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="showAnalysisDialog">查询分析</el-button>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="filter-row">
        <el-col :span="24" class="filter-col" label-position="left" label-width="auto">
          <el-form class="filter-form" inline label-position="left" label-width="auto">
            <!-- 时间选择行 -->
            <el-form-item class="compact-item" label="时间段选择">
              <el-row align="middle" justify="center" type="flex">
                <el-col :span="2">
                  <el-button icon="el-icon-arrow-left" @click="shiftTimeRange(-1)"/>
                </el-col>
                <el-col :span="20">
                  <el-date-picker
                      v-model="dateRange"
                      :default-time="['00:00:00', '23:59:59']"
                      end-placeholder="结束时间"
                      range-separator="至"
                      start-placeholder="开始时间"
                      type="datetimerange"
                      value-format="timestamp"
                  />
                </el-col>
                <el-col :span="2">
                  <el-button icon="el-icon-arrow-right" @click="shiftTimeRange(1)"/>
                </el-col>
              </el-row>
            </el-form-item>

            <!-- 快捷按钮行 -->
            <el-form-item class="compact-item">
              <div class="quick-buttons">
                <el-button
                    v-for="(btn, index) in quickButtons"
                    :key="index"
                    class="quick-button"
                    @click="selectQuickRange(btn.type)"
                >
                  {{ btn.label }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </div>

    <!-- 下侧数据展示 -->
    <div class="data-view">
      <GroupView
          :aggregation-time="aggregationTime"
          :date-range="dateRange"
          :devices="devices"
          :query-aggregate-func="queryAggregateFunc"
          :selected-device-ids="selectedDeviceIds "
      />
    </div>
    <!-- 查询分析对话框 -->
    <el-dialog
        :visible.sync="analysisDialogVisible"
        title="查询分析"
        width="600px"
    >
      <div v-if="isLoadingDataCount" class="loading-container">
        <i class="el-icon-loading" style="font-size: 24px;"></i>
        <p>正在加载数据...</p>
      </div>
      <div v-else>
        <div class="count-info">
          <span>数据点数量(实际)：</span>
          <strong>{{ dataPointCount.toLocaleString() }}</strong>
          <el-tag :type="countLevelType" class="count-tag">{{ countLevelLabel }}</el-tag>
        </div>
        <div class="analysis-content">
          <p>数据点数量=所有传感器数据点数量之和，这直接决定了耗时。</p>
          <p>大数据查询时，耗时主要为数据库查询耗时，网络传输耗时，前端渲染耗时。</p>
          <p>
            本系统已采用GZIP优化网络传输，不过由于服务器带宽较低，约为500KB~1MB/S，如果数据量较大，依然需要较久的网络传输时间。网络传输时间和服务器响应时间可从F12控制台网络页面查看。</p>
          <p class="warning-text">一般认为，当数据点数量达到10,000,000量级时，可能无法在10秒内得到结果。</p>

          <h4 class="optimize-title">参考优化方法：</h4>
          <p>1. 减少选择时间范围：这可以减少查询的数据点数量，从而减少数据库查询耗时，网络传输耗时，前端渲染耗时。</p>
          <p>2. 增大查询聚合时间粒度：这可以减少返回的数据点数量，从而减少网络传输时间和前端渲染耗时，
            对于支持快速聚合的存储策略可以减少数据库查询耗时（若不支持快速聚合则无法减少）。</p>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import GroupView from "@/views/front/ViewPlatform/GroupView.vue";
import aggregateMixin from "@/mixins/aggregation";
import groupMixin from "@/mixins/group";

export default {
  name: "ViewPlatform",
  mixins: [aggregateMixin, groupMixin],
  components: {GroupView},
  data() {
    return {
      devices: [], // 当前组设备
      selectedGroup: null, // 当前选择的组ID
      selectedDeviceIds: [], // 当前选择的设备ID
      dateRange: [1719072000000, 1719158400000], // 时间段选择

      currentQuickRange: '1d', // 当前选择的快捷范围类型
      quickButtons: [
        {type: '1m', label: '近1分钟'},
        {type: '15m', label: '近15分钟'},
        {type: '1h', label: '近1小时'},
        {type: '6h', label: '近6小时'},
        {type: '1d', label: '近1天'},
        {type: '1w', label: '近1周'},
        {type: '1M', label: '近1月'},
        {type: '1y', label: '近1年'}
      ],
      ranges: {
        '1m': 60 * 1000,
        '15m': 15 * 60 * 1000,
        '1h': 60 * 60 * 1000,
        '6h': 6 * 60 * 60 * 1000,
        '1d': 24 * 60 * 60 * 1000,
        '1w': 7 * 24 * 60 * 60 * 1000,
        '1M': 30 * 24 * 60 * 60 * 1000,
        '1y': 365 * 24 * 60 * 60 * 1000,
      },

      analysisDialogVisible: false,
      isLoadingDataCount: false,
      dataPointCount: 0,

    };
  },
  computed: {
    countLevelLabel() {
      const count = this.dataPointCount;
      if (count < 100000) return '低';
      if (count < 3000000) return '中';
      if (count < 20000000) return '高';
      return '极高';
    },
    countLevelType() {
      const count = this.dataPointCount;
      if (count < 100000) return 'success';
      if (count < 3000000) return 'warning';
      if (count < 20000000) return 'danger';
      return 'danger';
    },
  },
  watch: {
    allGroups: {
      immediate: true,
      handler(groups) {
        if (groups && groups.length > 0) {
          const savedGroupId = localStorage.getItem('lastSelectedGroup');
          if (savedGroupId) {
            // console.log('groups:',JSON.stringify(groups))
            const groupExists = groups.some(g => g.id === Number(savedGroupId));
            if (groupExists) {
              this.selectedGroup = Number(savedGroupId);
              console.log('lastSelectedGroup found:', savedGroupId);
              this.fetchDevices();
            } else {
              console.log('lastSelectedGroup not found and removed:', savedGroupId);
              localStorage.removeItem('lastSelectedGroup');
            }
          }
        }
      }
    },
    selectedGroup(newVal) {
      if (newVal) {
        // console.log('selectedGroup changed to', newVal);
        localStorage.setItem('lastSelectedGroup', newVal);
      } else {
        console.log('selectedGroup cleared');
        localStorage.removeItem('lastSelectedGroup');
      }
    }
  },
  methods: {
    fetchDevices() {
      if (!this.selectedGroup) {
        this.devices = [];
        this.selectedDeviceIds = [];
        return;
      }
      this.$request
          .get(`/group/getDevices/${this.selectedGroup}`)
          .then((res) => {
            if (res.code === "200") {
              this.devices = res.data;
              //填充组内全部设备
              this.selectedDeviceIds = this.allGroups.find(group => group.id === this.selectedGroup).deviceIds;
              //将时间设置为最近15分钟。
              this.selectQuickRange('15m');
            } else {
              this.$message.error("加载设备信息失败：" + res.msg);
            }
          })
          .catch((error) => {
            this.$message.error("请求设备信息失败：" + error.message);
          });

    },

    selectQuickRange(type) {
      this.currentQuickRange = type

      if (this.ranges[type]) {
        const duration = this.ranges[type]
        const endTime = Date.now()
        const startTime = endTime - duration
        this.dateRange = [startTime, endTime]
      }
    },

    shiftTimeRange(direction) {
      if (!this.dateRange || this.dateRange.length !== 2) return

      const duration = this.dateRange[1] - this.dateRange[0]
      const shift = direction * (this.ranges[this.currentQuickRange] || duration)

      this.dateRange = [
        this.dateRange[0] + shift,
        this.dateRange[1] + shift
      ]
    },

    async showAnalysisDialog() {
      if (this.selectedDeviceIds.length === 0) {
        this.$message.warning('请先选择设备');
        return;
      }
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请选择有效时间范围');
        return;
      }

      const [startTime, endTime] = this.dateRange;
      if (startTime > endTime) {
        this.$message.warning('开始时间不能大于结束时间');
        return;
      }

      this.analysisDialogVisible = true;
      this.isLoadingDataCount = true;
      this.dataPointCount = 0;

      try {
        const counts = await Promise.all(
            this.selectedDeviceIds.map(deviceId =>
                this.$request.post('/data/query/count', {
                  deviceId,
                  startTime,
                  endTime
                }).then(res => res.code === '200' ? res.data : 0)
            )
        );

        this.dataPointCount = counts.reduce((sum, count) => sum + (Number(count) || 0), 0);
      } catch (error) {
        this.$message.error('数据加载失败');
        console.error('查询数据点数量失败:', error);
      } finally {
        this.isLoadingDataCount = false;
      }
    },
  },
  created() {
  },
};
</script>

<style scoped>
.view-platform {
  display: flex;
  flex-direction: column;
  height: 100%; /* 填满父容器 */
  box-sizing: border-box;
  font-weight: bold;
  overflow-x: hidden;
}

.control-panel {
  padding: 10px 10px 0;
  border-bottom: 1px solid #e8e8e8;
  /*box-sizing: border-box; !* 包括内边距和边框在内的宽高计算 *!*/
}


.el-select, .el-input {
  width: 100%;
}

.data-view {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.compact-item {
  margin-bottom: 5px !important;
}

.quick-buttons {
  display: flex;
  gap: 0;
  flex-wrap: wrap;
}

.quick-button {

}

.filter-form .el-form-item__content {
  line-height: 1;
}

.el-date-editor--datetimerange {
  width: 98%;
}

.el-row--flex {
  align-items: center;
}

.el-button [class*="el-icon-"] {
  margin: 0;
}

::v-deep .el-button {
  /*font-weight: bold !important;*/
}

::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}
</style>