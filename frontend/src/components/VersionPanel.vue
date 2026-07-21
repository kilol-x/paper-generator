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
  ).then(() => {
    try {
      const snap = typeof v.contentSnapshot === 'string'
        ? JSON.parse(v.contentSnapshot)
        : v.contentSnapshot
      emit('restore', snap)
      ElMessage.success('已恢复该版本内容（请手动保存）')
    } catch {
      ElMessage.error('版本数据解析失败')
    }
  }).catch(() => {})
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
      </div>
    </div>
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
</style>
