<template>
  <div class="workspace-page">
    <section class="workspace-hero">
      <div>
        <p class="workspace-eyebrow">PROJECT WORKSPACE</p>
        <h1>{{ projectStore.currentProject?.name ?? 'Project Command Center' }}</h1>
        <p>{{ projectStore.currentProject?.description || '围绕一个项目管理论文、实验和周报。' }}</p>
      </div>
      <div class="workspace-actions">
        <el-button size="large" :icon="Back" @click="router.push('/projects')">返回项目</el-button>
        <el-button size="large" type="primary" :icon="VideoPlay" @click="router.push(`/projects/${projectId}/experiments`)">记录实验</el-button>
      </div>
      <div class="workspace-art" aria-hidden="true">
        <span class="art-ring"></span>
        <span class="art-card one"></span>
        <span class="art-card two"></span>
        <span class="art-line"></span>
      </div>
    </section>

    <section class="workspace-metrics">
      <article v-for="metric in metrics" :key="metric.label" class="workspace-stat" :class="metric.tone">
        <span class="workspace-stat-icon">
          <el-icon><component :is="metric.icon" /></el-icon>
        </span>
        <div>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.hint }}</small>
        </div>
      </article>
    </section>

    <section class="workspace-launchpad">
      <button class="launch-card papers" type="button" @click="router.push(`/projects/${projectId}/papers`)">
        <span><el-icon><Document /></el-icon></span>
        <strong>论文库</strong>
        <p>沉淀阅读笔记、关键 claim、PDF 与 AI 论文卡片。</p>
      </button>
      <button class="launch-card experiments" type="button" @click="router.push(`/projects/${projectId}/experiments`)">
        <span><el-icon><DataAnalysis /></el-icon></span>
        <strong>实验台</strong>
        <p>跟踪配置、指标、失败原因和下一步行动。</p>
      </button>
      <button class="launch-card reports" type="button" @click="router.push(`/projects/${projectId}/reports`)">
        <span><el-icon><Notebook /></el-icon></span>
        <strong>周报</strong>
        <p>自动填写草稿，支持 AI 优化后确认保存。</p>
      </button>
      <button class="launch-card rag" type="button" @click="router.push(`/projects/${projectId}/rag`)">
        <span><el-icon><Connection /></el-icon></span>
        <strong>RAG 问答</strong>
        <p>把论文、PDF、AI 卡片和实验记录变成项目内知识库。</p>
      </button>
    </section>

    <div class="workspace-grid">
      <section class="workspace-panel">
        <div class="workspace-panel-head">
          <div>
            <span>RECENT RUNS</span>
            <h2>最近实验</h2>
          </div>
          <el-button link type="primary" @click="router.push(`/projects/${projectId}/experiments`)">查看全部</el-button>
        </div>
        <el-table v-loading="loading" :data="experiments.slice(0, 6)" class="workspace-table">
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column label="状态" width="130">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="nextStep" label="下一步" min-width="220" show-overflow-tooltip />
        </el-table>
      </section>

      <aside class="workspace-panel action-panel">
        <div class="workspace-panel-head">
          <div>
            <span>NEXT ACTIONS</span>
            <h2>下一步行动</h2>
          </div>
        </div>
        <div v-if="nextActions.length" class="workspace-action-list">
          <button v-for="experiment in nextActions" :key="experiment.id" class="workspace-action" :class="{ high: experiment.status === 'FAILED' }" type="button">
            <i></i>
            <strong>{{ experiment.name }}</strong>
            <span>{{ experiment.nextStep || experiment.failureReason || '检查实验状态' }}</span>
          </button>
        </div>
        <EmptyState v-else title="暂无行动项" description="失败实验或带下一步的实验会显示在这里。" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Connection, DataAnalysis, Document, Files, Notebook, TrendCharts, VideoPlay } from '@element-plus/icons-vue'
import { listExperiments, listPapers, listReports } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { useProjectStore } from '../stores/project'
import type { Experiment, Paper, WeeklyReport } from '../types'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const projectId = route.params.projectId as string
const papers = ref<Paper[]>([])
const experiments = ref<Experiment[]>([])
const reports = ref<WeeklyReport[]>([])
const loading = ref(false)

const runningCount = computed(() => experiments.value.filter((experiment) => experiment.status === 'RUNNING').length)
const nextActions = computed(() =>
  experiments.value.filter((experiment) => experiment.status === 'FAILED' || Boolean(experiment.nextStep)).slice(0, 6)
)
const metrics = computed(() => [
  { label: 'Papers', value: papers.value.length, hint: '论文记录', icon: Document, tone: 'blue' },
  { label: 'Experiments', value: experiments.value.length, hint: '实验记录', icon: DataAnalysis, tone: 'green' },
  { label: 'Running', value: runningCount.value, hint: '正在推进', icon: TrendCharts, tone: 'orange' },
  { label: 'Reports', value: reports.value.length, hint: '周报数量', icon: Files, tone: 'purple' }
])

async function load() {
  loading.value = true
  try {
    await projectStore.load(projectId)
    const [paperData, experimentData, reportData] = await Promise.all([
      listPapers(projectId),
      listExperiments(projectId),
      listReports(projectId)
    ])
    papers.value = paperData
    experiments.value = experimentData
    reports.value = reportData
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.workspace-page {
  display: grid;
  gap: 22px;
}

.workspace-hero {
  position: relative;
  min-height: 220px;
  overflow: hidden;
  border-radius: 8px;
}

.workspace-eyebrow {
  margin: 0 0 18px;
  color: #1b75ff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.workspace-hero h1 {
  position: relative;
  z-index: 2;
  max-width: 760px;
  margin: 0;
  color: #0e1f38;
  font-size: clamp(34px, 4.6vw, 56px);
  line-height: 1.05;
  letter-spacing: 0;
}

.workspace-hero p:not(.workspace-eyebrow) {
  position: relative;
  z-index: 2;
  max-width: 700px;
  margin: 20px 0 0;
  color: #59687d;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.55;
}

.workspace-actions {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 0;
  display: flex;
  gap: 12px;
}

.workspace-actions :deep(.el-button) {
  height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.workspace-art {
  position: absolute;
  inset: 0 0 0 48%;
  pointer-events: none;
}

.art-ring {
  position: absolute;
  right: 280px;
  top: 48px;
  width: 112px;
  height: 112px;
  border: 22px solid rgba(27, 117, 255, 0.1);
  border-radius: 999px;
}

.art-card {
  position: absolute;
  display: block;
  border-radius: 12px;
  background: rgba(93, 142, 255, 0.18);
  box-shadow: 0 24px 54px rgba(27, 117, 255, 0.12);
}

.art-card.one {
  right: 114px;
  top: 46px;
  width: 170px;
  height: 96px;
  transform: rotate(-18deg);
}

.art-card.two {
  right: 22px;
  bottom: 30px;
  width: 112px;
  height: 78px;
  background: rgba(16, 185, 129, 0.14);
  transform: rotate(18deg);
}

.art-line {
  position: absolute;
  right: 60px;
  top: 82px;
  width: 420px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(27, 117, 255, 0.22), transparent);
  transform: rotate(-24deg);
}

.workspace-metrics,
.workspace-launchpad {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.workspace-stat {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 16px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  padding: 22px;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
}

.workspace-stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 16px;
  background: #e8f1ff;
  color: #1b75ff;
  font-size: 30px;
}

.workspace-stat.green .workspace-stat-icon {
  background: #dff8ef;
  color: #0f9f76;
}

.workspace-stat.orange .workspace-stat-icon {
  background: #fff3e3;
  color: #ff8a1f;
}

.workspace-stat.purple .workspace-stat-icon {
  background: #f0e7ff;
  color: #7c3cff;
}

.workspace-stat span:not(.workspace-stat-icon) {
  color: #58677e;
  font-size: 16px;
  font-weight: 800;
}

.workspace-stat strong {
  display: block;
  margin: 8px 0;
  color: #0e1f38;
  font-size: 38px;
  line-height: 1;
  font-weight: 900;
}

.workspace-stat small {
  color: #687991;
  font-weight: 750;
}

.workspace-launchpad {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.launch-card {
  display: grid;
  gap: 12px;
  min-height: 174px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  padding: 24px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
  transition: border-color 180ms ease, background 180ms ease;
}

.launch-card:hover {
  border-color: #1b75ff;
  background: #f8fbff;
}

.launch-card span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #e8f1ff;
  color: #1b75ff;
  font-size: 26px;
}

.launch-card.experiments span {
  background: #dff8ef;
  color: #0f9f76;
}

.launch-card.reports span {
  background: #f0e7ff;
  color: #7c3cff;
}

.launch-card.rag span {
  background: #dff7f4;
  color: #0f766e;
}

.launch-card strong {
  color: #10213c;
  font-size: 22px;
  font-weight: 900;
}

.launch-card p {
  margin: 0;
  color: #59687d;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.55;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.55fr);
  gap: 18px;
}

.workspace-panel {
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  padding: 22px;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
}

.workspace-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.workspace-panel-head span {
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workspace-panel-head h2 {
  margin: 5px 0 0;
  color: #0e1f38;
  font-size: 24px;
  font-weight: 900;
}

.workspace-table :deep(.el-table__header th) {
  background: #fbfdff;
  color: #74829a;
  font-weight: 850;
}

.workspace-action-list {
  display: grid;
  gap: 10px;
}

.workspace-action {
  position: relative;
  display: grid;
  gap: 5px;
  border: 1px solid #dce6f4;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px 16px 16px 40px;
  text-align: left;
  cursor: pointer;
}

.workspace-action i {
  position: absolute;
  top: 20px;
  left: 16px;
  width: 10px;
  height: 10px;
  border: 3px solid #1b75ff;
  border-radius: 999px;
}

.workspace-action.high i {
  border-color: #ef3340;
}

.workspace-action strong {
  color: #10213c;
  font-size: 16px;
}

.workspace-action span {
  color: #59687d;
  font-weight: 650;
  line-height: 1.45;
}

@media (max-width: 1180px) {
  .workspace-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-grid,
  .workspace-launchpad {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .workspace-hero {
    min-height: 300px;
  }

  .workspace-actions {
    position: relative;
    margin-top: 20px;
    flex-wrap: wrap;
  }

  .workspace-art {
    display: none;
  }

  .workspace-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
