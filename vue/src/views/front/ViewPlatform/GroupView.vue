<template>
  <div class="group-view">
    <el-row :gutter="30">
      <el-col
          :span="12"
          v-for="(fieldData, fieldName) in processedData"
          :key="fieldName"
          class="chart-container"
      >
        <LineChart :data="fieldData" :title="fieldName" width="100%" height="400px" />
      </el-col>
    </el-row>
  </div>
</template>


<script>
import LineChart from "@/views/charts/LineChart.vue";

export default {
  name: "GroupView",
  components: { LineChart },
  props: {
    selectedDevice: {
      type: Array,
      required: true,
    },
    selectedGroup: {
      type: String,
      required: true,
    }
  },
  data() {
    return {
      processedData: {}, // 存储整理后的字段数据
    };
  },
  methods: {
    async fetchData() {
      try {
        const promises = [1, 2, 3].map((deviceId) =>
            this.$request.get(`/data/test/${deviceId}/5`)
        );
        const responses = await Promise.all(promises);
        const rawData = responses.map((res) => res.data);

        this.processedData = this.organizeData(rawData);
      } catch (error) {
        this.$message.error("数据加载失败：" + error.message);
      }
    },
    organizeData(rawData) {
      const fieldsMap = {};
      console.log("rawData: "+JSON.stringify(rawData));
      rawData.forEach((deviceData) => {
        const { devicePath, records } = deviceData;
        const deviceName = devicePath.split(".").pop();

        Object.entries(records).forEach(([timestamp, { fields }]) => {
          const formattedTime = this.$options.filters.formatTime(timestamp);

          Object.entries(fields).forEach(([fieldName, value]) => {
            if (!fieldsMap[fieldName]) {
              fieldsMap[fieldName] = [];
            }

            let fieldData = fieldsMap[fieldName].find(
                (item) => item.deviceName === deviceName
            );
            if (!fieldData) {
              fieldData = { deviceName, timestamps: [], values: [] };
              fieldsMap[fieldName].push(fieldData);
            }

            fieldData.timestamps.push(formattedTime);
            fieldData.values.push(value);
          });
        });
      });
      console.log("fieldsMap: "+JSON.stringify(fieldsMap));
      return fieldsMap;
    },
  },
  created() {
    this.fetchData();
  },
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
