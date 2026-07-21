<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  section: { type: Object, required: true },
  allChapters: { type: Array, required: true },
  activeId: { type: String, default: null },
  editingId: { type: String, default: null },
  editingValue: { type: String, default: '' },
  depth: { type: Number, default: 0 }
})

const emit = defineEmits([
  'select', 'add', 'remove', 'edit', 'finish-edit',
  'update-editing', 'keydown', 'move', 'reorder'
])

const children = computed(() =>
  props.allChapters
    .filter(c => c.parentId === props.section.id)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
)

const indent = computed(() => props.depth * 20)

function countWords(html) {
  if (!html) return 0
  const text = html.replace(/<[^>]+>/g, '').replace(/&\w+;/g, '').trim()
  if (!text) return 0
  const cn = (text.match(/[一-鿿]/g) || []).length
  const en = (text.match(/[a-zA-Z]+/g) || []).length
  return cn + en
}

const dragId = ref(null)

function onDragStart(e, id) {
  dragId.value = id
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', id)
}
function onDragOver(e) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}
function onDrop(e, targetId) {
  e.preventDefault()
  const src = dragId.value
  if (src && src !== targetId) emit('reorder', src, targetId)
  dragId.value = null
}
</script>

<template>
  <div class="chapter-wrap">
    <div
      class="chapter-row"
      :class="{
        'chapter-row--active': section.id === activeId,
        'chapter-row--dragging': dragId === section.id
      }"
      :style="{ paddingLeft: `${12 + indent}px` }"
      draggable="true"
      @click.stop="emit('select', section.id)"
      @dragstart="onDragStart($event, section.id)"
      @dragover="onDragOver"
      @drop="onDrop($event, section.id)"
    >
      <!-- 折叠/展开指示器 -->
      <span class="chapter-caret" :class="{ 'chapter-caret--hidden': children.length === 0 }">
        <svg v-if="children.length > 0" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
        <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </span>

      <!-- 拖拽手柄 — 始终可见 -->
      <span class="chapter-grip" title="拖拽排序">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="12" x2="16" y2="12"/><line x1="8" y1="18" x2="16" y2="18"/></svg>
      </span>

      <!-- 标题 / 编辑框 -->
      <input
        v-if="editingId === section.id"
        class="chapter-input"
        :value="editingValue"
        @click.stop
        @input="emit('update-editing', $event.target.value)"
        @keydown="emit('keydown', $event)"
        @blur="emit('finish-edit')"
      />
      <span v-else class="chapter-title">{{ section.title }}</span>

      <!-- 字数 -->
      <span class="chapter-wordcount">{{ countWords(section.content) }}字</span>

      <!-- 操作按钮 — 始终可见，无任何 hover 依赖 -->
      <span class="chapter-btns">
        <button class="chapter-btn" title="添加子章节" @click.stop="emit('add', section.id)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
        <button class="chapter-btn" title="重命名" @click.stop="emit('edit', { id: section.id, title: section.title })">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"/></svg>
        </button>
        <button class="chapter-btn chapter-btn--danger" title="删除" @click.stop="emit('remove', section.id)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </span>
    </div>

    <!-- 递归子节点 -->
    <ChapterNode
      v-for="child in children"
      :key="child.id"
      :section="child"
      :all-chapters="allChapters"
      :active-id="activeId"
      :editing-id="editingId"
      :editing-value="editingValue"
      :depth="depth + 1"
      @select="emit('select', $event)"
      @add="emit('add', $event)"
      @remove="emit('remove', $event)"
      @edit="emit('edit', $event)"
      @finish-edit="emit('finish-edit')"
      @update-editing="emit('update-editing', $event)"
      @keydown="emit('keydown', $event)"
      @move="(id, dir) => emit('move', id, dir)"
      @reorder="(src, tgt) => emit('reorder', src, tgt)"
    />
  </div>
</template>

<style scoped>
/* ================================================================
   ChapterNode — 章节树节点（全新 CSS，无 hover-only 显示逻辑）
   ================================================================ */

.chapter-wrap {
  user-select: none;
}

.chapter-row {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main, #242622);
  border-left: 3px solid transparent;
  transition: background 0.12s;
}
.chapter-row:hover {
  background: #f7f8f4;
}
.chapter-row--active {
  background: var(--primary-tint, #e8efec);
  color: var(--primary, #4f776a);
  border-left-color: var(--primary, #4f776a);
  font-weight: 600;
}
.chapter-row--dragging {
  opacity: 0.35;
}

/* ---- 折叠箭头 ---- */
.chapter-caret {
  width: 14px;
  color: var(--text-dim, #858982);
  flex-shrink: 0;
}
.chapter-caret--hidden {
  visibility: hidden;
}

/* ---- 拖拽手柄（始终可见） ---- */
.chapter-grip {
  color: #bfbdb6;
  flex-shrink: 0;
  cursor: grab;
  display: flex;
  align-items: center;
}
.chapter-grip:hover {
  color: var(--primary, #4f776a);
}

/* ---- 标题 ---- */
.chapter-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

/* ---- 内联输入框 ---- */
.chapter-input {
  flex: 1;
  border: 1px solid var(--primary, #4f776a);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 13px;
  outline: none;
  min-width: 0;
}

/* ---- 字数 ---- */
.chapter-wordcount {
  font-size: 10px;
  color: #bfbdb6;
  flex-shrink: 0;
}

/* ---- 操作按钮容器（始终可见） ---- */
.chapter-btns {
  flex-shrink: 0;
  display: flex !important;
  gap: 2px;
}

/* ---- 单个操作按钮 ---- */
.chapter-btn {
  display: flex !important;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 5px;
  cursor: pointer;
  color: #bfbdb6;
  padding: 0;
  transition: background 0.1s, color 0.1s;
}
.chapter-btn:hover {
  background: var(--primary-tint, #e8efec);
  color: var(--primary, #4f776a);
}
.chapter-btn--danger:hover {
  background: #fce4e2;
  color: #d35b4e;
}
</style>
