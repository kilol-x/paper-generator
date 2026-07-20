<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PaperEditor from '../components/PaperEditor.vue'
import SectionNode from '../components/SectionNode.vue'
import request from '../api/request'

const route = useRoute()
const router = useRouter()

// ==================== 数据模型 ====================

const sections = ref([])
const activeSectionId = ref(null)
const paperId = ref(route.params.id || null)
const paperTitle = ref('未命名论文')

const editingTitleId = ref(null)
const editingTitleValue = ref('')

const saving = ref(false)
const loading = ref(false)
const dirty = ref(false)
const sidebarCollapsed = ref(false)

// ==================== 计算属性 ====================

const activeSection = computed(() =>
  sections.value.find(s => s.id === activeSectionId.value) || null
)

const sortedRootSections = computed(() =>
  sections.value
    .filter(s => !s.parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
)

function getSectionPath(id) {
  const parts = []
  let current = sections.value.find(s => s.id === id)
  while (current) {
    parts.unshift(current.title)
    current = sections.value.find(s => s.id === current.parentId)
  }
  return parts.join(' > ')
}

// ==================== 章节 CRUD ====================

let idCounter = 0
function genId() {
  return `sec_${Date.now()}_${idCounter++}`
}

function addSection(parentId = null) {
  const siblings = sections.value.filter(s => s.parentId === parentId)
  const level = parentId
    ? (sections.value.find(s => s.id === parentId)?.level || 1) + 1
    : 1

  sections.value.push({
    id: genId(),
    title: `新章节 ${sections.value.length + 1}`,
    content: '',
    level: Math.min(level, 4),
    parentId,
    sortOrder: siblings.length
  })

  activeSectionId.value = sections.value[sections.value.length - 1].id
  startRenameTitle(activeSectionId.value)
  dirty.value = true
}

function removeSection(id) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return

  const children = getDescendantIds(id)

  ElMessageBox.confirm(
    children.length
      ? `确定删除「${section.title}」及其 ${children.length} 个子章节？`
      : `确定删除「${section.title}」？`,
    '删除确认',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    sections.value = sections.value.filter(s => s.id !== id && !children.includes(s.id))
    if (activeSectionId.value === id || children.includes(activeSectionId.value)) {
      activeSectionId.value = sections.value[0]?.id || null
    }
    dirty.value = true
    ElMessage.success('已删除')
  }).catch(() => {})
}

function getDescendantIds(parentId) {
  const ids = []
  for (const child of sections.value.filter(s => s.parentId === parentId)) {
    ids.push(child.id, ...getDescendantIds(child.id))
  }
  return ids
}

function startRenameTitle(id) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return
  editingTitleId.value = id
  editingTitleValue.value = section.title
  nextTick(() => {
    document.querySelector(`.section-title-input[data-id="${id}"]`)?.focus()
  })
}

function finishRenameTitle() {
  if (!editingTitleId.value) return
  const section = sections.value.find(s => s.id === editingTitleId.value)
  if (section && editingTitleValue.value.trim()) {
    section.title = editingTitleValue.value.trim()
    dirty.value = true
  }
  editingTitleId.value = null
  editingTitleValue.value = ''
}

function moveSection(id, direction) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return
  const siblings = sections.value
    .filter(s => s.parentId === section.parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)

  const idx = siblings.findIndex(s => s.id === id)
  const targetIdx = idx + direction
  if (targetIdx < 0 || targetIdx >= siblings.length) return

  const tmp = siblings[idx].sortOrder
  siblings[idx].sortOrder = siblings[targetIdx].sortOrder
  siblings[targetIdx].sortOrder = tmp
  dirty.value = true
}

// ==================== 内容变更 ====================

function onContentChange(html) {
  if (activeSection.value) {
    activeSection.value.content = html
    dirty.value = true
  }
}

// ==================== 保存 ====================

async function savePaper() {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      title: paperTitle.value,
      sections: sections.value.map(s => ({
        id: s.id,
        title: s.title,
        content: s.content,
        level: s.level,
        parentId: s.parentId,
        sortOrder: s.sortOrder
      }))
    }

    let res
    if (paperId.value) {
      res = await request.put(`/api/papers/${paperId.value}`, payload)
    } else {
      res = await request.post('/api/papers', payload)
      paperId.value = res.data?.id || res.id
      router.replace({ name: 'EditPaper', params: { id: paperId.value } })
    }

    dirty.value = false
    ElMessage.success('保存成功')
  } catch (err) {
    const msg = err.response?.data?.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

// Ctrl+S 快捷键
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    savePaper()
  }
}

// 页面关闭前提醒
window.addEventListener('beforeunload', e => {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
})

// ==================== 加载 / 卸载 ====================

async function loadPaper() {
  if (!paperId.value) return
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${paperId.value}`)
    const data = res.data || res
    paperTitle.value = data.title || '未命名论文'
    sections.value = data.sections || []
    activeSectionId.value = sections.value[0]?.id || null
  } catch {
    ElMessage.error('加载论文失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPaper()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="edit-paper-layout" v-loading="loading">
    <!-- ====== 顶部栏 ====== -->
    <header class="top-bar">
      <div class="top-bar-left">
        <el-button size="small" text @click="toggleSidebar">
          {{ sidebarCollapsed ? '☰' : '✕' }}
        </el-button>
        <el-input
          v-model="paperTitle"
          class="paper-title-input"
          size="large"
          placeholder="论文标题"
          @input="dirty = true"
        />
      </div>
      <div class="top-bar-right">
        <span class="save-status" :class="{ dirty }">
          {{ dirty ? '● 未保存' : '● 已保存' }}
        </span>
        <el-button type="primary" :loading="saving" @click="savePaper">保 存</el-button>
      </div>
    </header>

    <div class="main-area">
      <!-- ====== 左侧章节大纲 ====== -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <span class="sidebar-title">章节大纲</span>
          <el-button size="small" :icon="Plus" circle @click="addSection(null)" title="添加顶级章节" />
        </div>

        <div class="section-list">
          <SectionNode
            v-for="section in sortedRootSections"
            :key="section.id"
            :section="section"
            :all-sections="sections"
            :active-id="activeSectionId"
            :editing-title-id="editingTitleId"
            :editing-title-value="editingTitleValue"
            :depth="0"
            @select="activeSectionId = $event"
            @add="addSection"
            @remove="removeSection"
            @rename="startRenameTitle"
            @update:editing-value="editingTitleValue = $event"
            @finish-rename="finishRenameTitle"
            @move="(id, dir) => moveSection(id, dir)"
          />
        </div>

        <div v-if="sections.length === 0" class="sidebar-empty">
          <el-button type="primary" :icon="Plus" @click="addSection(null)">添加章节</el-button>
        </div>
      </aside>

      <!-- ====== 右侧编辑器 ====== -->
      <main class="editor-area">
        <template v-if="activeSection">
          <div class="editor-breadcrumb">
            {{ getSectionPath(activeSection.id) }}
          </div>
          <PaperEditor
            :key="activeSection.id"
            :model-value="activeSection.content"
            :placeholder="`撰写「${activeSection.title}」内容…`"
            @update:model-value="onContentChange"
          />
        </template>

        <div v-else class="editor-placeholder">
          <el-empty description="请从左侧选择一个章节开始编辑" />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ========== 布局 ========== */
.edit-paper-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-page);
}

/* ========== 顶部栏 ========== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  z-index: 10;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.paper-title-input { max-width: 480px; }

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-status {
  font-size: 13px;
  color: var(--success);
}

.save-status.dirty {
  color: var(--warning);
}

/* ========== 主体 ========== */
.main-area {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.sidebar {
  width: 280px;
  min-width: 280px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: margin-left 0.25s ease, opacity 0.25s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  margin-left: -280px;
  opacity: 0;
  pointer-events: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--border);
}

.sidebar-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.sidebar-empty {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.section-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

/* ========== 编辑器区域 ========== */
.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 24px;
  gap: 12px;
}

.editor-breadcrumb {
  font-size: 13px;
  color: var(--text-dim);
  padding: 0 4px;
}

.editor-area :deep(.paper-editor) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.editor-area :deep(.editor-body) {
  flex: 1;
  max-height: none;
}

.editor-placeholder {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.paper-title-input :deep(.el-input__inner) {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 17px;
  color: var(--text-main);
}
</style>
