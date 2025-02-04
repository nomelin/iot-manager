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
    selectedDevices: {
      type: Array,
      required: true,
      description: "选中的设备列表",//选中设备变化时不重新加载数据
    },
    devices: {
      type: Array,
      required: true,
      description: "当前组全部设备列表",//设备列表变化时重新加载数据
    },
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
            this.$request.get(`/data/test/${deviceId}/3`)
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
      console.log("rawData: " + JSON.stringify(rawData));
      rawData.forEach((deviceData) => {
        const { devicePath, records } = deviceData;
        const deviceName = devicePath.split(".").pop();

        // 遍历每个时间戳及其对应的Record列表
        Object.entries(records).forEach(([timestamp, recordList]) => {
          // 遍历每个时间戳下的所有Record对象
          recordList.forEach(record => {
            const { fields } = record;
            const formattedTime = this.$options.filters.formatTime(timestamp);

            // 处理每个字段
            Object.entries(fields).forEach(([fieldName, value]) => {
              if (!fieldsMap[fieldName]) {
                fieldsMap[fieldName] = [];
              }

              // 查找或创建当前设备的字段数据
              let fieldData = fieldsMap[fieldName].find(
                  (item) => item.deviceName === deviceName
              );
              if (!fieldData) {
                fieldData = { deviceName, timestamps: [], values: [] };
                fieldsMap[fieldName].push(fieldData);
              }

              // 添加时间戳和值
              fieldData.timestamps.push(formattedTime);
              fieldData.values.push(value);
            });
          });
        });
      });
      console.log(fieldsMap);
      return fieldsMap;
    }
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
