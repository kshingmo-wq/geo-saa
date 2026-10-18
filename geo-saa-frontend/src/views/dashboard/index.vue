<template>
  <div class="dashboard" v-loading="pageLoading">
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <BrandScore :score="stats.visibilityScore" size="small" label="" />
            <div class="stat-info">
              <div class="stat-label">AI 可见度评分</div>
              <div class="stat-value stat-value--score">{{ stats.visibilityScore }}</div>
              <div class="stat-trend" :class="trendClass(stats.visibilityChange)">
                <el-icon><component :is="stats.visibilityChange >= 0 ? 'Top' : 'Bottom'" /></el-icon>
                {{ Math.abs(stats.visibilityChange) }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-box stat-icon-box--green">
              <el-icon :size="22"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">内容产出总量</div>
              <div class="stat-value">{{ formatNumber(stats.contentTotal) }}</div>
              <div class="stat-trend" :class="trendClass(stats.contentGrowth)">
                <el-icon><component :is="stats.contentGrowth >= 0 ? 'Top' : 'Bottom'" /></el-icon>
                {{ Math.abs(stats.contentGrowth) }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-box stat-icon-box--amber">
              <el-icon :size="22"><Promotion /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">分发成功量</div>
              <div class="stat-value">{{ formatNumber(stats.distributeSuccess) }}</div>
              <div class="stat-progress">
                <el-progress
                  :percentage="stats.distributeRate"
                  :stroke-width="6"
                  :show-text="false"
                  color="#e6a23c"
                />
                <span class="stat-progress-text">{{ stats.distributeRate }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-box stat-icon-box--red">
              <el-icon :size="22"><Medal /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">竞品排名</div>
              <div class="stat-value">#{{ stats.rank }}</div>
              <div class="stat-trend" :class="trendClass(stats.rankChange)">
                <!-- 排名数字变小是「上升」，与数值趋势方向相反 -->
                <el-icon><component :is="stats.rankChange >= 0 ? 'Top' : 'Bottom'" /></el-icon>
                {{ Math.abs(stats.rankChange) }} 位
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">品牌 AI 可见度趋势</span>
              <el-tag size="small" type="info" effect="plain">近7天</el-tag>
            </div>
          </template>
          <TrendChart :chart-data="trendData" type="line" height="300px" empty-text="暂无趋势数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="todo-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">今日待办</span>
              <span class="card-meta">{{ doneCount }}/{{ todos.length }}</span>
            </div>
          </template>
          <el-empty v-if="todos.length === 0" description="暂无待办" :image-size="60" />
          <el-scrollbar v-else max-height="300px">
            <el-timeline>
              <el-timeline-item
                v-for="item in todos"
                :key="item.id || item.text"
                :timestamp="item.time"
                :type="item.done ? 'success' : 'primary'"
                :hollow="item.done"
                size="small"
              >
                <span :class="{ 'todo-done': item.done }">{{ item.text }}</span>
              </el-timeline-item>
            </el-timeline>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">运行中任务</span>
              <el-tag v-if="runningTasks.length" size="small" type="warning" effect="plain">
                {{ runningTasks.length }} 个进行中
              </el-tag>
            </div>
          </template>
          <el-empty v-if="runningTasks.length === 0" description="暂无运行中的任务" :image-size="60" />
          <div v-else class="task-list">
            <TaskProgress
              v-for="task in runningTasks"
              :key="task.id"
              :name="task.name"
              :progress="task.progress"
              :status="task.status"
              :detail="task.detail"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最近报告</span>
          </template>
          <el-empty v-if="recentReports.length === 0" description="暂无报告" :image-size="60" />
          <div class="report-list" v-else>
            <div class="report-item" v-for="report in recentReports" :key="report.id">
              <div class="report-icon">
                <el-icon color="#409eff"><Document /></el-icon>
              </div>
              <el-link :underline="false" class="report-name">{{ report.name }}</el-link>
              <span class="report-date">{{ report.date }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
// main.js 只显式注册用到的图标（避免全量打入 2000+ 图标），
// 此处直接 import 组件供 <component :is> 动态使用，不依赖全局注册。
import { Top, Bottom } from '@element-plus/icons-vue'
import BrandScore from '@/components/BrandScore.vue'
import TrendChart from '@/components/TrendChart.vue'
import TaskProgress from '@/components/TaskProgress.vue'
import { getDashboardStatistics } from '@/api/statistics'

const pageLoading = ref(false)

const stats = reactive({
  visibilityScore: 0,
  visibilityChange: 0,
  contentTotal: 0,
  contentGrowth: 0,
  distributeSuccess: 0,
  distributeRate: 0,
  rank: '-',
  rankChange: 0
})

const trendData = reactive({ categories: [], series: [] })
const todos = ref([])
const runningTasks = ref([])
const recentReports = ref([])

const doneCount = computed(() => todos.value.filter((t) => t.done).length)

function num(v, fallback = 0) {
  return typeof v === 'number' ? v : fallback
}

/** 大数字加千分位，避免统计值在卡片里挤成一片 */
function formatNumber(v) {
  const n = num(v)
  return n >= 10000 ? n.toLocaleString('zh-CN') : String(n)
}

/**
 * 趋势配色遵循国内惯例：涨绿跌红。
 * 注意排名类指标方向相反（名次数字变小= 上升），故调用方传入原值即可。
 */
function trendClass(v) {
  return num(v) >= 0 ? 'trend-up' : 'trend-down'
}

async function loadDashboard() {
  pageLoading.value = true
  try {
    const res = await getDashboardStatistics()
    const data = res.data || {}
    stats.visibilityScore = num(data.visibilityScore)
    stats.visibilityChange = num(data.visibilityChange)
    stats.contentTotal = num(data.contentTotal)
    stats.contentGrowth = num(data.contentGrowth)
    stats.distributeSuccess = num(data.distributeSuccess)
    stats.distributeRate = num(data.distributeRate)
    stats.rank = data.rank == null ? '-' : data.rank
    stats.rankChange = num(data.rankChange)

    const td = data.trendData || {}
    trendData.categories = td.categories || []
    trendData.series = td.series || []

    todos.value = data.todos || []
    runningTasks.value = (data.runningTasks || []).map((t) => ({
      id: t.id,
      name: t.name,
      progress: num(t.progress),
      status: t.status,
      detail: t.detail
    }))
    recentReports.value = (data.recentReports || []).map((r) => ({
      id: r.id,
      name: r.name,
      date: r.date
    }))
  } catch {
    // 接口失败时保留默认空态，不阻断页面渲染
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard { padding: 0; }

.stats-row { margin-bottom: 16px; }

/* 卡片：统一边框与圆角，hover 时轻微上浮制造层次 */
.stat-card {
  margin-bottom: 16px;
  border-radius: 10px;
  border-color: var(--el-border-color-lighter);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card :deep(.el-card__body) {
  padding: 18px 20px;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.12);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 图标底色块：去掉 inline style，改为 class 便于统一维护 */
.stat-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon-box--green { background: #f0f9eb; color: #67c23a; }
.stat-icon-box--amber { background: #fdf6ec; color: #e6a23c; }
.stat-icon-box--red { background: #fef0f0; color: #f56c6c; }

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

/* 数值用等宽字形，位数变化时不会左右跳动 */
.stat-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.4px;
}
.stat-value--score { display: none; }

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.trend-up { color: var(--el-color-success); }
.trend-down { color: var(--el-color-danger); }

/* 进度条与百分比数字并排，比独占一行更紧凑 */
.stat-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}
.stat-progress :deep(.el-progress) { flex: 1; }
.stat-progress-text {
  font-size: 12px;
  color: var(--el-color-warning);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.content-row { margin-bottom: 16px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.card-meta {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.todo-card,
.chart-card { margin-bottom: 16px; }

.todo-done {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-list {
  display: flex;
  flex-direction: column;
}
.report-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.report-item:last-child { border-bottom: none; }
/* 图标加浅色底块，与 dashboard 的图标体系一致 */
.report-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.report-name {
  flex: 1;
  font-size: 14px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.report-date {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 768px) {
  .stat-card { margin-bottom: 12px; }
}
</style>
