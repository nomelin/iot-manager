<template>
  <div :style="{ width: width, height: height }" ref="chart"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "LineChart",
  props: {
    data: {
      type: Array,
      required: true,
      default: () => [
        { name: "Jan", value: 820 },
        { name: "Feb", value: 932 },
        { name: "Mar", value: 901 },
        { name: "Apr", value: 934 },
        { name: "May", value: 1290 },
        { name: "Jun", value: 1330 },
        { name: "Jul", value: 1320 },
      ],
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
        this.chartInstance.dispose(); // 避免重复初始化
      }
      this.chartInstance = echarts.init(this.$refs.chart);

      const option = {
        title: {
          text: "折线图示例",
          left: "center",
        },
        tooltip: {
          trigger: "axis",
        },
        xAxis: {
          type: "category",
          data: this.data.map((item) => item.name),
        },
        yAxis: {
          type: "value",
        },
        series: [
          {
            type: "line",
            data: this.data.map((item) => item.value),
          },
        ],
      };

      this.chartInstance.setOption(option);
    },
  },
  watch: {
    data: {
      handler: "initChart",
      deep: true,
    },
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
/* 可根据需要添加样式 */
</style>
