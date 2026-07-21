<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View } from '@element-plus/icons-vue'
import PaperEditor from '../components/PaperEditor.vue'
import ChapterTree from '../components/ChapterTree.vue'
import CoverInfoPanel from '../components/CoverInfoPanel.vue'
import AbstractPanel from '../components/AbstractPanel.vue'
import ReferencePanel from '../components/ReferencePanel.vue'
import VersionPanel from '../components/VersionPanel.vue'
import request from '../api/request'

const route = useRoute()
const router = useRouter()

// ==================== 常量 ====================
const SPECIAL_SECTIONS = [
  { id: 'meta-cover',  title: '封面信息',   type: 'cover',   sortOrder: -4 },
  { id: 'meta-abstract', title: '摘要',      type: 'abstract',sortOrder: -3 },
  { id: 'meta-references', title: '参考文献', type: 'references', sortOrder: -2 },
  { id: 'meta-acknowledgment', title: '致谢', type: 'acknowledgment', sortOrder: -1 },
]

// ==================== 状态 ====================
const sections = ref([])
const activeSectionId = ref(null)
const paperId = ref(route.params.id || null)
const paperTitle = ref('未命名论文')

// 章节编辑相关
const editingTitleId = ref(null)
const editingTitleValue = ref('')

// UI 状态
const saving = ref(false)
const loading = ref(false)
const dirty = ref(false)
const sidebarCollapsed = ref(false)
const sidebarTab = ref('outline') // 'outline' | 'versions'

let idCounter = 0
let autoSaveTimer = null

// ==================== 计算属性 ====================
const activeSection = computed(() =>
  sections.value.find(s => s.id === activeSectionId.value) || null
)

const activeSectionType = computed(() => activeSection.value?.type || 'chapter')

// 特殊章节数据解析
const coverData = computed({
  get: () => {
    const s = sections.value.find(s => s.id === 'meta-cover')
    if (!s?.content) return {}
    try { return JSON.parse(s.content) } catch { return {} }
  },
  set: (val) => {
    let s = sections.value.find(s => s.id === 'meta-cover')
    if (!s) {
      s = createSpecialSection('meta-cover', '封面信息', 'cover', -4)
      sections.value.push(s)
    }
    s.content = JSON.stringify(val)
    dirty.value = true
  }
})

const abstractData = computed({
  get: () => {
    const s = sections.value.find(s => s.id === 'meta-abstract')
    if (!s?.content) return {}
    try { return JSON.parse(s.content) } catch { return {} }
  },
  set: (val) => {
    let s = sections.value.find(s => s.id === 'meta-abstract')
    if (!s) {
      s = createSpecialSection('meta-abstract', '摘要', 'abstract', -3)
      sections.value.push(s)
    }
    s.content = JSON.stringify(val)
    dirty.value = true
  }
})

const referencesData = computed({
  get: () => {
    const s = sections.value.find(s => s.id === 'meta-references')
    if (!s?.content) return []
    try { return JSON.parse(s.content) } catch { return [] }
  },
  set: (val) => {
    let s = sections.value.find(s => s.id === 'meta-references')
    if (!s) {
      s = createSpecialSection('meta-references', '参考文献', 'references', -2)
      sections.value.push(s)
    }
    s.content = JSON.stringify(val)
    dirty.value = true
  }
})

// 仅章节部分（用于大纲树）
const chaptersOnly = computed(() =>
  sections.value.filter(s => !s.type || s.type === 'chapter')
)

// ==================== 特殊章节工具 ====================
function createSpecialSection(id, title, type, sortOrder) {
  return {
    id, title, type, content: '', level: 0,
    parentId: null, sortOrder
  }
}

function ensureSpecialSections() {
  for (const spec of SPECIAL_SECTIONS) {
    if (!sections.value.find(s => s.id === spec.id)) {
      sections.value.push(createSpecialSection(spec.id, spec.title, spec.type, spec.sortOrder))
    }
  }
}

// ==================== 章节 CRUD ====================
function genId() {
  return `sec_${Date.now()}_${idCounter++}`
}

function addSection(parentId = null) {
  const siblings = chaptersOnly.value.filter(s => s.parentId === parentId)
  const level = parentId
    ? ((sections.value.find(s => s.id === parentId)?.level || 1) + 1)
    : 1

  sections.value.push({
    id: genId(),
    title: `新章节 ${chaptersOnly.value.length + 1}`,
    content: '',
    type: 'chapter',
    level: Math.min(level, 4),
    parentId,
    sortOrder: siblings.length
  })

  const newId = sections.value[sections.value.length - 1].id
  activeSectionId.value = newId
  startRenameTitle(newId)
  dirty.value = true
}

async function removeSection(id) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return

  const children = getDescendantIds(id)
  try {
    await ElMessageBox.confirm(
      children.length
        ? `确定删除「${section.title}」及其 ${children.length} 个子章节？`
        : `确定删除「${section.title}」？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    sections.value = sections.value.filter(s => s.id !== id && !children.includes(s.id))
    if (activeSectionId.value === id || children.includes(activeSectionId.value)) {
      // 优先选中下一个章节
      const firstChapter = chaptersOnly.value[0]
      activeSectionId.value = firstChapter?.id || 'meta-cover'
    }
    dirty.value = true
    ElMessage.success('已删除')
  } catch {}
}

function getDescendantIds(parentId) {
  const ids = []
  for (const child of chaptersOnly.value.filter(s => s.parentId === parentId)) {
    ids.push(child.id, ...getDescendantIds(child.id))
  }
  return ids
}

function startRenameTitle(id) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return
  editingTitleId.value = id
  editingTitleValue.value = section.title
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

function renameSection(id, newTitle) {
  const section = sections.value.find(s => s.id === id)
  if (section) {
    section.title = newTitle
    dirty.value = true
  }
}

function moveSection(id, direction) {
  const section = sections.value.find(s => s.id === id)
  if (!section) return
  const siblings = chaptersOnly.value
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

function reorderSections(sourceId, targetId) {
  const source = sections.value.find(s => s.id === sourceId)
  const target = sections.value.find(s => s.id === targetId)
  if (!source || !target || source.parentId !== target.parentId) return

  const siblings = chaptersOnly.value
    .filter(s => s.parentId === source.parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)

  // 将 source 插入到 target 位置
  const srcIdx = siblings.findIndex(s => s.id === sourceId)
  const tgtIdx = siblings.findIndex(s => s.id === targetId)
  if (srcIdx < 0 || tgtIdx < 0) return

  siblings.splice(srcIdx, 1)
  siblings.splice(tgtIdx, 0, source)

  siblings.forEach((s, i) => { s.sortOrder = i })
  dirty.value = true
}

// ==================== 内容变更 ====================
function onContentChange(html) {
  if (activeSection.value && activeSection.value.type === 'chapter') {
    activeSection.value.content = html
    dirty.value = true
  }
}

function onAcknowledgmentChange(html) {
  const s = sections.value.find(s => s.id === 'meta-acknowledgment')
  if (s) { s.content = html; dirty.value = true }
}

// ==================== 键盘事件 ====================
function onKeydownHandler(e) {
  if (e.key === 'Enter') finishRenameTitle()
  if (e.key === 'Escape') { editingTitleId.value = null; editingTitleValue.value = '' }
}

// ==================== 保存 ====================
async function savePaper() {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      title: paperTitle.value,
      sections: sections.value.map(s => ({
        id: s.id, title: s.title, content: s.content,
        type: s.type, level: s.level, parentId: s.parentId,
        sortOrder: s.sortOrder
      }))
    }

    let res
    if (paperId.value) {
      res = await request.put(`/api/papers/${paperId.value}`, payload)
    } else {
      res = await request.post('/api/papers', payload)
      paperId.value = res?.id || res?.data?.id
      if (paperId.value) {
        router.replace({ name: 'EditPaper', params: { id: paperId.value } })
      }
    }

    // 保存版本快照
    if (paperId.value) {
      try {
        await request.post(`/api/papers/${paperId.value}/versions`, {
          versionNo: (res?.currentVersion || 0) + 1,
          action: 'SAVE',
          description: '手动保存',
          contentSnapshot: JSON.stringify(payload.sections)
        })
      } catch { /* 版本接口可能不可用，静默跳过 */ }
    }

    dirty.value = false
    ElMessage.success('保存成功')
  } catch (err) {
    const msg = err?.response?.data?.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

// 自动保存
function startAutoSave() {
  autoSaveTimer = setInterval(() => {
    if (dirty.value && paperId.value) {
      savePaper()
    }
  }, 30000)
}

// Ctrl+S 保存
function onGlobalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    savePaper()
  }
}

// 关闭前提醒
function onBeforeUnload(e) {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

// ==================== 加载 ====================
async function loadPaper() {
  if (!paperId.value) return
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${paperId.value}`)
    const data = res?.data || res
    paperTitle.value = data.title || '未命名论文'
    sections.value = data.sections || []
    ensureSpecialSections()
    // 默认选中第一个章节或封面
    const firstChapter = chaptersOnly.value[0]
    activeSectionId.value = firstChapter?.id || 'meta-cover'
  } catch {
    ElMessage.error('加载论文失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPaper()
  document.addEventListener('keydown', onGlobalKeydown)
  window.addEventListener('beforeunload', onBeforeUnload)
  startAutoSave()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})

// ==================== 导航 ====================
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function goPreview() {
  if (!paperId.value) {
    ElMessage.warning('请先保存论文再预览')
    return
  }
  const routeData = router.resolve({ name: 'PaperPreview', params: { id: paperId.value } })
  window.open(routeData.href, '_blank')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('paper-access-token')
  sessionStorage.removeItem('paper-user-session')
  router.push({ name: 'Login' })
}

// 版本恢复回调
function onRestoreVersion(snapshot) {
  if (Array.isArray(snapshot)) {
    sections.value = snapshot
    ensureSpecialSections()
    dirty.value = true
    ElMessage.success('版本内容已加载，请手动保存')
  }
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
        <el-button size="small" text @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
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
          {{ saving ? '⏳ 正在保存…' : (dirty ? '● 未保存' : '● 已保存') }}
        </span>
        <el-button type="primary" :loading="saving" @click="savePaper">保 存</el-button>
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </header>

    <div class="main-area">
      <!-- ====== 左侧侧栏 ====== -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <!-- 标签切换 -->
        <div class="sidebar-tabs">
          <button
            :class="{ active: sidebarTab === 'outline' }"
            @click="sidebarTab = 'outline'"
          >大纲</button>
          <button
            :class="{ active: sidebarTab === 'versions' }"
            @click="sidebarTab = 'versions'"
          >版本</button>
        </div>

        <!-- 章节大纲标签 -->
        <template v-if="sidebarTab === 'outline'">
          <div class="sidebar-header">
            <span class="sidebar-title">论文结构</span>
            <el-button size="small" :icon="Plus" circle @click="addSection(null)" title="添加顶级章节" />
          </div>

          <div class="section-list">
            <!-- 特殊章节（固定项） -->
            <div
              v-for="spec in SPECIAL_SECTIONS"
              :key="spec.id"
              class="special-item"
              :class="{ active: activeSectionId === spec.id }"
              @click="activeSectionId = spec.id"
            >
              <span class="spec-icon">
                <!-- 封面 -->
                <svg v-if="spec.type==='cover'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
                <!-- 摘要 -->
                <svg v-else-if="spec.type==='abstract'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                <!-- 参考文献 -->
                <svg v-else-if="spec.type==='references'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                <!-- 致谢 -->
                <svg v-else-if="spec.type==='acknowledgment'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </span>
              <span class="spec-title">{{ spec.title }}</span>
            </div>

            <!-- 分隔线 -->
            <div class="sidebar-divider" />

            <!-- 动态章节树 -->
            <ChapterTree
              :sections="chaptersOnly"
              :active-id="activeSectionId"
              :editing-id="editingTitleId"
              :editing-value="editingTitleValue"
              @select="activeSectionId = $event"
              @add="addSection"
              @remove="removeSection"
              @edit="(data) => startRenameTitle(data.id)"
              @finish-edit="finishRenameTitle"
              @rename="renameSection"
              @update-editing="editingTitleValue = $event"
              @keydown="onKeydownHandler"
              @move="moveSection"
              @reorder="reorderSections"
            />
          </div>

          <div v-if="chaptersOnly.length === 0 && sections.length <= SPECIAL_SECTIONS.length" class="sidebar-empty">
            <el-button type="primary" :icon="Plus" @click="addSection(null)">添加章节</el-button>
          </div>
        </template>

        <!-- 版本历史标签 -->
        <template v-if="sidebarTab === 'versions'">
          <VersionPanel
            :paper-id="paperId"
            @restore="onRestoreVersion"
          />
        </template>
      </aside>

      <!-- ====== 右侧主编辑区 ====== -->
      <main class="editor-area">
        <!-- 面包屑 -->
        <div class="editor-breadcrumb" v-if="activeSection">
          {{ activeSection.title }}
        </div>

        <!-- 封面编辑面板 -->
        <div v-if="activeSectionId === 'meta-cover'" class="panel-wrapper">
          <CoverInfoPanel v-model="coverData" />
        </div>

        <!-- 摘要编辑面板 -->
        <div v-else-if="activeSectionId === 'meta-abstract'" class="panel-wrapper">
          <AbstractPanel v-model="abstractData" />
        </div>

        <!-- 参考文献面板 -->
        <div v-else-if="activeSectionId === 'meta-references'" class="panel-wrapper">
          <ReferencePanel v-model="referencesData" />
        </div>

        <!-- 致谢面板（使用富文本编辑器） -->
        <div v-else-if="activeSectionId === 'meta-acknowledgment'" class="panel-wrapper">
          <h3 class="panel-title">致谢</h3>
          <PaperEditor
            :key="'ack'"
            :model-value="sections.find(s => s.id === 'meta-acknowledgment')?.content || ''"
            placeholder="撰写致谢…"
            @update:model-value="onAcknowledgmentChange"
          />
        </div>

        <!-- 普通章节编辑器 -->
        <template v-else-if="activeSection && (!activeSection.type || activeSection.type === 'chapter')">
          <PaperEditor
            :key="activeSection.id"
            :model-value="activeSection.content"
            :placeholder="`撰写「${activeSection.title}」内容…`"
            @update:model-value="onContentChange"
          />
          <!-- 引用提示 -->
          <div v-if="referencesData.length > 0" class="ref-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:4px"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5C8.26 12.26 8.73 13.02 8.91 14"/></svg>
            引用文献时，可在文中使用 <code>[1]</code> <code>[2]</code> 等标记对应参考文献序号
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="editor-placeholder">
          <el-empty description="请从左侧选择一个章节开始编辑" />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ========== CSS 变量覆写（Element Plus 主题色） ========== */
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
  height: 58px;
  padding: 0 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 4px rgba(79, 119, 106, 0.06);
  flex-shrink: 0;
  z-index: 10;
  gap: 14px;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.top-bar-left :deep(.el-button.is-text) {
  color: var(--text-mute); font-size: 14px;
  border-radius: var(--r); padding: 6px 10px;
}
.top-bar-left :deep(.el-button.is-text:hover) {
  background: var(--bg-hover); color: var(--primary);
}
.top-bar-left :deep(.el-button--primary) {
  background: var(--primary-tint); color: var(--primary);
  border-color: transparent; font-weight: 500;
}
.top-bar-left :deep(.el-button--primary:hover) {
  background: #d4e4dd; color: var(--primary-hover);
}

.paper-title-input {
  max-width: 480px; flex: 1; min-width: 140px;
}
.paper-title-input :deep(.el-input__inner) {
  font-family: var(--font-heading); font-weight: 600; font-size: 17px;
  color: var(--text-main); border: 1px solid transparent;
  background: transparent; border-radius: var(--r);
  padding: 0 10px; height: 40px; letter-spacing: 0.02em;
  transition: all var(--t-fast);
}
.paper-title-input :deep(.el-input__inner):hover { background: var(--bg-page); }
.paper-title-input :deep(.el-input__inner):focus {
  border-color: var(--primary); background: var(--bg-card);
  box-shadow: 0 0 0 3px rgba(79, 119, 106, 0.08);
}

.save-status {
  font-size: 12px; font-weight: 500; padding: 4px 12px;
  border-radius: 999px; letter-spacing: 0.02em;
  color: var(--success); background: #E3EEE9;
}
.save-status.dirty { color: #d97706; background: #F5E8D8; }

.top-bar-right :deep(.el-button--primary) {
  background: var(--primary); border-color: var(--primary);
  font-weight: 600; border-radius: var(--r); padding: 7px 18px;
  letter-spacing: 0.05em;
}
.top-bar-right :deep(.el-button--primary:hover) {
  background: var(--primary-hover);
}
.top-bar-right :deep(.el-button.is-text) {
  color: var(--text-mute); font-size: 13px;
}
.top-bar-right :deep(.el-button.is-text:hover) {
  color: var(--danger); background: var(--bg-hover);
}

/* ========== 主体区域 ========== */
.main-area { display: flex; flex: 1; overflow: hidden; }

/* ========== 侧边栏 ========== */
.sidebar {
  width: 280px; min-width: 280px;
  background: var(--bg-card); border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  transition: margin-left .28s cubic-bezier(.4,0,.2,1), opacity .28s;
  overflow: hidden; position: relative;
}
.sidebar.collapsed {
  margin-left: -280px; opacity: 0; pointer-events: none;
}

/* 标签切换 */
.sidebar-tabs {
  display: flex; border-bottom: 1px solid var(--border);
}
.sidebar-tabs button {
  flex: 1; border: none; background: transparent;
  padding: 10px 0; font-size: 13px; font-weight: 500;
  color: var(--text-mute); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all .15s;
}
.sidebar-tabs button.active {
  color: var(--primary); border-bottom-color: var(--primary);
}
.sidebar-tabs button:hover { color: var(--primary); }

.sidebar-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px 10px;
}
.sidebar-title {
  font-family: var(--font-heading); font-size: 14px;
  font-weight: 600; color: var(--text-main);
}
.sidebar-header :deep(.el-button.is-circle) {
  background: var(--primary-tint); color: var(--primary);
  border-color: transparent; width: 30px; height: 30px;
}
.sidebar-header :deep(.el-button.is-circle:hover) {
  background: var(--primary); color: #fff;
}

.section-list {
  flex: 1; overflow-y: auto; padding: 2px 0;
}
.section-list::-webkit-scrollbar { width: 5px; }
.section-list::-webkit-scrollbar-thumb { background: #d0d3ce; border-radius: 999px; }
.section-list::-webkit-scrollbar-thumb:hover { background: #b4b8b2; }

/* 特殊章节条目 */
.special-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px; cursor: pointer; font-size: 13px;
  color: var(--text-main); border-left: 3px solid transparent;
  transition: all .12s;
}
.special-item:hover { background: #f7f8f4; }
.special-item.active {
  background: var(--primary-tint); color: var(--primary);
  border-left-color: var(--primary); font-weight: 600;
}
.spec-icon { font-size: 15px; flex-shrink: 0; }
.spec-title { flex: 1; }

.sidebar-divider {
  height: 1px; background: var(--border); margin: 6px 14px;
}

.sidebar-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  padding: 30px;
}

/* ========== 编辑器区域 ========== */
.editor-area {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; padding: 16px 24px 24px; gap: 8px;
  background: var(--bg-page);
}

.editor-breadcrumb {
  font-size: 12px; color: var(--text-dim);
  padding: 0 6px; display: flex; align-items: center; gap: 6px;
}
.editor-breadcrumb::before {
  content: ''; display: inline-block; width: 6px; height: 6px;
  background: var(--primary); border-radius: 50%; opacity: 0.5;
}

.editor-area :deep(.paper-editor) {
  flex: 1; display: flex; flex-direction: column; min-height: 0;
}
.editor-area :deep(.editor-body) {
  flex: 1; max-height: none; min-height: 0;
}

.panel-wrapper {
  flex: 1; overflow-y: auto; background: var(--bg-card);
  border: 1px solid var(--border); border-radius: var(--r-lg);
}
.panel-wrapper::-webkit-scrollbar { width: 6px; }
.panel-wrapper::-webkit-scrollbar-thumb { background: #d0d3ce; border-radius: 999px; }

.panel-title {
  font-family: var(--font-heading); font-size: 17px;
  padding: 20px 24px 0; margin: 0; color: var(--text-main);
}

.editor-placeholder {
  flex: 1; display: flex; align-items: center; justify-content: center;
  background: var(--bg-card); border: 2px dashed var(--border);
  border-radius: var(--r-lg); color: var(--text-dim);
}

.ref-hint {
  padding: 8px 14px; font-size: 12px; color: var(--text-dim);
  background: #fdf6e8; border-radius: 6px; margin-top: 6px;
}
.ref-hint code {
  background: var(--primary-tint); color: var(--primary);
  padding: 1px 5px; border-radius: 3px; font-size: 11px;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .sidebar { width: 250px; min-width: 250px; }
  .sidebar.collapsed { margin-left: -250px; }
  .editor-area { padding: 12px 14px 18px; }
}

@media (max-width: 720px) {
  .sidebar {
    position: fixed; top: 58px; left: 0; bottom: 0; z-index: 100;
    box-shadow: var(--shadow-lg);
  }
  .save-status { display: none; }
}
</style>
