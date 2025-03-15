<template>
  <div class="view-platform">
    <!-- 上侧控制区域 -->
    <div class="control-panel">
      <el-row :gutter="20">
        <!-- 左上：组选择 -->
        <el-col :span="4">
          <el-form label-position="left" label-width="auto">
            <el-form-item label="组选择">
              <el-select v-model="selectedGroup" placeholder="选择组" @change="fetchDevices">
                <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id"/>
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
  </div>
</template>

<script>
import GroupView from "@/views/front/ViewPlatform/GroupView.vue";

export default {
  name: "ViewPlatform",
  components: {GroupView},
  data() {
    return {
      groups: [], // 全部组信息
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

      // 新增聚合相关数据
      aggregationTime: 0,
      queryAggregateFunc: null,
      aggregationTimeOptions: [
        {label: '不聚合', value: 0},
        {label: '1ms', value: 1},
        {label: '1s', value: 1000},
        {label: '5s', value: 5000},
        {label: '15s', value: 15000},
        {label: '30s', value: 30000},
        {label: '1m', value: 60000},
        {label: '5m', value: 300000},
        {label: '15m', value: 900000},
        {label: '30m', value: 1800000},
        {label: '1h', value: 3600000},
        {label: '1d', value: 86400000}
      ],
      aggregateFuncOptions: [],

    };
  },
  methods: {
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
    handleAggregationTimeChange(value) {
      if (value === 0) {
        this.queryAggregateFunc = null;
      }
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
        return;
      }
      this.$request
          .get(`/group/getDevices/${this.selectedGroup}`)
          .then((res) => {
            if (res.code === "200") {
              this.devices = res.data;
              //填充组内全部设备
              this.selectedDeviceIds = this.groups.find(group => group.id === this.selectedGroup).deviceIds;
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
  },
  created() {
    this.fetchGroups(); // 初始化加载组信息
    this.loadAggregateFunctions(); // 初始化加载聚合函数
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

/*.filter-col {*/
/*  padding-bottom: 0;*/
/*  margin-bottom: 0;*/
/*}*/

/*.filter-form {*/
/*  padding-bottom: 0;*/
/*  margin-bottom: 0;*/
/*}*/

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