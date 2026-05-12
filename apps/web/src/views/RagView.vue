<template>
  <div class="rag-page">
    <section class="rag-hero">
      <div>
        <p>KNOWLEDGE BASE</p>
        <h1>NIDS RAG Assistant</h1>
        <span>{{ projectStore.currentProject?.name || '当前项目' }}</span>
      </div>
      <div class="rag-hero-actions">
        <el-button size="large" :icon="FolderOpened" @click="router.push(`/projects/${projectId}`)">项目工作台</el-button>
        <el-button size="large" type="primary" :icon="Refresh" :loading="indexing" @click="indexKnowledge">索引知识库</el-button>
      </div>
      <div class="rag-hero-card">
        <strong>{{ chunkCount }}</strong>
        <span>知识片段</span>
        <small>{{ lastIndexedText }}</small>
      </div>
    </section>

    <section class="rag-grid">
      <div class="rag-chat-panel">
        <div class="rag-panel-head">
          <div>
            <span>ASK</span>
            <h2>项目内研究问答</h2>
          </div>
          <el-tag effect="light">Papers + Experiments</el-tag>
        </div>

        <div class="rag-question-box">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="例如：GNN 在 NIDS 里的主要优势和瓶颈是什么？"
            @keydown.meta.enter="ask"
            @keydown.ctrl.enter="ask"
          />
          <div class="rag-question-actions">
            <el-button :icon="Search" :loading="searching" @click="searchOnly">只检索来源</el-button>
            <el-button type="primary" :icon="ChatDotRound" :loading="asking" @click="ask">RAG 问答</el-button>
          </div>
        </div>

        <EmptyState
          v-if="!answerMarkdown && !loading"
          title="先索引，再提问"
          description="索引会把当前项目的论文、AI 论文卡片、PDF 正文和实验记录切成可检索片段。"
        />
        <div v-else-if="answerMarkdown" class="rag-answer" v-html="renderedAnswer" />
      </div>

      <aside class="rag-source-panel">
        <div class="rag-panel-head compact">
          <div>
            <span>SOURCES</span>
            <h2>检索来源</h2>
          </div>
          <el-button link type="primary" :loading="loading" @click="loadChunks">刷新</el-button>
        </div>

        <EmptyState v-if="!contexts.length" title="暂无来源" description="提问后会显示最相关的项目知识片段。" />
        <div v-else class="rag-source-list">
          <article v-for="(context, index) in contexts" :key="context.id || `${context.sourceType}-${index}`" class="rag-source-card">
            <div class="rag-source-meta">
              <span>{{ index + 1 }}</span>
              <el-tag size="small" effect="plain">{{ context.sourceType }}</el-tag>
              <small>{{ formatScore(context.score) }}</small>
            </div>
            <h3>{{ context.sourceTitle }}</h3>
            <p>{{ context.contentText }}</p>
          </article>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, FolderOpened, Refresh, Search } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { askRag, indexProjectRag, listRagChunks, searchRag } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import { useProjectStore } from '../stores/project'
import type { RagChunk } from '../types'
import { formatDateTime } from '../utils/datetime'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const projectId = route.params.projectId as string
const md = new MarkdownIt()

const loading = ref(false)
const indexing = ref(false)
const searching = ref(false)
const asking = ref(false)
const question = ref('GNN 在 NIDS 里的主要优势和瓶颈是什么？')
const answerMarkdown = ref('')
const contexts = ref<RagChunk[]>([])
const chunks = ref<RagChunk[]>([])
const lastIndexedAt = ref('')

const chunkCount = computed(() => chunks.value.length)
const renderedAnswer = computed(() => md.render(answerMarkdown.value || ''))
const lastIndexedText = computed(() => lastIndexedAt.value ? `刚刚索引于 ${formatDateTime(lastIndexedAt.value)}` : '使用当前项目数据')

async function loadChunks() {
  loading.value = true
  try {
    chunks.value = await listRagChunks(projectId)
  } finally {
    loading.value = false
  }
}

async function indexKnowledge() {
  indexing.value = true
  try {
    const result = await indexProjectRag(projectId)
    lastIndexedAt.value = result.indexedAt
    await loadChunks()
    ElMessage.success(`已索引 ${result.chunkCount} 个知识片段`)
  } finally {
    indexing.value = false
  }
}

async function searchOnly() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  searching.value = true
  try {
    const result = await searchRag(projectId, question.value.trim())
    contexts.value = result.results
    answerMarkdown.value = ''
  } finally {
    searching.value = false
  }
}

async function ask() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  asking.value = true
  try {
    const result = await askRag(projectId, question.value.trim())
    answerMarkdown.value = result.answerMarkdown
    contexts.value = result.contexts
  } finally {
    asking.value = false
  }
}

function formatScore(score: number) {
  return `score ${(score || 0).toFixed(3)}`
}

onMounted(async () => {
  await projectStore.load(projectId)
  await loadChunks()
})
</script>

<style scoped>
.rag-page {
  display: grid;
  gap: 22px;
}

.rag-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 20px;
  min-height: 230px;
  overflow: hidden;
  border: 1px solid #d9e5f5;
  border-radius: 8px;
  padding: 34px;
  background:
    linear-gradient(120deg, rgba(219, 234, 254, 0.92), rgba(240, 253, 250, 0.86)),
    #f8fafc;
  box-shadow: 0 24px 60px rgba(15, 33, 66, 0.08);
}

.rag-hero p,
.rag-panel-head span {
  margin: 0 0 12px;
  color: #0f766e;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.18em;
}

.rag-hero h1 {
  margin: 0;
  color: #0f1f38;
  font-size: clamp(38px, 5vw, 64px);
  line-height: 1;
  letter-spacing: 0;
}

.rag-hero span {
  display: block;
  margin-top: 18px;
  color: #526176;
  font-size: 20px;
  font-weight: 800;
}

.rag-hero-actions {
  display: flex;
  gap: 12px;
}

.rag-hero-actions :deep(.el-button) {
  height: 48px;
  border-radius: 8px;
  font-weight: 850;
}

.rag-hero-card {
  position: absolute;
  right: 34px;
  bottom: 28px;
  width: min(320px, calc(100% - 68px));
  border: 1px solid rgba(15, 118, 110, 0.18);
  border-radius: 8px;
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 38px rgba(15, 33, 66, 0.1);
}

.rag-hero-card strong,
.rag-hero-card span,
.rag-hero-card small {
  display: block;
}

.rag-hero-card strong {
  color: #0f766e;
  font-size: 42px;
  line-height: 1;
}

.rag-hero-card span {
  margin-top: 8px;
  color: #0f1f38;
  font-size: 18px;
  font-weight: 900;
}

.rag-hero-card small {
  margin-top: 8px;
  color: #64748b;
  font-weight: 750;
}

.rag-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 22px;
  align-items: start;
}

.rag-chat-panel,
.rag-source-panel {
  border: 1px solid #d9e5f5;
  border-radius: 8px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 48px rgba(15, 33, 66, 0.06);
}

.rag-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.rag-panel-head h2 {
  margin: 0;
  color: #0f1f38;
  font-size: 28px;
  letter-spacing: 0;
}

.rag-panel-head.compact h2 {
  font-size: 24px;
}

.rag-question-box {
  display: grid;
  gap: 14px;
  margin-bottom: 22px;
}

.rag-question-box :deep(.el-textarea__inner) {
  border-radius: 8px;
  padding: 18px;
  font-size: 17px;
  font-weight: 650;
  line-height: 1.65;
  box-shadow: none;
}

.rag-question-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.rag-question-actions :deep(.el-button) {
  min-height: 44px;
  border-radius: 8px;
  font-weight: 850;
}

.rag-answer {
  min-height: 280px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 26px;
  background: linear-gradient(180deg, #f8fbff, #ffffff);
  color: #243650;
  font-size: 17px;
  line-height: 1.85;
}

.rag-answer :deep(h2) {
  margin: 0 0 12px;
  color: #0f1f38;
  font-size: 25px;
}

.rag-source-list {
  display: grid;
  gap: 14px;
  max-height: 680px;
  overflow: auto;
  padding-right: 4px;
}

.rag-source-card {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 18px;
  background: #f8fbff;
}

.rag-source-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rag-source-meta > span {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #0f1f38;
  color: #ffffff;
  font-weight: 900;
}

.rag-source-meta small {
  margin-left: auto;
  color: #64748b;
  font-weight: 800;
}

.rag-source-card h3 {
  margin: 0 0 10px;
  color: #10213a;
  font-size: 17px;
  line-height: 1.35;
}

.rag-source-card p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #506176;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 7;
}

@media (max-width: 1180px) {
  .rag-grid,
  .rag-hero {
    grid-template-columns: 1fr;
  }

  .rag-hero-card {
    position: static;
    margin-top: 20px;
  }
}
</style>
