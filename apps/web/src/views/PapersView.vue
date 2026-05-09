<template>
  <div class="papers-page">
    <section class="papers-hero">
      <div class="papers-hero-copy">
        <p>READING LIBRARY</p>
        <h1>Papers</h1>
        <span>{{ projectStore.currentProject?.name || '当前项目' }}</span>
      </div>
      <div class="papers-actions">
        <el-button size="large" :icon="FolderOpened" @click="router.push(`/projects/${projectId}`)">项目工作台</el-button>
        <el-button size="large" type="primary" :icon="Plus" @click="openCreate">新增论文</el-button>
      </div>
      <div class="papers-art" aria-hidden="true">
        <div class="paper-sheet sheet-a"></div>
        <div class="paper-sheet sheet-b"></div>
        <div class="paper-node"></div>
        <div class="paper-line"></div>
      </div>
    </section>

    <section class="papers-metrics">
      <article class="paper-metric">
        <span><el-icon><Reading /></el-icon></span>
        <div>
          <strong>{{ papers.length }}</strong>
          <small>论文记录</small>
        </div>
      </article>
      <article class="paper-metric green">
        <span><el-icon><Document /></el-icon></span>
        <div>
          <strong>{{ papersWithPdf }}</strong>
          <small>已关联 PDF</small>
        </div>
      </article>
      <article class="paper-metric purple">
        <span><el-icon><MagicStick /></el-icon></span>
        <div>
          <strong>{{ papers.length - papersWithPdf }}</strong>
          <small>待补充 PDF</small>
        </div>
      </article>
    </section>

    <section class="papers-toolbar">
      <el-input v-model="query" clearable placeholder="搜索标题、作者、标签、claim 或笔记" size="large">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button size="large" :icon="Refresh" :loading="loading" @click="load" />
    </section>

    <EmptyState
      v-if="!loading && filteredPapers.length === 0"
      :title="emptyTitle"
      :description="emptyDescription"
    >
      <el-button v-if="!query.trim()" type="primary" @click="openCreate">新增论文</el-button>
    </EmptyState>

    <section v-else v-loading="loading" class="paper-library">
      <article
        v-for="paper in filteredPapers"
        :id="`paper-${paper.id}`"
        :key="paper.id"
        class="paper-item"
        :class="{ highlighted: highlightedPaperId === paper.id }"
      >
        <div class="paper-main">
          <div class="paper-title-row">
            <h2>{{ paper.title }}</h2>
            <el-tag v-if="paper.publishedDate" effect="light" round>{{ paper.publishedDate }}</el-tag>
          </div>
          <div class="paper-meta">
            <span>{{ paper.authors || '作者未记录' }}</span>
            <span v-if="paper.publication" class="paper-publication" :title="paper.publication">{{ paper.publication }}</span>
            <a v-if="paper.url" :href="paper.url" target="_blank" rel="noreferrer">
              <el-icon><Link /></el-icon>
              原文链接
            </a>
          </div>
          <div v-if="splitTags(paper.tags).length" class="paper-tags">
            <el-tag v-for="tag in splitTags(paper.tags)" :key="tag" effect="plain" round>{{ tag }}</el-tag>
          </div>
          <p v-if="paper.keyClaims" class="paper-claim">{{ paper.keyClaims }}</p>
          <p v-else class="paper-claim muted">暂无关键 claim</p>
        </div>
        <div class="paper-actions">
          <el-button v-if="paper.pdfFileId" type="primary" @click="openStoredPdf(paper.pdfFileId)">
            <el-icon><Document /></el-icon>
            查看 PDF
          </el-button>
          <el-button v-else disabled>
            <el-icon><Document /></el-icon>
            未上传
          </el-button>
          <el-button :loading="aiRunningPaperId === paper.id" @click="generatePaperCard(paper)">
            <el-icon><MagicStick /></el-icon>
            AI 卡片
          </el-button>
          <div class="paper-action-row">
            <el-button circle :icon="EditPen" aria-label="编辑论文" @click="openEdit(paper)" />
            <el-button circle :icon="DeleteIcon" type="danger" aria-label="删除论文" @click="remove(paper.id)" />
          </div>
        </div>
      </article>
    </section>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑论文' : '新增论文'" width="720px">
      <section class="recognition-panel">
        <div class="recognition-head">
          <div>
            <strong>{{ editingId ? '替换论文 PDF' : '自动识别论文信息' }}</strong>
            <p>{{ editingId ? '选择新的 PDF 后保存，会替换当前 PDF，并清理旧的 MinIO 文件。' : '拖入或选择本地 PDF，识别结果会回填标题、作者、出版物、日期和跳转链接。' }}</p>
          </div>
          <el-tag v-if="recognitionResult" effect="light" type="success">
            {{ recognitionResult.source }} · {{ Math.round(recognitionResult.confidence * 100) }}%
          </el-tag>
        </div>
        <el-tabs v-model="recognitionTab">
          <el-tab-pane label="PDF 文件" name="pdf">
            <div
              class="pdf-drop-zone"
              :class="{ dragging: pdfDragging }"
              @dragenter.prevent="pdfDragging = true"
              @dragover.prevent="pdfDragging = true"
              @dragleave.prevent="pdfDragging = false"
              @drop.prevent="recognizePdfFromDrop"
            >
              <strong>{{ localPdfName || '拖入 PDF 到这里' }}</strong>
              <span>{{ editingId ? '保存后会上传新 PDF，并替换这篇论文当前关联的 PDF。' : '或选择文件后自动识别。保存论文时会自动上传到 MinIO，之后列表里可长期查看。' }}</span>
              <input type="file" accept="application/pdf" @change="recognizePdfFromInput" />
              <a v-if="localPdfUrl" class="local-pdf-link" :href="localPdfUrl" target="_blank" rel="noreferrer">临时预览本地 PDF</a>
            </div>
          </el-tab-pane>
          <el-tab-pane label="Zotero 文本" name="zotero">
            <el-input
              v-model="zoteroContent"
              type="textarea"
              :rows="5"
              placeholder="从 Zotero 导出 BibTeX/RIS 后粘贴到这里"
            />
            <div class="recognition-actions">
              <el-button :loading="recognizing" @click="recognizeZoteroText">识别 Zotero 文本</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
        <el-alert v-if="recognitionResult?.warning" :title="recognitionResult.warning" type="warning" show-icon :closable="false" />
      </section>

      <el-form ref="paperFormRef" label-position="top">
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="作者"><el-input v-model="form.authors" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="标签"><el-input v-model="form.tags" placeholder="rag,survey,llm" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="出版物"><el-input v-model="form.publication" placeholder="NeurIPS / Nature / arXiv" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="日期"><el-input v-model="form.publishedDate" placeholder="YYYY / YYYY-MM-DD" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="URL"><el-input v-model="form.url" /></el-form-item>
        <el-form-item label="关键 claim"><el-input v-model="form.keyClaims" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="笔记"><el-input v-model="form.notes" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiDialogOpen" class="ai-card-dialog" width="860px" align-center>
      <template #header>
        <div class="ai-dialog-title">
          <span class="ai-dialog-mark"><el-icon><MagicStick /></el-icon></span>
          <div>
            <strong>AI 论文卡片</strong>
            <small>{{ aiJobStatusCaption }}</small>
          </div>
        </div>
      </template>

      <div v-if="selectedAiPaper" class="ai-card-hero">
        <div class="ai-card-kicker">
          <span>Paper</span>
          <span v-if="selectedAiPaper.publishedDate">{{ selectedAiPaper.publishedDate }}</span>
          <span v-if="selectedAiPaper.publication">{{ selectedAiPaper.publication }}</span>
        </div>
        <h2>{{ selectedAiPaper.title }}</h2>
        <p>{{ selectedAiPaper.authors || '作者未记录' }}</p>
        <div v-if="paperCard" class="ai-card-metrics">
          <span>{{ paperCard.contributions.length }} 个贡献点</span>
          <span>{{ paperCard.limitations.length }} 个局限性</span>
          <span v-if="aiCardGeneratedAt">{{ aiCardGeneratedAt }}</span>
        </div>
      </div>

      <div v-if="aiJob" class="ai-job-status" :class="aiJob.status.toLowerCase()">
        <div class="ai-job-status-row">
          <el-tag effect="light" :type="aiJobTagType">{{ aiJobStatusLabel }}</el-tag>
          <strong>{{ aiJobSourceLabel }}</strong>
          <span v-if="aiJob.updatedAt">{{ formatDateTime(aiJob.updatedAt) }}</span>
        </div>
        <div v-if="aiJob.status === 'PENDING' || aiJob.status === 'RUNNING'" class="ai-job-progress">
          <i></i>
        </div>
        <p v-if="aiJob.status === 'FAILED'">{{ aiJob.errorMessage || 'AI 任务失败，可以点击重新生成。' }}</p>
      </div>

      <el-alert
        v-if="aiJob?.status === 'FAILED'"
        :title="aiJob.errorMessage || 'AI 任务执行失败'"
        type="error"
        show-icon
        :closable="false"
      />
      <section v-else-if="paperCard" class="ai-paper-card">
        <article class="ai-summary-card">
          <span class="section-label">Summary</span>
          <h3>摘要</h3>
          <p>{{ paperCard.summary || '暂无摘要' }}</p>
        </article>

        <div class="ai-section-grid">
          <article class="ai-insight-panel strong">
            <div class="ai-section-head">
              <span class="section-label">Contributions</span>
              <strong>主要贡献</strong>
            </div>
            <ol v-if="paperCard.contributions.length" class="ai-number-list">
              <li v-for="(item, index) in paperCard.contributions" :key="item">
                <span>{{ index + 1 }}</span>
                <p>{{ item }}</p>
              </li>
            </ol>
            <p v-else class="ai-empty-text">暂无贡献总结</p>
          </article>

          <article class="ai-insight-panel">
            <div class="ai-section-head">
              <span class="section-label">Limitations</span>
              <strong>局限性</strong>
            </div>
            <ul v-if="paperCard.limitations.length" class="ai-dot-list">
              <li v-for="item in paperCard.limitations" :key="item">{{ item }}</li>
            </ul>
            <p v-else class="ai-empty-text">暂无局限性总结</p>
          </article>
        </div>
      </section>
      <EmptyState v-else title="暂无 AI 结果" description="点击论文列表中的 AI 卡片生成。请确认 AI 服务已启动。" />
      <template #footer>
        <el-button v-if="selectedAiPaper" :loading="aiRunningPaperId === selectedAiPaper.id" :icon="MagicStick" @click="regeneratePaperCard">
          重新生成
        </el-button>
        <el-button type="primary" @click="aiDialogOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete as DeleteIcon, Document, EditPen, FolderOpened, Link, MagicStick, Plus, Reading, Refresh, Search } from '@element-plus/icons-vue'
import { createAiJob, createPaper, deletePaper, fileInlineUrl, formatAiJobError, getLatestAiJobAny, listPapers, recognizePaperPdf, recognizePaperZotero, updatePaper, uploadPaperPdf, waitForAiJobCompletion } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import { useProjectStore } from '../stores/project'
import type { AiJob, Paper, PaperCard, PaperRecognition } from '../types'
import { formatDateTime } from '../utils/datetime'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const projectId = route.params.projectId as string
const papers = ref<Paper[]>([])
const query = ref('')
const loading = ref(false)
const saving = ref(false)
const recognizing = ref(false)
const dialogOpen = ref(false)
const editingId = ref<string | null>(null)
const recognitionTab = ref('pdf')
const recognitionResult = ref<PaperRecognition | null>(null)
const pdfDragging = ref(false)
const localPdfUrl = ref('')
const localPdfName = ref('')
const selectedPdfFile = ref<File | null>(null)
const zoteroContent = ref('')
const aiDialogOpen = ref(false)
const aiRunningPaperId = ref<string | null>(null)
const selectedAiPaper = ref<Paper | null>(null)
const aiJob = ref<AiJob | null>(null)
const paperCard = ref<PaperCard | null>(null)
const aiResultSource = ref<'cache' | 'new' | ''>('')
const paperFormRef = ref<{ $el?: HTMLElement } | null>(null)
const highlightedPaperId = ref<string | null>(null)
const form = reactive({ title: '', authors: '', url: '', tags: '', publication: '', publishedDate: '', keyClaims: '', notes: '' })

const papersWithPdf = computed(() => papers.value.filter((paper) => paper.pdfFileId).length)
const emptyTitle = computed(() => query.value.trim() ? '没有匹配的论文' : '还没有论文记录')
const emptyDescription = computed(() => query.value.trim() ? '换一个关键词，或清空搜索条件后再看全部论文。' : '把关键论文、claim 和笔记记录下来，后续周报会更完整。')
const aiCardGeneratedAt = computed(() => aiJob.value?.updatedAt ? `生成于 ${formatDateTime(aiJob.value.updatedAt)}` : '')
const aiJobStatusCaption = computed(() => {
  if (!aiJob.value) return 'Paper insight'
  if (aiJob.value.status === 'SUCCESS') return '已缓存到 AI Jobs'
  if (aiJob.value.status === 'FAILED') return '生成失败，可重试'
  return '后台生成中'
})
const aiJobStatusLabel = computed(() => {
  const status = aiJob.value?.status
  if (status === 'PENDING') return '等待中'
  if (status === 'RUNNING') return '生成中'
  if (status === 'SUCCESS') return '已完成'
  if (status === 'FAILED') return '失败'
  return '未生成'
})
const aiJobTagType = computed(() => {
  const status = aiJob.value?.status
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
})
const aiJobSourceLabel = computed(() => {
  if (!aiJob.value) return ''
  if (aiResultSource.value === 'new') return '本次新任务'
  if (aiJob.value.status === 'SUCCESS') return '缓存结果'
  return '最近一次任务'
})

const filteredPapers = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return papers.value
  return papers.value.filter((paper) =>
    [paper.title, paper.authors, paper.tags, paper.publication, paper.publishedDate, paper.keyClaims, paper.notes].some((value) => value?.toLowerCase().includes(keyword))
  )
})

function splitTags(tags?: string | null) {
  return (tags ?? '')
    .split(/[,;，；]/)
    .map((tag) => tag.replace(/^["']|["']$/g, '').trim())
    .filter(Boolean)
    .slice(0, 8)
}

function reset() {
  Object.assign(form, { title: '', authors: '', url: '', tags: '', publication: '', publishedDate: '', keyClaims: '', notes: '' })
  recognitionResult.value = null
  zoteroContent.value = ''
  recognitionTab.value = 'pdf'
  revokeLocalPdfUrl()
  localPdfName.value = ''
  selectedPdfFile.value = null
}

function openCreate() {
  editingId.value = null
  reset()
  dialogOpen.value = true
}

function openEdit(paper: Paper) {
  editingId.value = paper.id
  reset()
  Object.assign(form, {
    title: paper.title,
    authors: paper.authors ?? '',
    url: paper.url ?? '',
    tags: paper.tags ?? '',
    publication: paper.publication ?? '',
    publishedDate: paper.publishedDate ?? '',
    keyClaims: paper.keyClaims ?? '',
    notes: paper.notes ?? ''
  })
  dialogOpen.value = true
}

async function load() {
  loading.value = true
  try {
    await projectStore.load(projectId)
    papers.value = await listPapers(projectId)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写论文标题')
    return
  }
  saving.value = true
  try {
    let savedPaperId = editingId.value
    if (editingId.value) {
      const paper = await updatePaper(editingId.value, form)
      savedPaperId = paper.id
      if (selectedPdfFile.value) {
        const uploaded = await uploadPaperPdf(editingId.value, selectedPdfFile.value)
        savedPaperId = uploaded.id
      }
    } else {
      const paper = await createPaper(projectId, form)
      savedPaperId = paper.id
      if (selectedPdfFile.value) {
        const uploaded = await uploadPaperPdf(paper.id, selectedPdfFile.value)
        savedPaperId = uploaded.id
      }
    }
    ElMessage.success('论文已保存')
    dialogOpen.value = false
    query.value = ''
    await load()
    if (savedPaperId) {
      await scrollToPaper(savedPaperId)
    }
  } finally {
    saving.value = false
  }
}

async function recognizePdfFromInput(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await recognizePdfFile(file)
  input.value = ''
}

async function recognizePdfFromDrop(event: DragEvent) {
  pdfDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (!file) return
  await recognizePdfFile(file)
}

async function recognizePdfFile(file: File) {
  if (file.type && file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.warning('请选择 PDF 文件')
    return
  }
  setLocalPdfLink(file)
  recognizing.value = true
  try {
    const result = await recognizePaperPdf(projectId, file)
    applyRecognition(result)
    await scrollToPaperForm()
    ElMessage.success('PDF 识别完成，请核对后保存')
  } finally {
    recognizing.value = false
  }
}

async function recognizeZoteroText() {
  if (!zoteroContent.value.trim()) {
    ElMessage.warning('请粘贴 Zotero 导出的 BibTeX 或 RIS 文本')
    return
  }
  recognizing.value = true
  try {
    const result = await recognizePaperZotero(projectId, zoteroContent.value)
    applyRecognition(result)
    await scrollToPaperForm()
    ElMessage.success('Zotero 文本识别完成，请核对后保存')
  } finally {
    recognizing.value = false
  }
}

function applyRecognition(result: PaperRecognition) {
  recognitionResult.value = result
  form.title = result.title || form.title
  form.authors = result.authors || form.authors
  form.url = result.url || form.url
  form.tags = result.tags || form.tags
  form.publication = result.publication || form.publication
  form.publishedDate = result.publishedDate || form.publishedDate
  form.keyClaims = result.keyClaims || form.keyClaims
  form.notes = result.notes || form.notes
}

async function scrollToPaperForm() {
  await nextTick()
  paperFormRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function scrollToPaper(paperId: string) {
  highlightedPaperId.value = paperId
  await nextTick()
  document.getElementById(`paper-${paperId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  window.setTimeout(() => {
    if (highlightedPaperId.value === paperId) {
      highlightedPaperId.value = null
    }
  }, 2200)
}

function setLocalPdfLink(file: File) {
  revokeLocalPdfUrl()
  localPdfName.value = file.name
  selectedPdfFile.value = file
  localPdfUrl.value = URL.createObjectURL(file)
}

function revokeLocalPdfUrl() {
  if (localPdfUrl.value) {
    URL.revokeObjectURL(localPdfUrl.value)
    localPdfUrl.value = ''
  }
}

function openStoredPdf(fileId: string) {
  window.open(fileInlineUrl(fileId), '_blank', 'noopener,noreferrer')
}

async function generatePaperCard(paper: Paper) {
  selectedAiPaper.value = paper
  paperCard.value = null
  aiJob.value = null
  aiResultSource.value = ''
  aiDialogOpen.value = true
  aiRunningPaperId.value = paper.id
  try {
    const latest = await getLatestAiJobAny({ targetType: 'PAPER', targetId: paper.id, jobType: 'paper-card' })
    aiResultSource.value = 'cache'
    applyAiJobResult(latest)
    if (latest.status === 'PENDING' || latest.status === 'RUNNING') {
      const finished = await waitForAiJobCompletion(latest)
      applyAiJobResult(finished)
    } else if (latest.status === 'SUCCESS') {
      ElMessage.success('已加载缓存的 AI 论文卡片')
    }
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      await runPaperCardJob(paper)
    } else {
      ElMessage.error(error instanceof Error ? error.message : 'AI 任务执行失败')
    }
  } finally {
    aiRunningPaperId.value = null
  }
}

async function regeneratePaperCard() {
  if (!selectedAiPaper.value) return
  aiRunningPaperId.value = selectedAiPaper.value.id
  paperCard.value = null
  aiJob.value = null
  aiResultSource.value = ''
  try {
    await runPaperCardJob(selectedAiPaper.value)
  } finally {
    aiRunningPaperId.value = null
  }
}

async function runPaperCardJob(paper: Paper) {
  try {
    const createdJob = await createAiJob({
      projectId,
      targetType: 'PAPER',
      targetId: paper.id,
      jobType: 'paper-card',
      inputJson: JSON.stringify({
        paperId: paper.id,
        title: paper.title,
        abstract: paper.keyClaims || '',
        notes: [
          paper.notes,
          paper.authors ? `Authors: ${paper.authors}` : '',
          paper.publication ? `Publication: ${paper.publication}` : '',
          paper.publishedDate ? `Published: ${paper.publishedDate}` : '',
          paper.tags ? `Keywords: ${paper.tags}` : '',
          paper.url ? `URL: ${paper.url}` : ''
        ].filter(Boolean).join('\n')
      })
    })
    aiJob.value = createdJob
    aiResultSource.value = 'new'
    const job = await waitForAiJobCompletion(createdJob)
    applyAiJobResult(job)
    if (job.status === 'SUCCESS') {
      ElMessage.success('AI 论文卡片已生成')
    }
  } catch (error) {
    ElMessage.error(formatAiJobError(error, 'AI 任务执行失败'))
  }
}

function applyAiJobResult(job: AiJob) {
  aiJob.value = job
  if (job.status === 'SUCCESS' && job.outputJson) {
    paperCard.value = normalizePaperCard(JSON.parse(job.outputJson))
  } else if (job.status === 'FAILED') {
    ElMessage.error(job.errorMessage || 'AI 任务执行失败')
  }
}

function normalizePaperCard(value: Partial<PaperCard>): PaperCard {
  return {
    summary: value.summary ?? '',
    contributions: Array.isArray(value.contributions) ? value.contributions.map(String) : [],
    limitations: Array.isArray(value.limitations) ? value.limitations.map(String) : [],
    citations: Array.isArray(value.citations) ? value.citations.map(String) : []
  }
}

async function remove(paperId: string) {
  await ElMessageBox.confirm('确定删除这条论文记录吗？', '删除确认', { type: 'warning' })
  await deletePaper(paperId)
  ElMessage.success('论文已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.papers-page {
  display: grid;
  gap: 22px;
}

.papers-hero {
  position: relative;
  min-height: 220px;
  overflow: hidden;
  border-radius: 8px;
}

.papers-hero-copy {
  position: relative;
  z-index: 2;
  padding-top: 26px;
}

.papers-hero-copy p {
  margin: 0 0 22px;
  color: #1b75ff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.papers-hero-copy h1 {
  margin: 0;
  color: #0e1f38;
  font-size: clamp(40px, 5vw, 58px);
  line-height: 1;
  letter-spacing: 0;
}

.papers-hero-copy span {
  display: block;
  margin-top: 26px;
  color: #536176;
  font-size: 22px;
  font-weight: 650;
}

.papers-actions {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 0;
  display: flex;
  gap: 12px;
}

.papers-actions :deep(.el-button) {
  height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.papers-art {
  position: absolute;
  inset: 0 0 0 46%;
  pointer-events: none;
}

.paper-sheet {
  position: absolute;
  width: 150px;
  height: 106px;
  border-radius: 10px;
  background: linear-gradient(145deg, rgba(228, 237, 255, 0.95), rgba(112, 151, 245, 0.34));
  box-shadow: 0 24px 46px rgba(27, 117, 255, 0.14);
}

.sheet-a {
  right: 180px;
  top: 54px;
  transform: rotate(-16deg);
}

.sheet-b {
  right: 38px;
  bottom: 28px;
  transform: rotate(18deg);
}

.paper-node {
  position: absolute;
  top: 78px;
  left: 260px;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  background: #1b75ff;
  box-shadow: 0 14px 34px rgba(27, 117, 255, 0.28);
}

.paper-line {
  position: absolute;
  top: 116px;
  left: 80px;
  width: 520px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(27, 117, 255, 0.2), transparent);
  transform: rotate(-23deg);
}

.papers-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.paper-metric {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 16px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #ffffff;
  padding: 22px;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
}

.paper-metric > span {
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

.paper-metric.green > span {
  background: #dff8ef;
  color: #0f9f76;
}

.paper-metric.purple > span {
  background: #f0e7ff;
  color: #7c3cff;
}

.paper-metric strong {
  display: block;
  color: #0e1f38;
  font-size: 38px;
  line-height: 1;
  font-weight: 900;
}

.paper-metric small {
  display: block;
  margin-top: 8px;
  color: #687991;
  font-size: 15px;
  font-weight: 800;
}

.papers-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px;
  gap: 16px;
}

.papers-toolbar :deep(.el-input__wrapper),
.papers-toolbar :deep(.el-button) {
  min-height: 54px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dce6f4 inset;
}

.paper-library {
  gap: 16px;
}

.paper-library :deep(.paper-item),
.paper-item {
  border-radius: 10px;
  padding: 22px;
  box-shadow: 0 16px 34px rgba(28, 55, 98, 0.08);
  transition: border-color 180ms ease, background 180ms ease;
}

.paper-item:hover {
  border-color: #1b75ff;
  background: #fbfdff;
}

.paper-title-row h2 {
  font-size: 21px;
  line-height: 1.35;
}

.paper-meta {
  gap: 10px 16px;
}

.paper-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 800;
}

.recognition-panel {
  border-radius: 10px;
}

.ai-card-dialog :deep(.el-dialog) {
  border-radius: 12px;
}

.ai-job-status {
  display: grid;
  gap: 10px;
  margin: 16px 0;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #f8fbff;
  padding: 14px 16px;
}

.ai-job-status.success {
  border-color: #bcebdc;
  background: #f1fbf7;
}

.ai-job-status.failed {
  border-color: #ffd2d2;
  background: #fff5f5;
}

.ai-job-status.running,
.ai-job-status.pending {
  border-color: #c7dbff;
  background: #f3f8ff;
}

.ai-job-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-job-status-row strong {
  color: #10213c;
  font-weight: 900;
}

.ai-job-status-row span {
  color: #687991;
  font-size: 13px;
  font-weight: 750;
}

.ai-job-status p {
  margin: 0;
  color: #d94141;
  font-weight: 750;
  line-height: 1.5;
}

.ai-job-progress {
  position: relative;
  overflow: hidden;
  height: 8px;
  border-radius: 999px;
  background: #dce9ff;
}

.ai-job-progress i {
  position: absolute;
  inset: 0 auto 0 0;
  width: 38%;
  border-radius: inherit;
  background: #1b75ff;
  animation: ai-job-progress 1.15s ease-in-out infinite;
}

@keyframes ai-job-progress {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(265%);
  }
}

@media (max-width: 1180px) {
  .papers-metrics {
    grid-template-columns: 1fr;
  }

  .papers-art {
    opacity: 0.3;
  }
}

@media (max-width: 760px) {
  .papers-hero {
    min-height: 290px;
  }

  .papers-actions {
    position: relative;
    margin-top: 20px;
    flex-wrap: wrap;
  }

  .papers-art {
    display: none;
  }

  .papers-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
