<template>
  <div class="view-platform">
    <!-- 上侧控制区域 -->
    <el-row :gutter="20" class="control-panel">
      <!-- 左上：组选择 -->
      <el-col :span="3">
        <el-form label-position="left" label-width="auto">
          <el-form-item label="组选择">
            <el-select v-model="selectedGroup" placeholder="选择组" @change="fetchDevices">
              <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id"/>
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>

      <!-- 左下：设备选择 -->
      <el-col :span="6">
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

      <!-- 右上：筛选条件 -->
      <el-col :span="15" class="filter-col" label-position="left" label-width="auto">
        <el-form class="filter-form" label-position="left" label-width="auto">
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
                  @click="selectQuickRange(btn.type)"
                  class="quick-button"
              >
                {{ btn.label }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>

    <!-- 下侧数据展示 -->
    <div class="data-view">
      <GroupView
          :date-range="dateRange"
          :devices="devices"
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
      }
    };
  },
  methods: {
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
.quick-button{

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