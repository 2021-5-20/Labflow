export interface Project {
  id: string
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface Paper {
  id: string
  projectId: string
  title: string
  authors?: string
  url?: string
  tags?: string
  publication?: string
  publishedDate?: string
  keyClaims?: string
  notes?: string
  pdfFileId?: string
  createdAt: string
  updatedAt: string
}

export interface PaperRecognition {
  title?: string
  authors?: string
  url?: string
  tags?: string
  publication?: string
  publishedDate?: string
  keyClaims?: string
  notes?: string
  source: string
  confidence: number
  warning?: string
}

export type AiJobStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface AiJob {
  id: string
  projectId?: string
  targetType: string
  targetId?: string
  jobType: string
  status: AiJobStatus
  inputJson?: string
  outputJson?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface PaperCard {
  summary: string
  contributions: string[]
  limitations: string[]
  citations: string[]
}

export interface StoredFile {
  id: string
  projectId: string
  paperId?: string
  originalFilename: string
  contentType?: string
  sizeBytes?: number
  objectKey: string
  uploadEnabled: boolean
  createdAt: string
  updatedAt: string
}

export type ExperimentStatus = 'PLANNED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'PAUSED'

export interface Experiment {
  id: string
  projectId: string
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
  createdAt: string
  updatedAt: string
}

export interface WeeklyReport {
  id: string
  projectId: string
  startDate: string
  endDate: string
  contentMarkdown: string
  createdAt: string
  updatedAt: string
}

export interface WeeklyReportDraft {
  projectId: string
  startDate: string
  endDate: string
  contentMarkdown: string
}

export interface RagChunk {
  id: string
  projectId: string
  sourceType: 'PAPER' | 'EXPERIMENT' | string
  sourceId?: string
  sourceTitle: string
  contentText: string
  score: number
  metadataJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface RagIndexResult {
  projectId: string
  chunkCount: number
  indexedAt: string
}

export interface RagSearchResult {
  projectId: string
  question: string
  results: RagChunk[]
}

export interface RagAskResult {
  projectId: string
  question: string
  answerMarkdown: string
  contexts: RagChunk[]
}

export interface ProjectSummary {
  id: string
  name: string
  description?: string
  paperCount: number
  experimentCount: number
  runningExperiments: number
  failedExperiments: number
  reportCount: number
  updatedAt: string
}

export interface NextAction {
  projectId: string
  projectName: string
  experimentId?: string
  title: string
  detail?: string
  priority: 'high' | 'normal'
}

export interface RecentPaper {
  id: string
  projectId: string
  projectName: string
  title: string
  tags?: string
  createdAt: string
}

export interface RecentExperiment {
  id: string
  projectId: string
  projectName: string
  name: string
  status: ExperimentStatus
  nextStep?: string
  updatedAt: string
}

export interface RecentReport {
  id: string
  projectId: string
  projectName: string
  startDate: string
  endDate: string
  createdAt: string
}

export interface DashboardSummary {
  totalProjects: number
  activeProjects: number
  runningExperiments: number
  failedExperiments: number
  totalPapers: number
  totalReports: number
  projectSummaries: ProjectSummary[]
  nextActions: NextAction[]
  recentPapers: RecentPaper[]
  recentExperiments: RecentExperiment[]
  recentReports: RecentReport[]
}
