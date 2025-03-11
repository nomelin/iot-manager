<template>
  <div class="simple-fullscreen-chart">
    <div class="chart-controls">
      <div class="chart-title">{{ title }}</div>
      <el-switch v-model="showLegend" active-text="显示图例" class="switch-item" @change="updateChart"/>
      <el-switch v-model="isSmooth" active-text="平滑曲线" class="switch-item" @change="updateChart"/>
  </div>
    <!-- 图表容器 -->
    <div ref="chart" class="chart-class"></div>
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
  watch: {
    chartOption: {
      handler() {
        this.updateChart()
      },
      deep: true
    }
  },
  methods: {
    initChart() {
      this.chartInstance = echarts.init(this.$refs.chart)
      this.updateChart()
      window.addEventListener('resize', this.handleResize)
    },
    updateChart() {
      console.log('全屏模式updateChart')
      const option = JSON.parse(JSON.stringify(this.chartOption))

      option.series = option.series.map(series => ({
        ...series,
        smooth: this.isSmooth,
      }))
      option.legend = {
        show: this.showLegend,
        orient: 'vertical',
        left: 20,
        top: 'middle',
        itemGap: 10,
        textStyle: {lineHeight: 20},
        formatter: name => name.length > 20 ? name.slice(0, 20) + '...' : name,
      }
      option.grid = {
        left: 200,
        right: 50,
        bottom: 100,
        top: 40,
        containLabel: true
      }

      // 强制替换series配置
      this.chartInstance.setOption(option, {
        replaceMerge: ['series'],
        notMerge: false
      })
    },
    handleResize() {
      this.chartInstance?.resize()
    }
  },
  mounted() {
    this.initChart()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.updateChart)
    this.chartInstance.dispose()
  }
}
</script>

<style scoped>
.simple-fullscreen-chart {
  height: 90vh;
  display: flex;
  flex-direction: column;
}


.chart-controls {
  padding: 5px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  flex-wrap: wrap;
}

.chart-title {
  font-weight: bold;
  margin-right: 5%;
}

.switch-item {
  margin-right: 2%;
}

.chart-class {
  flex: 1;
  width: 100%;
}
</style>