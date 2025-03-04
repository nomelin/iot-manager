<template>
  <div class="tag-line-charts">
    <!-- 统一控制所有小图的开关 -->
    <div class="chart-controls">
      <el-switch v-model="deviceVisibility[0]" active-text="设备1" class="switch-item" @change="updateCharts"/>
      <el-switch v-model="deviceVisibility[1]" active-text="设备2" class="switch-item" @change="updateCharts"/>
      <el-switch v-model="isDualColor" active-text="双色模式" class="switch-item" @change="updateCharts"/>
      <el-switch v-model="showLegend" active-text="显示图例" class="switch-item" @change="updateCharts"/>
    </div>

    <el-row :gutter="30">
      <el-col
          v-for="(chart, index) in chartData"
          :key="chart.field"
          :span="12"
          class="chart-container"
      >
        <div class="chart-title">
          {{ chart.field }}
          <el-button @click="handleOpenFullscreen(index)">
            <i class="el-icon-full-screen"></i> 全屏显示
          </el-button>
        </div>
        <div ref="chart" :style="{ height: '400px' }" class="chart"></div>
      </el-col>
    </el-row>

    <!-- 全屏图表对话框 -->
    <el-dialog
        :center="true"
        :close-on-click-modal="false"
        :visible.sync="fullscreenVisible"
        fullscreen
    >
      <FullScreenChart
          v-if="fullscreenVisible"
          :chart-option="currentChartOption"
          :device1-name="device1Name"
          :device2-name="device2Name"
          :title="currentChartTitle"
          @close="fullscreenVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import FullScreenChart from './FullScreenChart.vue'

export default {
  components: {FullScreenChart},
  name: 'TagLineChart',
  props: {
    chartData: Array,
    device1Name: String,
    device2Name: String,
  },
  data() {
    return {
      charts: [],
      fullscreenVisible: false,
      currentChartOption: null,
      currentChartTitle: '',
      deviceVisibility: [true, true], // 控制设备1和设备2的可见性
      isDualColor: false, // 双色模式开关
      showLegend: true, // 控制是否显示图例
    }
  },
  watch: {
    chartData: {
      handler: 'renderCharts',
      deep: true
    }
  },
  methods: {
    renderCharts() {
      this.clearCharts()

      this.$nextTick(() => {
        if (!this.$refs.chart) return
        this.$refs.chart.forEach((container, index) => {
          const chart = echarts.init(container)
          const option = this.getChartOption(this.chartData[index])
          chart.setOption(option)
          this.charts.push(chart)
        })
      })
    },

    getChartOption(data) {
      return {
        title: {show: false},
        tooltip: {
          trigger: 'axis',
          axisPointer: {type: 'cross'}
        },
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
          }
        },
        // brush: {
        //   type: ['polygon', 'keep', 'clear'],
        //   xAxisIndex: 'all',  // 允许在所有 x 轴上选取
        //   yAxisIndex: 'all',  // 允许在所有 y 轴上选取
        // },
        legend: this.showLegend ? {
          data: data.series.map(s => s.name),
          orient: 'vertical',
          left: 0,
          top: 'middle',
        } : undefined,
        xAxis: [
          {type: 'value', name: '序号', position: 'bottom'},
        ],
        yAxis: [
          {type: 'value', name: '数值', position: 'left'},
        ],
        dataZoom: [
          {type: 'slider', xAxisIndex: 0, filterMode: 'none'},
          {type: 'inside', xAxisIndex: 0, filterMode: 'none'},
          {type: 'slider', yAxisIndex: 0, filterMode: 'none'},
          {type: 'inside', yAxisIndex: 0, filterMode: 'none'}
        ],
        series: data.series
            .filter(series => this.shouldShowSeries(series.name))
            .map(series => ({
              name: series.name,
              type: 'line',
              smooth: true,
              showSymbol: false,//不显示折线上的节点
              data: series.data,
              animation: false,//关闭动画
              silent: true,//图形不响应和触发鼠标事件
              large: true,//启用大规模路径图的优化
              largeThreshold: 1000,
              sampling: 'lttb',//降采样，采用 Largest-Triangle-Three-Bucket 算法，可以最大程度保证采样后线条的趋势，形状和极值。
              itemStyle: this.isDualColor ? {color: this.getDeviceColor(series.name)} : series.itemStyle,
              lineStyle: this.isDualColor ? {color: this.getDeviceColor(series.name)} : series.lineStyle
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

    shouldShowSeries(seriesName) {
      const isDevice1 = seriesName.startsWith(this.device1Name)
      return isDevice1 ? this.deviceVisibility[0] : this.deviceVisibility[1]
    },

    getDeviceColor(seriesName) {
      if (!this.isDualColor) return null
      return seriesName.startsWith(this.device1Name) ? '#1890FF' : '#FF4D4F'
    },

    clearCharts() {
      this.charts.forEach(chart => chart.dispose())
      this.charts = []
    },

    updateCharts() {
      this.renderCharts()
    },

    handleOpenFullscreen(chartIndex) {
      console.log("打开全屏图表, chartIndex: ", chartIndex)
      this.currentChartOption = this.getChartOption(this.chartData[chartIndex])
      this.currentChartTitle = this.chartData[chartIndex].field
      this.fullscreenVisible = true
    },

    handleResize() {
      this.charts.forEach(chart => chart.resize())
    },
  },

  mounted() {
    window.addEventListener('resize', this.handleResize)
  },

  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    this.clearCharts()
  }
}
</script>

<style scoped>
.chart-controls {
  display: flex;
  align-items: center;
  gap: 15px; /* 控制开关间距 */
  padding: 10px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}

.switch-item {
  margin-right: 10px; /* 单独设置开关间距 */
}

.chart-container {
  margin-bottom: 30px;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
