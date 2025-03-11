<template>
  <div class="group-view">
    <!-- 统一控制所有图表的开关 -->
    <div class="chart-controls">
      <el-switch v-model="showLegend" active-text="显示图例" @change="updateCharts" class="switch-item" />
      <el-switch v-model="isSmooth" active-text="平滑曲线" @change="updateCharts" class="switch-item" />
      <el-switch v-model="isSampling" active-text="降采样" @change="updateCharts" class="switch-item"/>
      <el-switch v-model="isAnimation" active-text="开启动画" @change="updateCharts" class="switch-item" />
       <el-switch v-model="isShowSymbol" active-text="显示数据点" @change="updateCharts" class="switch-item" />
      <el-switch v-model="isLarge" active-text="大数据量" @change="updateCharts" class="switch-item" />
    </div>

    <el-row :gutter="30">
      <el-col
          v-for="(fieldData, fieldName) in processedData"
          :key="fieldName"
          :span="12"
          class="chart-container"
      >
        <div class="chart-title">
          {{ fieldName }}
          <el-button @click="handleOpenFullscreen(fieldName)">
            <i class="el-icon-full-screen"></i> 全屏显示
          </el-button>
        </div>
        <div :ref="`chart-${fieldName}`" :style="{ height: '400px' }" class="chart"></div>
      </el-col>
    </el-row>

    <!-- 全屏图表对话框 -->
    <el-dialog
        :center="true"
        :close-on-click-modal="false"
        :visible.sync="fullscreenVisible"
        fullscreen
    >
      <SimpleFullScreenChart
          v-if="fullscreenVisible"
          :chart-option="currentChartOption"
          :title="currentChartTitle"
          @close="fullscreenVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import SimpleFullScreenChart from './SimpleFullScreenChart.vue'

export default {
  components: {SimpleFullScreenChart},
  name: "GroupView",
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
      charts: {},
      fullscreenVisible: false,
      currentChartOption: null,
      currentChartTitle: '',
      showLegend: true,
      isSmooth: true,
      isSampling: true,
      isAnimation: false,
      isShowSymbol: false,
      isLarge: false,
    };
  },
  computed: {
    allDeviceIds() {
      // console.log("devices: " + JSON.stringify(this.devices))
      return this.devices.map((device) => device.id);
    },
    selectedProcessedData() {
      const res = Object.keys(this.processedData).reduce((result, fieldName) => {
        result[fieldName] = this.selectedDeviceIds
            .map(deviceId => this.processedData[fieldName]?.[deviceId])
            .filter(Boolean);
        return result;
      }, {});
      // console.log("processedData",JSON.stringify(this.processedData))
      // console.log("selectedDeviceIds: " + JSON.stringify(this.selectedDeviceIds))
      // console.log("selectedProcessedData: " + JSON.stringify(res))
      return res
    }
  },
  methods: {
    async fetchData() {
      if (!this.dateRange) return
      // console.log("allDeviceIds", JSON.stringify(this.allDeviceIds))
      if (this.allDeviceIds.length === 0) return
      try {
        const queryStartTime = Date.now();
        const promises = this.allDeviceIds.map((deviceId) => {
          if (!deviceId) {
            console.warn("deviceId is null")
            return null
          }
          // console.log("device: " + JSON.stringify(deviceId));
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
          title: `数据获取成功`,
          message: "耗时：" + (queryEndTime - queryStartTime) + "ms",
          type: "success",
          position: "bottom-right",
          duration: 2000
        })
        // console.log("resp", JSON.stringify(responses));
        let totalLength = 0;
        responses.forEach((res) => {
          if (res.code !== "200") {
            this.$message.error("数据加载失败：" + res.msg);
            return;
          }
          const records = res.data.records;
          totalLength += Object.values(records).reduce((sum, arr) => sum + arr.length, 0);
        });
        const rawData = responses.map((res) => res.data);
        if (totalLength === 0) {
          this.$message.warning("所选时间范围内无任何数据");
          return;
        }

        this.processedData = this.organizeData(rawData);
        const processEndTime = Date.now();
        this.$notify({
          title: `数据转换成功，共 ${totalLength} 条数据`,
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
      // console.log("rawData: " + JSON.stringify(rawData))

      rawData.forEach((deviceData) => {
        const {devicePath, records} = deviceData;
        const deviceId = this.extractDeviceIdFromDevicePath(devicePath);
        console.log("deviceId" + deviceId);
        // 查找匹配的设备对象
        const device = this.devices.find(dev => dev.id === deviceId);
        const deviceName = device ? device.name : `Unknown(${deviceId})`;

        // 处理每个时间戳，只取第一个Record
        Object.entries(records).forEach(([timestamp, recordList]) => {
          if (!recordList.length) return;
          const record = recordList[0]; // 只取第一个Record
          const formattedTime = this.$options.filters.formatTime(timestamp);

          Object.entries(record.fields).forEach(([fieldName, value]) => {
            if(fieldName === 'tag'){
              return;//过滤掉名称为 "tag" 的数据
            }
            if (!fieldsMap[fieldName]) fieldsMap[fieldName] = {};

            // 设备数据按 deviceId 索引
            if (!fieldsMap[fieldName][deviceId]) {
              fieldsMap[fieldName][deviceId] = {deviceName, data: []};
            }

            // 添加时间和值
            fieldsMap[fieldName][deviceId].data.push([formattedTime, value]);
          });
        });
      });

      // 确保数据按时间排序
      Object.keys(fieldsMap).forEach((fieldName) => {
        Object.values(fieldsMap[fieldName]).forEach((fieldData) => {
          fieldData.data.sort((a, b) => new Date(a[0]) - new Date(b[0]));
        });
      });
      // console.log("fieldsMap: " + JSON.stringify(fieldsMap));
      return fieldsMap;
    },

    async updateCharts() {
      await this.$nextTick()
      this.renderCharts()
    },

    extractDeviceIdFromDevicePath(devicePath) {
      //耦合性很高
      return Number(devicePath.split('_').pop())
    },

    renderCharts() {
      Object.keys(this.selectedProcessedData).forEach(fieldName => {
        if (fieldName === 'tag') return; // 过滤掉名称为 "tag" 的图表
        const container = this.$refs[`chart-${fieldName}`][0]
        if (echarts.getInstanceByDom(container)) {
          echarts.dispose(container)
        }
        const chart = echarts.init(container)
        this.charts[fieldName] = chart
        chart.setOption(this.getChartOption(fieldName))
      })
    },

    getChartOption(fieldName) {
      const fieldData = this.selectedProcessedData[fieldName]
      return {
        title: {show: false},
        tooltip: {trigger: 'axis', axisPointer: {type: 'cross'}},
        toolbox: {
          show: true,
          feature: {
            saveAsImage: {title: '保存为PNG'},
            dataZoom: {show: true, yAxisIndex: 0, xAxisIndex: 0},
            dataView: {show: true, readOnly: false},
            magicType: {
              show: true,
              type: ['line', 'bar']
            },
            restore: {show: true},
          },
        },
        legend: this.showLegend ? {
          data: fieldData.map(d => d.deviceName),
          orient: 'vertical',
          left: 0,
          top: 'middle',
        } : undefined,
        xAxis: {type: 'time', name: '时间', position: 'bottom'},
        yAxis: {type: 'value', name: fieldName, position: 'left'},
        dataZoom: [
          {type: 'slider', xAxisIndex: 0, filterMode: 'none'},
          {type: 'inside', xAxisIndex: 0, filterMode: 'none'},
          {type: 'slider', yAxisIndex: 0, filterMode: 'none'},
          {type: 'inside', yAxisIndex: 0, filterMode: 'none'}
        ],
        series: fieldData.map(deviceData => ({
          name: deviceData.deviceName,
          type: 'line',
          data: deviceData.data,
          smooth: this.isSmooth,
          showSymbol: this.isShowSymbol,
          animation: this.isAnimation,
          sampling: this.isSampling ? 'lttb' : null,
          large: this.isLarge,//启用大规模路径图的优化
          largeThreshold: 1000,
        })),
        grid: {
          show: true,
          top: 40,
          bottom: 60,
          left: 80,
          containLabel: true
        },
      }
    },

    handleOpenFullscreen(fieldName) {
      console.log("打开全屏图表, fieldName: ", fieldName)
      this.currentChartOption = this.getChartOption(fieldName)
      this.currentChartTitle = fieldName
      this.fullscreenVisible = true
    },

    handleResize() {
      Object.values(this.charts).forEach(chart => chart.resize())
    }
  },
  created() {
    this.fetchData();
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(chart => chart.dispose())
  },

  watch: {
    selectedDeviceIds() {
      this.updateCharts();//不重新获取数据，只更新图表
    },
    devices() {
      this.fetchData();
    },
    dateRange() {
      this.fetchData();
    },
  }
}
</script>

<style scoped>
.group-view {
  height: 100%; /* 填满父容器 */
  overflow-y: auto; /* 启用垂直滚动条 */
  overflow-x: hidden; /* 隐藏水平滚动条 */
  /* 添加内边距避免元素紧贴容器边缘 */
  padding: 0 10px 10px;
  box-sizing: border-box; /* 包括内边距和边框在内的宽高计算 */
}
.chart-controls {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}
.switch-item {
  margin-right: 10px;
}

.chart-container {
  margin-bottom: 30px; /* 每个图表下方添加间距 */
}

.chart-container:last-child {
  margin-bottom: 0; /* 最后一个图表去掉间距 */
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.chart {
  background: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>