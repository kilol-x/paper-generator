<script setup>
import { reactive, watch, ref } from 'vue'
import PaperEditor from './PaperEditor.vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const DEFAULT = {
  abstractCn: '',
  abstractEn: '',
  keywordsCn: [],
  keywordsEn: []
}

const form = reactive({ ...DEFAULT })

// 从 props 同步
watch(() => props.modelValue, (v) => {
  if (v && typeof v === 'object') {
    Object.assign(form, { ...DEFAULT, ...v })
  }
}, { immediate: true, deep: true })

function emitChange() {
  emit('update:modelValue', { ...form })
}

function onCnChange(html) {
  form.abstractCn = html
  emitChange()
}
function onEnChange(html) {
  form.abstractEn = html
  emitChange()
}

// 关键词
function addKeyword(lang) {
  const key = lang === 'cn' ? 'keywordsCn' : 'keywordsEn'
  if (!form[key]) form[key] = []
  form[key].push('')
  emitChange()
}
function removeKeyword(lang, idx) {
  const key = lang === 'cn' ? 'keywordsCn' : 'keywordsEn'
  form[key].splice(idx, 1)
  emitChange()
}
</script>

<template>
  <div class="abstract-panel">
    <!-- 中文摘要 -->
    <section class="ab-section">
      <h3 class="panel-title">中文摘要</h3>
      <PaperEditor
        :model-value="form.abstractCn"
        placeholder="请输入中文摘要…"
        @update:model-value="onCnChange"
      />

      <div class="kw-section">
        <label>中文关键词</label>
        <div class="kw-tags">
          <el-tag
            v-for="(kw, i) in (form.keywordsCn || [])"
            :key="'cn'+i"
            closable
            @close="removeKeyword('cn', i)"
            style="margin:2px 4px"
          >
            <input
              :value="kw"
              @input="form.keywordsCn[i]=$event.target.value; emitChange()"
              class="kw-inp"
              placeholder="关键词"
            />
          </el-tag>
          <el-button size="small" :icon="Plus" @click="addKeyword('cn')">添加</el-button>
        </div>
      </div>
    </section>

    <el-divider />

    <!-- 英文摘要 -->
    <section class="ab-section">
      <h3 class="panel-title">英文摘要（Abstract）</h3>
      <PaperEditor
        :model-value="form.abstractEn"
        placeholder="Please enter English abstract…"
        @update:model-value="onEnChange"
      />
      <div class="kw-section">
        <label>英文关键词（Keywords）</label>
        <div class="kw-tags">
          <el-tag
            v-for="(kw, i) in (form.keywordsEn || [])"
            :key="'en'+i"
            closable
            @close="removeKeyword('en', i)"
            style="margin:2px 4px"
          >
            <input
              :value="kw"
              @input="form.keywordsEn[i]=$event.target.value; emitChange()"
              class="kw-inp"
              placeholder="Keyword"
            />
          </el-tag>
          <el-button size="small" :icon="Plus" @click="addKeyword('en')">添加</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.abstract-panel { padding: 20px 24px; }
.panel-title { font-family: var(--font-heading); font-size: 17px; margin: 0 0 12px; color: var(--text-main); }
.ab-section { margin-bottom: 8px; }
.kw-section { margin-top: 12px; }
.kw-section > label { display: block; font-size: 12px; font-weight: 600; color: var(--text-mute); margin-bottom: 6px; }
.kw-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.kw-inp { border: none; outline: none; background: transparent; font-size: 12px; width: 80px; }
</style>
