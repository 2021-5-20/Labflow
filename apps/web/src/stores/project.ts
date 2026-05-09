import { defineStore } from 'pinia'
import type { Project } from '../types'
import { getProject } from '../api/labflow'

export const useProjectStore = defineStore('project', {
  state: () => ({
    currentProject: null as Project | null
  }),
  actions: {
    async load(projectId: string) {
      this.currentProject = await getProject(projectId)
      return this.currentProject
    },
    clear() {
      this.currentProject = null
    }
  }
})
