<template>
  <div class="tag-line-charts">
    <el-row :gutter="30">
      <el-col
          v-for="(chart, index) in chartData"
          :key="chart.field"
          :span="12"
          class="chart-container"
      >
        <div
            class="chart-title"
        >
          {{ chart.field }}
          <el-button
              @click="handleOpenFullscreen(index)"
          ><i class="el-icon-full-screen"></i>全屏显示
          </el-button>

        </div>
        <div ref="chart" :style="{ height: '400px' }" class="chart"></div>
      </el-col>
    </el-row>

    <!-- 全屏图表对话框 -->
    <el-dialog
        :close-on-click-modal="false"
        :title="currentChartTitle"
        :visible.sync="fullscreenVisible"
        fullscreen
    >
      <FullScreenChart
          v-if="fullscreenVisible"
          :chart-option="currentChartOption"
          :device1-name="device1Name"
          :device2-name="device2Name"
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
      currentChartTitle: ''
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
          axisPointer: {
            type: 'cross'
          }
        },
        toolbox: {
          show: true,
          feature: {
            saveAsImage: {
              title: '保存为图片'
            },
            dataZoom: {
              show: true,
              yAxisIndex: 0,
              xAxisIndex: 0,
            }
          }
        },
        legend: {
          data: data.series.map(s => s.name),
          bottom: 0
        },
        xAxis: {
          type: 'value',
          name: '序号'
        },
        yAxis: {
          type: 'value',
          name: '数值'
        },
        dataZoom: [
          {
            type: 'slider',
            xAxisIndex: 0,
            filterMode: 'none'
          },
          {
            type: 'inside',
            xAxisIndex: 0,
            filterMode: 'none'
          },
          {
            type: 'slider',
            yAxisIndex: 0,
            filterMode: 'none'
          },
          {
            type: 'inside',
            yAxisIndex: 0,
            filterMode: 'none'
          },
        ],
        series: data.series.map(series => ({
          name: series.name,
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: series.data,
          animation: false,
          lineStyle: series.lineStyle, // 应用不同的线型

        })),
        grid: {
          top: 40,
          bottom: 60,
          containLabel: true
        }
      }
    },

    clearCharts() {
      this.charts.forEach(chart => chart.dispose())
      this.charts = []
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