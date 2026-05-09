<template>
  <div class="metric-card" :class="tone">
    <div class="metric-icon" v-if="$slots.icon">
      <slot name="icon" />
    </div>
    <div class="metric-body">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
      <small v-if="hint">{{ hint }}</small>
    </div>
    <div class="metric-trend" :class="{ negative: trendType === 'down' }">
      <span>{{ trendType === 'down' ? '↓' : '↑' }}</span>
      {{ trend }}
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'blue' | 'green' | 'orange' | 'purple'
  trend?: string
  trendType?: 'up' | 'down'
}>(), {
  tone: 'blue',
  trend: '较上周 +0%',
  trendType: 'up'
})
</script>
