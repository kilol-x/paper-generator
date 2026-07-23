<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Download, FullScreen, View, Printer } from '@element-plus/icons-vue'
import html2canvas from "html2canvas"
import { jsPDF } from "jspdf"
import axios from 'axios'
import { normalizeCitationMarker, normalizeCitationTagsHtml, stripLeadingCitationMarker } from '../../utils/citation.js'
import { extractEditorFonts, parseFontSize, resolveConfigFont } from '../../utils/fonts.js'

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
  const cfg = templateFormatConfig.value
  const fonts = extractEditorFonts(cfg)
  const style = {}

  // ── 标题字号 ──
  const h1 = parseFontSize(fonts.heading1FontSize)
  const h2 = parseFontSize(fonts.heading2FontSize)
  const h3 = parseFontSize(fonts.heading3FontSize)
  if (h1) style['--pv-h1-size'] = h1 + 'pt'
  if (h2) style['--pv-h2-size'] = h2 + 'pt'
  if (h3) style['--pv-h3-size'] = h3 + 'pt'

  // ── 标题字体 ──
  if (fonts.heading1Font) style['--pv-h1-font'] = fonts.heading1Font
  if (fonts.heading2Font) style['--pv-h2-font'] = fonts.heading2Font
  if (fonts.heading3Font) style['--pv-h3-font'] = fonts.heading3Font

  // ── 正文字号（body1 / body2 / body3，按章节层级） ──
  const b1 = parseFontSize(cfg?.body1?.fontSize) || parseFontSize(fonts.bodyFontSize)
  const b2 = parseFontSize(cfg?.body2?.fontSize) || b1
  const b3 = parseFontSize(cfg?.body3?.fontSize) || b2
  if (b1) style['--pv-body1-size'] = b1 + 'pt'
  if (b2) style['--pv-body2-size'] = b2 + 'pt'
  if (b3) style['--pv-body3-size'] = b3 + 'pt'

  // ── 通用回退（body） ──
  const body = parseFontSize(fonts.bodyFontSize)
  if (body) style['--pv-body-size'] = body + 'pt'
  if (fonts.bodyFont) style['--pv-body-font'] = fonts.bodyFont

  const refCfg = cfg?.references
  const refTitleFont = resolveConfigFont(refCfg)
  const refBodyFont = resolveConfigFont(refCfg)
  const refTitleSize = parseFontSize(refCfg?.titleFontSize)
  const refBodySize = parseFontSize(refCfg?.fontSize)
  const refLineHeight = Number(refCfg?.lineSpacing)
  const refHangingIndent = Number(refCfg?.hangingIndentSize)

  if (refTitleFont) style['--pv-ref-title-font'] = refTitleFont
  if (refTitleSize != null) style['--pv-ref-title-size'] = refTitleSize + 'pt'
  if (refCfg?.titleAlignment) style['--pv-ref-title-align'] = refCfg.titleAlignment
  if (refBodyFont) style['--pv-ref-font'] = refBodyFont
  if (refBodySize != null) style['--pv-ref-size'] = refBodySize + 'pt'
  if (!Number.isNaN(refLineHeight) && refLineHeight > 0) style['--pv-ref-line-height'] = String(refLineHeight)
  if (!Number.isNaN(refHangingIndent) && refHangingIndent > 0) style['--pv-ref-hanging-indent'] = String(refHangingIndent)

  return style
})

function formatReferenceNumber(index) {
  const numberFormat = templateFormatConfig.value?.references?.numberFormat || '[N]'
  return normalizeCitationMarker('', index, numberFormat)
}

function getReferenceText(ref, index) {
  return stripLeadingCitationMarker(ref.formattedText || '') || buildRefText(ref, index)
}

function getRenderedChapterContent(content) {
  return normalizeCitationTagsHtml(
    content || '',
    templateFormatConfig.value?.references?.numberFormat || '[N]'
  )
}

function resolveDocxFont(fontName, fallback) {
  const map = {
    '宋体': 'SimSun',
    '黑体': 'SimHei',
    '楷体': 'KaiTi',
    '楷体_GB2312': 'KaiTi_GB2312',
    '仿宋': 'FangSong',
    '仿宋_GB2312': 'FangSong_GB2312',
    '微软雅黑': 'Microsoft YaHei',
  }

  return map[fontName] || fontName || fallback
}

function resolveDocxAlignment(alignmentType, AlignmentType) {
  switch (alignmentType) {
    case 'left':
      return AlignmentType.LEFT
    case 'right':
      return AlignmentType.RIGHT
    default:
      return AlignmentType.CENTER
  }
}

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
    const h1Font = resolveDocxFont(templateFormatConfig.value?.heading1?.font, 'SimHei')
    const h2Font = resolveDocxFont(templateFormatConfig.value?.heading2?.font, 'SimHei')
    const h3Font = resolveDocxFont(templateFormatConfig.value?.heading3?.font, 'SimHei')
    const bodyFont = resolveDocxFont(templateFormatConfig.value?.body?.font, 'SimSun')

    for (const ch of tocChapters.value) {
      const headingSize = ch.depth === 1 ? h1Size : ch.depth === 2 ? h2Size : h3Size
      const headingFont = ch.depth === 1 ? h1Font : ch.depth === 2 ? h2Font : h3Font
      children.push(new Paragraph({
        alignment: ch.depth === 1 ? AlignmentType.CENTER : AlignmentType.LEFT,
        spacing: { before: 200, after: 150 },
        children: [new TextRun({ text: ch.title, size: headingSize, font: headingFont, bold: true })]
      }))
      if (ch.content && ch.content.trim()) {
        const parsed = htmlToDocxContent(ch.content, bodySize, bodyFont, { Paragraph:Paragraph, TextRun:TextRun, Table:Table, TableRow:TableRow, TableCell:TableCell, BorderStyle:BorderStyle, ImageRun:ImageRun, AlignmentType:AlignmentType })
        for (const el of parsed) children.push(el)
      }
    }

    // ── 参考文献 ──
    if (references.value.length > 0) {
      const refCfg = templateFormatConfig.value?.references || {}
      const refTitleSize = parseFontSize(refCfg.titleFontSize)
      const refBodySize = parseFontSize(refCfg.fontSize)
      const refLineSpacing = Number(refCfg.lineSpacing) || 1.5
      const hangingIndentChars = refCfg.hangingIndent === false ? 0 : (Number(refCfg.hangingIndentSize) || 2)
      const hangingIndent = Math.round(hangingIndentChars * 240)
      const refTitleFont = resolveDocxFont(refCfg.titleFont, 'SimHei')
      const refBodyFont = resolveDocxFont(refCfg.font, 'SimSun')

      children.push(new Paragraph({
        alignment: resolveDocxAlignment(refCfg.titleAlignment, AlignmentType), spacing: { before: 400, after: 300 },
        children: [new TextRun({ text: '参考文献', size: (refTitleSize || 16) * 2, font: refTitleFont, bold: true })]
      }))
      for (let i = 0; i < references.value.length; i++) {
        const ref = references.value[i]
        const refText = `${formatReferenceNumber(i + 1)} ${getReferenceText(ref, i + 1)}`
        children.push(new Paragraph({
          spacing: { before: 30, after: 30, line: Math.round(refLineSpacing * 240) },
          indent: hangingIndent > 0 ? { left: hangingIndent, hanging: hangingIndent } : undefined,
          children: [new TextRun({ text: refText, size: (refBodySize || 10.5) * 2, font: refBodyFont })]
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
    ElMessage.info('正在生成 PDF...')
    const container = document.querySelector('.paper-a4')
    if (!container) { ElMessage.error('未找到论文内容'); return }

    // 逐个截取每个分段元素，按段分页避免跨页断字
    const pages = container.querySelectorAll('.page-section, .section-page')
    if (!pages.length) { ElMessage.error('未找到论文段落'); return }

    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfW = 210

    for (let i = 0; i < pages.length; i++) {
      const sec = pages[i]
      if (sec.offsetHeight < 50) continue

      const canvas = await html2canvas(sec, {
        scale: 2, useCORS: true, logging: false,
        width: sec.scrollWidth,
        height: sec.scrollHeight
      })
      const imgData = canvas.toDataURL('image/jpeg', 0.95)
      const imgH = (canvas.height * pdfW) / canvas.width
      const pageH = 297

      if (i > 0) pdf.addPage()

      if (imgH <= pageH) {
        pdf.addImage(imgData, 'JPEG', 0, 0, pdfW, imgH)
      } else {
        let remain = imgH; let pos = 0
        pdf.addImage(imgData, 'JPEG', 0, pos, pdfW, imgH)
        remain -= pageH
        while (remain > 0) {
          pos -= pageH
          pdf.addPage()
          pdf.addImage(imgData, 'JPEG', 0, pos, pdfW, imgH)
          remain -= pageH
        }
      }
    }

    const fn = (paper.value?.title || '论文') + '.pdf'
    pdf.save(fn)
    ElMessage.success('PDF 导出成功')
  } catch (e) {
    console.error('PDF error', e)
    ElMessage.info('PDF 生成失败，使用浏览器打印...')
    setTimeout(() => { window.print() }, 500)
  }
}


// ═══════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════
function handlePrint() {
  window.print()
}

/**
 * 解析包含 HTML 标签（表格、图片、段落）的内容为 docx 元素数组
 */
function htmlToDocxContent(html, fontSize, fontName, D) {
  const { Paragraph, TextRun, Table, TableRow, TableCell, BorderStyle, ImageRun, AlignmentType } = D
  const elements = []
  if (!html || !html.trim()) return elements
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, "text/html")
  const body = doc.body
  function processNode(node) {
    if (node.nodeType === 3) {
      const t = node.textContent.trim()
      if (t) elements.push(new Paragraph({ spacing: { after: 60 }, indent: { firstLine: 480 }, children: [new TextRun({ text: t, size: fontSize, font: fontName })] }))
      return
    }
    if (node.nodeType !== 1) return
    const tag = node.tagName.toLowerCase()
    if (tag === "table") {
      const rows = []
      for (const tr of node.querySelectorAll("tr")) {
        const cells = []
        for (const td of tr.querySelectorAll("td, th")) {
          const isTh = td.tagName === "TH"
          cells.push(new TableCell({
            children: [new Paragraph({ children: [new TextRun({ text: td.textContent.trim(), size: fontSize, font: fontName, bold: isTh })] })],
            borders: { top: { style: BorderStyle.SINGLE, size: 1 }, bottom: { style: BorderStyle.SINGLE, size: 1 }, left: { style: BorderStyle.SINGLE, size: 1 }, right: { style: BorderStyle.SINGLE, size: 1 } }
          }))
        }
        if (cells.length) rows.push(new TableRow({ children: cells }))
      }
      if (rows.length) { elements.push(new Table({ rows })); elements.push(new Paragraph({ spacing: { after: 120 }, children: [] })) }
    } else if (tag === "img") {
      const src = node.getAttribute("src") || ""
      if (src.startsWith("data:")) {
        try {
          const w = parseInt(node.getAttribute("width")) || 400, h = parseInt(node.getAttribute("height")) || 300
          elements.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new ImageRun({ data: src, transformation: { width: Math.min(w, 500), height: Math.min(h, 400) } })] }))
        } catch (e) {}
      } else {
        elements.push(new Paragraph({ children: [new TextRun({ text: "[image]", size: fontSize, font: fontName })] }))
      }
    } else if (["h1","h2","h3","h4","h5","h6"].indexOf(tag) >= 0) {
      elements.push(new Paragraph({ spacing: { before: 200, after: 100 }, children: [new TextRun({ text: node.textContent.trim(), size: fontSize * 2, font: "SimHei", bold: true })] }))
    } else {
      for (const child of node.childNodes) processNode(child)
    }
  }
  for (const child of body.childNodes) processNode(child)
  if (elements.length === 0) {
    const t = body.textContent.trim()
    if (t) elements.push(new Paragraph({ spacing: { after: 60 }, indent: { firstLine: 480 }, children: [new TextRun({ text: t, size: fontSize, font: fontName })] }))
  }
  return elements
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
          <div class="section-text" :class="'body-level-' + (ch.depth || 1)" v-html="getRenderedChapterContent(ch.content)" />
        </div>

        <!-- 参考文献 -->
        <div class="page-section" v-if="references.length > 0">
          <h2 class="section-heading ref-heading">参考文献</h2>
          <div class="ref-list">
            <div v-for="(ref, i) in references" :key="ref.id || i" class="ref-entry">
              <span class="ref-marker">{{ formatReferenceNumber(i + 1) }}</span>
              <span>{{ getReferenceText(ref, i + 1) }}</span>
            </div>
          </div>
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
                  <div class="section-text" :class="'body-level-' + (ch.depth || 1)" v-html="getRenderedChapterContent(ch.content)" />
                </div>
              </template>
            </template>
          </div>
          <div v-else-if="currentPage === tocChapters.length + 4">
            <h2 class="section-heading ref-heading">参考文献</h2>
            <div class="ref-list">
              <div v-for="(ref, i) in references" :key="ref.id || i" class="ref-entry">
                <span class="ref-marker">{{ formatReferenceNumber(i + 1) }}</span>
                <span>{{ getReferenceText(ref, i + 1) }}</span>
              </div>
            </div>
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
.cover-title { font-family: SimHei, 'Noto Serif SC', serif;
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
.cover-row {
  display: flex; gap: 16px; padding: 10px 0;
  font-size: var(--pv-body-size, 12pt); border-bottom: 1px solid #eee;
}
.cover-row label { width: 80px; color: #666; font-weight: 500; flex-shrink: 0; }
.cover-row span { color: #242622; }

/* ═══════ 章节 ═══════ */
.section-heading {
  font-family: var(--pv-h1-font, "Noto Serif SC", serif);
  font-size: var(--pv-h1-size, 22px); font-weight: 700; text-align: center;
  margin: 0 0 30px; letter-spacing: 0.15em;
}
.section-text {
  font-family: var(--pv-body-font, SimSun, serif);
  font-size: var(--pv-body-size, 12pt); line-height: 1.9; color: #333; text-align: justify;
}
/* 按章节层级应用不同正文字号 */
.section-text.body-level-1 { font-size: var(--pv-body1-size, var(--pv-body-size, 12pt)); }
.section-text.body-level-2 { font-size: var(--pv-body2-size, var(--pv-body-size, 11pt)); }
.section-text.body-level-3,
.section-text.body-level-4 { font-size: var(--pv-body3-size, var(--pv-body-size, 10.5pt)); }
.section-text :deep(p) { text-indent: 2em; margin: 0.6em 0; }
.section-text :deep(.citation-tag-node) {
  font-size: 0.75em;
  line-height: 1;
  vertical-align: super;
  white-space: nowrap;
}
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
h4.chapter-head, .level-4 { font-size: 15px; font-family: SimHei, sans-serif; }

.keywords {
  margin-top: 16px; font-size: var(--pv-body-size, 12pt); color: #555; line-height: 1.6;
}
.keywords strong { color: #242622; }

/* ═══════ 目录 ═══════ */
.toc-list { margin-top: 16px; }
.toc-item {
  display: flex; align-items: baseline; padding: 5px 0;
  font-size: var(--pv-body-size, 12pt); color: #333;
}
.toc-title { flex-shrink: 0; }
.toc-dots { flex: 1; border-bottom: 1px dotted #ccc; margin: 0 6px; min-width: 20px; }

/* ═══════ 参考文献 ═══════ */
.ref-heading {
  font-family: var(--pv-ref-title-font, var(--pv-h1-font, "Noto Serif SC", serif));
  font-size: var(--pv-ref-title-size, var(--pv-h1-size, 22px));
  text-align: var(--pv-ref-title-align, center);
}

.ref-list {
  font-family: var(--pv-ref-font, var(--pv-body-font, SimSun, serif));
  font-size: var(--pv-ref-size, 10.5pt);
  line-height: var(--pv-ref-line-height, 1.5);
  color: #444;
}

.ref-entry {
  display: block;
  margin: 0.35em 0;
  padding-left: calc(var(--pv-ref-hanging-indent, 0) * 1em);
  text-indent: calc(var(--pv-ref-hanging-indent, 0) * -1em);
}

.ref-marker { white-space: pre; }

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


