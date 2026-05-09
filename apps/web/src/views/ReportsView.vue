<template>
  <div class="reports-page">
    <section class="reports-hero">
      <div class="reports-hero-copy">
        <p>WEEKLY REVIEW</p>
        <h1>Reports</h1>
        <span>{{ projectStore.currentProject?.name || '当前项目' }}</span>
      </div>
      <div class="reports-actions">
        <el-button size="large" :icon="FolderOpened" @click="router.push(`/projects/${projectId}`)">项目工作台</el-button>
        <el-button size="large" :icon="Files" @click="historyOpen = true">历史周报</el-button>
        <el-button size="large" :icon="CopyDocument" :disabled="!currentMarkdown" @click="copyMarkdown">复制 Markdown</el-button>
      </div>
      <div class="reports-art" aria-hidden="true">
        <div class="report-page page-a"></div>
        <div class="report-page page-b"></div>
        <div class="report-chart"><i></i><i></i><i></i></div>
      </div>
    </section>

    <section class="report-composer-card">
      <div class="report-card-head inline">
        <div>
          <span>CREATE DRAFT</span>
          <h2>生成周报</h2>
          <p>先自动填写草稿，再按需要让 AI 优化。确认保存后才会进入历史记录。</p>
        </div>
        <el-tag effect="light">{{ reports.length }} 份历史周报</el-tag>
      </div>
      <div class="report-stepper">
        <div class="report-step" :class="{ active: reportStep === 0, done: reportStep > 0 }">
          <span>1</span>
          <strong>自动草稿</strong>
        </div>
        <div class="report-step-line" :class="{ done: reportStep > 0 }"></div>
        <div class="report-step" :class="{ active: reportStep === 1, done: reportStep > 1 }">
          <span>2</span>
          <strong>AI 优化</strong>
        </div>
        <div class="report-step-line" :class="{ done: reportStep > 1 }"></div>
        <div class="report-step" :class="{ active: reportStep === 2 }">
          <span>3</span>
          <strong>确认保存</strong>
        </div>
      </div>
      <div class="report-controls">
        <div class="report-date-grid">
          <el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" size="large" />
          <el-date-picker v-model="endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" size="large" />
        </div>
        <div class="report-flow-actions">
          <el-button :icon="DocumentAdd" :loading="previewing" @click="preview">自动填写草稿</el-button>
          <el-button :icon="MagicStick" :disabled="!draftReady || !currentMarkdown" :loading="aiPolishing" @click="polishWithAi">AI 优化草稿</el-button>
          <el-button type="primary" :icon="CircleCheckFilled" :disabled="!draftReady" :loading="saving" @click="confirmGenerate">确认保存周报</el-button>
        </div>
      </div>
      <div class="draft-state" :class="draftMode || 'idle'">
        <strong>{{ draftMode === 'ai' ? 'AI 草稿' : draftMode === 'auto' ? '自动草稿' : '等待生成' }}</strong>
        <span>{{ draftReady ? draftMessage : '选择日期范围后开始生成草稿。' }}</span>
        <small v-if="aiPolishJob">AI job：{{ aiPolishJob.status }} · {{ formatDateTime(aiPolishJob.updatedAt) }}</small>
      </div>
    </section>

    <section class="report-preview-card">
      <div class="report-card-head inline">
        <div>
          <span>PREVIEW</span>
          <h2>周报预览</h2>
        </div>
        <el-tag v-if="draftMode" effect="light" :type="draftMode === 'ai' ? 'success' : 'info'">
          {{ draftMode === 'ai' ? 'AI 草稿' : '自动草稿' }}
        </el-tag>
      </div>
      <EmptyState
        v-if="!currentMarkdown"
        title="还没有可预览的周报"
        description="选择日期范围后，可以自动填写草稿；生成草稿后也可以交给 AI 优化，再确认保存。"
      />
      <ReportPreview
        v-else
        v-model="activeTab"
        v-model:markdown="currentMarkdown"
        :editable="draftReady"
      />
    </section>

    <el-drawer v-model="historyOpen" title="历史周报" size="620px" class="reports-history-drawer">
      <EmptyState v-if="!loading && reports.length === 0" title="暂无历史周报" description="生成第一份周报后，历史记录会显示在这里。" />
      <el-table v-else v-loading="loading" :data="reports" class="reports-table">
        <el-table-column prop="startDate" label="开始日期" width="140" />
        <el-table-column prop="endDate" label="结束日期" width="140" />
        <el-table-column label="生成时间" min-width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <strong>{{ formatDateTime(row.createdAt) }}</strong>
              <span>{{ formatRelativeTime(row.createdAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="openHistory(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CopyDocument, DocumentAdd, Files, FolderOpened, MagicStick } from '@element-plus/icons-vue'
import { createAiJob, formatAiJobError, generateWeeklyReport, listReports, previewWeeklyReport, waitForAiJobCompletion } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import ReportPreview from '../components/ReportPreview.vue'
import { useProjectStore } from '../stores/project'
import type { AiJob, WeeklyReport } from '../types'
import { formatDateTime, formatRelativeTime } from '../utils/datetime'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const projectId = route.params.projectId as string
const reports = ref<WeeklyReport[]>([])
const loading = ref(false)
const previewing = ref(false)
const aiPolishing = ref(false)
const saving = ref(false)
const activeTab = ref('preview')
const startDate = ref('')
const endDate = ref('')
const currentMarkdown = ref('')
const draftReady = ref(false)
const draftMode = ref<'auto' | 'ai' | ''>('')
const draftMessage = ref('当前是周报草稿，可在 Markdown 中修改，确认保存后才会进入历史周报。')
const historyOpen = ref(false)
const aiPolishJob = ref<AiJob | null>(null)
const reportStep = computed(() => {
  if (draftMode.value === 'ai') return 2
  if (draftReady.value) return 1
  return 0
})

async function load() {
  loading.value = true
  try {
    await projectStore.load(projectId)
    reports.value = await listReports(projectId)
    if (!currentMarkdown.value && reports.value.length > 0) currentMarkdown.value = reports.value[0].contentMarkdown
  } finally {
    loading.value = false
  }
}

async function preview() {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning('请选择开始日期和结束日期')
    return
  }
  previewing.value = true
  try {
    const draft = await previewWeeklyReport(projectId, { startDate: startDate.value, endDate: endDate.value })
    currentMarkdown.value = draft.contentMarkdown
    draftReady.value = true
    draftMode.value = 'auto'
    draftMessage.value = '当前是自动填写草稿，可继续手动修改，也可以点击 AI 优化草稿。确认保存后才会进入历史周报。'
    activeTab.value = 'source'
    ElMessage.success('自动草稿已生成，可修改后确认保存')
  } finally {
    previewing.value = false
  }
}

async function polishWithAi() {
  if (!currentMarkdown.value.trim()) {
    ElMessage.warning('请先生成自动草稿')
    return
  }
  aiPolishing.value = true
  try {
    const createdJob = await createAiJob({
      projectId,
      targetType: 'WEEKLY_REPORT',
      jobType: 'weekly-report-polish',
      inputJson: JSON.stringify({ contentMarkdown: currentMarkdown.value })
    })
    aiPolishJob.value = createdJob
    const job = await waitForAiJobCompletion(createdJob)
    aiPolishJob.value = job
    const polished = parseAiReportMarkdown(job)
    currentMarkdown.value = polished
    draftReady.value = true
    draftMode.value = 'ai'
    draftMessage.value = '当前是 AI 优化草稿，请核对事实和措辞后再确认保存。'
    activeTab.value = 'source'
    ElMessage.success('AI 优化草稿已生成')
  } catch (error) {
    ElMessage.error(formatAiJobError(error, 'AI 优化周报失败'))
  } finally {
    aiPolishing.value = false
  }
}

async function confirmGenerate() {
  if (!startDate.value || !endDate.value || !currentMarkdown.value.trim()) {
    ElMessage.warning('请先生成并确认周报内容')
    return
  }
  saving.value = true
  try {
    const report = await generateWeeklyReport(projectId, {
      startDate: startDate.value,
      endDate: endDate.value,
      contentMarkdown: currentMarkdown.value
    })
    currentMarkdown.value = report.contentMarkdown
    aiPolishJob.value = null
    draftReady.value = false
    draftMode.value = ''
    draftMessage.value = '当前是周报草稿，可在 Markdown 中修改，确认保存后才会进入历史周报。'
    activeTab.value = 'preview'
    ElMessage.success('周报已保存')
    await load()
  } finally {
    saving.value = false
  }
}

function openHistory(report: WeeklyReport) {
  currentMarkdown.value = report.contentMarkdown
  draftReady.value = false
  draftMode.value = ''
  aiPolishJob.value = null
  activeTab.value = 'preview'
  historyOpen.value = false
}

function parseAiReportMarkdown(job: AiJob) {
  if (job.status !== 'SUCCESS' || !job.outputJson) {
    throw new Error(job.errorMessage || 'AI 优化周报失败')
  }
  const parsed = JSON.parse(job.outputJson) as { contentMarkdown?: unknown }
  const contentMarkdown = typeof parsed.contentMarkdown === 'string' ? parsed.contentMarkdown : ''
  if (!contentMarkdown.trim()) {
    throw new Error('AI 返回的周报内容为空')
  }
  return contentMarkdown
}

async function copyMarkdown() {
  await navigator.clipboard.writeText(currentMarkdown.value)
  ElMessage.success('Markdown 已复制')
}

onMounted(load)
</script>

<style scoped>
.reports-page {
  display: grid;
  gap: 22px;
}

.reports-hero {
  position: relative;
  min-height: 220px;
  overflow: hidden;
  border-radius: 8px;
}

.reports-hero-copy {
  position: relative;
  z-index: 2;
  padding-top: 26px;
}

.reports-hero-copy p {
  margin: 0 0 22px;
  color: #1b75ff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.reports-hero-copy h1 {
  margin: 0;
  color: #0e1f38;
  font-size: clamp(40px, 5vw, 58px);
  line-height: 1;
  letter-spacing: 0;
}

.reports-hero-copy span {
  display: block;
  margin-top: 26px;
  color: #536176;
  font-size: 22px;
  font-weight: 650;
}

.reports-actions {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 0;
  display: flex;
  gap: 12px;
}

.reports-actions :deep(.el-button) {
  height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.reports-art {
  position: absolute;
  inset: 0 0 0 48%;
  pointer-events: none;
}

.report-page {
  position: absolute;
  width: 150px;
  height: 112px;
  border-radius: 10px;
  background: linear-gradient(145deg, rgba(235, 241, 255, 0.95), rgba(116, 151, 244, 0.32));
  box-shadow: 0 24px 46px rgba(27, 117, 255, 0.13);
}

.page-a {
  right: 210px;
  top: 42px;
  transform: rotate(-12deg);
}

.page-b {
  right: 80px;
  bottom: 38px;
  background: linear-gradient(145deg, rgba(232, 248, 239, 0.95), rgba(42, 185, 126, 0.22));
  transform: rotate(18deg);
}

.report-chart {
  position: absolute;
  right: 0;
  top: 72px;
  display: flex;
  align-items: end;
  gap: 10px;
  width: 126px;
  height: 82px;
  border: 5px solid rgba(255, 255, 255, 0.72);
  border-radius: 10px;
  background: rgba(232, 241, 255, 0.52);
  padding: 14px;
  transform: rotate(14deg);
}

.report-chart i {
  width: 16px;
  border-radius: 999px 999px 0 0;
  background: #1b75ff;
}

.report-chart i:nth-child(1) {
  height: 28px;
}

.report-chart i:nth-child(2) {
  height: 46px;
}

.report-chart i:nth-child(3) {
  height: 58px;
}

.report-composer-card,
.report-preview-card,
.reports-history-card {
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  padding: 22px;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
}

.report-card-head {
  margin-bottom: 18px;
}

.report-card-head.inline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.report-card-head span {
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.report-card-head h2 {
  margin: 6px 0 0;
  color: #0e1f38;
  font-size: 24px;
  font-weight: 900;
}

.report-card-head p {
  margin: 10px 0 0;
  color: #59687d;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.55;
}

.report-stepper {
  display: grid;
  grid-template-columns: minmax(120px, auto) minmax(36px, 1fr) minmax(120px, auto) minmax(36px, 1fr) minmax(120px, auto);
  align-items: center;
  gap: 12px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #f8fbff;
  padding: 14px;
  margin-bottom: 16px;
}

.report-step {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #687991;
  font-weight: 900;
}

.report-step span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #e8eef8;
  color: #5f718b;
  font-size: 14px;
  letter-spacing: 0;
}

.report-step strong {
  white-space: nowrap;
}

.report-step.active {
  color: #0e1f38;
}

.report-step.active span,
.report-step.done span {
  background: #1b75ff;
  color: #ffffff;
}

.report-step.done {
  color: #0f766e;
}

.report-step-line {
  height: 2px;
  border-radius: 999px;
  background: #dce6f4;
}

.report-step-line.done {
  background: #1b75ff;
}

.report-controls {
  display: grid;
  grid-template-columns: minmax(320px, 0.8fr) minmax(520px, 1.2fr);
  gap: 14px;
  align-items: end;
}

.report-date-grid,
.report-flow-actions {
  display: grid;
  gap: 12px;
}

.report-date-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 0;
}

.report-flow-actions {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 0;
}

.report-date-grid :deep(.el-date-editor) {
  width: 100%;
}

.report-date-grid :deep(.el-input__wrapper),
.report-flow-actions :deep(.el-button) {
  min-height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.report-flow-actions :deep(.el-button) {
  width: 100%;
  justify-content: center;
  margin-left: 0 !important;
}

.draft-state {
  display: grid;
  gap: 6px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #f8fbff;
  padding: 16px;
  margin-top: 16px;
}

.draft-state.auto {
  border-color: #bcd6ff;
  background: #f2f7ff;
}

.draft-state.ai {
  border-color: #9fe0cf;
  background: #f0fbf7;
}

.draft-state strong {
  color: #10213c;
  font-size: 16px;
  font-weight: 900;
}

.draft-state span {
  color: #59687d;
  font-weight: 650;
  line-height: 1.45;
}

.draft-state small {
  color: #7a8ba3;
  font-size: 13px;
  font-weight: 750;
}

.report-preview-card :deep(.el-tabs__item) {
  font-weight: 850;
}

.report-preview-card :deep(.markdown-source),
.report-preview-card :deep(.markdown-preview) {
  min-height: 520px;
  border-radius: 10px;
}

.reports-table :deep(.el-table__header th) {
  height: 58px;
  background: #fbfdff;
  color: #74829a;
  font-weight: 850;
}

.reports-table :deep(.el-table__row td) {
  height: 68px;
}

@media (max-width: 1180px) {
  .report-controls {
    grid-template-columns: 1fr;
  }

  .reports-art {
    opacity: 0.35;
  }
}

@media (max-width: 760px) {
  .reports-hero {
    min-height: 292px;
  }

  .reports-actions {
    position: relative;
    margin-top: 20px;
    flex-wrap: wrap;
  }

  .reports-art {
    display: none;
  }

  .report-card-head.inline {
    display: grid;
  }

  .report-date-grid,
  .report-flow-actions,
  .report-stepper {
    grid-template-columns: 1fr;
  }

  .report-step-line {
    display: none;
  }
}
</style>
