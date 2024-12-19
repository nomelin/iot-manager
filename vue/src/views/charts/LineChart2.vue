<template>
  <div :style="{ width: width + 'px', height: height + 'px' }">
    <div ref="chartContainer"></div>
  </div>
</template>

<script>
import echarts from 'echarts';

export default {
  name: 'LineChart',
  props: {
    // 接收外部传入的图表数据，格式可以根据实际需求调整，这里假设是包含x轴数据和y轴数据的对象数组
    chartData: {
      type: Array,
      default: () => []
    },
    width: {
      type: Number,
      default: 400
    },
    height: {
      type: Number,
      default: 300
    }
  },
  mounted() {
    this.initChart();
  },
  methods: {
    initChart() {
      const chartDom = this.$refs.chartContainer;
      const myChart = echarts.init(chartDom);
      const option = {
        xAxis: {
          type: 'category',
          data: this.chartData.map(item => item.xAxisData) // 根据实际数据结构调整提取x轴数据的方式
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            type: 'line',
            data: this.chartData.map(item => item.yAxisData) // 根据实际数据结构调整提取y轴数据的方式
          }
        ]
      };
      myChart.setOption(option);
    }
  }
};
</script>

<style scoped>

</style>