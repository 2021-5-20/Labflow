<template>
  <el-tag :type="tagType" effect="light" round>{{ status }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExperimentStatus } from '../types'

const props = defineProps<{ status: ExperimentStatus }>()

const tagType = computed(() => {
  if (props.status === 'SUCCESS') return 'success'
  if (props.status === 'FAILED') return 'danger'
  if (props.status === 'RUNNING') return 'warning'
  if (props.status === 'PAUSED') return 'info'
  return ''
})
</script>
