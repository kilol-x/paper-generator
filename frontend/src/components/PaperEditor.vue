<script setup>
import { ref, onBeforeUnmount, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import request from '@/api/request'

// ==================== Props & Emits ====================
const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:content'])

// ==================== 图片上传 ====================
const uploading = ref(false)
const fileInput = ref(null)

async function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  uploading.value = true
  try {
    const res = await request.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 根据后端返回的数据结构取 url，这里做了兼容处理
    const url = res?.data?.url || res?.url || ''
    if (url) {
      editor.value?.chain().focus().setImage({ src: url }).run()
    }
  } catch (err) {
    console.error('图片上传失败:', err)
  } finally {
    uploading.value = false
    // 清空 input 以便重复选择同一文件
    if (fileInput.value) fileInput.value.value = ''
  }
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) uploadImage(file)
}

// ==================== 表格插入 ====================
function insertTable() {
  const rows = parseInt(prompt('请输入行数:', '3')) || 3
  const cols = parseInt(prompt('请输入列数:', '3')) || 3
  editor.value?.chain().focus()
    .insertTable({ rows, cols, withHeaderRow: true })
    .run()
}

// ==================== 编辑器实例 ====================
const editor = useEditor({
  content: props.content,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4] }
    }),
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    Underline,
    Image.configure({
      inline: false,
      allowBase64: false
    }),
    Table.configure({
      resizable: true
    }),
    TableRow,
    TableCell,
    TableHeader
  ],
  onUpdate: ({ editor }) => {
    emit('update:content', editor.getHTML())
  }
})

// 外部 content 变化时同步到编辑器
watch(() => props.content, (val) => {
  const isSame = editor.value?.getHTML() === val
  if (!isSame) {
    editor.value?.commands.setContent(val, false)
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

// ==================== 工具栏状态 ====================
const headingLevel = ref(0)

watch(() => editor.value?.isActive('heading'), () => {
  if (!editor.value) return
  for (let i = 1; i <= 4; i++) {
    if (editor.value.isActive('heading', { level: i })) {
      headingLevel.value = i
      return
    }
  }
  headingLevel.value = 0
})

function setHeading(level) {
  if (level === 0) {
    editor.value?.chain().focus().setParagraph().run()
  } else {
    editor.value?.chain().focus().toggleHeading({ level }).run()
  }
}

// ==================== 键盘快捷键注册 ====================
// TipTap StarterKit 自带大部分快捷键 (Ctrl+B/I/U 等)，无需额外注册
</script>

<template>
  <div class="paper-editor">
    <!-- ========== 工具栏 ========== -->
    <div class="toolbar" v-if="editor">
      <!-- 段落组 -->
      <div class="toolbar-group">
        <button
          v-for="level in 4"
          :key="'h' + level"
          class="toolbar-btn"
          :class="{ active: editor.isActive('heading', { level }) }"
          :title="'标题 ' + level"
          @click="setHeading(level)"
        >
          H{{ level }}
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('paragraph') }"
          title="正文"
          @click="setHeading(0)"
        >
          ¶
        </button>
      </div>

      <!-- 文字样式组 -->
      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('bold') }"
          title="加粗 (Ctrl+B)"
          @click="editor.chain().focus().toggleBold().run()"
        >
          <b>B</b>
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('italic') }"
          title="斜体 (Ctrl+I)"
          @click="editor.chain().focus().toggleItalic().run()"
        >
          <i>I</i>
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('underline') }"
          title="下划线 (Ctrl+U)"
          @click="editor.chain().focus().toggleUnderline().run()"
        >
          <u>U</u>
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('strike') }"
          title="删除线"
          @click="editor.chain().focus().toggleStrike().run()"
        >
          <s>S</s>
        </button>
      </div>

      <!-- 对齐组 -->
      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive({ textAlign: 'left' }) }"
          title="左对齐"
          @click="editor.chain().focus().setTextAlign('left').run()"
        >
          左
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive({ textAlign: 'center' }) }"
          title="居中"
          @click="editor.chain().focus().setTextAlign('center').run()"
        >
          中
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive({ textAlign: 'right' }) }"
          title="右对齐"
          @click="editor.chain().focus().setTextAlign('right').run()"
        >
          右
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive({ textAlign: 'justify' }) }"
          title="两端对齐"
          @click="editor.chain().focus().setTextAlign('justify').run()"
        >
          齐
        </button>
      </div>

      <!-- 列表组 -->
      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('bulletList') }"
          title="无序列表"
          @click="editor.chain().focus().toggleBulletList().run()"
        >
          • 列表
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('orderedList') }"
          title="有序列表"
          @click="editor.chain().focus().toggleOrderedList().run()"
        >
          1. 列表
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('blockquote') }"
          title="引用"
          @click="editor.chain().focus().toggleBlockquote().run()"
        >
          ❝
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: editor.isActive('codeBlock') }"
          title="代码块"
          @click="editor.chain().focus().toggleCodeBlock().run()"
        >
          &lt;/&gt;
        </button>
      </div>

      <!-- 插入组 -->
      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          title="插入图片"
          @click="fileInput?.click()"
        >
          🖼 图片
        </button>
        <button
          class="toolbar-btn"
          title="插入表格"
          @click="insertTable"
        >
          表
        </button>
        <button
          class="toolbar-btn"
          title="水平分割线"
          @click="editor.chain().focus().setHorizontalRule().run()"
        >
          —
        </button>
      </div>

      <!-- 撤销重做 -->
      <div class="toolbar-group">
        <button
          class="toolbar-btn"
          title="撤销 (Ctrl+Z)"
          :disabled="!editor.can().undo()"
          @click="editor.chain().focus().undo().run()"
        >
          ↩
        </button>
        <button
          class="toolbar-btn"
          title="重做 (Ctrl+Y)"
          :disabled="!editor.can().redo()"
          @click="editor.chain().focus().redo().run()"
        >
          ↪
        </button>
      </div>
    </div>

    <!-- ========== 编辑器主体 ========== -->
    <div class="editor-content">
      <EditorContent :editor="editor" />
      <div v-if="uploading" class="upload-mask">图片上传中…</div>
    </div>

    <!-- 隐藏文件选择器 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onFileChange"
    />
  </div>
</template>

<style scoped>
.paper-editor {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  display: flex;
  flex-direction: column;
}

/* ========== 工具栏 ========== */
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 10px 12px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-right: 12px;
  margin-right: 4px;
  border-right: 1px solid #e8e8e8;
}

.toolbar-group:last-child {
  border-right: none;
  padding-right: 0;
  margin-right: 0;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #333;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.toolbar-btn:hover {
  background: #e8e8e8;
  border-color: #d9d9d9;
}

.toolbar-btn.active {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

.toolbar-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ========== 编辑区 ========== */
.editor-content {
  position: relative;
  flex: 1;
  min-height: 400px;
}

/* TipTap 渲染根节点 */
.editor-content :deep(.tiptap) {
  padding: 16px 20px;
  min-height: 400px;
  outline: none;
  line-height: 1.8;
  font-size: 15px;
  color: #333;
}

.editor-content :deep(.tiptap) p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  color: #bfbfbf;
  pointer-events: none;
}

/* ========== TipTap 内置样式覆写 ========== */
.editor-content :deep(.tiptap) h1 { font-size: 2em; margin: 0.6em 0; }
.editor-content :deep(.tiptap) h2 { font-size: 1.5em; margin: 0.5em 0; }
.editor-content :deep(.tiptap) h3 { font-size: 1.25em; margin: 0.4em 0; }
.editor-content :deep(.tiptap) h4 { font-size: 1.1em; margin: 0.35em 0; }

.editor-content :deep(.tiptap) ul,
.editor-content :deep(.tiptap) ol {
  padding-left: 1.5em;
}

.editor-content :deep(.tiptap) blockquote {
  border-left: 3px solid #ddd;
  padding-left: 16px;
  margin: 1em 0;
  color: #666;
}

.editor-content :deep(.tiptap) pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 14px;
}

.editor-content :deep(.tiptap) code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
}

.editor-content :deep(.tiptap) pre code {
  background: none;
  padding: 0;
}

.editor-content :deep(.tiptap) img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 1em 0;
}

.editor-content :deep(.tiptap) hr {
  border: none;
  border-top: 2px solid #e8e8e8;
  margin: 1.5em 0;
}

/* ========== 表格样式 ========== */
.editor-content :deep(.tiptap) table {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  overflow: hidden;
}

.editor-content :deep(.tiptap) th,
.editor-content :deep(.tiptap) td {
  border: 1px solid #d9d9d9;
  padding: 8px 12px;
  min-width: 60px;
  text-align: left;
}

.editor-content :deep(.tiptap) th {
  background: #f5f5f5;
  font-weight: 600;
}

.editor-content :deep(.tiptap) .selectedCell {
  background: rgba(22, 119, 255, 0.08);
}

/* ========== 上传遮罩 ========== */
.upload-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  color: #1677ff;
  font-size: 16px;
  z-index: 10;
}
</style>
