<script setup>
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { ResizableImage } from '../extensions/ResizableImage.js'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { TextAlign } from '@tiptap/extension-text-align'
import { Underline } from '@tiptap/extension-underline'
import { Placeholder } from '@tiptap/extension-placeholder'
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '开始撰写论文…' }
})

const emit = defineEmits(['update:modelValue'])

// ========== 编辑器实例 ==========
const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4] }
    }),
    Underline,
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
      types: ['heading', 'paragraph']
    }),
    Placeholder.configure({
      placeholder: props.placeholder
    })
  ],
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  },
  editorProps: {
    attributes: {
      class: 'tiptap-editor-content'
    }
  }
})

// 外部 v-model 同步到编辑器
watch(() => props.modelValue, value => {
  if (editor.value && editor.value.getHTML() !== value) {
    editor.value.commands.setContent(value, false)
  }
})

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
function insertTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

// ========== 各按钮状态 ==========
const active = ref({
  bold: false, italic: false, underline: false,
  heading1: false, heading2: false, heading3: false, heading4: false,
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
    heading1: e.isActive('heading', { level: 1 }),
    heading2: e.isActive('heading', { level: 2 }),
    heading3: e.isActive('heading', { level: 3 }),
    heading4: e.isActive('heading', { level: 4 }),
    bulletList: e.isActive('bulletList'),
    orderedList: e.isActive('orderedList'),
    alignLeft: e.isActive({ textAlign: 'left' }),
    alignCenter: e.isActive({ textAlign: 'center' }),
    alignRight: e.isActive({ textAlign: 'right' }),
    alignJustify: e.isActive({ textAlign: 'justify' })
  }
}

// 监听编辑器选区变化
watch(() => editor.value, val => {
  if (val) {
    val.on('selectionUpdate', updateStates)
    val.on('transaction', updateStates)
  }
}, { immediate: true })
</script>

<template>
  <div class="paper-editor">
    <!-- 工具栏 -->
    <div v-if="editor" class="editor-toolbar">
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

      <!-- 标题 -->
      <div class="toolbar-group">
        <button
          v-for="level in [1, 2, 3, 4]"
          :key="level"
          :class="{ active: active[`heading${level}`] }"
          :title="`标题 ${level}`"
          @click="editor.chain().focus().toggleHeading({ level }).run()"
        >
          H{{ level }}
        </button>
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
        <button title="插入表格" @click="insertTable">
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
    <editor-content :editor="editor" class="editor-body" />
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

/* ===== 编辑区域 ===== */
.editor-body {
  flex: 1;
  min-height: 400px;
  overflow-y: auto;
}

.editor-body :deep(.tiptap-editor-content) {
  padding: 28px 34px;
  outline: none;
  font-size: 15px;
  line-height: 1.85;
  color: var(--text-main);
  min-height: 100%;
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

/* ---- 标题 ---- */
.editor-body :deep(h1) {
  font-size: 1.75em;
  margin: 0.8em 0 0.4em;
  font-family: var(--font-heading);
  font-weight: 700;
  color: #1d2b26;
  border-bottom: 1px solid var(--border);
  padding-bottom: 0.25em;
}
.editor-body :deep(h2) {
  font-size: 1.45em;
  margin: 0.7em 0 0.35em;
  font-family: var(--font-heading);
  font-weight: 600;
  color: #283832;
}
.editor-body :deep(h3) {
  font-size: 1.2em;
  margin: 0.6em 0 0.25em;
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--text-main);
}
.editor-body :deep(h4) {
  font-size: 1.05em;
  margin: 0.5em 0 0.2em;
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--text-mute);
}

/* ---- 段落 ---- */
.editor-body :deep(p) {
  margin: 0.5em 0;
  text-align: justify;
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

/* ---- 表格 ---- */
.editor-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.8em 0;
  border-radius: var(--r);
  overflow: hidden;
}
.editor-body :deep(th),
.editor-body :deep(td) {
  border: 1px solid var(--border);
  padding: 9px 14px;
  text-align: left;
  min-width: 60px;
}
.editor-body :deep(th) {
  background: var(--bg-page);
  font-weight: 600;
  color: var(--text-main);
  font-size: 13px;
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
