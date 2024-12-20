<template>
  <div class="view-platform">
    <!-- 上侧控制区域 -->
    <el-row :gutter="10" class="control-panel">
      <!-- 组选择 -->
      <el-col :span="6">
        <el-form>
          <div class="group-select">
            <el-form-item label="组选择">
              <el-select v-model="selectedGroup" placeholder="选择组">
                <el-option v-for="group in groups" :key="group" :label="group" :value="group"/>
              </el-select>
            </el-form-item>
          </div>
        </el-form>
      </el-col>

      <!-- 设备选择 -->
      <el-col :span="6">
        <el-form>
          <el-form-item label="设备选择">
            <el-select
                v-model="selectedDevices"
                :clearable="true"
                filterable
                multiple
                placeholder="选择设备"
            >
              <el-option
                  v-for="device in devices"
                  :key="device"
                  :label="device"
                  :value="device"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>

      <!-- 右上：筛选条件 -->
      <el-col :span="12">
        <el-form inline>
          <el-form-item label="时间段选择">
            <el-date-picker
                v-model="dateRange"
                :clearable="true"
                end-placeholder="结束日期"
                range-separator="至"
                start-placeholder="开始日期"
                type="daterange"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="selectQuickRange('1m')">最近1分钟</el-button>
            <el-button @click="selectQuickRange('1h')">最近1小时</el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>

    <!-- 下侧数据展示 -->
    <div class="data-view">
      <GroupView :selectedGroup="selectedGroup" :selectedDevices="selectedDevices"/>
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
      groups: ["组1", "组2", "组3"], // 模拟组数据
      devices: ["设备1", "设备2", "设备3", "设备4", "设备5"], // 模拟设备数据
      selectedGroup: null, // 当前选择的组
      selectedDevices: [], // 当前选择的设备
      dateRange: null, // 时间段选择
    };
  },
  methods: {
    selectQuickRange(type) {
      const now = new Date();
      let start, end;

      if (type === "1m") {
        start = new Date(now.getTime() - 60 * 1000);
      } else if (type === "1h") {
        start = new Date(now.getTime() - 60 * 60 * 1000);
      }
      end = now;

      this.dateRange = [start, end];
    },
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
}


.control-panel {
  padding: 15px 15px 0;
  border-bottom: 1px solid #e8e8e8;
  box-sizing: border-box; /* 包括内边距和边框在内的宽高计算 */
}

.data-view {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

</style>
