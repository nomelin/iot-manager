<template>
  <div class="group-view">
    <el-row :gutter="30">
      <el-col
          v-for="(fieldData, fieldName) in processedData"
          :key="fieldName"
          :span="12"
          class="chart-container"
      >
        <LineChart :data="fieldData" :title="fieldName" height="400px" width="100%"/>
      </el-col>
    </el-row>
  </div>
</template>


<script>
import LineChart from "@/views/front/ViewPlatform/charts/LineChart.vue";

export default {
  name: "GroupView",
  components: {LineChart},
  props: {
    selectedDeviceIds: {
      type: Array,
      required: true,
      description: "选中的设备列表",//选中设备变化时不重新加载数据
    },
    devices: {
      type: Array,
      required: true,
      description: "当前组全部设备列表",//设备列表变化时重新加载数据
    },
    dateRange: {
      type: Array,
      required: true,
      description: "时间范围选择",//时间范围变化时重新加载数据
    }
  },
  data() {
    return {
      processedData: {}, // 存储整理后的字段数据
    };
  },
  methods: {
    async fetchData() {
      if (!this.selectedDeviceIds || !this.selectedDeviceIds.length || !this.dateRange) return
      try {
        const queryStartTime = Date.now();
        const promises = this.selectedDeviceIds.map((deviceId) => {
          console.log("device: " + JSON.stringify(deviceId));
          const requestBody = {
            deviceId: deviceId,
            startTime: this.dateRange[0],
            endTime: this.dateRange[1],
          }
          console.log("requestBody: ", JSON.stringify(requestBody));
          return this.$request.post(`/data/query`, requestBody);
        });
        const responses = await Promise.all(promises); // 获取所有设备的查询结果
        const queryEndTime = Date.now();
        this.$notify({
          title: "从服务器获取数据成功",
          message: "耗时：" + (queryEndTime - queryStartTime) + "ms",
          type: "success",
          position: "bottom-right",
          duration: 2000
        })
        // console.log("resp", JSON.stringify(responses));
        responses.forEach((res) => {
          if (res.code !== "200") {
            this.$message.error("数据加载失败：" + res.msg);
          }
        });
        const rawData = responses.map((res) => res.data);

        this.processedData = this.organizeData(rawData);
        const processEndTime = Date.now();
        this.$notify({
          title: "数据转换成功",
          message: "耗时：" + (processEndTime - queryEndTime) + "ms",
          type: "success",
          position: "bottom-right",
          duration: 2500,
          offset: 80
        })
      } catch (error) {
        console.log("数据加载失败：" + error.message);
      }
    },
    organizeData(rawData) {
      const fieldsMap = {};
      // console.log("rawData: " + JSON.stringify(rawData));
      rawData.forEach((deviceData) => {
        const { devicePath, records } = deviceData;
        const deviceName = devicePath.split(".").pop();

        // 处理每个时间戳，只取第一个Record
        Object.entries(records).forEach(([timestamp, recordList]) => {
          if (!recordList.length) return;
          const record = recordList[0]; // 只取第一个Record
          const formattedTime = this.$options.filters.formatTime(timestamp);

          Object.entries(record.fields).forEach(([fieldName, value]) => {
            if (!fieldsMap[fieldName]) fieldsMap[fieldName] = [];

            // 查找或创建当前设备的字段数据
            let fieldData = fieldsMap[fieldName].find(
                (item) => item.deviceName === deviceName
            );
            if (!fieldData) {
              fieldData = { deviceName, data: [] };
              fieldsMap[fieldName].push(fieldData);
            }

            // 添加时间和值到数据数组
            fieldData.data.push([formattedTime, value]);
          });
        });
      });

      // 对每个字段的每个设备数据进行时间排序
      Object.keys(fieldsMap).forEach((fieldName) => {
        fieldsMap[fieldName].forEach((fieldData) => {
          // 按时间戳排序
          fieldData.data.sort((a, b) => {
            return new Date(a[0]) - new Date(b[0]);
          });
        });
      });
      // console.log("fieldsMap: " + JSON.stringify(fieldsMap));
      return fieldsMap;
    }
  },
  created() {
    this.fetchData();
  },
  watch: {
    selectedDeviceIds() {
      this.fetchData();//TODO 选择设备变化时不需要重新加载数据
    },
    devices() {
      this.fetchData();
    },
    dateRange() {
      this.fetchData();
    }
  }
};
</script>

<style scoped>
.group-view {
  height: 100%; /* 填满父容器 */
  overflow-y: auto; /* 启用垂直滚动条 */
  padding: 20px; /* 添加内边距避免元素紧贴容器边缘 */
  box-sizing: border-box; /* 包括内边距和边框在内的宽高计算 */
}

.chart-container {
  margin-bottom: 30px; /* 每个图表下方添加间距 */
}

.chart-container:last-child {
  margin-bottom: 0; /* 最后一个图表去掉间距 */
}
</style>
