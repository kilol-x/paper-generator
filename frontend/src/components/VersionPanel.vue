<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'

const props = defineProps({
  paperId: { type: [String, Number], default: null }
})
const emit = defineEmits(['restore'])

const versions = ref([])
const loading = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewVersion = ref(null)

async function loadVersions() {
  if (!props.paperId) return
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${props.paperId}/drafts`)
    versions.value = (res?.data || res) || []
  } catch {
    try {
      // 兼容旧接口
      const fallback = await request.get(`/api/reviews/${props.paperId}/versions`)
      versions.value = (fallback?.data || fallback) || []
    } catch {
      versions.value = []
    }
  } finally {
    loading.value = false
  }
}

function fmtTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function actionLabel(a) {
  const map = {
    SUBMIT: '提交',
    REVIEW: '评阅',
    RETURN: '退回',
    GRADE: '评分',
    SAVE: '保存',
    CREATE: '创建',
    AUTO_SAVE: '自动保存',
    MANUAL_SAVE: '手动保存'
  }
  return map[a] || a || '—'
}

function actionColor(a) {
  const map = {
    SUBMIT: '#4F776A',
    RETURN: '#d97706',
    GRADE: '#2563eb',
    SAVE: '#787b75',
    CREATE: '#787b75',
    REVIEW: '#8b5cf6',
    AUTO_SAVE: '#4F776A',
    MANUAL_SAVE: '#d97706'
  }
  return map[a] || '#787b75'
}

function onRestoreClick(v) {
  ElMessageBox.confirm(
    `确定恢复到版本 ${v.versionNo}？当前未保存的修改将被覆盖。`,
    '恢复版本',
    { confirmButtonText: '确定恢复', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const detail = await loadVersionDetail(v.id)
      emit('restore', detail.snapshot)
      ElMessage.success('已恢复该版本内容（请手动保存）')
    } catch {
      ElMessage.error('版本数据解析失败')
    }
  }).catch(() => {})
}

async function loadVersionDetail(versionId) {
  const res = await request.get(`/api/papers/${props.paperId}/drafts/${versionId}`)
  return res?.data || res
}

async function onPreviewClick(v) {
  previewVisible.value = true
  previewLoading.value = true
  previewVersion.value = null
  try {
    previewVersion.value = await loadVersionDetail(v.id)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '读取历史版本失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

function previewSections(snapshot) {
  if (!Array.isArray(snapshot?.sections)) {
    return []
  }
  return snapshot.sections.filter(section => !section.type || section.type === 'chapter')
}

function previewReferences(snapshot) {
  return Array.isArray(snapshot?.references) ? snapshot.references : []
}

watch(() => props.paperId, () => {
  loadVersions()
}, { immediate: true })

onMounted(loadVersions)
</script>

<template>
  <div class="ver-panel">
    <h3 class="panel-title">版本历史</h3>
    <p class="panel-desc">每次保存、提交或评阅操作都会生成版本快照</p>

    <div v-loading="loading" class="ver-list">
      <div v-if="versions.length === 0 && !loading" class="ver-empty">
        暂无版本记录
      </div>
      <div
        v-for="v in versions"
        :key="v.id"
        class="ver-item"
      >
        <div class="ver-dot" :style="{ background: actionColor(v.action) }" />
        <div class="ver-body">
          <div class="ver-head">
            <span class="ver-tag" :style="{ background: actionColor(v.action)+'18', color: actionColor(v.action) }">
              版本 {{ v.versionNo }}
            </span>
            <span class="ver-action">{{ actionLabel(v.action) }}</span>
            <span class="ver-time">{{ fmtTime(v.createdAt) }}</span>
          </div>
          <div v-if="v.description" class="ver-desc">{{ v.description }}</div>
        </div>
        <el-button size="small" text type="primary" @click="onRestoreClick(v)">
          恢复
        </el-button>
        <el-button size="small" text @click="onPreviewClick(v)">
          查看
        </el-button>
      </div>
    </div>

    <el-dialog v-model="previewVisible" width="760px" destroy-on-close>
      <template #header>
        <div class="preview-header">
          <strong>历史版本详情</strong>
          <span v-if="previewVersion" class="preview-meta">V{{ previewVersion.versionNo }} · {{ actionLabel(previewVersion.action) }} · {{ fmtTime(previewVersion.createdAt) }}</span>
        </div>
      </template>

      <div v-loading="previewLoading" class="preview-body">
        <template v-if="previewVersion">
          <p class="preview-desc">{{ previewVersion.description || '无版本说明' }}</p>

          <section class="preview-block">
            <div class="preview-block-head">
              <h4>正文结构</h4>
              <span>{{ previewSections(previewVersion.snapshot).length }} 节</span>
            </div>
            <div v-if="previewSections(previewVersion.snapshot).length === 0" class="preview-empty">该版本没有正文快照</div>
            <div v-else class="preview-sections">
              <div v-for="section in previewSections(previewVersion.snapshot)" :key="section.id || section.title" class="preview-section-item">
                <strong>{{ section.title || '未命名章节' }}</strong>
                <p>{{ (section.content || '').replace(/<[^>]+>/g, '').trim() || '该章节无正文内容' }}</p>
              </div>
            </div>
          </section>

          <section class="preview-block">
            <div class="preview-block-head">
              <h4>参考文献</h4>
              <span>{{ previewReferences(previewVersion.snapshot).length }} 条</span>
            </div>
            <div v-if="previewReferences(previewVersion.snapshot).length === 0" class="preview-empty">该版本没有保存参考文献</div>
            <div v-else class="preview-references">
              <div v-for="ref in previewReferences(previewVersion.snapshot)" :key="ref.id || `${ref.citationNo}-${ref.title}`" class="preview-reference-item">
                <span class="preview-reference-no">[{{ ref.citationNo || '?' }}]</span>
                <span>{{ ref.formattedText || [ref.authors, ref.title, ref.journal, ref.year].filter(Boolean).join(' / ') }}</span>
              </div>
            </div>
          </section>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.ver-panel { padding: 20px 24px; }
.panel-title { font-family: var(--font-heading); font-size: 17px; margin: 0 0 4px; color: var(--text-main); }
.panel-desc { font-size: 12px; color: var(--text-dim); margin: 0 0 16px; }
.ver-empty { text-align: center; color: var(--text-dim); padding: 40px 0; font-size: 13px; }
.ver-list { display: flex; flex-direction: column; gap: 2px; }
.ver-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px; border-radius: 6px; font-size: 13px;
  transition: background .12s; border: 1px solid transparent;
}
.ver-item:hover { background: var(--bg-page); border-color: var(--border); }
.ver-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.ver-body { flex: 1; min-width: 0; }
.ver-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ver-tag { font-size: 11px; padding: 1px 6px; border-radius: 4px; font-weight: 600; }
.ver-action { color: var(--text-main); font-weight: 500; }
.ver-time { color: var(--text-dim); font-size: 11px; margin-left: auto; }
.ver-desc { color: var(--text-mute); font-size: 12px; margin-top: 3px; }
.preview-header { display: flex; flex-direction: column; gap: 6px; }
.preview-meta { font-size: 12px; color: var(--text-dim); }
.preview-body { min-height: 220px; }
.preview-desc { margin: 0 0 16px; color: var(--text-main); line-height: 1.7; }
.preview-block { border: 1px solid var(--border); border-radius: 10px; padding: 14px 16px; margin-top: 14px; }
.preview-block-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.preview-block-head h4 { margin: 0; font-size: 14px; color: var(--text-main); }
.preview-block-head span { font-size: 12px; color: var(--text-dim); }
.preview-empty { font-size: 12px; color: var(--text-dim); }
.preview-sections, .preview-references { display: flex; flex-direction: column; gap: 10px; }
.preview-section-item { padding: 10px 12px; border-radius: 8px; background: var(--bg-page); }
.preview-section-item strong { display: block; margin-bottom: 6px; color: var(--text-main); }
.preview-section-item p { margin: 0; font-size: 12px; line-height: 1.7; color: var(--text-mute); white-space: pre-wrap; }
.preview-reference-item { display: flex; gap: 8px; padding: 8px 10px; border-radius: 8px; background: var(--bg-page); }
.preview-reference-no { color: var(--primary); font-weight: 600; }
</style>
