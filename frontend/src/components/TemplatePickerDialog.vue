<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '../api/request'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const colleges = ref([])
const templates = ref([])
const loading = ref(false)

const filterCollegeId = ref('')
const filterType = ref('')
const keyword = ref('')

const TYPES = ['毕业论文', '课程论文', '项目论文']
const selectedId = ref(null)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw
    ? templates.value.filter(t => t.name.toLowerCase().includes(kw))
    : templates.value
})

async function loadColleges() {
  try {
    const res = await request.get('/api/colleges')
    colleges.value = res.data || res || []
  } catch { colleges.value = [] }
}

async function loadTemplates() {
  loading.value = true
  try {
    const params = {}
    if (filterCollegeId.value) params.collegeId = filterCollegeId.value
    if (filterType.value) params.type = filterType.value
    const res = await request.get('/api/templates', { params })
    templates.value = res.data || res || []
    selectedId.value = null
  } catch {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (v) => {
  if (v) {
    selectedId.value = null
    keyword.value = ''
    loadTemplates()
  }
})

onMounted(() => {
  loadColleges()
})

async function handleConfirm() {
  if (!selectedId.value) return ElMessage.warning('请先选择一个模板')
  loading.value = true
  try {
    const res = await request.get(`/api/templates/${selectedId.value}`)
    const data = res.data || res
    emit('confirm', data)
    emit('update:modelValue', false)
  } catch {
    ElMessage.error('加载模板详情失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
}

function collegeName(id) {
  const c = colleges.value.find(c => c.id === id)
  return c ? c.name : id
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="选择论文模板"
    width="760px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <!-- 筛选栏 -->
    <div class="tpl-filters">
      <el-select v-model="filterCollegeId" placeholder="全部学院" clearable style="width:160px" @change="loadTemplates">
        <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filterType" placeholder="全部类型" clearable style="width:130px" @change="loadTemplates">
        <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索模板名称"
        :prefix-icon="Search"
        clearable
        style="width:220px"
      />
    </div>

    <!-- 模板卡片列表 -->
    <div v-loading="loading" class="tpl-grid">
      <div
        v-for="t in filtered"
        :key="t.id"
        class="tpl-card"
        :class="{ selected: selectedId === t.id }"
        @click="selectedId = t.id"
      >
        <div class="tpl-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
        </div>
        <div class="tpl-card-info">
          <div class="tpl-name">{{ t.name }}</div>
          <div class="tpl-meta">
            <span class="tpl-type">{{ t.type }}</span>
            <span class="tpl-college">{{ collegeName(t.collegeId) }}</span>
          </div>
          <div v-if="t.description" class="tpl-desc">{{ t.description }}</div>
        </div>
        <div v-if="selectedId === t.id" class="tpl-check">✓</div>
      </div>

      <div v-if="!loading && filtered.length === 0" class="tpl-empty">
        <p>暂无可用模板</p>
        <small>请联系管理员添加论文模板</small>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!selectedId" @click="handleConfirm">
        套用模板并创建论文
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tpl-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.tpl-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding: 2px;
  min-height: 180px;
}

.tpl-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border: 2px solid #e4e3de;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color .18s, background .18s;
  position: relative;
  background: #fff;
}
.tpl-card:hover { border-color: #4f776a; background: #f4f7f5; }
.tpl-card.selected { border-color: #4f776a; background: #eef4f1; }

.tpl-card-icon {
  width: 40px;
  height: 40px;
  background: #dce6e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #4f776a;
}
.tpl-card-icon svg { width: 22px; height: 22px; }

.tpl-card-info { flex: 1; min-width: 0; }
.tpl-name { font-weight: 600; font-size: 14px; color: #242622; margin-bottom: 6px; }
.tpl-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.tpl-type {
  display: inline-block; padding: 1px 8px;
  background: #e3eee9; color: #386858;
  border-radius: 999px; font-size: 11px; font-weight: 600;
}
.tpl-college {
  display: inline-block; padding: 1px 8px;
  background: #e6edf5; color: #506d8f;
  border-radius: 999px; font-size: 11px; font-weight: 600;
}
.tpl-desc { font-size: 12px; color: #787b75; line-height: 1.5; }

.tpl-check {
  position: absolute;
  top: 10px; right: 12px;
  width: 22px; height: 22px;
  background: #4f776a; color: #fff;
  border-radius: 50%; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700;
}

.tpl-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 56px 20px;
  color: #787b75;
}
.tpl-empty p { font-size: 15px; margin-bottom: 6px; }
.tpl-empty small { font-size: 12px; color: #a0a39e; }
</style>
