<template>
  <div class="experiments-page">
    <section class="experiment-hero">
      <div class="hero-copy">
        <p class="hero-eyebrow">EXECUTION TRACKER</p>
        <h1>Experiments</h1>
        <div class="hero-project">
          <span>{{ projectStore.currentProject?.name || '当前项目' }}</span>
          <el-icon><InfoFilled /></el-icon>
        </div>
      </div>

      <div class="hero-actions">
        <el-button size="large" :icon="FolderOpened" @click="router.push(`/projects/${projectId}`)">项目工作台</el-button>
        <el-button size="large" type="primary" :icon="Plus" @click="openCreate">新增实验</el-button>
        <div class="user-chip">U</div>
      </div>

      <div class="hero-art" aria-hidden="true">
        <div class="art-line one"></div>
        <div class="art-line two"></div>
        <div class="art-node node-a"></div>
        <div class="art-node node-b"></div>
        <div class="art-node node-c"></div>
        <div class="art-card tilted">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="art-chart">
          <i></i>
          <i></i>
          <i></i>
        </div>
      </div>
    </section>

    <section class="experiment-metrics">
      <article
        v-for="metric in metrics"
        :key="metric.status"
        class="experiment-stat"
        :class="metric.tone"
      >
        <div class="stat-icon">
          <el-icon><component :is="metric.icon" /></el-icon>
        </div>
        <div>
          <span class="stat-label">{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
        <div class="stat-trend">较上周 <span>↑ 0%</span></div>
        <svg class="stat-spark" viewBox="0 0 140 54" role="img" aria-label="trend">
          <path :d="metric.sparkline" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" />
        </svg>
      </article>
    </section>

    <section class="experiment-toolbar">
      <el-input v-model="query" clearable placeholder="搜索实验名称、数据集、指标、结论或下一步" size="large">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="statusFilter" clearable placeholder="状态：全部" size="large" class="status-select">
        <template #prefix>
          <el-icon><Filter /></el-icon>
        </template>
        <el-option label="状态：全部" value="" />
        <el-option v-for="status in statuses" :key="status" :label="statusLabel(status)" :value="status" />
      </el-select>
      <el-button size="large" :icon="Refresh" :loading="loading" @click="load" />
    </section>

    <section class="experiment-table-card">
      <el-table
        v-loading="loading"
        :data="paginatedExperiments"
        class="experiment-table"
        row-key="id"
      >
        <el-table-column label="名称" min-width="220">
          <template #default="{ row }">
            <div class="experiment-name-cell">
              <strong>{{ row.name }}</strong>
              <span v-if="row.commitHash">{{ row.commitHash }}</span>
              <span v-else>暂无版本备注</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <span class="status-pill" :class="statusMeta(row.status).tone">
              <i></i>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="仓库" min-width="180">
          <template #default="{ row }">
            <a v-if="row.repoUrl" class="experiment-link" :href="row.repoUrl" target="_blank" rel="noreferrer">GitHub Repo</a>
            <span v-else class="cell-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="数据集" min-width="160">
          <template #default="{ row }">
            <span>{{ row.dataset || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="指标" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.metrics || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="下一步" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.nextStep || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="EditPen" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="experiment-empty">
            <strong>还没有实验记录</strong>
            <p>记录实验配置、指标和下一步，Dashboard 会自动生成行动队列。</p>
            <el-button type="primary" :icon="Plus" @click="openCreate">新增实验</el-button>
          </div>
        </template>
      </el-table>

      <div class="experiment-pagination">
        <span>共 {{ filteredExperiments.length }} 条记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="filteredExperiments.length"
          background
        />
      </div>
    </section>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑实验' : '新增实验'" width="820px" class="experiment-dialog">
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :xs="24" :md="12">
            <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="状态" required>
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="status in statuses" :key="status" :label="statusLabel(status)" :value="status" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="GitHub 仓库 URL"><el-input v-model="form.repoUrl" placeholder="https://github.com/org/repo" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="数据集"><el-input v-model="form.dataset" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Commit / 版本备注"><el-input v-model="form.commitHash" placeholder="可选：commit hash、branch 或 tag" /></el-form-item>
        <el-form-item label="配置"><el-input v-model="form.config" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="指标"><el-input v-model="form.metrics" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="结论"><el-input v-model="form.conclusion" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="失败原因"><el-input v-model="form.failureReason" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="下一步"><el-input v-model="form.nextStep" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  CircleCheckFilled,
  CircleCloseFilled,
  Delete,
  EditPen,
  Filter,
  FolderOpened,
  InfoFilled,
  Plus,
  Refresh,
  Search,
  VideoPlay
} from '@element-plus/icons-vue'
import { createExperiment, deleteExperiment, listExperiments, updateExperiment } from '../api/labflow'
import { useProjectStore } from '../stores/project'
import type { Experiment, ExperimentStatus } from '../types'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const projectId = route.params.projectId as string
const statuses: ExperimentStatus[] = ['PLANNED', 'RUNNING', 'SUCCESS', 'FAILED', 'PAUSED']
const experiments = ref<Experiment[]>([])
const query = ref('')
const statusFilter = ref<ExperimentStatus | ''>('')
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const form = reactive({
  name: '',
  status: 'PLANNED' as ExperimentStatus,
  commitHash: '',
  repoUrl: '',
  dataset: '',
  config: '',
  metrics: '',
  conclusion: '',
  failureReason: '',
  nextStep: ''
})

const filteredExperiments = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return experiments.value.filter((experiment) => {
    const matchesStatus = !statusFilter.value || experiment.status === statusFilter.value
    const matchesKeyword =
      !keyword ||
      [experiment.name, experiment.repoUrl, experiment.dataset, experiment.metrics, experiment.conclusion, experiment.failureReason, experiment.nextStep]
        .some((value) => value?.toLowerCase().includes(keyword))
    return matchesStatus && matchesKeyword
  })
})

const paginatedExperiments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredExperiments.value.slice(start, start + pageSize.value)
})

const metrics = computed(() => [
  {
    status: 'PLANNED',
    label: 'Planned',
    value: countByStatus('PLANNED'),
    tone: 'planned',
    icon: Calendar,
    sparkline: 'M4 42 C22 40 28 32 42 34 S62 52 78 31 S99 26 108 16 S125 9 136 13'
  },
  {
    status: 'RUNNING',
    label: 'Running',
    value: countByStatus('RUNNING'),
    tone: 'running',
    icon: VideoPlay,
    sparkline: 'M4 43 C20 42 29 37 38 27 S58 16 70 30 S88 44 100 25 S119 7 136 13'
  },
  {
    status: 'SUCCESS',
    label: 'Success',
    value: countByStatus('SUCCESS'),
    tone: 'success',
    icon: CircleCheckFilled,
    sparkline: 'M4 44 C20 42 29 35 39 30 S57 15 70 29 S90 44 103 24 S121 8 136 13'
  },
  {
    status: 'FAILED',
    label: 'Failed',
    value: countByStatus('FAILED'),
    tone: 'failed',
    icon: CircleCloseFilled,
    sparkline: 'M4 43 C21 40 30 34 43 31 S62 23 75 35 S91 47 105 28 S122 9 136 14'
  }
])

watch([query, statusFilter, pageSize], () => {
  currentPage.value = 1
})

function countByStatus(status: ExperimentStatus) {
  return experiments.value.filter((experiment) => experiment.status === status).length
}

function statusLabel(status: ExperimentStatus) {
  return {
    PLANNED: 'PLANNED',
    RUNNING: 'RUNNING',
    SUCCESS: 'SUCCESS',
    FAILED: 'FAILED',
    PAUSED: 'PAUSED'
  }[status]
}

function statusMeta(status: ExperimentStatus) {
  return {
    PLANNED: { tone: 'planned' },
    RUNNING: { tone: 'running' },
    SUCCESS: { tone: 'success' },
    FAILED: { tone: 'failed' },
    PAUSED: { tone: 'paused' }
  }[status]
}

function reset() {
  Object.assign(form, {
    name: '',
    status: 'PLANNED',
    commitHash: '',
    repoUrl: '',
    dataset: '',
    config: '',
    metrics: '',
    conclusion: '',
    failureReason: '',
    nextStep: ''
  })
}

function openCreate() {
  editingId.value = null
  reset()
  dialogOpen.value = true
}

function openEdit(experiment: Experiment) {
  editingId.value = experiment.id
  Object.assign(form, {
    name: experiment.name,
    status: experiment.status,
    commitHash: experiment.commitHash ?? '',
    repoUrl: experiment.repoUrl ?? '',
    dataset: experiment.dataset ?? '',
    config: experiment.config ?? '',
    metrics: experiment.metrics ?? '',
    conclusion: experiment.conclusion ?? '',
    failureReason: experiment.failureReason ?? '',
    nextStep: experiment.nextStep ?? ''
  })
  dialogOpen.value = true
}

async function load() {
  loading.value = true
  try {
    await projectStore.load(projectId)
    experiments.value = await listExperiments(projectId)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写实验名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) await updateExperiment(editingId.value, form)
    else await createExperiment(projectId, form)
    ElMessage.success('实验已保存')
    dialogOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(experimentId: string) {
  await ElMessageBox.confirm('确定删除这条实验记录吗？', '删除确认', { type: 'warning' })
  await deleteExperiment(experimentId)
  ElMessage.success('实验已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.experiments-page {
  display: grid;
  gap: 24px;
}

.experiment-hero {
  position: relative;
  min-height: 230px;
  overflow: hidden;
  border-radius: 6px;
}

.hero-copy {
  position: relative;
  z-index: 2;
  padding-top: 28px;
}

.hero-eyebrow {
  margin: 0 0 22px;
  color: #1b75ff;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 0;
  color: #0e1f38;
  font-size: clamp(40px, 5vw, 58px);
  line-height: 1;
  letter-spacing: 0;
}

.hero-project {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 26px;
  color: #536176;
  font-size: 22px;
  font-weight: 650;
}

.hero-actions {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-actions :deep(.el-button) {
  height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 999px;
  background: #1b75ff;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(27, 117, 255, 0.28);
  font-size: 22px;
  font-weight: 850;
}

.hero-art {
  position: absolute;
  inset: 0 0 auto 44%;
  height: 230px;
  pointer-events: none;
  opacity: 0.92;
}

.art-line {
  position: absolute;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(27, 117, 255, 0.18), transparent);
  transform-origin: left center;
}

.art-line.one {
  top: 90px;
  left: 42px;
  width: 430px;
  transform: rotate(-23deg);
}

.art-line.two {
  top: 150px;
  left: 190px;
  width: 360px;
  transform: rotate(26deg);
}

.art-node {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background: #5aa0ff;
  box-shadow: 0 12px 30px rgba(27, 117, 255, 0.35);
}

.node-a {
  top: 78px;
  left: 250px;
}

.node-b {
  top: 117px;
  left: 340px;
}

.node-c {
  top: 72px;
  right: 320px;
  opacity: 0.45;
}

.art-card {
  position: absolute;
  right: 190px;
  bottom: 24px;
  display: grid;
  gap: 11px;
  width: 138px;
  height: 96px;
  border-radius: 6px;
  background: linear-gradient(145deg, #b7cbff, #5a82ee);
  padding: 22px;
  box-shadow: 0 24px 40px rgba(39, 91, 190, 0.2);
}

.art-card.tilted {
  transform: rotate(62deg) skewX(-8deg);
}

.art-card span {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
}

.art-chart {
  position: absolute;
  right: 12px;
  bottom: 32px;
  display: flex;
  align-items: end;
  gap: 9px;
  width: 118px;
  height: 78px;
  border: 5px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  background: rgba(226, 237, 255, 0.35);
  padding: 12px 14px;
  transform: rotate(18deg);
}

.art-chart i {
  width: 14px;
  border-radius: 999px 999px 0 0;
  background: #2d7df1;
}

.art-chart i:nth-child(1) {
  height: 22px;
}

.art-chart i:nth-child(2) {
  height: 42px;
}

.art-chart i:nth-child(3) {
  height: 56px;
}

.experiment-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.experiment-stat {
  position: relative;
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  min-height: 174px;
  overflow: hidden;
  border: 1px solid #dbe7f7;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  padding: 28px 26px;
  box-shadow: 0 18px 38px rgba(28, 55, 98, 0.08);
}

.experiment-stat.running {
  border-color: #8cd8ca;
  box-shadow: 0 18px 38px rgba(15, 139, 126, 0.12);
}

.experiment-stat.failed {
  border-color: #f2c9cf;
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  border-radius: 16px;
  background: #e8f1ff;
  color: #1b75ff;
  font-size: 32px;
}

.experiment-stat.running .stat-icon,
.experiment-stat.running .stat-trend span {
  background: #dff8ef;
  color: #0f9f76;
}

.experiment-stat.success .stat-icon,
.experiment-stat.success .stat-trend span {
  background: #e8f9e5;
  color: #3caf56;
}

.experiment-stat.failed .stat-icon {
  background: #fff0f1;
  color: #ef3340;
}

.stat-label {
  display: block;
  margin-bottom: 12px;
  color: #41506a;
  font-size: 20px;
  font-weight: 750;
}

.experiment-stat strong {
  display: block;
  color: #0e1f38;
  font-size: 42px;
  line-height: 1;
  font-weight: 900;
}

.stat-trend {
  align-self: end;
  grid-column: 1 / 2;
  color: #6b778a;
  font-size: 17px;
  font-weight: 750;
}

.stat-trend span {
  color: #0f9f76;
  font-weight: 900;
}

.stat-spark {
  position: absolute;
  right: -4px;
  bottom: 20px;
  width: 128px;
  height: 54px;
  color: #1b75ff;
  opacity: 0.9;
}

.experiment-stat.running .stat-spark,
.experiment-stat.success .stat-spark {
  color: #20a979;
}

.experiment-stat.failed .stat-spark {
  color: #ef3340;
}

.experiment-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px 54px;
  gap: 16px;
  align-items: center;
}

.experiment-toolbar :deep(.el-input__wrapper),
.experiment-toolbar :deep(.el-select__wrapper),
.experiment-toolbar :deep(.el-button) {
  min-height: 54px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce6f4 inset;
}

.status-select {
  width: 100%;
}

.experiment-table-card {
  overflow: hidden;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
}

.experiment-table {
  width: 100%;
}

.experiment-table :deep(.el-table__header th) {
  height: 66px;
  background: #fbfdff;
  color: #74829a;
  font-size: 15px;
  font-weight: 850;
}

.experiment-table :deep(.el-table__row td) {
  height: 78px;
  color: #26364f;
  font-size: 15px;
}

.experiment-name-cell {
  display: grid;
  gap: 5px;
}

.experiment-name-cell strong {
  color: #1b2b46;
  font-size: 16px;
}

.experiment-name-cell span,
.cell-muted {
  color: #8a96a8;
  font-weight: 700;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid currentColor;
  border-radius: 999px;
  padding: 7px 13px;
  background: #f8fbff;
  color: #1b75ff;
  font-size: 13px;
  font-weight: 900;
}

.status-pill i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.status-pill.running {
  background: #effaf6;
  color: #0f9f76;
}

.status-pill.success {
  background: #effaf0;
  color: #3caf56;
}

.status-pill.failed {
  background: #fff4f5;
  color: #ef3340;
}

.status-pill.paused {
  background: #f3f6fb;
  color: #6b778a;
}

.experiment-link {
  color: #1b75ff;
  font-weight: 850;
  text-decoration: none;
}

.experiment-link:hover {
  text-decoration: underline;
}

.experiment-empty {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 60px 20px;
}

.experiment-empty strong {
  color: #11213b;
  font-size: 18px;
}

.experiment-empty p {
  margin: 0;
  color: #687991;
  font-weight: 650;
}

.experiment-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid #edf2f8;
  padding: 18px 24px;
  color: #687991;
  font-weight: 750;
}

@media (max-width: 1180px) {
  .experiment-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-art {
    opacity: 0.35;
  }
}

@media (max-width: 760px) {
  .experiment-hero {
    min-height: 280px;
  }

  .hero-actions {
    position: relative;
    margin-top: 22px;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .hero-art {
    display: none;
  }

  .experiment-metrics,
  .experiment-toolbar {
    grid-template-columns: 1fr;
  }

  .experiment-stat {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .experiment-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
