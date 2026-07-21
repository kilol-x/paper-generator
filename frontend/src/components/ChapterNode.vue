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
  <div class="cn-wrap">
    <div
      class="cn-row"
      :class="{ active: section.id === activeId, 'is-drag': dragId === section.id }"
      :style="{ paddingLeft: `${12 + indent}px` }"
      draggable="true"
      @click.stop="emit('select', section.id)"
      @dragstart="onDragStart($event, section.id)"
      @dragover="onDragOver"
      @drop="onDrop($event, section.id)"
    >
      <span class="cn-toggle" :class="{ inv: children.length === 0 }">
        <svg v-if="children.length > 0" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
        <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </span>
      <span class="cn-drag" title="拖拽排序">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="12" x2="16" y2="12"/><line x1="8" y1="18" x2="16" y2="18"/></svg>
      </span>

      <span class="cn-sort-btns">
        <button class="cn-sb" title="上移" @click.stop="emit('move', section.id, -1)">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
        </button>
        <button class="cn-sb" title="下移" @click.stop="emit('move', section.id, 1)">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
        </button>
      </span>

      <input
        v-if="editingId === section.id"
        class="cn-input"
        :value="editingValue"
        @click.stop
        @input="emit('update-editing', $event.target.value)"
        @keydown="emit('keydown', $event)"
        @blur="emit('finish-edit')"
      />
      <span v-else class="cn-title">{{ section.title }}</span>

      <span class="cn-wc">{{ countWords(section.content) }}字</span>

      <span class="cn-actions">
        <button class="cn-btn" title="添加子章节" @click.stop="emit('add', section.id)">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
        <button class="cn-btn" title="重命名" @click.stop="emit('edit', { id: section.id, title: section.title })">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"/></svg>
        </button>
        <button class="cn-btn danger" title="删除" @click.stop="emit('remove', section.id)">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </span>
    </div>

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
.cn-wrap { user-select: none; }
.cn-row {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 12px; cursor: pointer; font-size: 13px;
  color: var(--text-main); border-left: 3px solid transparent;
  transition: all 0.12s; position: relative;
}
.cn-row:hover { background: #f7f8f4; }
.cn-row.active {
  background: var(--primary-tint, #e8efec); color: var(--primary);
  border-left-color: var(--primary); font-weight: 600;
}
.cn-row.is-drag { opacity: 0.35; }

.cn-toggle { width: 14px; font-size: 10px; color: var(--text-dim); flex-shrink: 0; }
.cn-toggle.inv { visibility: hidden; }

.cn-drag { color: var(--text-dim); font-size: 10px; cursor: grab; letter-spacing: -2px; flex-shrink: 0; opacity: 0; }
.cn-row:hover .cn-drag { opacity: 0.5; }

.cn-sort-btns { display: none; flex-shrink: 0; gap: 1px; }
.cn-row:hover .cn-sort-btns { display: flex; }
.cn-sb { border: none; background: transparent; color: var(--text-dim); font-size: 10px; padding: 1px 3px; cursor: pointer; border-radius: 3px; }
.cn-sb:hover { background: var(--primary-tint); color: var(--primary); }

.cn-input { flex: 1; border: 1px solid var(--primary); border-radius: 4px; padding: 2px 6px; font-size: 13px; outline: none; min-width: 0; }

.cn-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }

.cn-wc { font-size: 10px; color: var(--text-dim); flex-shrink: 0; opacity: 0; transition: opacity .12s; }
.cn-row:hover .cn-wc { opacity: 1; }

.cn-actions { display: none; gap: 2px; flex-shrink: 0; }
.cn-row:hover .cn-actions { display: flex; }
.cn-btn { border: none; background: transparent; width: 22px; height: 22px; border-radius: 4px; cursor: pointer; font-size: 12px; display: flex; align-items: center; justify-content: center; color: var(--text-dim); transition: all .1s; }
.cn-btn:hover { background: var(--primary-tint); color: var(--primary); }
.cn-btn.danger:hover { background: #fce4e2; color: #d35b4e; }
</style>
