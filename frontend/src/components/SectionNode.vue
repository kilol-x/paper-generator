<script>
import { computed } from 'vue'

export default {
  name: 'SectionNode',
  props: {
    section: Object,
    allSections: { type: Array, default: () => [] },
    activeId: String,
    editingTitleId: String,
    editingTitleValue: String,
    depth: { type: Number, default: 0 }
  },
  emits: ['select', 'add', 'remove', 'rename', 'update:editingValue', 'finish-rename', 'move'],
  setup(props, { emit }) {
    const childSections = computed(() =>
      props.allSections
        .filter(s => s.parentId === props.section.id)
        .sort((a, b) => a.sortOrder - b.sortOrder)
    )
    return { childSections, emit }
  }
}
</script>

<template>
  <div class="section-node">
    <!-- 节点行 -->
    <div
      :class="['section-row', `depth-${depth}`, { active: activeId === section.id }]"
      @click="emit('select', section.id)"
    >
      <!-- 排序按钮 -->
      <span class="sort-arrows">
        <button class="sort-btn" title="上移" @click.stop="emit('move', section.id, -1)">▲</button>
        <button class="sort-btn" title="下移" @click.stop="emit('move', section.id, 1)">▼</button>
      </span>

      <!-- 标题（普通 / 编辑模式） -->
      <span
        v-if="editingTitleId !== section.id"
        class="section-label"
        @dblclick.stop="emit('rename', section.id)"
      >{{ section.title || '未命名章节' }}</span>
      <input
        v-else
        :data-id="section.id"
        class="section-title-input"
        :value="editingTitleValue"
        @input="emit('update:editingValue', $event.target.value)"
        @blur="emit('finish-rename')"
        @keyup.enter="emit('finish-rename')"
        @click.stop
      />

      <!-- 操作按钮 -->
      <span class="section-actions">
        <button class="action-btn" title="添加子章节" @click.stop="emit('add', section.id)">+</button>
        <button class="action-btn" title="重命名" @click.stop="emit('rename', section.id)">✎</button>
        <button class="action-btn danger" title="删除" @click.stop="emit('remove', section.id)">✕</button>
      </span>
    </div>

    <!-- 子节点（递归） -->
    <div v-if="childSections.length" class="section-children">
      <SectionNode
        v-for="child in childSections"
        :key="child.id"
        :section="child"
        :all-sections="allSections"
        :active-id="activeId"
        :editing-title-id="editingTitleId"
        :editing-title-value="editingTitleValue"
        :depth="depth + 1"
        @select="id => emit('select', id)"
        @add="id => emit('add', id)"
        @remove="id => emit('remove', id)"
        @rename="id => emit('rename', id)"
        @update:editing-value="val => emit('update:editingValue', val)"
        @finish-rename="() => emit('finish-rename')"
        @move="(id, dir) => emit('move', id, dir)"
      />
    </div>
  </div>
</template>

<style scoped>
.section-node {
  user-select: none;
}

.section-row {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 7px 12px 7px calc(12px + var(--depth-offset, 0px));
  cursor: pointer;
  transition: background var(--t-fast);
  font-size: 13px;
  color: var(--text-mute);
}

.section-row:hover {
  background: var(--bg-hover);
}

.section-row.active {
  background: var(--primary-tint);
  color: var(--primary);
  font-weight: 500;
}

.section-row.active .section-label {
  color: var(--primary);
}

.depth-0 { --depth-offset: 0px; }
.depth-1 { --depth-offset: 16px; }
.depth-2 { --depth-offset: 32px; }
.depth-3 { --depth-offset: 48px; }

.sort-arrows {
  display: flex;
  flex-direction: column;
  opacity: 0;
  transition: opacity var(--t-fast);
  flex-shrink: 0;
}

.section-row:hover .sort-arrows {
  opacity: 1;
}

.sort-btn {
  border: none;
  background: none;
  color: var(--text-dim);
  font-size: 8px;
  cursor: pointer;
  line-height: 1;
  padding: 0 2px;
}

.sort-btn:hover {
  color: var(--primary);
}

.section-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-title-input {
  flex: 1;
  border: 1px solid var(--primary);
  border-radius: var(--r-sm);
  padding: 3px 8px;
  font-size: 13px;
  outline: none;
  min-width: 0;
  color: var(--text-main);
  background: var(--bg-card);
}

.section-actions {
  display: flex;
  gap: 1px;
  opacity: 0;
  transition: opacity var(--t-fast);
  flex-shrink: 0;
}

.section-row:hover .section-actions {
  opacity: 1;
}

.action-btn {
  border: none;
  background: none;
  color: var(--text-dim);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 5px;
  border-radius: var(--r-sm);
  line-height: 1;
  transition: background var(--t-fast), color var(--t-fast);
}

.action-btn:hover {
  background: var(--primary-tint);
  color: var(--primary);
}

.action-btn.danger:hover {
  background: var(--danger-bg);
  color: var(--danger);
}
</style>
