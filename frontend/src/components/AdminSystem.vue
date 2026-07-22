<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

/* ==================== 默认数据 ==================== */
const DEFAULT_SECTIONS = [
  { key: 'cover',           name: '封面',       required: true,  visible: true, editable: false, order: 1  },
  { key: 'declaration',     name: '原创声明',   required: true,  visible: true, editable: false, order: 2  },
  { key: 'abstract_zh',     name: '中文摘要',   required: true,  visible: true, editable: true,  order: 3  },
  { key: 'keywords_zh',     name: '中文关键词', required: true,  visible: true, editable: true,  order: 4  },
  { key: 'abstract_en',     name: '英文摘要',   required: true,  visible: true, editable: true,  order: 5  },
  { key: 'keywords_en',     name: '英文关键词', required: true,  visible: true, editable: true,  order: 6  },
  { key: 'toc',             name: '目录',       required: true,  visible: true, editable: false, order: 7  },
  { key: 'chapters',        name: '正文',       required: true,  visible: true, editable: true,  order: 8, multiLevel: true, maxLevel: 3 },
  { key: 'references',      name: '参考文献',   required: true,  visible: true, editable: true,  order: 9  },
  { key: 'acknowledgement', name: '致谢',       required: false, visible: true, editable: true,  order: 10 },
  { key: 'appendix',        name: '附录',       required: false, visible: true, editable: true,  order: 11 },
]

const DEFAULT_COVER_FIELDS = [
  { key: 'title',           label: '论文题目',     type: 'text', required: true,  maxLength: 100, order: 1 },
  { key: 'college',         label: '学院',         type: 'text', required: true,  maxLength: 50,  order: 2 },
  { key: 'major',           label: '专业',         type: 'text', required: true,  maxLength: 50,  order: 3 },
  { key: 'studentName',     label: '学生姓名',     type: 'text', required: true,  maxLength: 20,  order: 4 },
  { key: 'studentId',       label: '学号',         type: 'text', required: true,  maxLength: 20,  order: 5 },
  { key: 'supervisor',      label: '指导老师',     type: 'text', required: true,  maxLength: 20,  order: 6 },
  { key: 'supervisorTitle', label: '指导老师职称', type: 'text', required: false, maxLength: 20,  order: 7 },
  { key: 'date',            label: '完成日期',     type: 'date', required: true,  maxLength: 0,   order: 8 },
]

// 预置字体选项（与 fonts.js 映射表保持一致）
const FONT_OPTIONS = ['宋体', '黑体', '楷体', '楷体_GB2312', '仿宋', '仿宋_GB2312', '微软雅黑', 'Times New Roman', 'Arial', 'Courier New']
// 预置字号选项（pt），覆盖中文学术论文常用字号
const FONT_SIZE_OPTIONS = [9, 10, 10.5, 11, 12, 14, 15, 16, 18, 20, 22, 24, 26, 28, 36]

const DEFAULT_FORMAT = {
  page:            { size: 'A4', marginTop: 2.54, marginBottom: 2.54, marginLeft: 3.17, marginRight: 3.17 },
  body:            { font: '宋体', fontSize: 12, lineSpacing: 1.5, firstLineIndent: 2, alignment: 'justify' },
  heading1:        { font: '黑体', fontSize: 16, bold: true, alignment: 'center', spaceBefore: 24, spaceAfter: 18, numbering: true },
  heading2:        { font: '黑体', fontSize: 14, bold: true, alignment: 'left',   spaceBefore: 18, spaceAfter: 12, numbering: true },
  heading3:        { font: '黑体', fontSize: 12, bold: true, alignment: 'left',   spaceBefore: 12, spaceAfter: 6,  numbering: true },
  abstract:        { titleFont: '黑体', titleFontSize: 16, titleAlignment: 'center', bodyFont: '宋体', bodyFontSize: 12, lineSpacing: 1.5 },
  references:      { titleFont: '黑体', titleFontSize: 16, titleAlignment: 'center', font: '宋体', fontSize: 10.5, lineSpacing: 1.5, numberFormat: '[N]', hangingIndent: true, hangingIndentSize: 2 },
  acknowledgement: { titleFont: '黑体', titleFontSize: 16, titleAlignment: 'center', bodyFont: '宋体', bodyFontSize: 12, lineSpacing: 1.5, firstLineIndent: 2 },
  coverTitle:      { font: '黑体', fontSize: 18, bold: true, alignment: 'center' },
  coverBody:       { font: '宋体', fontSize: 14, alignment: 'left', lineSpacing: 2.0 },
  header:          { text: '{collegeName}本科毕业论文', font: '宋体', fontSize: 9, alignment: 'center', showDivider: true },
  footer:          { showPageNumber: true, pageNumberFormat: 'arabic', alignment: 'center', font: 'Times New Roman', fontSize: 9 },
}

const FORMAT_SCHEMA = [
  { group: 'page', label: '页面设置', fields: [
    { key: 'size',         label: '页面尺寸',     type: 'select', options: ['A4','A3','B5'] },
    { key: 'marginTop',    label: '上边距(cm)',   type: 'number', step: '0.01' },
    { key: 'marginBottom', label: '下边距(cm)',   type: 'number', step: '0.01' },
    { key: 'marginLeft',   label: '左边距(cm)',   type: 'number', step: '0.01' },
    { key: 'marginRight',  label: '右边距(cm)',   type: 'number', step: '0.01' },
  ]},
  { group: 'body', label: '正文样式', fields: [
    { key: 'font',            label: '字体',          type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',        label: '字号(pt)',      type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'lineSpacing',     label: '行间距',        type: 'number', step: '0.1' },
    { key: 'firstLineIndent', label: '首行缩进(字符)',type: 'number', step: '1' },
    { key: 'alignment',       label: '对齐',          type: 'select', options: ['justify','left','center','right'] },
  ]},
  { group: 'heading1', label: '一级标题', fields: [
    { key: 'font',        label: '字体',    type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',    label: '字号',    type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'bold',        label: '粗体',    type: 'checkbox' },
    { key: 'alignment',   label: '对齐',    type: 'select', options: ['center','left','right'] },
    { key: 'spaceBefore', label: '段前(pt)',type: 'number' },
    { key: 'spaceAfter',  label: '段后(pt)',type: 'number' },
    { key: 'numbering',   label: '自动编号',type: 'checkbox' },
  ]},
  { group: 'heading2', label: '二级标题', fields: [
    { key: 'font',        label: '字体',    type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',    label: '字号',    type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'bold',        label: '粗体',    type: 'checkbox' },
    { key: 'alignment',   label: '对齐',    type: 'select', options: ['left','center','right'] },
    { key: 'spaceBefore', label: '段前(pt)',type: 'number' },
    { key: 'spaceAfter',  label: '段后(pt)',type: 'number' },
    { key: 'numbering',   label: '自动编号',type: 'checkbox' },
  ]},
  { group: 'heading3', label: '三级标题', fields: [
    { key: 'font',        label: '字体',    type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',    label: '字号',    type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'bold',        label: '粗体',    type: 'checkbox' },
    { key: 'alignment',   label: '对齐',    type: 'select', options: ['left','center','right'] },
    { key: 'spaceBefore', label: '段前(pt)',type: 'number' },
    { key: 'spaceAfter',  label: '段后(pt)',type: 'number' },
    { key: 'numbering',   label: '自动编号',type: 'checkbox' },
  ]},
  { group: 'abstract', label: '摘要样式', fields: [
    { key: 'titleFont',      label: '标题字体', type: 'select', options: FONT_OPTIONS },
    { key: 'titleFontSize',  label: '标题字号', type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'titleAlignment', label: '标题对齐', type: 'select', options: ['center','left'] },
    { key: 'bodyFont',       label: '正文字体', type: 'select', options: FONT_OPTIONS },
    { key: 'bodyFontSize',   label: '正文字号', type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'lineSpacing',    label: '行间距',   type: 'number', step: '0.1' },
  ]},
  { group: 'references', label: '参考文献', fields: [
    { key: 'titleFont',         label: '标题字体',     type: 'select', options: FONT_OPTIONS },
    { key: 'titleFontSize',     label: '标题字号',     type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'titleAlignment',    label: '标题对齐',     type: 'select', options: ['center','left'] },
    { key: 'font',              label: '正文字体',     type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',          label: '正文字号',     type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'lineSpacing',       label: '行间距',       type: 'number', step: '0.1' },
    { key: 'numberFormat',      label: '编号格式',     type: 'text' },
    { key: 'hangingIndent',     label: '悬挂缩进',     type: 'checkbox' },
    { key: 'hangingIndentSize', label: '缩进量(字符)', type: 'number' },
  ]},
  { group: 'acknowledgement', label: '致谢样式', fields: [
    { key: 'titleFont',       label: '标题字体',      type: 'select', options: FONT_OPTIONS },
    { key: 'titleFontSize',   label: '标题字号',      type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'titleAlignment',  label: '标题对齐',      type: 'select', options: ['center','left'] },
    { key: 'bodyFont',        label: '正文字体',      type: 'select', options: FONT_OPTIONS },
    { key: 'bodyFontSize',    label: '正文字号',      type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'lineSpacing',     label: '行间距',        type: 'number', step: '0.1' },
    { key: 'firstLineIndent', label: '首行缩进(字符)',type: 'number', step: '1' },
  ]},
  { group: 'coverTitle', label: '封面标题', fields: [
    { key: 'font',      label: '字体', type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',  label: '字号', type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'bold',      label: '粗体', type: 'checkbox' },
    { key: 'alignment', label: '对齐', type: 'select', options: ['center','left','right'] },
  ]},
  { group: 'coverBody', label: '封面正文', fields: [
    { key: 'font',        label: '字体',   type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',    label: '字号',   type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'alignment',   label: '对齐',   type: 'select', options: ['left','center','justify'] },
    { key: 'lineSpacing', label: '行间距', type: 'number', step: '0.1' },
  ]},
  { group: 'header', label: '页眉', fields: [
    { key: 'text',        label: '页眉文字', type: 'text' },
    { key: 'font',        label: '字体',     type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',    label: '字号',     type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'alignment',   label: '对齐',     type: 'select', options: ['center','left','right'] },
    { key: 'showDivider', label: '显示分隔线',type: 'checkbox' },
  ]},
  { group: 'footer', label: '页脚', fields: [
    { key: 'showPageNumber',   label: '显示页码', type: 'checkbox' },
    { key: 'pageNumberFormat', label: '页码格式', type: 'select', options: ['arabic','roman'] },
    { key: 'font',             label: '字体',     type: 'select', options: FONT_OPTIONS },
    { key: 'fontSize',         label: '字号',     type: 'select', options: FONT_SIZE_OPTIONS },
    { key: 'alignment',        label: '对齐',     type: 'select', options: ['center','left','right'] },
  ]},
]

const TEMPLATE_TYPES = ['毕业论文', '课程论文', '项目论文']

/* ==================== 状态 ==================== */
const tab = ref('colleges')
const colleges = ref([])
const templates = ref([])
const loading = ref(false)
const notice = ref('')

// 学院搜索 & 模板筛选
const collegeKeyword = ref('')
const tplKeyword = ref('')
const tplTypeFilter = ref('')
const tplCollegeFilter = ref('')
const tplStatusFilter = ref('')

// 学院表单
const collegeModal = ref(false)
const collegeForm = reactive({ id: null, name: '', description: '' })

// 模板表单（四标签页）
const templateModal = ref(false)
const templateActiveTab = ref(0)
const templateForm = reactive({
  id: null,
  name: '',
  type: '毕业论文',
  collegeId: '',
  description: '',
  status: 0,
  sections: [],
  coverFields: [],
  format: {},
})

// 模板详情弹窗
const detailModal = ref(false)
const detailData = ref({ template: null, sections: [], coverFields: [], format: {} })

// 删除确认
const confirmModal = ref(false)
const confirmText = ref('')
const confirmAction = ref(null)

const collegeNames = computed(() =>
  Object.fromEntries(colleges.value.map(c => [c.id, c.name]))
)

/* ==================== 请求辅助 ==================== */
async function request(method, path, body) {
  const token = localStorage.getItem('paper-access-token') || localStorage.getItem('token')
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
  }
  if (body !== undefined) opts.body = JSON.stringify(body)
  const res = await fetch('http://localhost:8080' + path, opts)
  const data = await res.json().catch(() => ({ code: res.status, message: '响应解析失败' }))
  if (data.code !== 200) throw new Error(data.message || '请求失败')
  return data
}

function tip(msg) {
  notice.value = msg
  setTimeout(() => (notice.value = ''), 2400)
}

function fmtDate(str) {
  if (!str) return '—'
  return str.replace('T', ' ').slice(0, 16)
}

/* ==================== 学院管理 ==================== */
async function loadColleges() {
  loading.value = true
  try {
    const qs = collegeKeyword.value ? '?keyword=' + encodeURIComponent(collegeKeyword.value) : ''
    const r = await request('GET', '/api/admin/colleges' + qs)
    colleges.value = r.data || []
  } catch (e) { tip(e.message) }
  finally { loading.value = false }
}

function openCollege(c) {
  if (c) {
    collegeForm.id = c.id; collegeForm.name = c.name; collegeForm.description = c.description || ''
  } else {
    collegeForm.id = null; collegeForm.name = ''; collegeForm.description = ''
  }
  collegeModal.value = true
}

async function saveCollege() {
  const name = collegeForm.name.trim()
  if (!name) return tip('学院名称不能为空')
  try {
    const body = { name, description: collegeForm.description.trim() }
    if (collegeForm.id) await request('PUT', '/api/admin/colleges/' + collegeForm.id, body)
    else await request('POST', '/api/admin/colleges', body)
    collegeModal.value = false
    await loadColleges()
    tip('保存成功')
  } catch (e) { tip(e.message) }
}

function askRemoveCollege(c) {
  confirmText.value = '确定删除学院「' + c.name + '」吗？删除后无法恢复。'
  confirmAction.value = async () => {
    try {
      await request('DELETE', '/api/admin/colleges/' + c.id)
      await loadColleges()
      tip('删除成功')
    } catch (e) { tip(e.message) }
  }
  confirmModal.value = true
}

/* ==================== 模板管理 ==================== */
async function loadTemplates() {
  loading.value = true
  try {
    const q = new URLSearchParams()
    if (tplCollegeFilter.value) q.set('collegeId', tplCollegeFilter.value)
    if (tplTypeFilter.value) q.set('type', tplTypeFilter.value)
    if (tplStatusFilter.value !== '') q.set('status', tplStatusFilter.value)
    const r = await request('GET', '/api/admin/templates' + (q.size ? '?' + q : ''))
    templates.value = r.data || []
  } catch (e) { tip(e.message) }
  finally { loading.value = false }
}

const filteredTemplates = computed(() => {
  const kw = tplKeyword.value.trim().toLowerCase()
  return kw ? templates.value.filter(t => (t.name || '').toLowerCase().includes(kw)) : templates.value
})

function resetTemplateForm(existing) {
  templateForm.id = existing?.id || null
  templateForm.name = existing?.name || ''
  templateForm.type = existing?.type || '毕业论文'
  templateForm.collegeId = existing ? String(existing.collegeId) : (colleges.value[0] ? String(colleges.value[0].id) : '')
  templateForm.description = existing?.description || ''
  templateForm.status = existing?.status ?? 0
  templateForm.sections = JSON.parse(JSON.stringify(DEFAULT_SECTIONS))
  templateForm.coverFields = JSON.parse(JSON.stringify(DEFAULT_COVER_FIELDS))
  templateForm.format = JSON.parse(JSON.stringify(DEFAULT_FORMAT))
  templateActiveTab.value = 0
}

async function openTemplateAdd() {
  resetTemplateForm(null)
  templateModal.value = true
}

async function openTemplateEdit(t) {
  resetTemplateForm(t)
  try {
    const r = await request('GET', '/api/admin/templates/' + t.id)
    const cfg = r.data?.config || {}
    if (cfg.structureJson) {
      try {
        const parsed = JSON.parse(cfg.structureJson)
        if (parsed.sections && Array.isArray(parsed.sections)) templateForm.sections = parsed.sections
      } catch (_) {}
    }
    if (cfg.coverFields) {
      try {
        const parsed = JSON.parse(cfg.coverFields)
        if (Array.isArray(parsed)) templateForm.coverFields = parsed
      } catch (_) {}
    }
    if (cfg.formatJson) {
      try {
        const parsed = JSON.parse(cfg.formatJson)
        if (parsed && typeof parsed === 'object') templateForm.format = { ...DEFAULT_FORMAT, ...parsed }
      } catch (_) {}
    }
    templateModal.value = true
  } catch (e) { tip(e.message) }
}

function moveCover(idx, dir) {
  const t = idx + dir
  if (t < 0 || t >= templateForm.coverFields.length) return
  const arr = templateForm.coverFields
  ;[arr[idx], arr[t]] = [arr[t], arr[idx]]
  arr.forEach((f, i) => (f.order = i + 1))
}

function addCoverField() {
  templateForm.coverFields.push({
    key: '', label: '新字段', type: 'text',
    required: false, maxLength: 50,
    order: templateForm.coverFields.length + 1,
  })
}

function removeCover(idx) {
  templateForm.coverFields.splice(idx, 1)
  templateForm.coverFields.forEach((f, i) => (f.order = i + 1))
}

async function saveTemplate() {
  const name = templateForm.name.trim()
  if (!name) return tip('模板名称不能为空')
  if (!templateForm.collegeId) return tip('请选择所属学院')
  if (!templateForm.type) return tip('请选择模板类型')

  const body = {
    name,
    type: templateForm.type,
    collegeId: Number(templateForm.collegeId),
    description: templateForm.description.trim(),
    status: templateForm.status,
    structureJson: JSON.stringify({ sections: templateForm.sections }),
    coverFields: JSON.stringify(templateForm.coverFields),
    formatJson: JSON.stringify(templateForm.format),
  }
  try {
    if (templateForm.id) await request('PUT', '/api/admin/templates/' + templateForm.id, body)
    else await request('POST', '/api/admin/templates', body)
    templateModal.value = false
    await loadTemplates()
    tip('保存成功')
  } catch (e) { tip(e.message) }
}

async function switchTemplateStatus(t) {
  try {
    const next = t.status === 1 ? 0 : 1
    await request('PUT', '/api/admin/templates/' + t.id + '/status?status=' + next)
    await loadTemplates()
    tip(next === 1 ? '已启用' : '已停用')
  } catch (e) { tip(e.message) }
}

function askRemoveTemplate(t) {
  confirmText.value = '确定删除模板「' + t.name + '」吗？已使用该模板的论文不受影响。'
  confirmAction.value = async () => {
    try {
      await request('DELETE', '/api/admin/templates/' + t.id)
      await loadTemplates()
      tip('删除成功')
    } catch (e) { tip(e.message) }
  }
  confirmModal.value = true
}

/* ==================== 模板详情 ==================== */
async function openTemplateDetail(t) {
  try {
    const r = await request('GET', '/api/admin/templates/' + t.id)
    const tpl = r.data?.template || t
    const cfg = r.data?.config || {}
    let sections = [], coverFields = [], format = {}
    try { sections = JSON.parse(cfg.structureJson || '{}').sections || [] } catch (_) {}
    try { coverFields = JSON.parse(cfg.coverFields || '[]') } catch (_) {}
    try { format = JSON.parse(cfg.formatJson || '{}') } catch (_) {}
    detailData.value = { template: tpl, sections, coverFields, format }
    detailModal.value = true
  } catch (e) { tip(e.message) }
}

/* ==================== 确认对话 ==================== */
async function runConfirm() {
  if (confirmAction.value) await confirmAction.value()
  confirmModal.value = false
  confirmAction.value = null
}

/* ==================== 切换 tab ==================== */
function switchTab(v) {
  tab.value = v
  if (v === 'colleges') loadColleges()
  else loadTemplates()
}

function isLocked(sec) {
  return sec.key === 'cover' || sec.key === 'declaration' || sec.key === 'toc'
}

const TAB_LABELS = ['基本信息', '封面字段', '章节结构', '格式参数']

onMounted(async () => {
  await loadColleges()
  await loadTemplates()
})
</script>

<template>
  <section class="admin-vue">
    <!-- 主 Tab -->
    <div class="admin-tabs">
      <button :class="{ active: tab === 'colleges' }"  @click="switchTab('colleges')">学院管理</button>
      <button :class="{ active: tab === 'templates' }" @click="switchTab('templates')">模板管理</button>
    </div>

    <!-- 学院列表 -->
    <div v-if="tab === 'colleges'" class="admin-card">
      <div class="admin-head">
        <b>学院列表</b>
        <div>
          <input v-model="collegeKeyword" placeholder="搜索学院名称" @keyup.enter="loadColleges" />
          <button @click="loadColleges">搜索</button>
          <button class="admin-primary" @click="openCollege(null)">＋ 新增学院</button>
        </div>
      </div>
      <div class="admin-table">
        <div class="admin-tr admin-th">
          <span>ID</span><span>学院名称</span><span>描述</span><span>创建时间</span><span>操作</span>
        </div>
        <div v-if="loading" class="admin-empty">正在加载…</div>
        <div v-else-if="!colleges.length" class="admin-empty">暂无学院数据</div>
        <div v-for="c in colleges" :key="c.id" class="admin-tr">
          <span>{{ c.id }}</span>
          <b>{{ c.name }}</b>
          <span>{{ c.description || '—' }}</span>
          <span>{{ fmtDate(c.createTime) }}</span>
          <span>
            <button @click="openCollege(c)">编辑</button>
            <button class="admin-danger" @click="askRemoveCollege(c)">删除</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 模板列表 -->
    <div v-else class="admin-card">
      <div class="admin-head">
        <b>模板列表</b>
        <div>
          <select v-model="tplCollegeFilter" @change="loadTemplates">
            <option value="">全部学院</option>
            <option v-for="c in colleges" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
          <select v-model="tplTypeFilter" @change="loadTemplates">
            <option value="">全部类型</option>
            <option v-for="t in TEMPLATE_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
          <select v-model="tplStatusFilter" @change="loadTemplates">
            <option value="">全部状态</option>
            <option value="1">已启用</option>
            <option value="0">已停用</option>
          </select>
          <input v-model="tplKeyword" placeholder="搜索模板名称" />
          <button class="admin-primary" @click="openTemplateAdd">＋ 新增模板</button>
        </div>
      </div>
      <div class="admin-table">
        <div class="admin-tr template admin-th">
          <span>模板名称</span><span>类型</span><span>所属学院</span><span>版本 / 状态</span><span>操作</span>
        </div>
        <div v-if="loading" class="admin-empty">正在加载…</div>
        <div v-else-if="!filteredTemplates.length" class="admin-empty">暂无模板数据</div>
        <div v-for="t in filteredTemplates" :key="t.id" class="admin-tr template">
          <b>{{ t.name }}</b>
          <span>{{ t.type }}</span>
          <span>{{ collegeNames[t.collegeId] || t.collegeId }}</span>
          <span>v{{ t.version }} · {{ t.status === 1 ? '已启用' : '已停用' }}</span>
          <span>
            <button @click="openTemplateDetail(t)">详情</button>
            <button @click="openTemplateEdit(t)">编辑</button>
            <button @click="switchTemplateStatus(t)">{{ t.status === 1 ? '停用' : '启用' }}</button>
            <button class="admin-danger" @click="askRemoveTemplate(t)">删除</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 学院表单弹窗 -->
    <div v-if="collegeModal" class="admin-overlay" @click.self="collegeModal = false">
      <div class="admin-dialog">
        <h3>{{ collegeForm.id ? '编辑学院' : '新增学院' }}</h3>
        <label>学院名称 <span class="required">*</span>
          <input v-model="collegeForm.name" maxlength="100" placeholder="请输入学院名称" />
        </label>
        <label>描述
          <textarea v-model="collegeForm.description" placeholder="选填" rows="3"></textarea>
        </label>
        <div>
          <button @click="collegeModal = false">取消</button>
          <button class="admin-primary" @click="saveCollege">保存</button>
        </div>
      </div>
    </div>

    <!-- 模板表单弹窗（四 Tab） -->
    <div v-if="templateModal" class="admin-overlay" @click.self="templateModal = false">
      <div class="admin-dialog admin-dialog-lg">
        <h3>{{ templateForm.id ? '编辑模板' : '新增模板' }}</h3>

        <div class="admin-form-tabs">
          <button v-for="(t, i) in TAB_LABELS" :key="i"
                  :class="{ on: templateActiveTab === i }"
                  @click="templateActiveTab = i">{{ i + 1 }}. {{ t }}</button>
        </div>

        <!-- Tab 0 · 基本信息 -->
        <div v-show="templateActiveTab === 0" class="admin-form-body">
          <div class="admin-form-row">
            <label>模板名称 <span class="required">*</span>
              <input v-model="templateForm.name" maxlength="200" placeholder="请输入模板名称" />
            </label>
            <label>模板类型 <span class="required">*</span>
              <select v-model="templateForm.type">
                <option v-for="t in TEMPLATE_TYPES" :key="t" :value="t">{{ t }}</option>
              </select>
            </label>
          </div>
          <div class="admin-form-row">
            <label>所属学院 <span class="required">*</span>
              <select v-model="templateForm.collegeId">
                <option value="" disabled>请选择</option>
                <option v-for="c in colleges" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
              </select>
            </label>
            <label>状态
              <select v-model.number="templateForm.status">
                <option :value="0">停用</option>
                <option :value="1">启用</option>
              </select>
            </label>
          </div>
          <label>描述
            <textarea v-model="templateForm.description" rows="3" placeholder="选填"></textarea>
          </label>
        </div>

        <!-- Tab 1 · 封面字段 -->
        <div v-show="templateActiveTab === 1" class="admin-form-body">
          <div class="admin-form-note">
            <span>定义论文封面需要学生填写的字段</span>
            <button @click="addCoverField">＋ 新增字段</button>
          </div>
          <div class="admin-cf-head">
            <span>序</span><span>标签名</span><span>Key</span><span>类型</span><span>最大长度</span><span>必填</span><span>操作</span>
          </div>
          <div v-for="(f, i) in templateForm.coverFields" :key="i" class="admin-cf-row">
            <span class="admin-cf-order">{{ i + 1 }}</span>
            <input v-model="f.label" placeholder="标签名" />
            <input v-model="f.key" placeholder="key(英文)" />
            <select v-model="f.type">
              <option value="text">文本</option>
              <option value="date">日期</option>
            </select>
            <input v-model.number="f.maxLength" type="number" min="0" />
            <span class="admin-cf-req">
              <input type="checkbox" v-model="f.required" />
            </span>
            <span class="admin-cf-actions">
              <button @click="moveCover(i, -1)" title="上移">↑</button>
              <button @click="moveCover(i, 1)"  title="下移">↓</button>
              <button class="admin-danger" @click="removeCover(i)" title="删除">×</button>
            </span>
          </div>
        </div>

        <!-- Tab 2 · 章节结构 -->
        <div v-show="templateActiveTab === 2" class="admin-form-body">
          <div class="admin-form-note">
            <span>控制每节的显示、必填与可编辑状态；封面 / 原创声明 / 目录为固定内容不可编辑</span>
          </div>
          <table class="admin-sec-table">
            <thead>
              <tr><th>顺序</th><th>章节</th><th>必填</th><th>显示</th><th>可编辑</th><th>最大层级</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in templateForm.sections" :key="s.key">
                <td>{{ s.order }}</td>
                <td>{{ s.name }}</td>
                <td><input type="checkbox" v-model="s.required" /></td>
                <td><input type="checkbox" v-model="s.visible" /></td>
                <td>
                  <span v-if="isLocked(s)" class="admin-locked">固定</span>
                  <input v-else type="checkbox" v-model="s.editable" />
                </td>
                <td>
                  <input v-if="s.key === 'chapters'" type="number" v-model.number="s.maxLevel" min="1" max="5" style="width:50px" />
                  <span v-else style="color:#aaa">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Tab 3 · 格式参数 -->
        <div v-show="templateActiveTab === 3" class="admin-form-body">
          <div v-for="g in FORMAT_SCHEMA" :key="g.group" class="admin-fmt-group">
            <div class="admin-fmt-title">{{ g.label }}</div>
            <div class="admin-fmt-grid">
              <div v-for="f in g.fields" :key="f.key" class="admin-fmt-field">
                <label>{{ f.label }}</label>
                <template v-if="f.type === 'checkbox'">
                  <input type="checkbox" v-model="templateForm.format[g.group][f.key]" />
                </template>
                <template v-else-if="f.type === 'select'">
                  <select v-model="templateForm.format[g.group][f.key]">
                    <option v-for="o in f.options" :key="o" :value="o">{{ o }}</option>
                  </select>
                </template>
                <template v-else-if="f.type === 'number'">
                  <input type="number" :step="f.step || 1" v-model.number="templateForm.format[g.group][f.key]" />
                </template>
                <template v-else>
                  <input type="text" v-model="templateForm.format[g.group][f.key]" />
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="admin-dialog-foot">
          <button @click="templateModal = false">取消</button>
          <button class="admin-primary" @click="saveTemplate">保存</button>
        </div>
      </div>
    </div>

    <!-- 模板详情弹窗 -->
    <div v-if="detailModal" class="admin-overlay" @click.self="detailModal = false">
      <div class="admin-dialog admin-dialog-lg">
        <h3>模板详情 · {{ detailData.template?.name }}</h3>

        <div class="admin-detail-section">
          <div class="admin-detail-title">基本信息</div>
          <div class="admin-detail-kv">
            <div>模板名称</div><div>{{ detailData.template?.name }}</div>
            <div>模板类型</div><div>{{ detailData.template?.type }}</div>
            <div>所属学院</div><div>{{ collegeNames[detailData.template?.collegeId] || detailData.template?.collegeId }}</div>
            <div>状态</div><div>{{ detailData.template?.status === 1 ? '已启用' : '已停用' }}</div>
            <div>版本</div><div>v{{ detailData.template?.version }}</div>
            <div>更新时间</div><div>{{ fmtDate(detailData.template?.updateTime) }}</div>
          </div>
        </div>

        <div class="admin-detail-section">
          <div class="admin-detail-title">章节结构</div>
          <table class="admin-sec-table">
            <thead><tr><th>顺序</th><th>章节</th><th>必填</th><th>显示</th><th>可编辑</th></tr></thead>
            <tbody>
              <tr v-for="s in detailData.sections" :key="s.key">
                <td>{{ s.order }}</td><td>{{ s.name }}</td>
                <td>{{ s.required ? '是' : '否' }}</td>
                <td>{{ s.visible ? '是' : '否' }}</td>
                <td>{{ s.editable ? '是' : '否' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="admin-detail-section">
          <div class="admin-detail-title">封面字段</div>
          <table class="admin-sec-table">
            <thead><tr><th>序</th><th>标签</th><th>Key</th><th>类型</th><th>必填</th><th>最大长度</th></tr></thead>
            <tbody>
              <tr v-for="f in detailData.coverFields" :key="f.key">
                <td>{{ f.order }}</td><td>{{ f.label }}</td><td><code>{{ f.key }}</code></td>
                <td>{{ f.type }}</td><td>{{ f.required ? '是' : '否' }}</td><td>{{ f.maxLength || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="admin-detail-section">
          <div class="admin-detail-title">格式参数</div>
          <div v-for="(v, k) in detailData.format" :key="k" class="admin-fmt-group">
            <div class="admin-fmt-title">{{ FORMAT_SCHEMA.find(g => g.group === k)?.label || k }}</div>
            <div class="admin-detail-kv">
              <template v-for="(vv, kk) in v" :key="kk">
                <div>{{ FORMAT_SCHEMA.find(g => g.group === k)?.fields.find(f => f.key === kk)?.label || kk }}</div>
                <div>{{ String(vv) }}</div>
              </template>
            </div>
          </div>
        </div>

        <div class="admin-dialog-foot">
          <button @click="detailModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 通用二次确认 -->
    <div v-if="confirmModal" class="admin-overlay" @click.self="confirmModal = false">
      <div class="admin-dialog admin-dialog-sm">
        <h3>请确认</h3>
        <p>{{ confirmText }}</p>
        <div>
          <button @click="confirmModal = false">取消</button>
          <button class="admin-danger admin-primary-inv" @click="runConfirm">确认删除</button>
        </div>
      </div>
    </div>

    <div v-if="notice" class="admin-notice">{{ notice }}</div>
  </section>
</template>

<style scoped>
.admin-vue { padding: 24px 30px; max-width: 1280px; margin: 0 auto; }

.admin-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.admin-tabs button { padding: 9px 18px; }
.admin-tabs .active { background: #4f776a; color: #fff; border-color: #4f776a; }

.admin-card { background: #fff; border: 1px solid #e4e3de; border-radius: 13px; overflow: hidden; }

.admin-head { padding: 16px 18px; border-bottom: 1px solid #e4e3de; display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.admin-head > div { display: flex; gap: 8px; flex-wrap: wrap; }
.admin-head input { width: 200px; }
.admin-primary { background: #4f776a; color: #fff; border-color: #4f776a; }
.admin-primary-inv { background: #a8544d; color: #fff !important; border-color: #a8544d; }

.admin-table { width: 100%; }
.admin-tr { display: grid; grid-template-columns: 60px 1.2fr 1.5fr 150px 170px; gap: 12px; align-items: center; padding: 13px 18px; border-bottom: 1px solid #eee; }
.admin-tr.template { grid-template-columns: 1.3fr .7fr 1fr .8fr 280px; }
.admin-th { background: #f6f5f2; color: #777; font-size: 12px; font-weight: 700; }
.admin-tr > span:last-child { display: flex; gap: 5px; flex-wrap: wrap; }
.admin-danger { color: #a8544d; }
.admin-empty { text-align: center; padding: 50px; color: #888; }

.admin-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); z-index: 30; display: grid; place-items: center; }
.admin-dialog { width: 520px; background: #fff; padding: 22px 24px; border-radius: 13px; max-height: 88vh; overflow-y: auto; }
.admin-dialog-sm { width: 420px; }
.admin-dialog-lg { width: 820px; }
.admin-dialog h3 { margin: 0 0 16px; font-size: 18px; }
.admin-dialog label { display: block; margin: 12px 0; color: #4c5148; font-size: 13px; }
.admin-dialog input, .admin-dialog textarea, .admin-dialog select { width: 100%; margin-top: 5px; padding: 8px 10px; }
.admin-dialog textarea { min-height: 80px; resize: vertical; }
.admin-dialog > div:last-child, .admin-dialog-foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
.required { color: #c1121f; }

/* 模板表单专用 */
.admin-form-tabs { display: flex; border-bottom: 1px solid #e4e3de; margin-bottom: 16px; gap: 4px; }
.admin-form-tabs button {
  padding: 8px 16px; border: 0; background: transparent; color: #777;
  border-bottom: 2px solid transparent; margin-bottom: -1px; font-weight: 500;
  border-radius: 0;
}
.admin-form-tabs button.on { color: #4f776a; border-bottom-color: #4f776a; font-weight: 700; }
.admin-form-body { max-height: 60vh; overflow-y: auto; padding-right: 2px; }
.admin-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.admin-form-row > label { margin: 12px 0 0; }
.admin-form-note {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; background: #f6f5f2; border-radius: 8px; margin-bottom: 12px;
  font-size: 13px; color: #666;
}

/* 封面字段编辑器 */
.admin-cf-head, .admin-cf-row {
  display: grid; grid-template-columns: 40px 1.5fr 1.5fr 90px 90px 50px 100px;
  gap: 8px; align-items: center; padding: 8px 10px;
}
.admin-cf-head { font-size: 12px; font-weight: 700; color: #888; border-bottom: 1px solid #e4e3de; }
.admin-cf-row { border-bottom: 1px solid #f0efeb; }
.admin-cf-row input, .admin-cf-row select { margin: 0; padding: 6px 8px; }
.admin-cf-order { background: #4f776a; color: #fff; border-radius: 6px; text-align: center; padding: 4px 0; font-weight: 700; font-size: 12px; }
.admin-cf-req, .admin-cf-actions { display: flex; justify-content: center; align-items: center; gap: 4px; }
.admin-cf-actions button { padding: 4px 8px; }

/* 章节结构表格 */
.admin-sec-table { width: 100%; border-collapse: collapse; }
.admin-sec-table th, .admin-sec-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0efeb; }
.admin-sec-table th { background: #f6f5f2; color: #666; font-size: 12px; font-weight: 700; }
.admin-sec-table input[type="checkbox"] { width: 16px; height: 16px; }
.admin-locked { color: #999; font-size: 11px; padding: 2px 6px; background: #eee; border-radius: 4px; }

/* 格式参数分组 */
.admin-fmt-group { margin-bottom: 20px; padding: 14px; background: #faf9f6; border-radius: 10px; border: 1px solid #ececea; }
.admin-fmt-title {
  font-size: 12px; font-weight: 700; color: #4f776a;
  letter-spacing: .04em; text-transform: uppercase;
  margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid #e4e3de;
}
.admin-fmt-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.admin-fmt-field { display: flex; flex-direction: column; gap: 4px; }
.admin-fmt-field label { margin: 0; font-size: 11px; color: #777; font-weight: 500; }
.admin-fmt-field input, .admin-fmt-field select { margin: 0; padding: 6px 8px; font-size: 13px; }
.admin-fmt-field input[type="checkbox"] { width: 16px; height: 16px; margin-top: 2px; }

/* 详情视图 */
.admin-detail-section { margin-bottom: 20px; padding: 14px 16px; background: #faf9f6; border-radius: 10px; border: 1px solid #ececea; }
.admin-detail-title { font-weight: 700; color: #4f776a; font-size: 13px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #e4e3de; }
.admin-detail-kv { display: grid; grid-template-columns: 130px 1fr; gap: 6px 0; font-size: 13px; }
.admin-detail-kv > div:nth-child(odd) { color: #888; }
.admin-detail-kv > div:nth-child(even) { color: #333; }
.admin-detail-section code { background: #fff; padding: 2px 6px; border-radius: 4px; border: 1px solid #e4e3de; font-size: 12px; }

.admin-notice { position: fixed; right: 24px; bottom: 24px; background: #30322f; color: #fff; padding: 11px 18px; border-radius: 8px; z-index: 50; font-size: 13px; box-shadow: 0 8px 20px rgba(0,0,0,.15); }
</style>
