<script setup>
import { defineExpose } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { CitationTag } from '../extensions/CitationTag.js'
import StarterKit from '@tiptap/starter-kit'
import { ResizableImage } from '../extensions/ResizableImage.js'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { TextAlign } from '@tiptap/extension-text-align'
import { TextStyle } from '@tiptap/extension-text-style'
import { FontFamily } from '@tiptap/extension-font-family'
import { Underline } from '@tiptap/extension-underline'
import { Placeholder } from '@tiptap/extension-placeholder'
import { ref, watch, onBeforeUnmount, computed } from 'vue'
import { extractEditorFonts, resolveConfigFont, parseFontSize } from '../utils/fonts.js'
import { normalizeCitationMarker, normalizeCitationTagsHtml } from '../utils/citation.js'
import TableEditor from './TableEditor.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '开始撰写论文…' },
  /** 模板 formatJson 解析后的对象，用于自动套用模板字体 */
  formatConfig: { type: Object, default: undefined },
  /** 当前编辑章节的标题层级：0=正文/特殊章节, 1=一级标题, 2=二级标题, 3=三级标题 */
  headingLevel: { type: Number, default: 0 },
  /** 章节类型：chapter / abstract / acknowledgment */
  sectionType: { type: String, default: 'chapter' }
})

const emit = defineEmits(['update:modelValue'])

const citationNumberFormat = computed(() => props.formatConfig?.references?.numberFormat || '[N]')

// ========== 编辑器实例 ==========
const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: false       // 章节标题由侧边栏大纲控制，编辑区内不产生 h1-h4
    }),
    Underline,
    CitationTag,
    TextStyle,         // 文本样式（font-family 的前置依赖）
    FontFamily,        // 字体切换
    ResizableImage.configure({
      inline: true,
      allowBase64: true
    }),
    Table.configure({
      resizable: true
    }),
    TableRow,
    TableCell,
    TableHeader,
    TextAlign.configure({
      types: ['paragraph']   // 只对段落生效，标题由模板样式控制
    }),
    Placeholder.configure({
      placeholder: props.placeholder
    })
  ],
  onUpdate: ({ editor }) => {
    emit('update:modelValue', normalizeCitationTagsHtml(editor.getHTML(), citationNumberFormat.value))
  },
  editorProps: {
    attributes: {
      class: 'tiptap-editor-content'
    }
  }
})

// 外部 v-model 同步到编辑器
watch([() => props.modelValue, citationNumberFormat], ([value, numberFormat]) => {
  const normalizedValue = normalizeCitationTagsHtml(value || '', numberFormat)
  if (editor.value && editor.value.getHTML() !== normalizedValue) {
    editor.value.commands.setContent(normalizedValue, false)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  editor.value?.destroy()
})

// ========== 图片上传 ==========
const imageInput = ref(null)

function triggerImageUpload() {
  imageInput.value?.click()
}

function handleImageUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = e => {
    editor.value?.chain().focus().setImage({ src: e.target.result }).run()
  }
  reader.readAsDataURL(file)
  // 重置 input 以便重复选择同一文件
  event.target.value = ''
}

// ========== 表格插入 ==========
const showTableEditor = ref(false)

function openTableEditor() {
  showTableEditor.value = true
}

function onTableInsert(html) {
  editor.value?.chain().focus().insertContent(html).run()
}

function insertCitationTag(payload) {
  if (!editor.value || !payload?.marker) {
    return false
  }

  const citationNo = payload.citationNo ?? payload.ref?.citationNo ?? null
  const numberFormat = payload.numberFormat || '[N]'
  const marker = normalizeCitationMarker(payload.marker, citationNo, numberFormat)

  return editor.value
    .chain()
    .focus()
    .insertCitationTag({
      marker,
      label: marker,
      referenceId: payload.ref?.id ?? null,
      citationNo,
      numberFormat,
      year: payload.ref?.year || ''
    })
    .insertContent(' ')
    .run()
}

function normalizeCurrentCitations() {
  if (!editor.value) {
    return false
  }

  const normalizedValue = normalizeCitationTagsHtml(editor.value.getHTML(), citationNumberFormat.value)
  if (editor.value.getHTML() === normalizedValue) {
    return false
  }

  editor.value.commands.setContent(normalizedValue, false)
  emit('update:modelValue', normalizedValue)
  return true
}

defineExpose({
  insertCitationTag,
  normalizeCurrentCitations
})

// ========== 字体选择 ==========
const FONT_FAMILIES = [
  { label: '默认',   value: '' },
  { label: '宋体',   value: '"SimSun", "Songti SC", "Noto Serif SC", serif' },
  { label: '黑体',   value: '"SimHei", "Heiti SC", "PingFang SC", "Microsoft YaHei", sans-serif' },
  { label: '楷体',   value: '"KaiTi", "Kaiti SC", "Noto Serif SC", serif' },
  { label: '仿宋',   value: '"FangSong", "FangSongti SC", "Noto Serif SC", serif' },
  { label: '微软雅黑', value: '"Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", sans-serif' },
  { label: 'Times New Roman', value: '"Times New Roman", "Noto Serif SC", serif' },
]

function setFontFamily(value) {
  if (!value) {
    editor.value?.chain().focus().unsetFontFamily().run()
  } else {
    editor.value?.chain().focus().setFontFamily(value).run()
  }
}

// 当前选中区域的字体
const currentFont = ref('')

// ========== 各按钮状态 ==========
const active = ref({
  bold: false, italic: false, underline: false,
  alignLeft: false, alignCenter: false, alignRight: false, alignJustify: false,
  bulletList: false, orderedList: false
})

function updateStates() {
  const e = editor.value
  if (!e) return
  active.value = {
    bold: e.isActive('bold'),
    italic: e.isActive('italic'),
    underline: e.isActive('underline'),
    bulletList: e.isActive('bulletList'),
    orderedList: e.isActive('orderedList'),
    alignLeft: e.isActive({ textAlign: 'left' }),
    alignCenter: e.isActive({ textAlign: 'center' }),
    alignRight: e.isActive({ textAlign: 'right' }),
    alignJustify: e.isActive({ textAlign: 'justify' })
  }
  // 追踪当前字体
  currentFont.value = e.getAttributes('textStyle').fontFamily || ''
}

// 监听编辑器选区变化
watch(() => editor.value, val => {
  if (val) {
    val.on('selectionUpdate', updateStates)
    val.on('transaction', updateStates)
  }
}, { immediate: true })

// ========== 模板字体 / 字号按章节层级动态套用 ==========
const FONT_SIZE_LABEL = { 1: '一级标题', 2: '二级标题', 3: '三级标题' }

/**
 * 根据章节类型和层级，返回正文内容的字号（纯数字，方便拼接单位）。
 */
function resolveBodyFontSize(cfg, sectionType, level) {
  if (!cfg) return undefined
  // 摘要
  if (sectionType === 'abstract') {
    const v = parseFontSize(cfg.abstract?.fontSize)
    if (v != null) return v
  }
  // 致谢
  if (sectionType === 'acknowledgment') {
    const v = parseFontSize(cfg.acknowledgment?.fontSize)
    if (v != null) return v
  }
  // 章节正文：body1/body2/body3 链式回退
  if (sectionType === 'chapter' && level >= 1 && level <= 3) {
    for (let lv = level; lv >= 1; lv--) {
      const v = parseFontSize(cfg['body' + lv]?.fontSize)
      if (v != null) return v
    }
  }
  return parseFontSize(cfg.body?.fontSize)
}

/**
 * 根据章节类型和层级，返回正文内容的 CSS font-family 值。
 *
 * 规则：
 * - sectionType='abstract'         → abstract.font / abstract.fontFamily → body.font
 * - sectionType='chapter'          → bodyN.font → ... → body.font（链式回退）
 * - sectionType='acknowledgment'   → body.font
 */
function resolveBodyFontFamily(cfg, sectionType, level) {
  if (!cfg) return undefined
  // 摘要
  if (sectionType === 'abstract') {
    const f = resolveConfigFont(cfg.abstract)
    if (f) return f
  }
  // 致谢
  if (sectionType === 'acknowledgment') {
    const f = resolveConfigFont(cfg.acknowledgment)
    if (f) return f
  }
  // 章节正文：bodyN 链式回退（body1 → body2 → body3 → body）
  if (sectionType === 'chapter' && level >= 1 && level <= 3) {
    for (let lv = level; lv >= 1; lv--) {
      const f = resolveConfigFont(cfg['body' + lv])
      if (f) return f
    }
  }
  // 最终回退到 body
  return resolveConfigFont(cfg.body)
}

const editorFontStyles = computed(() => {
  const cfg = props.formatConfig
  const style = {}

  // ── 正文字号 ──
  const fontSize = resolveBodyFontSize(cfg, props.sectionType, props.headingLevel)
  if (fontSize != null) style['--editor-body-font-size'] = fontSize + 'pt'

  // ── 正文字体 ──
  const fontFamily = resolveBodyFontFamily(cfg, props.sectionType, props.headingLevel)
  if (fontFamily) style['--editor-body-font'] = fontFamily

  // ── 行高 ──
  if (cfg?.body?.lineSpacing) style['--editor-body-line-height'] = cfg.body.lineSpacing

  // ── 各级标题字体（供面包屑使用） ──
  const fonts = extractEditorFonts(cfg)
  if (fonts.heading1Font) style['--editor-h1-font'] = fonts.heading1Font
  if (fonts.heading2Font) style['--editor-h2-font'] = fonts.heading2Font
  if (fonts.heading3Font) style['--editor-h3-font'] = fonts.heading3Font
  const h1s = parseFontSize(fonts.heading1FontSize)
  const h2s = parseFontSize(fonts.heading2FontSize)
  const h3s = parseFontSize(fonts.heading3FontSize)
  if (h1s != null) style['--editor-h1-font-size'] = h1s + 'pt'
  if (h2s != null) style['--editor-h2-font-size'] = h2s + 'pt'
  if (h3s != null) style['--editor-h3-font-size'] = h3s + 'pt'

  return style
})

/** 当前章节的正文有效字号文本（用于工具栏徽章） */
const effectiveFontSizeText = computed(() => {
  const size = resolveBodyFontSize(props.formatConfig, props.sectionType, props.headingLevel)
  return size != null ? size + 'pt' : ''
})

/** 当前激活章节的层级中文标签 */
const activeLevelLabel = computed(() => {
  if (props.headingLevel >= 1 && props.headingLevel <= 3) {
    return FONT_SIZE_LABEL[props.headingLevel]
  }
  if (props.sectionType === 'abstract') return '摘要'
  if (props.sectionType === 'acknowledgment') return '致谢'
  return ''
})

/**
 * 从模板配置中提取当前区域预设的原始字体名（中文名，如 "黑体"、"宋体"）。
 * 遍历逻辑与 resolveBodyFontFamily 一致，但返回的是原始配置值而非 CSS 值。
 */
function resolveRawFontName(cfg, sectionType, level) {
  if (!cfg) return undefined
  // 摘要 → 回退 body
  if (sectionType === 'abstract') {
    return cfg.abstract?.font || cfg.abstract?.fontFamily
      || cfg.body?.font || cfg.body?.fontFamily
  }
  // 致谢 → 回退 body
  if (sectionType === 'acknowledgment') {
    return cfg.acknowledgment?.font || cfg.acknowledgment?.fontFamily
      || cfg.body?.font || cfg.body?.fontFamily
  }
  // 章节正文：bodyN 链式回退 → body
  if (sectionType === 'chapter' && level >= 1 && level <= 3) {
    for (let lv = level; lv >= 1; lv--) {
      const name = cfg['body' + lv]?.font || cfg['body' + lv]?.fontFamily
      if (name) return name
    }
  }
  return cfg.body?.font || cfg.body?.fontFamily
}

/** 「📌 模板规定」提示文本 */
const templateFontHint = computed(() => {
  if (!props.formatConfig) return ''
  const rawName = resolveRawFontName(props.formatConfig, props.sectionType, props.headingLevel)
  if (!rawName) return ''
  const sectionLabel = activeLevelLabel.value || '正文'
  // 如果 rawName 已经是 CSS 值（含引号/逗号），摘取第一段作为显示名
  const displayName = /["']/.test(rawName) || rawName.includes(',')
    ? rawName.split(/["',\s]+/).filter(Boolean)[0] || rawName
    : rawName
  return `模板规定：${sectionLabel}区域使用 ${displayName}`
})
</script>

<template>
  <div class="paper-editor">
    <!-- 工具栏 -->
    <div v-if="editor" class="editor-toolbar">
      <!-- 标题层级 & 模板字号指示（仅章节） -->
      <div v-if="headingLevel >= 1 && headingLevel <= 3" class="heading-level-badge"
           :title="`「${activeLevelLabel}」章节 · 编辑器文字自动使用模板预设字号 ${effectiveFontSizeText}`">
        <span class="hlb-level">H{{ headingLevel }}</span>
        <span class="hlb-label">{{ activeLevelLabel }}</span>
        <span v-if="effectiveFontSizeText" class="hlb-size">{{ effectiveFontSizeText }}</span>
      </div>
      <!-- 模板字体提示（所有区域：章节/摘要/致谢） -->
      <span v-if="templateFontHint" class="tpl-font-hint" :title="templateFontHint">
        {{ templateFontHint }}
      </span>
      <span v-if="(headingLevel >= 1 && headingLevel <= 3) || templateFontHint" class="toolbar-divider" />

      <!-- 文本样式 -->
      <div class="toolbar-group">
        <button
          :class="{ active: active.bold }"
          title="加粗 (Ctrl+B)"
          @click="editor.chain().focus().toggleBold().run()"
        >
          <strong>B</strong>
        </button>
        <button
          :class="{ active: active.italic }"
          title="斜体 (Ctrl+I)"
          @click="editor.chain().focus().toggleItalic().run()"
        >
          <em>I</em>
        </button>
        <button
          :class="{ active: active.underline }"
          title="下划线 (Ctrl+U)"
          @click="editor.chain().focus().toggleUnderline().run()"
        >
          <u>U</u>
        </button>
      </div>

      <span class="toolbar-divider" />

      <!-- 字体选择（仅字体，不含字号） -->
      <div class="toolbar-group">
        <select
          class="font-select"
          :value="currentFont"
          @change="setFontFamily($event.target.value)"
          title="选择字体"
        >
          <option
            v-for="f in FONT_FAMILIES"
            :key="f.value"
            :value="f.value"
            :style="{ fontFamily: f.value || 'inherit' }"
          >{{ f.label }}</option>
        </select>
      </div>

      <span class="toolbar-divider" />

      <!-- 对齐 -->
      <div class="toolbar-group">
        <button
          :class="{ active: active.alignLeft }"
          title="左对齐"
          @click="editor.chain().focus().setTextAlign('left').run()"
        >&#xE2;<!-- ← -->
        </button>
        <button
          :class="{ active: active.alignCenter }"
          title="居中"
          @click="editor.chain().focus().setTextAlign('center').run()"
        >&#x2194;<!-- ↔ -->
        </button>
        <button
          :class="{ active: active.alignRight }"
          title="右对齐"
          @click="editor.chain().focus().setTextAlign('right').run()"
        >&#xE3;<!-- → -->
        </button>
        <button
          :class="{ active: active.alignJustify }"
          title="两端对齐"
          @click="editor.chain().focus().setTextAlign('justify').run()"
        >&#x2261;<!-- ≡ -->
        </button>
      </div>

      <span class="toolbar-divider" />

      <!-- 列表 -->
      <div class="toolbar-group">
        <button
          :class="{ active: active.bulletList }"
          title="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <circle cx="3" cy="4" r="1.5"/>
            <rect x="6" y="3" width="8" height="2" rx="0.5"/>
            <circle cx="3" cy="8" r="1.5"/>
            <rect x="6" y="7" width="8" height="2" rx="0.5"/>
            <circle cx="3" cy="12" r="1.5"/>
            <rect x="6" y="11" width="8" height="2" rx="0.5"/>
          </svg>
        </button>
        <button
          :class="{ active: active.orderedList }"
          title="有序列表"
          @click="editor.chain().focus().toggleOrderedList().run()"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <text x="0" y="5" font-size="10" font-weight="bold">1.</text>
            <rect x="8" y="3" width="8" height="2" rx="0.5"/>
            <text x="0" y="10" font-size="10" font-weight="bold">2.</text>
            <rect x="8" y="8" width="8" height="2" rx="0.5"/>
            <text x="0" y="15" font-size="10" font-weight="bold">3.</text>
            <rect x="8" y="13" width="8" height="2" rx="0.5"/>
          </svg>
        </button>
      </div>

      <span class="toolbar-divider" />

      <!-- 图片 & 表格 -->
      <div class="toolbar-group">
        <button title="插入图片" @click="triggerImageUpload">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <rect x="1" y="2" width="14" height="12" rx="1.5" stroke="currentColor" fill="none" stroke-width="1.2"/>
            <circle cx="4.5" cy="5.5" r="1.5"/>
            <path d="M1 11l4-3 3 2 3-4 4 6" stroke="currentColor" fill="none" stroke-width="1.2"/>
          </svg>
        </button>
        <input
          ref="imageInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleImageUpload"
        />
        <button title="插入表格" @click="openTableEditor">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <rect x="1" y="1" width="14" height="14" rx="1" stroke="currentColor" fill="none" stroke-width="1.2"/>
            <line x1="6" y1="1" x2="6" y2="15" stroke="currentColor" stroke-width="0.8"/>
            <line x1="11" y1="1" x2="11" y2="15" stroke="currentColor" stroke-width="0.8"/>
            <line x1="1" y1="6" x2="15" y2="6" stroke="currentColor" stroke-width="0.8"/>
            <line x1="1" y1="11" x2="15" y2="11" stroke="currentColor" stroke-width="0.8"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 编辑区域 -->
    <editor-content :editor="editor" class="editor-body" :style="editorFontStyles" />

    <!-- 表格插入弹窗 -->
    <TableEditor v-model="showTableEditor" @insert="onTableInsert" />
  </div>
</template>

<style scoped>
/* ================================================================
   PaperEditor — 莫兰迪风格富文本编辑器
   ================================================================ */

.paper-editor {
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  background: var(--bg-card);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

/* ===== 工具栏 ===== */
.editor-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 1px;
  padding: 8px 14px;
  background: #FAFAF8;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 5;
}

.toolbar-group {
  display: flex;
  gap: 2px;
}

.toolbar-group button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-mute);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.toolbar-group button:hover {
  background: var(--primary-tint);
  color: var(--primary);
}

.toolbar-group button:active {
  transform: scale(0.94);
}

.toolbar-group button.active {
  background: var(--primary-tint);
  color: var(--primary);
  font-weight: 600;
  box-shadow: inset 0 1px 2px rgba(79, 119, 106, 0.08);
}

.toolbar-divider {
  display: inline-block;
  width: 1px;
  height: 22px;
  background: var(--border);
  margin: 0 10px;
  align-self: center;
  border-radius: 1px;
}

/* ---- 字体选择下拉框 ---- */
.font-select {
  height: 32px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--text-main);
  font-size: 13px;
  cursor: pointer;
  min-width: 100px;
  outline: none;
  transition: all 0.15s ease;
}
.font-select:hover {
  background: var(--primary-tint);
  color: var(--primary);
}
.font-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 119, 106, 0.08);
}

/* ---- 标题层级指示徽章 ---- */
.heading-level-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--primary-tint);
  color: var(--primary);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  cursor: default;
}
.hlb-level {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}
.hlb-label {
  color: var(--text-main);
  font-size: 11px;
}
.hlb-size {
  color: var(--text-mute);
  font-size: 10px;
  font-weight: 400;
}

/* ---- 模板字体提示 ---- */
.tpl-font-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #fdf6e8;
  color: #92400e;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  cursor: default;
  animation: tplFontFadeIn 0.3s ease;
}
@keyframes tplFontFadeIn {
  from { opacity: 0; transform: translateX(-4px); }
  to   { opacity: 1; transform: translateX(0); }
}

/* ===== 编辑区域 ===== */
.editor-body {
  flex: 1;
  min-height: 400px;
  overflow-y: auto;
}

.editor-body :deep(.tiptap-editor-content) {
  padding: 28px 34px;
  outline: none;
  font-size: var(--editor-body-font-size, 15px);
  line-height: var(--editor-body-line-height, 1.85);
  color: var(--text-main);
  min-height: 100%;
  /* 模板字体：由 formatConfig.body.font 自动注入，无模板时回退到系统默认 */
  font-family: var(--editor-body-font, "SimSun", "Songti SC", "Noto Serif SC", serif);
}

.editor-body::-webkit-scrollbar {
  width: 6px;
}
.editor-body::-webkit-scrollbar-thumb {
  background: #d0d3ce;
  border-radius: 999px;
}
.editor-body::-webkit-scrollbar-thumb:hover {
  background: #b4b8b2;
}

/* ---- Placeholder ---- */
.editor-body :deep(.tiptap-editor-content p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #c0c3bd;
  pointer-events: none;
  height: 0;
  font-style: italic;
}

/* ---- 段落（章节正文，标题由侧边栏大纲/模板层级控制，编辑区不产出 h1-h4） ---- */
.editor-body :deep(p) {
  margin: 0.5em 0;
  text-align: justify;
}

.editor-body :deep(.citation-tag-node) {
  display: inline;
  margin-left: 2px;
  color: inherit;
  font-size: 0.75em;
  font-weight: 400;
  line-height: 1;
  vertical-align: super;
  white-space: nowrap;
  user-select: all;
}

/* ---- 图片 ---- */
.editor-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--r);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--t-fast);
}
.editor-body :deep(img:hover) {
  box-shadow: var(--shadow-lg);
}

/* ---- 可缩放图片包装器 ---- */
.editor-body :deep(.resizable-image-wrapper) {
  position: relative;
  display: inline-block;
  line-height: 0;
  vertical-align: bottom;
}
.editor-body :deep(.resizable-image-wrapper.is-selected) {
  outline: 2px solid var(--primary);
  outline-offset: 3px;
  border-radius: var(--r);
}

/* -- 拖拽手柄 -- */
.editor-body :deep(.resize-handle) {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: nwse-resize;
  box-shadow: 0 1px 4px rgba(0,0,0,.18);
  z-index: 10;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.editor-body :deep(.resize-handle:hover) {
  transform: scale(1.25);
  box-shadow: 0 2px 8px rgba(0,0,0,.25);
}
.editor-body :deep(.resize-handle:active) {
  transform: scale(1.15);
}

/* -- 拖拽中的状态 -- */
.editor-body :deep(.resizable-image-wrapper.is-resizing) {
  outline: 2px dashed var(--primary);
  outline-offset: 3px;
  border-radius: var(--r);
}
.editor-body :deep(.resizable-image-wrapper.is-resizing img) {
  opacity: 0.85;
  cursor: nwse-resize;
}

/* ---- 表格（默认全框线） ---- */
.editor-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.8em 0;
}
.editor-body :deep(th),
.editor-body :deep(td) {
  border: 1px solid var(--border);
  padding: 9px 14px;
  text-align: center;
  min-width: 60px;
  font-size: 10.5pt;
}
.editor-body :deep(th) {
  background: var(--bg-page);
  font-weight: 600;
  color: var(--text-main);
  font-size: 10.5pt;
}

/* ---- 三线表（学术论文格式） ---- */
.editor-body :deep(.table-three-line) {
  /* 容器不设边框，只靠 th/td 画出三线 */
}
.editor-body :deep(.table-three-line th) {
  border: none;
  border-top: 1.5pt solid #000;
  border-bottom: 0.75pt solid #000;
  padding: 6px 10px;
  background: transparent;
  font-weight: 600;
  font-size: 10.5pt;
  text-align: center;
}
.editor-body :deep(.table-three-line td) {
  border: none;
  padding: 6px 10px;
  font-size: 10.5pt;
  text-align: center;
}
.editor-body :deep(.table-three-line tr:last-child td) {
  border-bottom: 1.5pt solid #000;
}

/* 三线表内可选居中 */
.editor-body :deep(.table-three-line caption) {
  font-weight: 600;
  text-align: center;
  margin-bottom: 6px;
  font-size: 10.5pt;
}

/* ---- 列表 ---- */
.editor-body :deep(ul),
.editor-body :deep(ol) {
  padding-left: 1.6em;
}
.editor-body :deep(li) {
  margin: 0.25em 0;
}

/* ---- 分割线 ---- */
.editor-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border);
  margin: 1.2em 0;
}

/* ---- 代码块 ---- */
.editor-body :deep(pre) {
  background: #f7f7f5;
  border: 1px solid var(--border);
  border-radius: var(--r);
  padding: 14px 18px;
  font-size: 13px;
  overflow-x: auto;
}
.editor-body :deep(code) {
  background: #f0f1ed;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.9em;
  color: #5c605a;
}

/* ---- 引用块 ---- */
.editor-body :deep(blockquote) {
  border-left: 3px solid var(--primary);
  margin: 0.8em 0;
  padding: 6px 16px;
  background: var(--primary-tint);
  border-radius: 0 var(--r) var(--r) 0;
  color: var(--text-mute);
}

/* ---- 选中表格 ---- */
.editor-body :deep(.selectedCell) {
  background: var(--primary-tint);
}
</style>
