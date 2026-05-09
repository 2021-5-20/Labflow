import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import ProjectDetailView from '../views/ProjectDetailView.vue'
import PapersView from '../views/PapersView.vue'
import ExperimentsView from '../views/ExperimentsView.vue'
import ReportsView from '../views/ReportsView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: DashboardView },
    { path: '/projects', component: ProjectsView },
    { path: '/projects/:projectId', component: ProjectDetailView },
    { path: '/projects/:projectId/papers', component: PapersView },
    { path: '/projects/:projectId/experiments', component: ExperimentsView },
    { path: '/projects/:projectId/reports', component: ReportsView }
  ]
})
