<template>
  <el-tabs class="report-preview-tabs" :model-value="modelValue" @update:model-value="$emit('update:modelValue', String($event))">
    <el-tab-pane label="Markdown" name="source">
      <section class="report-source-panel">
        <div class="report-preview-toolbar">
          <span>Markdown Source</span>
          <small>{{ lineCount }} 行 · {{ characterCount }} 字符</small>
        </div>
        <textarea
          :value="markdown"
          class="markdown-source"
          :readonly="!editable"
          :placeholder="editable ? '可以在这里修改周报 Markdown，确认保存后才会写入历史记录。' : ''"
          @input="$emit('update:markdown', ($event.target as HTMLTextAreaElement).value)"
        />
      </section>
    </el-tab-pane>
    <el-tab-pane label="Preview" name="preview">
      <article class="report-document">
        <header class="report-document-head">
          <div>
            <span>RESEARCH WEEKLY</span>
            <strong>{{ documentTitle }}</strong>
          </div>
          <small>{{ sectionCount }} 个章节 · 约 {{ readingMinutes }} 分钟阅读</small>
        </header>
        <div class="markdown-preview report-renderer" v-html="renderedMarkdown" />
      </article>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  markdown: string
  modelValue: string
  editable?: boolean
}>()

defineEmits<{
  'update:modelValue': [value: string]
  'update:markdown': [value: string]
}>()

const md = new MarkdownIt()
const renderedMarkdown = computed(() => md.render(props.markdown || ''))
const characterCount = computed(() => props.markdown.trim().length)
const lineCount = computed(() => props.markdown ? props.markdown.split('\n').length : 0)
const sectionCount = computed(() => (props.markdown.match(/^##\s+/gm) ?? []).length)
const readingMinutes = computed(() => Math.max(1, Math.ceil(characterCount.value / 700)))
const documentTitle = computed(() => {
  const match = props.markdown.match(/^#\s+(.+)$/m)
  return match?.[1]?.trim() || 'LabFlow 周报'
})
</script>

<style scoped>
.report-preview-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.report-preview-tabs :deep(.el-tabs__item) {
  color: #66758c;
  font-weight: 850;
}

.report-preview-tabs :deep(.el-tabs__item.is-active) {
  color: #1b75ff;
}

.report-source-panel,
.report-document {
  border: 1px solid #dce6f4;
  border-radius: 12px;
  background: #ffffff;
  overflow: hidden;
}

.report-preview-toolbar,
.report-document-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e7eef8;
  background: linear-gradient(110deg, #f8fbff, #ffffff);
  padding: 16px 18px;
}

.report-preview-toolbar span,
.report-document-head span {
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.report-preview-toolbar small,
.report-document-head small {
  color: #687991;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.report-document-head strong {
  display: block;
  margin-top: 5px;
  color: #0e1f38;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 950;
}

.markdown-source {
  display: block;
  min-height: 520px;
  width: 100%;
  resize: vertical;
  border: 0;
  border-radius: 0;
  background:
    linear-gradient(#ffffff 31px, #f3f7fc 32px),
    #ffffff;
  background-size: 100% 32px;
  padding: 18px 20px;
  color: #21314c;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 14px;
  line-height: 32px;
  outline: none;
}

.markdown-source:read-only {
  color: #536176;
}

.report-renderer {
  min-height: 520px;
  border: 0;
  border-radius: 0;
  background:
    radial-gradient(circle at top right, rgba(27, 117, 255, 0.08), transparent 270px),
    #ffffff;
  padding: 0 26px 30px;
  color: #26364f;
}

.report-renderer :deep(h1) {
  position: relative;
  margin: 0 -26px 28px;
  border-bottom: 1px solid #dce8f8;
  background: linear-gradient(135deg, #eef6ff, #ffffff 58%, #f1fff9);
  padding: 34px 32px 36px;
  color: #0d1f3b;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.15;
  letter-spacing: 0;
  font-weight: 950;
}

.report-renderer :deep(h1::before) {
  content: "LABFLOW WEEKLY";
  display: block;
  margin-bottom: 12px;
  color: #1b75ff;
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.16em;
}

.report-renderer :deep(h2) {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 28px 0 14px;
  color: #0e1f38;
  font-size: 23px;
  line-height: 1.25;
  font-weight: 950;
}

.report-renderer :deep(h2::before) {
  content: "";
  width: 10px;
  height: 30px;
  border-radius: 999px;
  background: #0f8b7e;
}

.report-renderer :deep(h3) {
  margin: 22px 0 10px;
  color: #21314c;
  font-size: 18px;
  font-weight: 900;
}

.report-renderer :deep(p) {
  margin: 10px 0;
  color: #3b4a62;
  font-size: 16px;
  line-height: 1.78;
  font-weight: 600;
}

.report-renderer :deep(p:first-of-type) {
  display: inline-flex;
  align-items: center;
  border: 1px solid #dce8f8;
  border-radius: 999px;
  background: #f8fbff;
  padding: 8px 14px;
  color: #536176;
  font-size: 14px;
  font-weight: 850;
}

.report-renderer :deep(ul),
.report-renderer :deep(ol) {
  display: grid;
  gap: 12px;
  margin: 12px 0 18px;
  padding-left: 0;
  list-style: none;
}

.report-renderer :deep(li) {
  position: relative;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  background: #fbfdff;
  padding: 14px 16px 14px 42px;
  color: #33425b;
  font-size: 15px;
  line-height: 1.65;
  font-weight: 650;
}

.report-renderer :deep(li::before) {
  content: "";
  position: absolute;
  top: 21px;
  left: 18px;
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #1b75ff;
}

.report-renderer :deep(li > ul),
.report-renderer :deep(li > ol) {
  margin: 12px 0 0;
}

.report-renderer :deep(li li) {
  border: 0;
  border-top: 1px solid #e6edf7;
  border-radius: 0;
  background: transparent;
  padding: 10px 0 0 24px;
}

.report-renderer :deep(li li::before) {
  top: 18px;
  left: 4px;
  width: 6px;
  height: 6px;
  background: #0f8b7e;
}

.report-renderer :deep(strong) {
  color: #0e1f38;
  font-weight: 950;
}

.report-renderer :deep(code) {
  border: 1px solid #dbe6f4;
  border-radius: 7px;
  background: #eef5ff;
  padding: 2px 7px;
  color: #0f4fb8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.92em;
  font-weight: 800;
}

.report-renderer :deep(blockquote) {
  margin: 18px 0;
  border-left: 5px solid #1b75ff;
  border-radius: 0 10px 10px 0;
  background: #f4f8ff;
  padding: 14px 18px;
}

.report-renderer :deep(hr) {
  border: 0;
  border-top: 1px solid #e2eaf5;
  margin: 26px 0;
}

@media (max-width: 760px) {
  .report-preview-toolbar,
  .report-document-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .report-preview-toolbar small,
  .report-document-head small {
    white-space: normal;
  }

  .report-renderer {
    padding: 0 18px 22px;
  }

  .report-renderer :deep(h1) {
    margin-right: -18px;
    margin-left: -18px;
    padding: 26px 20px;
  }
}
</style>
