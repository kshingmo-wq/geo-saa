<template>
  <div class="trend-chart">
    <!-- 空数据时不初始化 echarts：空白画布 + 轴线比 el-empty 更让人困惑 -->
    <div v-if="!hasData" class="chart-empty">
      <el-empty :description="emptyText" :image-size="60" />
    </div>
    <div v-else ref="chartRef" class="chart-container" :style="{ height: height }"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
// 按需引入：全量 import * as echarts 会打进约 1MB 产物，
// 这里只注册实际用到的图表类型与组件。
import * as echarts from 'echarts/core'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
  RadarComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  RadarChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  RadarComponent,
  CanvasRenderer
])

const props = defineProps({
  chartData: { type: Object, default: () => ({}) },
  type: { type: String, default: 'line' },
  height: { type: String, default: '300px' },
  emptyText: { type: String, default: '暂无数据' },
  /** 单位后缀，显示在 y 轴名称上，如「次」「%」 */
  unit: { type: String, default: '' }
})

const chartRef = ref(null)
let chartInstance = null

// 统一色板：与 Element Plus 主色 #409eff 同族，明暗背景下都可读。
// 之前各 series 用 echarts 默认色板，与页面其它 Element 组件的配色不统一。
const PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9254de']

const AXIS_LABEL_COLOR = '#909399'
const AXIS_LINE_COLOR = '#e4e7ed'
const SPLIT_LINE_COLOR = '#f0f2f5'
const TEXT_MAIN = '#303133'

/** 数据是否可用于绘图。series 全空或只有一项单点时画出来是空坐标系。 */
const hasData = computed(() => {
  const data = props.chartData || {}
  if (props.type === 'radar') {
    return (data.indicator || []).length > 0
  }
  return (data.series || []).some((s) => (s.data || []).length > 0)
})

function buildTooltip() {
  return {
    trigger: 'axis',
    // 指示线让多序列对比时更容易定位到具体 x 轴位置
    axisPointer: {
      type: 'line',
      lineStyle: { color: '#c0c4cc', type: 'dashed', width: 1 }
    },
    backgroundColor: 'rgba(255, 255, 255, 0.96)',
    borderColor: AXIS_LINE_COLOR,
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: TEXT_MAIN, fontSize: 12 },
    extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,0.10); border-radius: 8px;'
  }
}

function getOption() {
  // 修复：原实现写的是 `const { data = {} } = props`，但组件根本没有名为 data 的 prop，
  // 解构结果恒为{}，导致 categories / series 永远为空——图表从来没渲染出过真实数据。
  const data = props.chartData || {}
  const isRadar = props.type === 'radar'
  const baseOption = {
    tooltip: buildTooltip(),
    color: PALETTE,
    textStyle: { fontFamily: 'inherit' },
    animation: true,
    animationDuration: 600,
    animationEasing: 'cubicOut'
  }

  if (isRadar) {
    return {
      ...baseOption,
      legend: {
        type: 'scroll',
        bottom: 0,
        left: 'center',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 18,
        textStyle: { color: AXIS_LABEL_COLOR, fontSize: 12 }
      },
      radar: {
        indicator: data.indicator || [],
        shape: 'polygon',
        splitNumber: 5,
        axisName: { color: AXIS_LABEL_COLOR, fontSize: 12 },
        axisLine: { lineStyle: { color: AXIS_LINE_COLOR } },
        splitLine: { lineStyle: { color: SPLIT_LINE_COLOR } },
        splitArea: {
          areaStyle: { color: ['rgba(64,158,255,0.03)', 'rgba(64,158,255,0.06)'] }
        }
      },
      series: (data.series || []).map((s) => ({
        type: 'radar',
        name: s.name || '',
        data: (s.data || []).map((v) => v),
        areaStyle: { opacity: 0.12 },
        lineStyle: { width: 2 },
        itemStyle: { borderWidth: 2 },
        symbolSize: 5
      }))
    }
  }

  const isBar = props.type === 'bar'
  return {
    ...baseOption,
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      icon: isBar ? 'roundRect' : 'roundRect',
      itemWidth: 12,
      itemHeight: 8,
      itemGap: 18,
      textStyle: { color: AXIS_LABEL_COLOR, fontSize: 12 }
    },
    grid: {
      left: 8,
      right: 16,
      top: 24,
      // 底部留足空间给 legend，避免系列名与 x 轴标签重叠
      bottom: isBar || !hasLegend(data) ? 8 : 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.categories || [],
      boundaryGap: isBar,
      axisLabel: {
        color: AXIS_LABEL_COLOR,
        fontSize: 12,
        // 类目过多时倾斜而非相互压叠，保证末位日期始终可见
        interval: 'auto',
        hideOverlap: true,
        rotate: (data.categories || []).length > 8 ? 30 : 0
      },
      axisLine: { lineStyle: { color: AXIS_LINE_COLOR } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: props.unit || '',
      nameTextStyle: {
        color: AXIS_LABEL_COLOR,
        fontSize: 12,
        align: 'right',
        padding: [0, 6, 0, 0]
      },
      axisLabel: { color: AXIS_LABEL_COLOR, fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: SPLIT_LINE_COLOR, type: 'dashed' } }
    },
    series: (data.series || []).map((s, i) => ({
      type: isBar ? 'bar' : 'line',
      data: s.data || [],
      name: s.name || '',
      smooth: !isBar,
      // 数据点少时显示 symbol，多时隐藏避免糊成一片
      symbol: 'circle',
      symbolSize: (s.data || []).length <= 14 ? 6 : 0,
      showSymbol: (s.data || []).length <= 14,
      lineStyle: isBar ? undefined : { width: 2.5 },
      // 渐变面积：比纯色 opacity 0.1 更有层次，且不遮挡网格线
      areaStyle: isBar
        ? undefined
        : {
            opacity: 1,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: hexToRgba(PALETTE[i % PALETTE.length], 0.26) },
              { offset: 1, color: hexToRgba(PALETTE[i % PALETTE.length], 0.02) }
            ])
          },
      barMaxWidth: 28,
      itemStyle: {
        color: PALETTE[i % PALETTE.length],
        borderRadius: isBar ? [4, 4, 0, 0] : 0
      },
      emphasis: { focus: 'series' }
    }))
  }
}

function hasLegend(data) {
  return (data.series || []).length > 1
}

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

function initChart() {
  if (!chartRef.value || !hasData.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(getOption())
}

function resizeChart() {
  chartInstance?.resize()
}

watch(() => [props.chartData, props.type], () => {
  nextTick(() => {
    if (!hasData.value) {
      // 数据被清空时必须dispose：残留实例会持有已移除的 DOM 节点
      chartInstance?.dispose()
      chartInstance = null
      return
    }
    if (chartInstance) {
      chartInstance.setOption(getOption(), true)
    } else {
      initChart()
    }
  })
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', resizeChart)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
}
.chart-container {
  width: 100%;
}
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>