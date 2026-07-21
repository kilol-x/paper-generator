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

function handleLogout() {
  localStorage.removeItem('token')
  router.push({ name: 'Login' })
}
// ==================== 预览跳转 ====================
const goPreview = () => {
  if (!paperId.value) {
    ElMessage.warning('请先保存论文再预览')
    return
  }
  // 新窗口打开预览页面
  const routeData = router.resolve(`/preview/${paperId.value}`)
  window.open(routeData.href, '_blank')
}
</script>

<template>
  <div class="edit-paper-layout" v-loading="loading">
    <!-- ====== 顶部栏 ====== -->
    <header class="top-bar">
      <div class="top-bar-left">
        <el-button size="small" text @click="router.push({ name: 'Papers' })" title="返回列表">
          ← 返回
        </el-button>
        <el-button size="small" text @click="toggleSidebar">
          {{ sidebarCollapsed ? '☰' : '✕' }}
        </el-button>
        <el-button type="primary" @click="goPreview">
          <el-icon><View /></el-icon>
          预览论文
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
        <el-button text @click="handleLogout">退出</el-button>
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
/* ================================================================
   EditPaper — 论文编辑页  莫兰迪风格
   ================================================================ */

/* ---- Element Plus 主题色覆写 ---- */
.edit-paper-layout {
  --el-color-primary:       #4F776A;
  --el-color-primary-light-3: #729487;
  --el-color-primary-light-5: #90ACA2;
  --el-color-primary-light-7: #B8CEC6;
  --el-color-primary-light-8: #D0DFD9;
  --el-color-primary-light-9: #E8EFEC;
  --el-color-primary-dark-2:  #3F5F55;
}

/* ========== 整体布局 ========== */
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
  height: 60px;
  padding: 0 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 4px rgba(79, 119, 106, 0.06);
  flex-shrink: 0;
  z-index: 10;
  gap: 16px;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* -- 返回 / 侧栏切换按钮 -- */
.top-bar-left :deep(.el-button.is-text) {
  color: var(--text-mute);
  font-size: 14px;
  border-radius: var(--r);
  padding: 6px 10px;
  transition: all var(--t-fast);
}
.top-bar-left :deep(.el-button.is-text:hover) {
  background: var(--bg-hover);
  color: var(--primary);
}

/* -- 预览按钮 -- */
.top-bar-left :deep(.el-button--primary) {
  background: var(--primary-tint);
  color: var(--primary);
  border-color: transparent;
  font-weight: 500;
  border-radius: var(--r);
  transition: all var(--t-fast);
}
.top-bar-left :deep(.el-button--primary:hover) {
  background: #d4e4dd;
  color: var(--primary-hover);
}

/* -- 标题输入框 -- */
.paper-title-input {
  max-width: 520px;
  flex: 1;
  min-width: 160px;
}
.paper-title-input :deep(.el-input__inner) {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 18px;
  color: var(--text-main);
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--r);
  padding-left: 12px;
  padding-right: 12px;
  height: 42px;
  transition: all var(--t-fast);
  letter-spacing: 0.02em;
}
.paper-title-input :deep(.el-input__inner):hover {
  background: var(--bg-page);
}
.paper-title-input :deep(.el-input__inner):focus {
  border-color: var(--primary);
  background: var(--bg-card);
  box-shadow: 0 0 0 3px rgba(79, 119, 106, 0.08);
}

/* -- 保存状态 -- */
.save-status {
  font-size: 12px;
  font-weight: 500;
  color: var(--success);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 999px;
  background: #E3EEE9;
  letter-spacing: 0.02em;
  transition: all var(--t-fast);
}
.save-status.dirty {
  color: var(--warning);
  background: #F5E8D8;
}

/* -- 保存按钮 -- */
.top-bar-right :deep(.el-button--primary) {
  background: var(--primary);
  border-color: var(--primary);
  font-weight: 600;
  border-radius: var(--r);
  padding: 8px 20px;
  letter-spacing: 0.05em;
  transition: all var(--t-fast);
  box-shadow: 0 1px 2px rgba(79, 119, 106, 0.12);
}
.top-bar-right :deep(.el-button--primary:hover) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
  box-shadow: 0 4px 12px rgba(79, 119, 106, 0.22);
}

/* -- 退出按钮 -- */
.top-bar-right :deep(.el-button.is-text) {
  color: var(--text-mute);
  font-size: 13px;
  border-radius: var(--r);
  transition: all var(--t-fast);
}
.top-bar-right :deep(.el-button.is-text:hover) {
  background: var(--bg-hover);
  color: var(--danger);
}

/* ========== 主体区域 ========== */
.main-area {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ========== 侧边栏 ========== */
.sidebar {
  width: 290px;
  min-width: 290px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: margin-left 0.28s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}
.sidebar.collapsed {
  margin-left: -290px;
  opacity: 0;
  pointer-events: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
  border-bottom: 1px solid var(--border);
}

.sidebar-title {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 8px;
}
.sidebar-title::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 16px;
  background: var(--primary);
  border-radius: 2px;
}

/* -- 添加章节按钮 -- */
.sidebar-header :deep(.el-button.is-circle) {
  background: var(--primary-tint);
  color: var(--primary);
  border-color: transparent;
  width: 32px;
  height: 32px;
  padding: 0;
  transition: all var(--t-fast);
  font-size: 16px;
}
.sidebar-header :deep(.el-button.is-circle:hover) {
  background: var(--primary);
  color: #fff;
}

/* -- 章节列表 -- */
.section-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.section-list::-webkit-scrollbar {
  width: 5px;
}
.section-list::-webkit-scrollbar-thumb {
  background: #d0d3ce;
  border-radius: 999px;
}
.section-list::-webkit-scrollbar-thumb:hover {
  background: #b4b8b2;
}

/* -- 空状态 -- */
.sidebar-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 24px;
  gap: 14px;
  text-align: center;
}
.sidebar-empty::before {
  content: '📄';
  font-size: 40px;
  opacity: 0.35;
  margin-bottom: 4px;
}
.sidebar-empty :deep(.el-button--primary) {
  background: var(--primary-tint);
  color: var(--primary);
  border-color: transparent;
  font-weight: 600;
  border-radius: var(--r);
  padding: 9px 22px;
  transition: all var(--t-fast);
}
.sidebar-empty :deep(.el-button--primary:hover) {
  background: #d4e4dd;
  color: var(--primary-hover);
}

/* ========== 编辑器区域 ========== */
.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 28px 28px;
  gap: 10px;
  background: var(--bg-page);
}

.editor-breadcrumb {
  font-size: 12px;
  color: var(--text-dim);
  padding: 0 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 0.02em;
}
.editor-breadcrumb::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 50%;
  opacity: 0.4;
}

/* -- 编辑器占满剩余高度 -- */
.editor-area :deep(.paper-editor) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.editor-area :deep(.editor-body) {
  flex: 1;
  max-height: none;
  min-height: 0;
}

/* -- 编辑器空状态占位 -- */
.editor-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--bg-card);
  border: 2px dashed var(--border);
  border-radius: var(--r-lg);
  color: var(--text-dim);
  font-size: 14px;
  gap: 10px;
  margin: 8px 0;
}
.editor-placeholder :deep(.el-empty__description) {
  color: var(--text-dim);
  font-size: 14px;
}
.editor-placeholder :deep(.el-empty__image) {
  opacity: 0.35;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .top-bar {
    padding: 0 14px;
  }
  .paper-title-input {
    max-width: 320px;
  }
  .sidebar {
    width: 250px;
    min-width: 250px;
  }
  .sidebar.collapsed {
    margin-left: -250px;
  }
  .editor-area {
    padding: 14px 16px 20px;
  }
}

@media (max-width: 720px) {
  .top-bar-left {
    gap: 6px;
  }
  .paper-title-input {
    max-width: 180px;
  }
  .top-bar-right :deep(.el-button--primary) {
    padding: 6px 14px;
    font-size: 13px;
  }
  .save-status {
    display: none;
  }
  .sidebar {
    position: fixed;
    top: 60px;
    left: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: var(--shadow-lg);
  }
  .sidebar.collapsed {
    margin-left: -290px;
  }
}
</style>
