<script setup>
import { reactive, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

const DEFAULT = {
  titleCn: '',
  titleEn: '',
  author: '',
  studentId: '',
  college: '',
  major: '',
  advisor: '',
  advisorTitle: '',
  date: new Date().toISOString().slice(0, 10),
  keywordsCn: [],
  keywordsEn: []
}

const form = reactive({ ...DEFAULT, ...props.modelValue })

watch(() => props.modelValue, (v) => {
  if (v) Object.assign(form, { ...DEFAULT, ...v })
}, { deep: true })

function emitChange() {
  emit('update:modelValue', { ...form })
}

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
  <div class="cover-panel">
    <h3 class="panel-title">封面信息</h3>
    <p class="panel-desc">以下信息将展示在论文封面页</p>

    <div class="form-grid">
      <div class="form-item full">
        <label>论文标题（中文）<span class="req">*</span></label>
        <el-input v-model="form.titleCn" placeholder="请输入中文论文标题" @input="emitChange" />
      </div>
      <div class="form-item full">
        <label>论文标题（英文）</label>
        <el-input v-model="form.titleEn" placeholder="English Title" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>作者姓名<span class="req">*</span></label>
        <el-input v-model="form.author" placeholder="姓名" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>学号<span class="req">*</span></label>
        <el-input v-model="form.studentId" placeholder="学号" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>学院</label>
        <el-input v-model="form.college" placeholder="所属学院" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>专业</label>
        <el-input v-model="form.major" placeholder="专业名称" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>导师姓名</label>
        <el-input v-model="form.advisor" placeholder="导师姓名" @input="emitChange" />
      </div>
      <div class="form-item">
        <label>导师职称</label>
        <el-select v-model="form.advisorTitle" placeholder="选择职称" @change="emitChange" style="width:100%">
          <el-option label="教授" value="教授" />
          <el-option label="副教授" value="副教授" />
          <el-option label="讲师" value="讲师" />
          <el-option label="研究员" value="研究员" />
        </el-select>
      </div>
      <div class="form-item">
        <label>提交日期</label>
        <el-date-picker v-model="form.date" type="date" placeholder="选择日期" @change="emitChange" style="width:100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
      </div>
    </div>

    <!-- 中文关键词 -->
    <div class="kw-section">
      <label>中文关键词</label>
      <div class="kw-tags">
        <el-tag
          v-for="(kw, i) in (form.keywordsCn || [])"
          :key="'cn'+i"
          closable
          @close="removeKeyword('cn', i)"
          style="margin: 2px 4px"
        >
          <input
            :value="kw"
            @input="form.keywordsCn[i] = $event.target.value; emitChange()"
            placeholder="关键词"
            class="kw-input"
          />
        </el-tag>
        <el-button size="small" :icon="Plus" @click="addKeyword('cn')">添加</el-button>
      </div>
    </div>

    <div class="kw-section">
      <label>英文关键词</label>
      <div class="kw-tags">
        <el-tag
          v-for="(kw, i) in (form.keywordsEn || [])"
          :key="'en'+i"
          closable
          @close="removeKeyword('en', i)"
          style="margin: 2px 4px"
        >
          <input
            :value="kw"
            @input="form.keywordsEn[i] = $event.target.value; emitChange()"
            placeholder="Keyword"
            class="kw-input"
          />
        </el-tag>
        <el-button size="small" :icon="Plus" @click="addKeyword('en')">添加</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cover-panel { padding: 24px 28px; }
.panel-title { font-family: var(--font-heading); font-size: 18px; margin: 0 0 4px; color: var(--text-main); }
.panel-desc { font-size: 12px; color: var(--text-dim); margin: 0 0 20px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-item.full { grid-column: 1 / -1; }
.form-item label { display: block; font-size: 13px; font-weight: 600; color: var(--text-mute); margin-bottom: 4px; }
.req { color: #d35b4e; }
.kw-section { margin-top: 18px; }
.kw-section > label { display: block; font-size: 13px; font-weight: 600; color: var(--text-mute); margin-bottom: 6px; }
.kw-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.kw-input { border: none; outline: none; background: transparent; font-size: 12px; width: 80px; }
</style>
