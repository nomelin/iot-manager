<template>
  <div class="fullscreen-chart">
    <!-- 控制栏 -->
    <div class="chart-controls">
      <el-button-group>
        <el-button @click="toggleDevice(0)">
          {{ device1Name }} {{ deviceVisibility[0] ? '显示' : '隐藏' }}
        </el-button>
        <el-button @click="toggleDevice(1)">
          {{ device2Name }} {{ deviceVisibility[1] ? '显示' : '隐藏' }}
        </el-button>
        <el-button @click="toggleColorMode">
          双色模式（{{ isDualColor ? '开' : '关' }}）
        </el-button>
      </el-button-group>
    </div>

    <!-- 图表容器 -->
    <div ref="chart" class="chart-class"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";

const COLOR_SCHEME = [
  '#5470C6', '#91CC75', '#FAC858', '#EE6666', // 原色系
  '#73C0DE', '#3BA272', '#FC8452', '#9A60B4'
]
const DUAL_COLORS = ['#1890FF', '#FF4D4F'] // 双色模式颜色

export default {
  props: {
    chartOption: Object,
    device1Name: String,
    device2Name: String
  },
  data() {
    return {
      chartInstance: null,
      isDualColor: false,
      deviceVisibility: [true, true]
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
      const option = JSON.parse(JSON.stringify(this.chartOption))

      // // 处理可见性
      // option.series = option.series.map(series => ({
      //   ...series,
      //   show: this.shouldShowSeries(series.name)
      // }))

      // 处理颜色
      if (this.isDualColor) {
        option.series = option.series.map(series => ({
          ...series,
          itemStyle: { color: this.getDeviceColor(series.name) },
          lineStyle: {
            ...series.lineStyle,
            color: this.getDeviceColor(series.name),
            type: 'solid'
          }
        }))
      }else {
        option.series = option.series.map(series => ({
          ...series,
          itemStyle: { color: COLOR_SCHEME[series.name] },
          lineStyle: {
            ...series.lineStyle,
            color: COLOR_SCHEME[series.name]
          }
        }))
      }

      // 同步图例选中状态
      const legendSelected = {};
      option.series.forEach(s => {
        const isDevice1 = s.name.startsWith(this.device1Name);
        legendSelected[s.name] = isDevice1 ?
            this.deviceVisibility[0] :
            this.deviceVisibility[1];
      });


      // 布局配置&处理选择系列显示
      option.legend = {
        orient: 'vertical',
        left: 20,
        top: 'middle',
        itemGap: 10,
        textStyle: {lineHeight: 20},
        formatter: name => name.length > 20 ? name.slice(0, 20) + '...' : name,
        selected: legendSelected
      }

      option.grid = {
        left: 200,
        right: 50,
        bottom: 100,
        top: 40,
        containLabel: true
      }

      // option.dataZoom = [
      //   {
      //     type: 'slider',
      //     xAxisIndex: 0,
      //     bottom: 20,
      //     height: 30
      //   },
      //   {
      //     type: 'inside',
      //     xAxisIndex: 0
      //   }
      // ]

      // 强制替换series配置
      this.chartInstance.setOption(option, {
        replaceMerge: ['series'],
        notMerge: false
      })
    },

    getDeviceColor(seriesName) {
      if (!this.isDualColor) return null
      return seriesName.startsWith(this.device1Name)
          ? DUAL_COLORS[0]
          : DUAL_COLORS[1]
    },

    shouldShowSeries(seriesName) {
      const isDevice1 = seriesName.startsWith(this.device1Name)
      const res = isDevice1 ? this.deviceVisibility[0] : this.deviceVisibility[1]
      console.log(seriesName + "是否可见：" + res)
      return res
    },

    toggleDevice(deviceIndex) {
      this.$set(this.deviceVisibility, deviceIndex, !this.deviceVisibility[deviceIndex]);
      this.updateChart()
    },

    toggleColorMode() {
      this.isDualColor = !this.isDualColor
      this.updateChart()
      // console.log("双色模式：", this.isDualColor)
    },

    handleResize() {
      this.chartInstance?.resize()
    }
  },
  mounted() {
    this.initChart()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.chartInstance?.dispose()
  }
}
</script>

<style scoped>
.fullscreen-chart {
  height: 80vh;
  display: flex;
  flex-direction: column;
}

.chart-controls {
  padding: 15px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}

.chart-class {
  flex: 1; /* 关键样式：占据剩余空间 */
  width: 100%;
}
</style>