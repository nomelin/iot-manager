<template>
  <div class="fullscreen-chart">
    <!-- 控制栏 -->
    <div class="chart-controls">
      <div class="chart-title">
        {{ title }}
      </div>
      <el-dropdown class="dropdown"
                   :hide-on-click=false
      >
        <span class="el-dropdown-link">
            设备选择<i class="el-icon-arrow-down el-icon--right"></i>
          </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item v-for="device in deviceNames" :key="device">
            <el-switch
                v-model="deviceVisibility[device]"
                :active-text="device"
                @change="updateChart"
            />
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>

      <el-switch v-model="isSolidColor" active-text="纯色模式" class="switch-item" @change="updateChart"/>
      <el-switch v-model="showLegend" active-text="显示图例" class="switch-item" @change="updateChart"/>
      <el-switch v-model="isSmooth" active-text="平滑曲线" class="switch-item" @change="updateChart"/>
    </div>
    <!-- 图表容器 -->
    <div ref="chart" class="chart-class"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";

const COLOR_SCHEME = [
  '#5470C6', '#91CC75', '#FAC858', '#EE6666',
  '#73C0DE', '#3BA272', '#FC8452', '#9A60B4'
]

export default {
  props: {
    chartOption: Object,
    title: String,
  },
  data() {
    return {
      chartInstance: null,
      isSolidColor: false,
      isSmooth: true,
      deviceVisibility: {},
      showLegend: true,
      deviceNames: []
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

      // 提取设备名称并初始化可见性状态
      const devices = new Set()
      option.series.forEach(s => {
        const device = this.extractDeviceName(s.name)
        devices.add(device)
      })
      this.deviceNames = Array.from(devices)
      this.deviceNames.forEach(device => {
        if (!Object.prototype.hasOwnProperty.call(this.deviceVisibility, device)) {
          this.$set(this.deviceVisibility, device, true)
        }
      })

      // 处理系列可见性
      option.series = option.series.map(series => ({
        ...series,
        show: this.shouldShowSeries(series.name)
      }))

      // 处理颜色配置
      if (this.isSolidColor) {
        const deviceColors = {}
        this.deviceNames.forEach((device, index) => {
          deviceColors[device] = COLOR_SCHEME[index % COLOR_SCHEME.length]
        })

        option.series = option.series.map(series => {
          const device = this.extractDeviceName(series.name)
          return {
            ...series,
            itemStyle: {color: deviceColors[device]},
            lineStyle: {
              ...series.lineStyle,
              color: deviceColors[device],
              type: 'solid'
            },
            smooth: this.isSmooth,
          }
        })
      } else {
        option.series = option.series.map((series, index) => ({
          ...series,
          itemStyle: {color: COLOR_SCHEME[index % COLOR_SCHEME.length]},
          lineStyle: {
            ...series.lineStyle,
            color: COLOR_SCHEME[index % COLOR_SCHEME.length]
          },
          smooth: this.isSmooth,
        }))
      }

      // 配置图例
      const legendSelected = {}
      option.series.forEach(s => {
        const device = this.extractDeviceName(s.name)
        legendSelected[s.name] = this.deviceVisibility[device]
      })

      option.legend = {
        show: this.showLegend,
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

    extractDeviceName(seriesName) {
      return seriesName.split('-')[0]
    },

    shouldShowSeries(seriesName) {
      const device = this.extractDeviceName(seriesName)
      return this.deviceVisibility[device] ?? true
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
.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
}
.dropdown{
  font-size: 1.1rem;
  margin-right: 2%;
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