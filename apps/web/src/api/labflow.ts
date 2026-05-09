import axios from 'axios'
import { apiBaseUrl, apiClient } from './client'
import type { AiJob, DashboardSummary, Experiment, ExperimentStatus, Paper, PaperRecognition, Project, StoredFile, WeeklyReport, WeeklyReportDraft } from '../types'

export interface ProjectPayload {
  name: string
  description?: string
}

export interface PaperPayload {
  title: string
  authors?: string
  url?: string
  tags?: string
  publication?: string
  publishedDate?: string
  keyClaims?: string
  notes?: string
}

export interface ExperimentPayload {
  name: string
  status: ExperimentStatus
  commitHash?: string
  repoUrl?: string
  dataset?: string
  config?: string
  metrics?: string
  conclusion?: string
  failureReason?: string
  nextStep?: string
}

const AI_JOB_CREATE_TIMEOUT_MS = 30000
const AI_JOB_POLL_INTERVAL_MS = 1500
const AI_JOB_POLL_TIMEOUT_MS = 240000

export function formatAiJobError(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = String(error.message ?? '').toLowerCase()
    if (error.code === 'ECONNABORTED' || message.includes('timeout')) {
      return 'AI 请求等待超时，但后端任务可能仍在继续。请稍后重新打开 AI 卡片或周报页面读取缓存结果。'
    }
  }
  return error instanceof Error ? error.message : fallback
}

export async function listProjects() {
  const { data } = await apiClient.get<Project[]>('/api/projects')
  return data
}

export async function getDashboard() {
  const { data } = await apiClient.get<DashboardSummary>('/api/dashboard')
  return data
}

export async function createProject(payload: ProjectPayload) {
  const { data } = await apiClient.post<Project>('/api/projects', payload)
  return data
}

export async function getProject(projectId: string) {
  const { data } = await apiClient.get<Project>(`/api/projects/${projectId}`)
  return data
}

export async function updateProject(projectId: string, payload: ProjectPayload) {
  const { data } = await apiClient.put<Project>(`/api/projects/${projectId}`, payload)
  return data
}

export async function deleteProject(projectId: string) {
  await apiClient.delete(`/api/projects/${projectId}`)
}

export async function listPapers(projectId: string) {
  const { data } = await apiClient.get<Paper[]>(`/api/projects/${projectId}/papers`)
  return data
}

export async function createPaper(projectId: string, payload: PaperPayload) {
  const { data } = await apiClient.post<Paper>(`/api/projects/${projectId}/papers`, payload)
  return data
}

export async function updatePaper(paperId: string, payload: PaperPayload) {
  const { data } = await apiClient.put<Paper>(`/api/papers/${paperId}`, payload)
  return data
}

export async function deletePaper(paperId: string) {
  await apiClient.delete(`/api/papers/${paperId}`)
}

export async function uploadPaperPdf(paperId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await apiClient.post<Paper>(`/api/papers/${paperId}/pdf/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
  return data
}

export function fileInlineUrl(fileId: string) {
  return `${apiBaseUrl.replace(/\/$/, '')}/api/files/${fileId}/inline`
}

export async function getFile(fileId: string) {
  const { data } = await apiClient.get<StoredFile>(`/api/files/${fileId}`)
  return data
}

export async function recognizePaperPdf(projectId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await apiClient.post<PaperRecognition>(`/api/projects/${projectId}/papers/recognize/pdf`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
  return data
}

export async function recognizePaperZotero(projectId: string, content: string) {
  const { data } = await apiClient.post<PaperRecognition>(`/api/projects/${projectId}/papers/recognize/zotero`, { content })
  return data
}

export async function createAiJob(payload: {
  projectId?: string
  targetType: string
  targetId?: string
  jobType: string
  inputJson?: string
}) {
  const { data } = await apiClient.post<AiJob>('/api/ai/jobs', payload, { timeout: AI_JOB_CREATE_TIMEOUT_MS })
  return data
}

export async function getAiJob(jobId: string) {
  const { data } = await apiClient.get<AiJob>(`/api/ai/jobs/${jobId}`)
  return data
}

export async function getLatestAiJob(payload: { targetType: string; targetId: string; jobType: string }) {
  const { data } = await apiClient.get<AiJob>('/api/ai/jobs/latest', { params: payload })
  return data
}

export async function getLatestAiJobAny(payload: { targetType: string; targetId: string; jobType: string }) {
  const { data } = await apiClient.get<AiJob>('/api/ai/jobs/latest-any', { params: payload })
  return data
}

export async function waitForAiJobCompletion(initialJob: AiJob, timeoutMs = AI_JOB_POLL_TIMEOUT_MS) {
  let job = initialJob
  const deadline = Date.now() + timeoutMs
  while (job.status === 'PENDING' || job.status === 'RUNNING') {
    if (Date.now() > deadline) {
      throw new Error('AI 任务仍在后台运行，请稍后重新打开页面读取缓存结果。')
    }
    await new Promise((resolve) => setTimeout(resolve, AI_JOB_POLL_INTERVAL_MS))
    job = await getAiJob(job.id)
  }
  return job
}

export async function listExperiments(projectId: string) {
  const { data } = await apiClient.get<Experiment[]>(`/api/projects/${projectId}/experiments`)
  return data
}

export async function createExperiment(projectId: string, payload: ExperimentPayload) {
  const { data } = await apiClient.post<Experiment>(`/api/projects/${projectId}/experiments`, payload)
  return data
}

export async function updateExperiment(experimentId: string, payload: ExperimentPayload) {
  const { data } = await apiClient.put<Experiment>(`/api/experiments/${experimentId}`, payload)
  return data
}

export async function deleteExperiment(experimentId: string) {
  await apiClient.delete(`/api/experiments/${experimentId}`)
}

export async function listReports(projectId: string) {
  const { data } = await apiClient.get<WeeklyReport[]>(`/api/projects/${projectId}/reports`)
  return data
}

export async function previewWeeklyReport(projectId: string, payload: { startDate: string; endDate: string }) {
  const { data } = await apiClient.post<WeeklyReportDraft>(`/api/projects/${projectId}/reports/weekly/preview`, payload)
  return data
}

export async function generateWeeklyReport(projectId: string, payload: { startDate: string; endDate: string; contentMarkdown?: string }) {
  const { data } = await apiClient.post<WeeklyReport>(`/api/projects/${projectId}/reports/weekly`, payload)
  return data
}
