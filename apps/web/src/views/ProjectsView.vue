<template>
  <div>
    <PageHeader title="Projects" eyebrow="Research portfolio" description="管理研究项目，并从这里进入单项目工作台。">
      <template #actions>
        <el-button type="primary" @click="openCreate">创建项目</el-button>
      </template>
    </PageHeader>

    <div class="filter-bar">
      <el-input v-model="query" clearable placeholder="搜索项目名称或描述" />
      <el-button @click="load">刷新</el-button>
    </div>

    <EmptyState
      v-if="!loading && filteredProjects.length === 0"
      title="没有匹配的项目"
      description="创建一个项目后，就可以继续记录论文、实验和周报。"
    >
      <el-button type="primary" @click="openCreate">创建项目</el-button>
    </EmptyState>

    <section v-else class="panel">
      <el-table v-loading="loading" :data="filteredProjects" border>
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/projects/${row.id}`)">{{ row.name }}</el-button>
            <div class="muted">{{ row.description || '暂无描述' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <div class="time-cell">
              <strong>{{ formatDateTime(row.createdAt) }}</strong>
              <span>{{ formatRelativeTime(row.createdAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            <div class="time-cell">
              <strong>{{ formatDateTime(row.updatedAt) }}</strong>
              <span>{{ formatRelativeTime(row.updatedAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/projects/${row.id}`)">进入</el-button>
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑项目' : '创建项目'" width="560px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="RAG Survey" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createProject, deleteProject, listProjects, updateProject } from '../api/labflow'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import type { Project } from '../types'
import { formatDateTime, formatRelativeTime } from '../utils/datetime'

const router = useRouter()
const projects = ref<Project[]>([])
const query = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ name: '', description: '' })

const filteredProjects = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return projects.value
  return projects.value.filter((project) =>
    [project.name, project.description].some((value) => value?.toLowerCase().includes(keyword))
  )
})

function reset() {
  Object.assign(form, { name: '', description: '' })
}

function openCreate() {
  editingId.value = null
  reset()
  dialogOpen.value = true
}

function openEdit(project: Project) {
  editingId.value = project.id
  Object.assign(form, { name: project.name, description: project.description ?? '' })
  dialogOpen.value = true
}

async function load() {
  loading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写项目名称')
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name, description: form.description }
    const project = editingId.value ? await updateProject(editingId.value, payload) : await createProject(payload)
    ElMessage.success('项目已保存')
    dialogOpen.value = false
    await load()
    if (!editingId.value) await router.push(`/projects/${project.id}`)
  } finally {
    saving.value = false
  }
}

async function remove(projectId: string) {
  await ElMessageBox.confirm('删除项目会同时删除它的论文、实验和周报，确定继续吗？', '删除确认', { type: 'warning' })
  await deleteProject(projectId)
  ElMessage.success('项目已删除')
  await load()
}

onMounted(load)
</script>
