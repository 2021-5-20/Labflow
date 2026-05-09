<template>
  <div class="dashboard-page">
    <PageHeader
      title="研究仪表盘"
      description="跟踪所有研究项目的推进状态、实验风险、论文沉淀和周报产出。"
    />

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="metric-grid" v-loading="loading">
      <MetricCard label="项目总数" :value="dashboard?.totalProjects ?? 0" hint="全部研究课题" tone="blue" trend="较上周 +20%">
        <template #icon><el-icon><FolderOpened /></el-icon></template>
      </MetricCard>
      <MetricCard label="进行中的实验" :value="dashboard?.runningExperiments ?? 0" hint="正在推进" tone="green" trend="较上周 +14%">
        <template #icon><el-icon><DataAnalysis /></el-icon></template>
      </MetricCard>
      <MetricCard label="已记录论文" :value="dashboard?.totalPapers ?? 0" hint="论文记录" tone="purple" trend="较上周 +18%">
        <template #icon><el-icon><Document /></el-icon></template>
      </MetricCard>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-title">
          <h2>项目概览</h2>
        </div>
        <el-table :data="dashboard?.projectSummaries ?? []" class="progress-table">
          <el-table-column prop="name" label="项目" min-width="180">
            <template #default="{ row }">
              <div class="project-cell">
                <span class="project-mark">{{ row.name.slice(0, 1) }}</span>
                <button type="button" @click="router.push(`/projects/${row.id}`)">
                  <strong>{{ row.name }}</strong>
                  <small>{{ row.description || '暂无描述' }}</small>
                </button>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="paperCount" label="论文" width="90" />
          <el-table-column prop="experimentCount" label="实验" width="90" />
          <el-table-column prop="runningExperiments" label="运行中" width="90" />
          <el-table-column prop="reportCount" label="周报" width="90" />
          <el-table-column label="最近更新" width="150">
            <template #default="{ row }">
              <div class="time-cell">
                <strong>{{ formatDateTime(row.updatedAt) }}</strong>
                <span>{{ formatRelativeTime(row.updatedAt) }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <button class="panel-link" type="button" @click="router.push('/projects')">
          查看全部项目
          <el-icon><ArrowRight /></el-icon>
        </button>
      </section>

      <aside class="panel">
        <div class="panel-title">
          <h2><el-icon><Tickets /></el-icon> 下一步行动</h2>
        </div>
        <div class="action-summary">
          <span class="action-summary-icon"><el-icon><Finished /></el-icon></span>
          <div>
            <strong>继续推进研究</strong>
            <p>已提交 {{ dashboard?.totalReports ?? 0 }} 份周报，下一步行动会从实验计划中自动汇总。</p>
          </div>
        </div>
        <div v-if="dashboard?.nextActions.length" class="action-list">
          <button
            v-for="action in dashboard.nextActions"
            :key="`${action.projectId}-${action.experimentId}-${action.title}`"
            class="action-item"
            :class="{ high: action.priority === 'high' }"
            @click="router.push(`/projects/${action.projectId}/experiments`)"
          >
            <strong>{{ action.title }}</strong>
            <span>{{ action.projectName }}：{{ action.detail || '检查实验状态' }}</span>
            <em>{{ action.priority === 'high' ? '高优先级' : '本周截止' }}</em>
          </button>
        </div>
        <EmptyState v-else title="暂无下一步" description="当实验记录包含 next step 时，会在这里形成行动队列。" />
        <button class="panel-link" type="button" @click="router.push('/projects')">
          查看全部待办事项
          <el-icon><ArrowRight /></el-icon>
        </button>
      </aside>
    </div>

    <div class="quick-grid">
      <section class="quick-card">
        <div class="quick-card-head">
          <h3>最近论文</h3>
          <button type="button" @click="router.push('/projects')">查看全部 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <el-table :data="dashboard?.recentPapers ?? []" size="small">
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="projectName" label="项目" width="140" />
        </el-table>
      </section>
      <section class="quick-card">
        <div class="quick-card-head">
          <h3>最近实验</h3>
          <button type="button" @click="router.push('/projects')">查看全部 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <el-table :data="dashboard?.recentExperiments ?? []" size="small">
          <el-table-column prop="name" label="实验" min-width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </section>
      <section class="quick-card">
        <div class="quick-card-head">
          <h3>最近周报</h3>
          <button type="button" @click="router.push('/projects')">查看全部 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <el-table :data="dashboard?.recentReports ?? []" size="small">
          <el-table-column label="周期" min-width="160">
            <template #default="{ row }">{{ row.startDate }} - {{ row.endDate }}</template>
          </el-table-column>
          <el-table-column prop="projectName" label="项目" width="140" />
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, DataAnalysis, Document, Finished, FolderOpened, Tickets } from '@element-plus/icons-vue'
import { getDashboard } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import MetricCard from '../components/MetricCard.vue'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import type { DashboardSummary } from '../types'
import { formatDateTime, formatRelativeTime } from '../utils/datetime'

const router = useRouter()
const dashboard = ref<DashboardSummary | null>(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await getDashboard()
  } catch {
    error.value = 'Dashboard 加载失败，请确认 API 服务已启动。'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
