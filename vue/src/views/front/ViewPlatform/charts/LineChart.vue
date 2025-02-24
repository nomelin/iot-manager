<template>
  <div class="chart-container">
    <div class="setting">
      <el-row>
        <el-col :span="6" :offset="18">
          <el-button>配置</el-button>
        </el-col>
      </el-row>
    </div>
    <div ref="chart" :style="{ width: width, height: height }" class="chart"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "LineChart",
  props: {
    data: {
      type: Array,
      required: true,
      default: () => [],
    },
    title: {
      type: String,
      default: "",
    },
    width: {
      type: String,
      default: "100%",
    },
    height: {
      type: String,
      default: "400px",
    },
  },
  data() {
    return {
      chartInstance: null,
    };
  },
  methods: {
    initChart() {
      if (this.chartInstance) {
        this.chartInstance.dispose();
      }
      this.chartInstance = echarts.init(this.$refs.chart);

      const option = {
        title: {
          text: this.title,
          left: "center",
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: 'cross'
          }
        },
        legend: {
          top: "10%",
          data: this.data.map(item => item.deviceName) // 确保legend显示设备名
        },
        xAxis: {
          type: "time", // 使用时间轴
          axisLabel: {
            formatter: (value) => {
              return echarts.format.formatTime('yyyy-MM-dd HH:mm', value);
            }
          }
        },
        yAxis: {
          type: "value",
        },
        dataZoom: [
          {
            type: "slider",
            xAxisIndex: 0,
            filterMode: "none",
          },
          {
            type: "slider",
            yAxisIndex: 0,
            filterMode: "none",
          },
        ],
        series: this.data.map((line) => ({
          name: line.deviceName,
          type: "line",
          data: line.data, // 直接使用排序后的数据
          smooth: true,
          showSymbol: false // 数据点较多时可关闭折线图的折线点显示。鼠标悬浮时仍然显示折线点
        })),
      };

      this.chartInstance.setOption(option);
    },
  },
  watch: {
    data: {
      handler: "initChart",
      deep: true,
    },
    title: "initChart",
    width: "initChart",
    height: "initChart",
  },
  mounted() {
    this.initChart();
    window.addEventListener("resize", this.chartInstance.resize);
  },
  beforeDestroy() {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
    window.removeEventListener("resize", this.chartInstance.resize);
  },
};
</script>

<style scoped>
/* 可自定义样式 */
.chart-container {
  background-color: #ffffff;
  border-radius: 8px;
}
</style>
