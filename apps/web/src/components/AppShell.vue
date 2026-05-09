<template>
  <div class="research-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="side-nav">
      <RouterLink to="/" class="side-brand" title="LabFlow">
        <img class="brand-mark" src="/assets/labflow-logo.png" alt="LabFlow logo" />
        <span class="brand-copy">
          <strong>LabFlow</strong>
          <small>科研操作系统</small>
        </span>
      </RouterLink>

      <nav class="nav-list">
        <RouterLink to="/" class="nav-link" title="仪表盘">
          <el-icon><House /></el-icon>
          <span>仪表盘</span>
        </RouterLink>

        <section class="nav-section" :class="{ active: route.path.startsWith('/projects') }">
          <RouterLink to="/projects" class="nav-link nav-parent" title="项目">
            <el-icon><Folder /></el-icon>
            <span>项目</span>
          </RouterLink>
          <div class="sub-nav" :aria-label="activeProjectId ? '当前项目二级目录' : '项目二级目录'">
            <template v-if="activeProjectId">
              <RouterLink v-for="item in projectSubItems" :key="item.label" :to="item.to" class="sub-nav-link" :title="item.title">
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </RouterLink>
            </template>
            <template v-else>
              <button v-for="item in projectSubItems" :key="item.label" class="sub-nav-link disabled" type="button" :title="item.disabledTitle" disabled>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </button>
            </template>
          </div>
        </section>
      </nav>

      <div class="side-footer">
        <button class="collapse-button" type="button" :aria-label="sidebarCollapsed ? '展开侧栏' : '收起侧栏'" @click="toggleSidebar">
          <el-icon><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
          <span>{{ sidebarCollapsed ? '展开侧栏' : '收起侧栏' }}</span>
        </button>
      </div>
    </aside>

    <div class="shell-main">
      <header class="top-bar">
        <div>
          <strong>{{ sectionLabel }}</strong>
          <span>项目、论文、实验和周报的研究推进台</span>
        </div>
        <div class="top-actions">
          <RouterLink to="/" class="top-action secondary">项目总览</RouterLink>
          <RouterLink to="/projects" class="top-action primary">管理项目</RouterLink>
        </div>
      </header>
      <main class="content-frame">
        <slot />
      </main>
      <footer class="app-footer">LabFlow Research OS</footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, Document, Expand, Fold, Folder, House, Notebook } from '@element-plus/icons-vue'
import { useProjectStore } from '../stores/project'

const route = useRoute()
const projectStore = useProjectStore()
const projectId = computed(() => route.params.projectId as string | undefined)
const activeProjectId = computed(() => projectId.value ?? projectStore.currentProject?.id)
const sidebarCollapsed = ref(false)
const sidebarStorageKey = 'labflow.sidebarCollapsed'

const projectSubItems = computed(() => [
  {
    label: '详情',
    title: '项目详情',
    disabledTitle: '先进入一个项目后可用',
    to: `/projects/${activeProjectId.value}`,
    icon: Folder
  },
  {
    label: '论文',
    title: '项目论文',
    disabledTitle: '先进入一个项目后可用',
    to: `/projects/${activeProjectId.value}/papers`,
    icon: Document
  },
  {
    label: '实验',
    title: '项目实验',
    disabledTitle: '先进入一个项目后可用',
    to: `/projects/${activeProjectId.value}/experiments`,
    icon: DataAnalysis
  },
  {
    label: '周报',
    title: '项目周报',
    disabledTitle: '先进入一个项目后可用',
    to: `/projects/${activeProjectId.value}/reports`,
    icon: Notebook
  }
])

const sectionLabel = computed(() => {
  if (route.path === '/') return '仪表盘'
  if (route.path.includes('/papers')) return '论文'
  if (route.path.includes('/experiments')) return '实验'
  if (route.path.includes('/reports')) return '周报'
  return '项目'
})

onMounted(() => {
  sidebarCollapsed.value = localStorage.getItem(sidebarStorageKey) === 'true'
})

watch(sidebarCollapsed, (value) => {
  localStorage.setItem(sidebarStorageKey, String(value))
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>
