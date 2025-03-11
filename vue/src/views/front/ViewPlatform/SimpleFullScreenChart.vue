<template>
  <div class="simple-fullscreen-chart">
    <div class="chart-controls">
      <div class="title">{{ title }}</div>
      <el-switch v-model="showLegend" active-text="显示图例" @change="updateChart"/>
      <el-switch v-model="isSmooth" active-text="平滑曲线" @change="updateChart"/>
    </div>
    <div ref="chart" class="chart-container"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  props: {
    chartOption: Object,
    title: String
  },
  data() {
    return {
      chartInstance: null,
      showLegend: true,
      isSmooth: true
    }
  },
  methods: {
    initChart() {
      this.chartInstance = echarts.init(this.$refs.chart)
      this.updateChart()
    },
    updateChart() {
      const option = {
        ...this.chartOption,
        legend: this.showLegend ? this.chartOption.legend : undefined,
        series: this.chartOption.series.map(s => ({
          ...s,
          smooth: this.isSmooth
        }))
      }
      this.chartInstance.setOption(option)
    }
  },
  mounted() {
    this.initChart()
    window.addEventListener('resize', this.updateChart)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.updateChart)
    this.chartInstance.dispose()
  }
}
</script>

<style scoped>
.simple-fullscreen-chart {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-controls {
  padding: 10px;
  background: #fff;
  display: flex;
  gap: 20px;
  align-items: center;
}

.chart-container {
  flex: 1;
  min-height: 0;
}
</style>