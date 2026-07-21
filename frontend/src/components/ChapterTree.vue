<script setup>
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import ChapterNode from './ChapterNode.vue'

const props = defineProps({
  sections: { type: Array, required: true },
  activeId: { type: String, default: null },
  editingId: { type: String, default: null },
  editingValue: { type: String, default: '' }
})

const emit = defineEmits([
  'select', 'add', 'remove', 'edit', 'finish-edit',
  'update-editing', 'keydown', 'move', 'reorder'
])

const chapters = computed(() =>
  props.sections.filter(s => !s.type || s.type === 'chapter')
)

const rootChapters = computed(() =>
  chapters.value
    .filter(s => !s.parentId)
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
)
</script>

<template>
  <div class="chapter-tree">
    <ChapterNode
      v-for="section in rootChapters"
      :key="section.id"
      :section="section"
      :all-chapters="chapters"
      :active-id="activeId"
      :editing-id="editingId"
      :editing-value="editingValue"
      :depth="0"
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
    <div v-if="rootChapters.length === 0" class="empty-hint">
      <el-button type="primary" :icon="Plus" @click="emit('add', null)">
        添加章节
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.chapter-tree {
  padding: 4px 0;
}
.empty-hint {
  padding: 24px 16px;
  text-align: center;
}
</style>
