<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Download, FullScreen, View, Printer } from '@element-plus/icons-vue'
import axios from 'axios'
import { extractEditorFonts } from '../../utils/fonts.js'

const route = useRoute()
const router = useRouter()
const paperId = route.params.id

// ─── 数据状态 ───
const paper = ref(null)
const loading = ref(true)
const exporting = ref(false)

const coverInfo = ref({})
const abstractData = ref({ abstractCn: '', abstractEn: '', keywordsCn: [], keywordsEn: [] })
const references = ref([])
const acknowledgment = ref('')
const chapters = ref([])

// ─── 模板格式配置（从 templateSnapshot 解析） ───
const templateFormatConfig = ref(null)
const headingFontStyles = computed(() => {
  const fonts = extractEditorFonts(templateFormatConfig.value)
  const style = {}
  if (fonts.heading1FontSize) style['--pv-h1-size'] = fonts.heading1FontSize + 'pt'
  if (fonts.heading2FontSize) style['--pv-h2-size'] = fonts.heading2FontSize + 'pt'
  if (fonts.heading3FontSize) style['--pv-h3-size'] = fonts.heading3FontSize + 'pt'
  if (fonts.heading1Font)     style['--pv-h1-font'] = fonts.heading1Font
  if (fonts.heading2Font)     style['--pv-h2-font'] = fonts.heading2Font
  if (fonts.heading3Font)     style['--pv-h3-font'] = fonts.heading3Font
  return style
})

// ─── 分页状态 ───
const currentPage = ref(1)
const totalPages = computed(() => {
  let count = 1 // cover
  if (abstractData.value.abstractCn) count++
  if (abstractData.value.abstractEn) count++
  if (tocChapters.value.length > 0) count++
  count += tocChapters.value.length
  if (references.value.length > 0) count++
  if (acknowledgment.value) count++
  return count
})
const viewMode = ref('continuous') // 'continuous' | 'paginated'

// ─── 加载论文数据 ───
async function loadPaper() {
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('paper-access-token')
    const res = await axios.get(`http://localhost:8080/api/papers/${paperId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    paper.value = res.data?.data || res.data
    // 解析模板快照中的格式配置，用于标题字号
    try {
      const snap = paper.value?.templateSnapshot
      if (snap) {
        const parsed = typeof snap === 'string' ? JSON.parse(snap) : snap
        if (parsed?.formatJson) {
          templateFormatConfig.value = typeof parsed.formatJson === 'string'
            ? JSON.parse(parsed.formatJson)
            : parsed.formatJson
        }
      }
    } catch { /* ignore */ }
    const sections = paper.value?.sections || []

    for (const s of sections) {
      switch (s.type) {
        case 'cover':
          try { coverInfo.value = JSON.parse(s.content) } catch { coverInfo.value = {} }
          break
        case 'abstract':
          try { abstractData.value = JSON.parse(s.content) } catch { abstractData.value = {} }
          break
        case 'references':
          try { references.value = JSON.parse(s.content) } catch { references.value = [] }
          break
        case 'acknowledgment':
          acknowledgment.value = s.content || ''
          break
        default:
          chapters.value.push(s)
      }
    }
    chapters.value.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch (e) {
    console.error('加载论文失败', e)
    ElMessage.error('加载论文数据失败')
  } finally {
    loading.value = false
  }
}

// ─── 扁平化章节（用于目录和分页） ───
const tocChapters = computed(() => {
  const result = []
  function walk(list, depth) {
    const sorted = [...list].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    for (const ch of sorted) {
      result.push({ ...ch, depth })
      const children = chapters.value.filter(c => c.parentId === ch.id)
      if (children.length > 0) walk(children, depth + 1)
    }
  }
  const roots = chapters.value.filter(c => !c.parentId)
  walk(roots, 1)
  return result
})

// ─── 页面元素列表（用于分页） ───
const pageElements = computed(() => {
  const items = []
  items.push({ type: 'cover', title: '封面', data: null })
  if (abstractData.value.abstractCn) items.push({ type: 'abstract', title: '摘要', data: null })
  if (abstractData.value.abstractEn) items.push({ type: 'abstract-en', title: 'Abstract', data: null })
  if (tocChapters.value.length > 0) items.push({ type: 'toc', title: '目录', data: null })
  for (const ch of tocChapters.value) {
    items.push({ type: 'chapter', title: ch.title, data: ch })
  }
  if (references.value.length > 0) items.push({ type: 'references', title: '参考文献', data: null })
  if (acknowledgment.value) items.push({ type: 'ack', title: '致谢', data: null })
  return items
})

onMounted(loadPaper)

// ─── 分页导航 ───
function goPage(n) {
  currentPage.value = Math.max(1, Math.min(n, totalPages.value))
}

function toggleViewMode() {
  viewMode.value = viewMode.value === 'continuous' ? 'paginated' : 'continuous'
  currentPage.value = 1
}

// ─── 返回编辑 / 返回列表 ───
function goBack() { router.push({ name: 'Papers' }) }
function goEdit() { router.push({ name: 'EditPaper', params: { id: paperId } }) }

// ═══════════════════════════════════════════════════
// DOCX 导出（使用 docx 包）
// ═══════════════════════════════════════════════════
async function exportDocx() {
  exporting.value = true
  try {
    const { Document, Packer, Paragraph, TextRun, HeadingLevel,
            AlignmentType, PageBreak, Header, Footer,
            PageNumber, Table, TableRow, TableCell,
            WidthType, BorderStyle, ImageRun, ExternalHyperlink } = await import('docx')

    // 构建文档内容
    const children = []

    // ── 封面 ──
    children.push(new Paragraph({ spacing: { before: 3000 }, children: [] }))
    children.push(new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 400 },
      children: [new TextRun({ text: coverInfo.value.college || '', size: 32, font: 'SimHei', bold: true })]
    }))
    children.push(new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 200 },
      children: [new TextRun({ text: paper.value?.title || '论文题目', size: 36, font: 'SimHei', bold: true })]
    }))

    const coverLines = [
      ['姓    名', coverInfo.value.author || paper.value?.studentName || ''],
      ['学    号', coverInfo.value.studentId || ''],
      ['学    院', coverInfo.value.college || ''],
      ['专    业', coverInfo.value.major || ''],
      ['指导教师', (coverInfo.value.advisor || '') + ' ' + (coverInfo.value.advisorTitle || '')],
      ['日    期', coverInfo.value.date || '']
    ]
    for (const [label, value] of coverLines) {
      if (value.trim()) {
        children.push(new Paragraph({
          alignment: AlignmentType.LEFT,
          spacing: { before: 200, after: 200 },
          indent: { left: 2000 },
          children: [
            new TextRun({ text: label + '：', size: 24, font: 'SimSun', bold: true }),
            new TextRun({ text: value, size: 24, font: 'SimSun' })
          ]
        }))
      }
    }
    children.push(new Paragraph({ children: [new PageBreak()] }))

    // ── 摘要 ──
    if (abstractData.value.abstractCn) {
      children.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { after: 300 },
        children: [new TextRun({ text: '摘  要', size: 32, font: 'SimHei', bold: true })]
      }))
      children.push(new Paragraph({
        spacing: { after: 100 }, indent: { firstLine: 480 },
        children: [new TextRun({ text: stripHtml(abstractData.value.abstractCn), size: 24, font: 'SimSun' })]
      }))
      if (abstractData.value.keywordsCn?.length) {
        children.push(new Paragraph({
          spacing: { before: 200 },
          children: [
            new TextRun({ text: '关键词：', size: 24, font: 'SimHei', bold: true }),
            new TextRun({ text: abstractData.value.keywordsCn.join('；'), size: 24, font: 'SimSun' })
          ]
        }))
      }
      children.push(new Paragraph({ children: [new PageBreak()] }))
    }

    // ── 英文摘要 ──
    if (abstractData.value.abstractEn) {
      children.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { after: 300 },
        children: [new TextRun({ text: 'Abstract', size: 32, font: 'Times New Roman', bold: true })]
      }))
      children.push(new Paragraph({
        spacing: { after: 100 }, indent: { firstLine: 480 },
        children: [new TextRun({ text: stripHtml(abstractData.value.abstractEn), size: 24, font: 'Times New Roman' })]
      }))
      if (abstractData.value.keywordsEn?.length) {
        children.push(new Paragraph({
          spacing: { before: 200 },
          children: [
            new TextRun({ text: 'Keywords: ', size: 24, font: 'Times New Roman', bold: true }),
            new TextRun({ text: abstractData.value.keywordsEn.join('; '), size: 24, font: 'Times New Roman' })
          ]
        }))
      }
      children.push(new Paragraph({ children: [new PageBreak()] }))
    }

    // ── 目录 ──
    if (tocChapters.value.length > 0) {
      children.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { after: 300 },
        children: [new TextRun({ text: '目  录', size: 32, font: 'SimHei', bold: true })]
      }))
      for (const ch of tocChapters.value) {
        const indent = (ch.depth - 1) * 400
        children.push(new Paragraph({
          spacing: { before: 60, after: 60 }, indent: { left: indent },
          children: [new TextRun({ text: ch.title, size: 24, font: 'SimSun' })]
        }))
      }
      children.push(new Paragraph({ children: [new PageBreak()] }))
    }

    // ── 正文章节 ──
    // 从模板配置中获取各级标题字号（pt → half-pt，docx 以半磅为单位）
    const h1Size = templateFormatConfig.value?.heading1?.fontSize
      ? templateFormatConfig.value.heading1.fontSize * 2 : 28
    const h2Size = templateFormatConfig.value?.heading2?.fontSize
      ? templateFormatConfig.value.heading2.fontSize * 2 : 26
    const h3Size = templateFormatConfig.value?.heading3?.fontSize
      ? templateFormatConfig.value.heading3.fontSize * 2 : 24
    const bodySize = templateFormatConfig.value?.body?.fontSize
      ? templateFormatConfig.value.body.fontSize * 2 : 24
    const h1Font = templateFormatConfig.value?.heading1?.font || 'SimHei'
    const h2Font = templateFormatConfig.value?.heading2?.font || 'SimHei'
    const h3Font = templateFormatConfig.value?.heading3?.font || 'SimHei'
    const bodyFont = templateFormatConfig.value?.body?.font || 'SimSun'

    for (const ch of tocChapters.value) {
      const headingSize = ch.depth === 1 ? h1Size : ch.depth === 2 ? h2Size : h3Size
      const headingFont = ch.depth === 1 ? h1Font : ch.depth === 2 ? h2Font : h3Font
      children.push(new Paragraph({
        alignment: ch.depth === 1 ? AlignmentType.CENTER : AlignmentType.LEFT,
        spacing: { before: 200, after: 150 },
        children: [new TextRun({ text: ch.title, size: headingSize, font: headingFont, bold: true })]
      }))
      if (ch.content) {
        const cleanText = stripHtml(ch.content).trim()
        if (cleanText) {
          const paragraphs = cleanText.split(/`n+/).filter(p => p.trim())
          for (const p of paragraphs) {
            children.push(new Paragraph({
              spacing: { after: 60 }, indent: { firstLine: 480 },
              children: [new TextRun({ text: p.trim(), size: bodySize, font: bodyFont })]
            }))
          }
        }
      }
    }

    // ── 参考文献 ──
    if (references.value.length > 0) {
      children.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 400, after: 300 },
        children: [new TextRun({ text: '参考文献', size: 32, font: 'SimHei', bold: true })]
      }))
      for (let i = 0; i < references.value.length; i++) {
        const ref = references.value[i]
        const refText = buildRefText(ref, i + 1)
        children.push(new Paragraph({
          spacing: { before: 30, after: 30 },
          indent: { left: 360, hanging: 360 },
          children: [new TextRun({ text: refText, size: 21, font: 'SimSun' })]
        }))
      }
    }

    // ── 致谢 ──
    if (acknowledgment.value) {
      children.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 400, after: 300 },
        children: [new TextRun({ text: '致  谢', size: 32, font: 'SimHei', bold: true })]
      }))
      const cleanAck = stripHtml(acknowledgment.value).trim()
      if (cleanAck) {
        const ackParagraphs = cleanAck.split(/`n+/).filter(p => p.trim())
        for (const p of ackParagraphs) {
          children.push(new Paragraph({
            spacing: { after: 60 }, indent: { firstLine: 480 },
            children: [new TextRun({ text: p.trim(), size: 24, font: 'SimSun' })]
          }))
        }
      }
    }

    // ── 创建 Document ──
    const doc = new Document({
      styles: {
        default: {
          document: {
            run: { font: bodyFont, size: bodySize },
            paragraph: { spacing: { after: 60 } }
          }
        }
      },
      sections: [{
        properties: {
          page: {
            size: { width: 11906, height: 16838 }, // A4
            margin: { top: 1440, bottom: 1440, left: 1800, right: 1800 }
          }
        },
        children: children
      }]
    })

    // ── 下载 ──
    const blob = await Packer.toBlob(doc)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${paper.value?.title || '论文'}.docx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('Word 文档导出成功')
  } catch (e) {
    console.error('DOCX 导出失败', e)
    ElMessage.error('Word 导出失败：' + (e.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

// ═══════════════════════════════════════════════════
// PDF 导出（使用 window.print / 后端）
// ═══════════════════════════════════════════════════
async function exportPdf() {
  try {
    // 优先尝试调用后端 PDF 导出接口
    const token = localStorage.getItem('token') || localStorage.getItem('paper-access-token')
    const response = await axios({
      url: `http://localhost:8080/api/papers/${paperId}/export/pdf`,
      method: 'GET',
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      timeout: 30000
    })
    const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `${paper.value?.title || '论文'}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('PDF 文档导出成功')
  } catch (e) {
    // 后端导出失败，使用浏览器打印功能作为备选
    console.warn('后端PDF导出不可用，使用浏览器打印功能', e)
    ElMessage.info('后端PDF服务不可用，使用浏览器打印功能导出PDF...')
    setTimeout(() => { window.print() }, 500)
  }
}

// ═══════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════
function handlePrint() {
  window.print()
}
function stripHtml(html) {
  let text = html.replace(/<[^>]*>/g, '')
  text = text.replace(/&nbsp;/g, ' ')
  text = text.replace(/&amp;/g, '&')
  text = text.replace(/&lt;/g, '<')
  text = text.replace(/&gt;/g, '>')
  text = text.replace(/&quot;/g, '"')
  return text
}
function buildRefText(ref, index) {
  const parts = [`[${index}] `]
  if (ref.title) parts.push(`《${ref.title}》`)
  if (ref.authors) parts.push(ref.authors + '. ')
  const typeMap = { '期刊': '[J]', '专著': '[M]', '学位论文': '[D]', '电子文献': '[EB/OL]' }
  parts.push(typeMap[ref.type] || '[J]')
  if (ref.journal) parts.push('. ' + ref.journal)
  if (ref.year) parts.push(', ' + ref.year)
  if (ref.volume) parts.push(', ' + ref.volume)
  if (ref.issue) parts.push('(' + ref.issue + ')')
  if (ref.pages) parts.push(': ' + ref.pages)
  parts.push('.')
  return parts.join('')
}
</script>

<template>
  <div class="preview-shell" v-loading="loading">
    <!-- ═══ 顶部工具栏 ═══ -->
    <div class="preview-toolbar">
      <div class="pt-left">
        <el-button size="small" text @click="goBack" :icon="ArrowLeft">返回列表</el-button>
        <el-button size="small" @click="goEdit" :icon="View">编辑</el-button>
        <el-divider direction="vertical" />
        <el-button size="small" @click="exportDocx" :icon="Download" :loading="exporting">
          导出 Word
        </el-button>
        <el-button size="small" type="primary" @click="exportPdf" :icon="Download">
          导出 PDF
        </el-button>
        <el-button size="small" text @click="toggleViewMode">
          {{ viewMode === 'continuous' ? '分页模式' : '连续模式' }}
        </el-button>
      </div>
      <div class="pt-center">
        <span class="pt-title">{{ paper?.title || '未命名论文' }}</span>
        <span class="pt-status" v-if="paper">{{ paper.status }}</span>
      </div>
      <div class="pt-right">
        <el-button size="small" text @click="handlePrint" :icon="Printer">打印</el-button>
      </div>
    </div>

    <!-- ═══ 分页导航（分页模式下显示） ═══ -->
    <div class="page-nav" v-if="!loading && viewMode === 'paginated'">
      <el-button size="small" @click="currentPage--" :disabled="currentPage <= 1">上一页</el-button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <el-button size="small" @click="currentPage++" :disabled="currentPage >= totalPages">下一页</el-button>
      <el-pagination
        small
        :total="totalPages"
        :page-size="1"
        :current-page="currentPage"
        @current-change="goPage"
        layout="prev, pager, next"
        style="margin-left: 12px;"
      />
    </div>

    <!-- ═══ 论文正文 ═══ -->
    <div class="preview-body" v-if="!loading" :style="headingFontStyles">
      <!-- 连续模式 -->
      <div v-if="viewMode === 'continuous'" class="paper-a4" ref="paperBodyRef">
        <!-- 封面 -->
        <div class="page-section cover-page">
          <div class="cover-school" v-if="coverInfo.college">{{ coverInfo.college }}</div>
          <h1 class="cover-title">{{ coverInfo.titleCn || paper?.title || '论文题目' }}</h1>
          <h2 class="cover-title-sub" v-if="coverInfo.titleEn">{{ coverInfo.titleEn }}</h2>
          <div class="cover-meta">
            <div class="cover-row" v-if="coverInfo.author || paper?.studentName">
              <label>姓    名</label><span>{{ coverInfo.author || paper?.studentName }}</span>
            </div>
            <div class="cover-row" v-if="coverInfo.studentId">
              <label>学    号</label><span>{{ coverInfo.studentId }}</span>
            </div>
            <div class="cover-row" v-if="coverInfo.college">
              <label>学    院</label><span>{{ coverInfo.college }}</span>
            </div>
            <div class="cover-row" v-if="coverInfo.major">
              <label>专    业</label><span>{{ coverInfo.major }}</span>
            </div>
            <div class="cover-row" v-if="coverInfo.advisor">
              <label>指导教师</label><span>{{ coverInfo.advisor }} {{ coverInfo.advisorTitle || '' }}</span>
            </div>
            <div class="cover-row" v-if="coverInfo.date">
              <label>日    期</label><span>{{ coverInfo.date }}</span>
            </div>
          </div>
        </div>

        <!-- 中文摘要 -->
        <div class="page-section" v-if="abstractData.abstractCn">
          <h2 class="section-heading">摘  要</h2>
          <div class="section-text" v-html="abstractData.abstractCn" />
          <div class="keywords" v-if="abstractData.keywordsCn?.length">
            <strong>关键词：</strong>{{ (abstractData.keywordsCn || []).join('；') }}
          </div>
        </div>

        <!-- 英文摘要 -->
        <div class="page-section" v-if="abstractData.abstractEn">
          <h2 class="section-heading">Abstract</h2>
          <div class="section-text" v-html="abstractData.abstractEn" />
          <div class="keywords" v-if="abstractData.keywordsEn?.length">
            <strong>Keywords: </strong>{{ (abstractData.keywordsEn || []).join('; ') }}
          </div>
        </div>

        <!-- 目录 -->
        <div class="page-section" v-if="tocChapters.length > 0">
          <h2 class="section-heading">目  录</h2>
          <div class="toc-list">
            <div v-for="ch in tocChapters" :key="ch.id" class="toc-item"
                 :style="{ paddingLeft: (ch.depth - 1) * 20 + 'px' }">
              <span class="toc-title">{{ ch.title }}</span>
              <span class="toc-dots" />
            </div>
          </div>
        </div>

        <!-- 正文章节 -->
        <div v-for="ch in tocChapters" :key="ch.id" class="page-section">
          <component :is="'h' + Math.min(ch.level || 1, 4)" class="chapter-head"
                     :class="'level-' + (ch.depth || 1)">
            {{ ch.title }}
          </component>
          <div class="section-text" v-html="ch.content" />
        </div>

        <!-- 参考文献 -->
        <div class="page-section" v-if="references.length > 0">
          <h2 class="section-heading">参考文献</h2>
          <ol class="ref-list">
            <li v-for="(ref, i) in references" :key="ref.id || i" class="ref-entry">
              <span v-if="ref.authors">{{ ref.authors }}. </span>
              <span v-if="ref.title">《{{ ref.title }}》</span>
              <span v-if="ref.type">[{{ ref.type === '期刊' ? 'J' : ref.type === '专著' ? 'M' : ref.type === '学位论文' ? 'D' : 'EB/OL' }}]. </span>
              <span v-if="ref.journal">{{ ref.journal }}</span>
              <span v-if="ref.year">, {{ ref.year }}</span>
              <span v-if="ref.volume">, {{ ref.volume }}</span>
              <span v-if="ref.issue">({{ ref.issue }})</span>
              <span v-if="ref.pages">: {{ ref.pages }}</span>
              <span>.</span>
            </li>
          </ol>
        </div>

        <!-- 致谢 -->
        <div class="page-section" v-if="acknowledgment">
          <h2 class="section-heading">致  谢</h2>
          <div class="section-text" v-html="acknowledgment" />
        </div>
      </div>

      <!-- 分页模式 -->
      <div v-else class="paper-a4 paginated">
        <div class="page-section">
          <!-- 当前页内容 -->
          <div v-if="currentPage <= 3 + tocChapters.length">
            <template v-if="currentPage === 1">
              <!-- 第1页：封面 -->
              <div class="cover-page">
                <div class="cover-school" v-if="coverInfo.college">{{ coverInfo.college }}</div>
                <h1 class="cover-title">{{ coverInfo.titleCn || paper?.title || '论文题目' }}</h1>
                <div class="cover-meta">
                  <div class="cover-row" v-if="coverInfo.author || paper?.studentName"><label>姓    名</label><span>{{ coverInfo.author || paper?.studentName }}</span></div>
                  <div class="cover-row" v-if="coverInfo.studentId"><label>学    号</label><span>{{ coverInfo.studentId }}</span></div>
                  <div class="cover-row" v-if="coverInfo.college"><label>学    院</label><span>{{ coverInfo.college }}</span></div>
                  <div class="cover-row" v-if="coverInfo.major"><label>专    业</label><span>{{ coverInfo.major }}</span></div>
                  <div class="cover-row" v-if="coverInfo.advisor"><label>指导教师</label><span>{{ coverInfo.advisor }} {{ coverInfo.advisorTitle || '' }}</span></div>
                  <div class="cover-row" v-if="coverInfo.date"><label>日    期</label><span>{{ coverInfo.date }}</span></div>
                </div>
              </div>
            </template>
            <template v-else-if="currentPage === 2">
              <!-- 第2页：摘要 -->
              <h2 class="section-heading">摘  要</h2>
              <div class="section-text" v-html="abstractData.abstractCn" />
              <div class="keywords" v-if="abstractData.keywordsCn?.length">
                <strong>关键词：</strong>{{ (abstractData.keywordsCn || []).join('；') }}
              </div>
              <div style="margin-top:30px">
                <h2 class="section-heading">Abstract</h2>
                <div class="section-text" v-html="abstractData.abstractEn" />
              </div>
            </template>
            <template v-else-if="currentPage === 3">
              <!-- 第3页：目录 -->
              <h2 class="section-heading">目  录</h2>
              <div class="toc-list">
                <div v-for="ch in tocChapters" :key="ch.id" class="toc-item"
                     :style="{ paddingLeft: (ch.depth - 1) * 20 + 'px' }">
                  <span class="toc-title">{{ ch.title }}</span>
                  <span class="toc-dots" />
                </div>
              </div>
            </template>
            <template v-else>
              <!-- 正文章节 -->
              <template v-for="(ch, idx) in tocChapters" :key="ch.id">
                <div v-if="currentPage === 4 + idx">
                  <component :is="'h' + Math.min(ch.level || 1, 4)" class="chapter-head" :class="'level-' + (ch.depth || 1)">
                    {{ ch.title }}
                  </component>
                  <div class="section-text" v-html="ch.content" />
                </div>
              </template>
            </template>
          </div>
          <div v-else-if="currentPage === tocChapters.length + 4">
            <h2 class="section-heading">参考文献</h2>
            <ol class="ref-list">
              <li v-for="(ref, i) in references" :key="ref.id || i" class="ref-entry">
                <span v-if="ref.authors">{{ ref.authors }}. </span>
                <span v-if="ref.title">《{{ ref.title }}》</span>
                <span v-if="ref.journal">{{ ref.journal }}, </span>
                <span v-if="ref.year">{{ ref.year }}</span><span v-if="ref.volume">, {{ ref.volume }}</span><span v-if="ref.issue">({{ ref.issue }})</span><span v-if="ref.pages">: {{ ref.pages }}</span><span>.</span>
              </li>
            </ol>
          </div>
          <div v-else-if="currentPage === tocChapters.length + 5 && acknowledgment">
            <h2 class="section-heading">致  谢</h2>
            <div class="section-text" v-html="acknowledgment" />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!paper" class="preview-empty">
        <el-empty description="论文数据加载失败" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ═══════ 整体 ═══════ */
.preview-shell {
  display: flex; flex-direction: column;
  height: 100vh; background: #e8e7e2;
}
.preview-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  height: 50px; padding: 0 18px;
  background: #fff; border-bottom: 1px solid #d8d7d2;
  flex-shrink: 0; z-index: 5; gap: 12px;
}
.pt-left, .pt-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.pt-center { flex: 1; text-align: center; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.pt-title { font-weight: 600; font-size: 15px; color: #242622; }
.pt-status { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: #EDF0EC; color: #5C605A; margin-left: 8px; }

/* ═══════ 分页导航 ═══════ */
.page-nav {
  display: flex; align-items: center; justify-content: center;
  padding: 8px 18px; background: #f5f4f0; border-bottom: 1px solid #d8d7d2;
  flex-shrink: 0; gap: 8px;
}
.page-info { font-size: 13px; color: #666; font-weight: 500; }

/* ═══════ 正文区域 ═══════ */
.preview-body {
  flex: 1; overflow-y: auto; padding: 20px 0;
}
.preview-body::-webkit-scrollbar { width: 8px; }
.preview-body::-webkit-scrollbar-thumb { background: #c0beb8; border-radius: 999px; }

.paper-a4 {
  width: 794px; margin: 0 auto; background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,.08);
}
.paper-a4.paginated .page-section { min-height: 1123px; padding: 60px 70px 50px; }

.page-section { padding: 50px 70px 40px; }
.page-section + .page-section { border-top: 1px dashed #ddd; }

/* ═══════ 封面 ═══════ */
.cover-page { text-align: center; min-height: 1123px; padding: 80px 70px 60px; }
.cover-school { font-size: 22px; font-weight: 600; margin-bottom: 50px; letter-spacing: 0.1em; }
.cover-title {
  font-family: "Noto Serif SC", "Microsoft YaHei", serif;
  font-size: 30px; font-weight: 700; color: #1a1a1a;
  margin: 0 0 16px; line-height: 1.4;
}
.cover-title-sub {
  font-family: "Noto Serif SC", serif;
  font-size: 18px; font-weight: 400; color: #555;
  margin: 0 0 50px; font-style: italic;
}
.cover-meta { display: inline-block; text-align: left; margin-top: 30px; }
.cover-row { display: flex; gap: 16px; padding: 10px 0; font-size: 16px; border-bottom: 1px solid #eee; }
.cover-row label { width: 80px; color: #666; font-weight: 500; flex-shrink: 0; }
.cover-row span { color: #242622; }

/* ═══════ 章节 ═══════ */
.section-heading {
  font-family: "Noto Serif SC", serif;
  font-size: 22px; font-weight: 700; text-align: center;
  margin: 0 0 30px; letter-spacing: 0.15em;
}
.section-text { font-size: 15px; line-height: 1.9; color: #333; text-align: justify; }
.section-text :deep(p) { text-indent: 2em; margin: 0.6em 0; }
.section-text :deep(img) { max-width: 100%; display: block; margin: 10px auto; }
.section-text :deep(table) { border-collapse: collapse; width: 100%; margin: 0.8em 0; }
.section-text :deep(th), .section-text :deep(td) { border: 1px solid #ccc; padding: 6px 10px; }
.section-text :deep(th) { background: #f7f7f5; font-weight: 600; }

.chapter-head { color: #1a1a1a; margin: 0 0 18px; }
h1.chapter-head, .level-1 {
  font-size: var(--pv-h1-size, 20px); text-align: center;
  font-family: var(--pv-h1-font, "Noto Serif SC", serif); font-weight: 700;
}
h2.chapter-head, .level-2 {
  font-size: var(--pv-h2-size, 18px);
  font-family: var(--pv-h2-font, "Noto Serif SC", serif); font-weight: 700;
}
h3.chapter-head, .level-3 {
  font-size: var(--pv-h3-size, 16px);
  font-family: var(--pv-h3-font, "Noto Serif SC", serif); font-weight: 700;
}
h4.chapter-head, .level-4 { font-size: 15px; }

.keywords { margin-top: 16px; font-size: 14px; color: #555; line-height: 1.6; }
.keywords strong { color: #242622; }

/* ═══════ 目录 ═══════ */
.toc-list { margin-top: 16px; }
.toc-item { display: flex; align-items: baseline; padding: 5px 0; font-size: 15px; color: #333; }
.toc-title { flex-shrink: 0; }
.toc-dots { flex: 1; border-bottom: 1px dotted #ccc; margin: 0 6px; min-width: 20px; }

/* ═══════ 参考文献 ═══════ */
.ref-list { padding-left: 1.6em; }
.ref-entry { font-size: 13px; line-height: 1.8; color: #444; margin: 4px 0; }

.preview-empty { display: flex; justify-content: center; padding: 100px 0; }

/* ═══════ 打印样式 ═══════ */
@media print {
  .preview-toolbar, .page-nav { display: none !important; }
  .preview-shell { height: auto; background: white; overflow: visible; }
  .preview-body { overflow: visible; padding: 0; }
  .paper-a4 { width: 100%; box-shadow: none; margin: 0; }
  .page-section { page-break-after: always; break-after: page; border: none !important; padding: 2.54cm 3.17cm; }
  .page-section:last-child { page-break-after: auto; }
}
</style>


