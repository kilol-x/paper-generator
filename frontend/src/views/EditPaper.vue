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
import TemplatePickerDialog from '../components/TemplatePickerDialog.vue'
import { resolveConfigFont, parseFontSize } from '../utils/fonts'
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
const chapterEditorRef = ref(null)
const lastActiveChapterId = ref(null)
const showCitationPicker = ref(false)
const referenceWorkspaceOpen = ref(true)

// 模板状态
const currentTemplateId = ref(null)
const currentTemplateName = ref('')
const currentTemplateSnapshot = ref(null) // 解析后的对象
const showChangeTplPicker = ref(false)

let idCounter = 0
let autoSaveTimer = null
const levelPickerParentId = ref(undefined)  // null=root, undefined=closed, string=parentId
const maxHeadingLevel = ref(3)             // 模板规定的最大标题层级

// ==================== 计算属性 ====================

/** 从模板快照中解析 formatJson，供编辑器自动套用模板字体 */
const editorFormatConfig = computed(() => {
  const snap = currentTemplateSnapshot.value
  if (!snap?.formatJson) return undefined
  try {
    return typeof snap.formatJson === 'string'
      ? JSON.parse(snap.formatJson)
      : snap.formatJson
  } catch { return undefined }
})

/** 根据当前章节层级和模板配置，计算面包屑标题的内联样式 */
const headingBreadcrumbStyle = computed(() => {
  const cfg = editorFormatConfig.value
  const level = activeSection.value?.level
  if (!cfg || !level || level < 1 || level > 3) return {}
  const headingCfg = cfg['heading' + level]
  if (!headingCfg) return {}
  const style = {}
  const size = parseFontSize(headingCfg.fontSize)
  if (size != null) style.fontSize = size + 'pt'
  const cssFont = resolveConfigFont(headingCfg)
  if (cssFont) style.fontFamily = cssFont
  if (headingCfg.bold !== false) style.fontWeight = 'bold'
  return style
})

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

const citationCandidates = computed(() =>
  referencesData.value.map((ref, index) => {
    const citationNo = ref.citationNo || index + 1
    return {
      ...ref,
      citationNo,
      marker: `[${citationNo}]`,
      displayLabel: ref.year ? `${citationNo} (${ref.year})` : String(citationNo)
    }
  })
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

/**
 * 从模板 snapshot 的 structureJson 中提取 chapters.maxLevel
 */
function extractMaxLevelFromSnapshot(snap) {
  try {
    const structure = typeof snap?.structureJson === 'string'
      ? JSON.parse(snap.structureJson)
      : snap?.structureJson
    if (!structure?.sections) return 3
    const chaptersSec = structure.sections.find(s => s.key === 'chapters')
    if (chaptersSec?.maxLevel) {
      const v = Number(chaptersSec.maxLevel)
      return v >= 1 && v <= 5 ? v : 3
    }
  } catch { /* ignore */ }
  return 3
}

/** 添加根章节（一级标题，直接创建不弹窗） */
function addRootChapter() {
  addSection(null, 1)
}

/** 打开层级选择器（为子章节弹窗）：parentId 为 null → 根，字符串 → 某章节的子 */
function openLevelPicker(parentId) {
  levelPickerParentId.value = parentId
}

/** 当前待添加位置的父章节名称（供弹窗展示） */
const currentParentId = computed(() => levelPickerParentId.value)
const isAddingChild = computed(() => !!levelPickerParentId.value) // true=子章节，false=根

const pendingParentTitle = computed(() => {
  const pid = levelPickerParentId.value
  if (!pid) return ''
  const parent = sections.value.find(s => s.id === pid)
  return parent?.title || ''
})

/** 子章节可选起始层级：必须严格大于父级层级 */
const minChildLevel = computed(() => {
  const pid = levelPickerParentId.value
  if (!pid) return 1
  const parent = sections.value.find(s => s.id === pid)
  return (parent?.level || 1) + 1
})

function onLevelPicked(level) {
  // 🔑 在清空 ref 之前先保存 parentId（null=根，string=子）
  const pid = levelPickerParentId.value
  levelPickerParentId.value = undefined   // 关闭弹窗
  addSection(pid, level)
}

function addSection(parentId, targetLevel) {
  const MAX_LEVEL = maxHeadingLevel.value
  const parentSection = parentId ? sections.value.find(s => s.id === parentId) : null

  // 子章节层级必须 > 父级，顶级无限制
  const minLevel = parentSection ? parentSection.level + 1 : 1
  const level = Math.max(minLevel, Math.min(targetLevel ?? minLevel, MAX_LEVEL))

  const siblings = chaptersOnly.value.filter(s => s.parentId === parentId)

  const headingLabel = ['', '一级标题', '二级标题', '三级标题', '四级标题', '五级标题']
  const newSection = {
    id: genId(),
    title: `${headingLabel[level] || level + '级标题'} ${chaptersOnly.value.length + 1}`,
    content: '',
    type: 'chapter',
    level,
    parentId: parentId || null,   // null 或父章节 id 字符串
    sortOrder: siblings.length
  }
  sections.value.push(newSection)

  activeSectionId.value = newSection.id
  startRenameTitle(newSection.id)
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

watch(activeSection, section => {
  if (section && (!section.type || section.type === 'chapter')) {
    lastActiveChapterId.value = section.id
  }
}, { immediate: true })

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
async function savePaper({ mode = 'MANUAL_SAVE', silent = false } = {}) {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      title: paperTitle.value,
      sections: sections.value.map(s => ({
        id: s.id, title: s.title, content: s.content,
        type: s.type, level: s.level, parentId: s.parentId,
        sortOrder: s.sortOrder
      })),
      references: referencesData.value.map(ref => ({
        id: ref.id,
        authors: ref.authors || '',
        title: ref.title || '',
        journal: ref.journal || '',
        year: ref.year || '',
        pages: ref.pages || '',
        citationNo: ref.citationNo || undefined,
        formattedText: ref.formattedText || ''
      })),
      saveMode: mode,
      templateId: currentTemplateId.value || undefined,
      templateSnapshot: currentTemplateSnapshot.value
        ? JSON.stringify(currentTemplateSnapshot.value)
        : undefined,
    }

    let res
    if (paperId.value) {
      res = await request.put(`/api/papers/${paperId.value}`, payload)
    } else {
      res = await request.post('/api/papers', payload)
    }

    // 检查后端业务状态码（后端错误也返回 HTTP 200）
    if (res.code !== 200) {
      throw new Error(res.message || '保存失败')
    }

    // 从响应同步 paperId（保底：route.params.id）
    const idFromRes = res?.data?.id
    if (idFromRes) {
      paperId.value = idFromRes
    } else if (route.params.id) {
      paperId.value = route.params.id
    }

    // 新建论文时补充路由（首次 POST 保存后 URL 获得 id）
    if (paperId.value && !route.params.id) {
      router.replace({ name: 'EditPaper', params: { id: paperId.value } })
    }

    dirty.value = false
    if (!silent) {
      ElMessage.success('保存成功')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err.message || '保存失败'
    if (!silent) {
      ElMessage.error(msg)
    }
  } finally {
    saving.value = false
  }
}

// 自动保存
function startAutoSave() {
  autoSaveTimer = setInterval(() => {
    if (dirty.value && paperId.value) {
      savePaper({ mode: 'AUTO_SAVE', silent: true })
    }
  }, 30000)
}

// Ctrl+S 保存
function onGlobalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    savePaper({ mode: 'MANUAL_SAVE' })
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
let loadingPaper = false // 防重入标志，避免 loadPaper 并发执行互相覆盖
async function loadPaper() {
  if (!paperId.value) return
  if (loadingPaper) return // 已在加载中，跳过重复调用
  loadingPaper = true
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${paperId.value}`)
    const data = res?.data || res
    paperTitle.value = data.title || '未命名论文'
    sections.value = data.sections || []
    ensureSpecialSections()
    if (Array.isArray(data.references)) {
      referencesData.value = data.references
    }

    // 读取模板信息
    if (data.templateId) currentTemplateId.value = data.templateId
    if (data.templateSnapshot) {
      try {
        const snap = typeof data.templateSnapshot === 'string'
          ? JSON.parse(data.templateSnapshot)
          : data.templateSnapshot
        currentTemplateSnapshot.value = snap
        currentTemplateName.value = snap.templateName || ''
        maxHeadingLevel.value = extractMaxLevelFromSnapshot(snap)
        // 首次进入（章节为空）时按模板骨架生成
        const hasChapters = sections.value.some(s => !s.type || s.type === 'chapter')
        if (!hasChapters && snap.structureJson) {
          applyTemplateSkeleton(snap, { keepCover: true })
        }
      } catch (e) {
        console.warn('templateSnapshot 解析失败', e)
      }
    }

    // 默认选中第一个章节或封面
    const firstChapter = chaptersOnly.value[0]
    activeSectionId.value = firstChapter?.id || 'meta-cover'
  } catch {
    ElMessage.error('加载论文失败')
  } finally {
    loading.value = false
    loadingPaper = false
  }
}

// ==================== 模板套用 / 更换 ====================
/**
 * 根据模板 snapshot 里的 structureJson 生成章节骨架
 * @param {Object} snapshot   { templateId, templateName, structureJson, ... }
 * @param {Object} opts       { keepCover: boolean }
 */
function applyTemplateSkeleton(snapshot, opts = {}) {
  let structure = null
  try {
    structure = typeof snapshot.structureJson === 'string'
      ? JSON.parse(snapshot.structureJson)
      : snapshot.structureJson
  } catch { structure = null }

  // 备份要保留的封面信息
  const savedCover = opts.keepCover
    ? sections.value.find(s => s.id === 'meta-cover')
    : null

  // 清空 sections（章节 + 摘要 + 参考文献 + 致谢）
  sections.value = []

  // 加回封面（如果保留）
  if (savedCover) sections.value.push({ ...savedCover })

  // 保证有特殊章节骨架
  ensureSpecialSections()

  // 根据 structureJson.sections 生成正文章节
  const src = structure?.sections || []
  const chapterList = src.filter(s => s.key === 'chapters' || (s.visible && !['cover','declaration','abstract_zh','abstract_en','keywords_zh','keywords_en','toc','references','acknowledgement','appendix'].includes(s.key)))

  // 简单方式：从 structureJson 里 visible=true 的普通节生成一级章节
  let order = 0
  for (const sec of src) {
    if (!sec.visible) continue
    if (sec.key === 'chapters') {
      // 生成一个默认的正文第一章
      sections.value.push({
        id: `sec_${Date.now()}_${idCounter++}`,
        title: '第一章',
        content: '', type: 'chapter', level: 1, parentId: null, sortOrder: order++
      })
    }
  }

  dirty.value = true
}

async function openChangeTemplate() {
  try {
    await ElMessageBox.confirm(
      '更换论文模板会清空当前所有章节正文与摘要内容（封面信息将保留），此操作不可撤销。\n\n确定要继续吗？',
      '更换模板确认',
      {
        confirmButtonText: '继续更换',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true,
      }
    )
    showChangeTplPicker.value = true
  } catch { /* user cancelled */ }
}

function onTemplateChanged(templateData) {
  const tpl = templateData.template
  const cfg = templateData.config || {}
  currentTemplateId.value = tpl.id
  currentTemplateName.value = tpl.name
  const snapshot = {
    templateId: tpl.id,
    templateName: tpl.name,
    templateType: tpl.type,
    structureJson: cfg.structureJson || '',
    formatJson: cfg.formatJson || '',
    coverFields: cfg.coverFields || '',
  }
  currentTemplateSnapshot.value = snapshot
  maxHeadingLevel.value = extractMaxLevelFromSnapshot(snapshot)
  applyTemplateSkeleton(snapshot, { keepCover: true })
  // 重置选中章节
  const firstChapter = chaptersOnly.value[0]
  activeSectionId.value = firstChapter?.id || 'meta-cover'
  ElMessage.success(`已套用模板「${tpl.name}」`)
}

// 路由参数是 paperId 的唯一权威来源，始终与 URL 保持同步
watch(() => route.params.id, (newId) => {
  if (newId) {
    if (!paperId.value || String(paperId.value) !== String(newId)) {
      paperId.value = newId
      loadPaper()
    }
  }
})

onMounted(async () => {
  await loadPaper()
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

/** 当前论文 ID — route 是唯一权威来源，paperId ref 仅作缓存 */
function resolvePaperId() {
  return paperId.value || route.params.id || null
}

async function goPreview() {
  // 如果有未保存的修改，自动保存后再预览
  if (dirty.value) {
    await savePaper({ mode: 'MANUAL_SAVE', silent: true })
    if (dirty.value) {
      ElMessage.warning('保存失败，请检查网络后重试')
      return
    }
  }

  // 以路由参数为第一优先，ref 为兜底
  const id = resolvePaperId()
  if (!id) {
    ElMessage.warning('请先保存论文再预览')
    return
  }

  const routeData = router.resolve({ name: 'PaperPreview', params: { id } })
  window.open(routeData.href, '_blank')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('paper-access-token')
  sessionStorage.removeItem('paper-user-session')
  window.location.href = '/'
}

// 版本恢复回调
function onRestoreVersion(snapshot) {
  const resolvedSections = Array.isArray(snapshot)
    ? snapshot
    : Array.isArray(snapshot?.sections) ? snapshot.sections : null

  if (!resolvedSections) {
    ElMessage.error('历史版本内容格式不正确')
    return
  }

  sections.value = resolvedSections
  ensureSpecialSections()
  if (Array.isArray(snapshot?.references)) {
    referencesData.value = snapshot.references
  }
  const firstChapter = chaptersOnly.value[0]
  activeSectionId.value = firstChapter?.id || 'meta-cover'
  sidebarTab.value = 'outline'
  dirty.value = true
  ElMessage.success('版本内容已加载，请手动保存')
}

function openCitationPicker() {
  referenceWorkspaceOpen.value = true
  if (!citationCandidates.value.length) {
    ElMessage.warning('请先添加参考文献，再插入引用书签')
    return
  }
  showCitationPicker.value = true
}

function toggleReferenceWorkspace() {
  referenceWorkspaceOpen.value = !referenceWorkspaceOpen.value
}

function buildCitationPayload(reference) {
  return {
    marker: reference.marker || `[${reference.citationNo}]`,
    displayLabel: reference.displayLabel || (reference.year ? `${reference.citationNo} (${reference.year})` : String(reference.citationNo)),
    citationNo: reference.citationNo,
    ref: reference
  }
}

function insertCitationIntoEditor(payload) {
  const inserted = chapterEditorRef.value?.insertCitationTag?.(payload)
  if (!inserted) {
    ElMessage.warning('请先将光标定位到正文编辑区，再插入引用书签')
    return false
  }
  dirty.value = true
  return true
}

function onCitationPicked(reference) {
  if (insertCitationIntoEditor(buildCitationPayload(reference))) {
    showCitationPicker.value = false
    ElMessage.success(`已插入引用 ${reference.marker || `[${reference.citationNo}]`}`)
  }
}

async function onReferenceCite(payload) {
  if (!payload?.marker) {
    return
  }

  const targetChapterId = activeSection.value?.type === 'chapter'
    ? activeSection.value.id
    : lastActiveChapterId.value

  if (!targetChapterId) {
    ElMessage.warning('请先进入一个正文章节，再插入引用书签')
    return
  }

  if (activeSectionId.value !== targetChapterId) {
    activeSectionId.value = targetChapterId
    await nextTick()
  }

  if (insertCitationIntoEditor(payload)) {
    ElMessage.success(`已插入引用 ${payload.marker}`)
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
        <template v-if="currentTemplateName">
          <span class="tpl-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            {{ currentTemplateName }}
          </span>
          <el-button size="small" @click="openChangeTemplate">更换模板</el-button>
        </template>
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
            <el-button size="small" :icon="Plus" circle @click="addRootChapter" title="添加一级标题章节" />
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
              @add="openLevelPicker"
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
            <el-button type="primary" :icon="Plus" @click="addRootChapter">添加章节</el-button>
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
        <!-- 面包屑（自动应用模板预设的标题字号） -->
        <div class="editor-breadcrumb" v-if="activeSection" :style="headingBreadcrumbStyle">
          <span class="breadcrumb-level-tag" v-if="activeSection.level >= 1 && activeSection.level <= 3">
            H{{ activeSection.level }}
          </span>
          {{ activeSection.title }}
        </div>

        <!-- 封面编辑面板 -->
        <div v-if="activeSectionId === 'meta-cover'" class="panel-wrapper">
          <CoverInfoPanel v-model="coverData" />
        </div>

        <!-- 摘要编辑面板 -->
        <div v-else-if="activeSectionId === 'meta-abstract'" class="panel-wrapper">
          <AbstractPanel v-model="abstractData" :format-config="editorFormatConfig" />
        </div>

        <!-- 参考文献面板 -->
        <div v-else-if="activeSectionId === 'meta-references'" class="panel-wrapper">
          <ReferencePanel
            :paper-id="paperId"
            v-model="referencesData"
            @cite="onReferenceCite"
          />
        </div>

        <!-- 致谢面板（使用富文本编辑器） -->
        <div v-else-if="activeSectionId === 'meta-acknowledgment'" class="panel-wrapper">
          <h3 class="panel-title">致谢</h3>
          <PaperEditor
            :key="'ack'"
            :model-value="sections.find(s => s.id === 'meta-acknowledgment')?.content || ''"
            placeholder="撰写致谢…"
            :format-config="editorFormatConfig"
            section-type="acknowledgment"
            :heading-level="0"
            @update:model-value="onAcknowledgmentChange"
          />
        </div>

        <!-- 普通章节编辑器 -->
        <template v-else-if="activeSection && (!activeSection.type || activeSection.type === 'chapter')">
          <div class="chapter-workspace" :class="{ 'reference-collapsed': !referenceWorkspaceOpen }">
            <section class="chapter-main">
              <PaperEditor
                ref="chapterEditorRef"
                :key="activeSection.id"
                :model-value="activeSection.content"
                :placeholder="`撰写「${activeSection.title}」内容…`"
                :format-config="editorFormatConfig"
                section-type="chapter"
                :heading-level="activeSection.level || 0"
                @update:model-value="onContentChange"
              />
              <div class="citation-toolbar">
                <div class="citation-toolbar__info">
                  <strong>正文引用</strong>
                  <span>正文与参考文献现已支持同页协同修改。先在正文里定位光标，再从右侧直接新增、编辑或引用文献。</span>
                </div>
                <div class="citation-toolbar__actions">
                  <el-button size="small" @click="toggleReferenceWorkspace">
                    {{ referenceWorkspaceOpen ? '收起文献面板' : '展开文献面板' }}
                  </el-button>
                  <el-button size="small" type="primary" plain @click="openCitationPicker">
                    插入引用书签
                  </el-button>
                </div>
              </div>
              <div v-if="referencesData.length > 0" class="ref-hint">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:4px"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5C8.26 12.26 8.73 13.02 8.91 14"/></svg>
                点击右侧“引用”后，书签会回到当前正文编辑器；文献内容也可不离开章节页直接维护。
              </div>
            </section>

            <aside v-if="referenceWorkspaceOpen" class="chapter-reference-side panel-wrapper">
              <ReferencePanel
                :paper-id="paperId"
                v-model="referencesData"
                @cite="onReferenceCite"
              />
            </aside>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="editor-placeholder">
          <el-empty description="请从左侧选择一个章节开始编辑" />
        </div>
      </main>
    </div>

    <!-- 更换模板选择器 -->
    <TemplatePickerDialog
      v-model="showChangeTplPicker"
      @confirm="onTemplateChanged"
    />

    <el-dialog v-model="showCitationPicker" title="插入引用书签" width="620px" destroy-on-close>
      <div class="citation-picker">
        <div v-if="citationCandidates.length === 0" class="citation-picker__empty">
          暂无可引用的参考文献
        </div>
        <button
          v-for="reference in citationCandidates"
          :key="reference.id || reference.citationNo"
          class="citation-picker__item"
          @click="onCitationPicked(reference)"
        >
          <span class="citation-picker__badge">{{ reference.displayLabel }}</span>
          <span class="citation-picker__text">{{ reference.formattedText || [reference.authors, reference.title, reference.journal, reference.year].filter(Boolean).join(' / ') || '未命名文献' }}</span>
        </button>
      </div>
    </el-dialog>

    <!-- 层级选择弹出层 -->
    <Teleport to="body">
      <div v-if="levelPickerParentId !== undefined" class="lp-overlay" @click.self="levelPickerParentId = undefined">
        <div class="lp-popover">
          <div class="lp-title">选择标题层级</div>
          <div class="lp-desc">
            <template v-if="isAddingChild">
              作为「<strong>{{ pendingParentTitle }}</strong>」的子章节 · 模板最多 {{ maxHeadingLevel }} 级
            </template>
            <template v-else>
              新增根章节 · 模板最多支持 {{ maxHeadingLevel }} 级标题
            </template>
          </div>
          <div class="lp-options">
            <button
              v-for="lv in maxHeadingLevel"
              :key="lv"
              class="lp-option"
              :class="{ disabled: lv < minChildLevel }"
              :disabled="lv < minChildLevel"
              @click="lv >= minChildLevel && onLevelPicked(lv)"
            >
              <span class="lp-badge">H{{ lv }}</span>
              <span class="lp-label">{{ ['','一级标题','二级标题','三级标题','四级标题','五级标题'][lv] }}</span>
              <span v-if="lv < minChildLevel" class="lp-hint">需比父级深</span>
            </button>
          </div>
          <button class="lp-cancel" @click="levelPickerParentId = undefined">取消</button>
        </div>
      </div>
    </Teleport>
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

.tpl-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 600; color: #386858;
  background: #E3EEE9; padding: 4px 12px; border-radius: 999px;
  max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

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
.breadcrumb-level-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
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

.chapter-workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
}

.chapter-workspace.reference-collapsed {
  grid-template-columns: minmax(0, 1fr);
}

.chapter-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chapter-reference-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chapter-reference-side :deep(.ref-panel) {
  height: 100%;
  overflow-y: auto;
}

.ref-hint {
  padding: 8px 14px; font-size: 12px; color: var(--text-dim);
  background: #fdf6e8; border-radius: 6px; margin-top: 6px;
}
.ref-hint code {
  background: var(--primary-tint); color: var(--primary);
  padding: 1px 5px; border-radius: 3px; font-size: 11px;
}

.citation-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-top: 8px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}

.citation-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.citation-toolbar__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.citation-toolbar__info strong {
  color: var(--text-main);
  font-size: 13px;
}

.citation-toolbar__info span {
  color: var(--text-dim);
  font-size: 12px;
  line-height: 1.6;
}

.citation-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.citation-picker__empty {
  padding: 28px;
  text-align: center;
  color: var(--text-dim);
}

.citation-picker__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all .15s ease;
}

.citation-picker__item:hover {
  border-color: var(--primary);
  background: var(--primary-tint);
}

.citation-picker__badge {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff3e7;
  border: 1px solid #e6b089;
  color: #9a4f1f;
  font-size: 12px;
  font-weight: 700;
}

.citation-picker__text {
  color: var(--text-main);
  line-height: 1.7;
  font-size: 13px;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .sidebar { width: 250px; min-width: 250px; }
  .sidebar.collapsed { margin-left: -250px; }
  .editor-area { padding: 12px 14px 18px; }
  .chapter-workspace {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .sidebar {
    position: fixed; top: 58px; left: 0; bottom: 0; z-index: 100;
    box-shadow: var(--shadow-lg);
  }
  .save-status { display: none; }
  .citation-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .citation-toolbar__actions {
    justify-content: flex-end;
  }
}
</style>

<style>
/* ================================================================
   层级选择弹出层（Teleport to body，需非 scoped 样式）
   ================================================================ */
.lp-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(36,38,34,.35);
  display: flex; align-items: center; justify-content: center;
}
.lp-popover {
  background: #fff; border-radius: 14px; padding: 26px 30px;
  box-shadow: 0 16px 48px rgba(0,0,0,.15);
  min-width: 280px; max-width: 340px; text-align: center;
  animation: lpFadeIn .18s ease;
}
@keyframes lpFadeIn {
  from { opacity: 0; transform: translateY(8px) scale(.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
.lp-title {
  font-family: var(--font-heading, "Noto Serif SC", serif);
  font-size: 18px; font-weight: 700; color: #242622; margin-bottom: 4px;
}
.lp-desc {
  font-size: 12px; color: #858982; margin-bottom: 18px; line-height: 1.5;
}
.lp-desc strong {
  color: #4f776a;
}
.lp-options {
  display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px;
}
.lp-option {
  display: flex; align-items: center; gap: 12px;
  width: 100%; padding: 11px 14px;
  border: 2px solid #e4e3de; border-radius: 10px;
  background: #fff; cursor: pointer; text-align: left;
  transition: all .15s;
}
.lp-option:hover:not(:disabled) {
  border-color: #4f776a; background: #f4f7f5;
}
.lp-option:disabled,
.lp-option.disabled {
  opacity: 0.35; cursor: not-allowed;
}
.lp-option:disabled:hover,
.lp-option.disabled:hover {
  border-color: #e4e3de; background: #fff;
}
.lp-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 34px; height: 34px;
  background: #e8ece9; color: #4f776a;
  border-radius: 8px; font-size: 13px; font-weight: 700;
  flex-shrink: 0;
  transition: all .15s;
}
.lp-option:hover:not(:disabled) .lp-badge {
  background: #4f776a; color: #fff;
}
.lp-label {
  font-size: 14px; font-weight: 500; color: #242622; flex: 1;
}
.lp-hint {
  font-size: 11px; color: #c0c3bd; font-style: italic;
}
.lp-cancel {
  border: 1px solid #d8d9d4; border-radius: 8px;
  background: #fff; color: #858982;
  padding: 8px 20px; font-size: 13px; cursor: pointer;
  transition: all .12s;
}
.lp-cancel:hover {
  background: #f0f1ed; color: #5c605a;
}
</style>
