<script setup>
import { reactive, watch, ref } from 'vue'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../api/request'

const props = defineProps({
  paperId: { type: [String, Number], default: null },
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'cite'])

const refs = ref([])
const loading = ref(false)

watch(() => props.paperId, () => {
  loadRefs()
}, { immediate: true })

watch(() => props.modelValue, (value) => {
  const normalized = normalizeRefs(value)
  if (JSON.stringify(normalized) !== JSON.stringify(refs.value)) {
    refs.value = normalized
  }
}, { deep: true, immediate: true })

function emitChange() {
  emit('update:modelValue', [...refs.value])
}

function normalizeRefs(list) {
  if (!Array.isArray(list)) {
    return []
  }
  return list.map(item => ({
    ...item,
    authors: item.authors || '',
    title: item.title || '',
    journal: item.journal || '',
    year: item.year || '',
    pages: item.pages || ''
  }))
}

// 表单对话框
const dialogVisible = ref(false)
const editingId = ref(null)

const emptyRef = () => ({
  authors: '',
  title: '',
  journal: '',
  year: '',
  pages: ''
})

const form = reactive(emptyRef())

async function loadRefs() {
  if (!props.paperId) {
    refs.value = []
    emitChange()
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${props.paperId}/references`)
    refs.value = (res?.data || res || []).map(item => ({
      ...item,
      id: item.id,
      citationNo: item.citationNo,
      formattedText: item.formattedText
    }))
    emitChange()
  } catch {
    refs.value = []
    emitChange()
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  Object.assign(form, emptyRef())
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    authors: item.authors || '',
    title: item.title || '',
    journal: item.journal || '',
    year: item.year || '',
    pages: item.pages || ''
  })
  dialogVisible.value = true
}

async function saveRef() {
  if (!props.paperId) {
    ElMessage.warning('请先保存论文，再录入参考文献')
    return
  }
  if (![form.authors, form.title, form.journal, form.year, form.pages].some(value => value.trim())) {
    ElMessage.warning('请至少填写一项文献信息')
    return
  }

  const payload = {
    authors: form.authors.trim(),
    title: form.title.trim(),
    journal: form.journal.trim(),
    year: form.year.trim(),
    pages: form.pages.trim()
  }

  try {
    if (editingId.value) {
      await request.put(`/api/papers/${props.paperId}/references/${editingId.value}`, payload)
      ElMessage.success('文献已更新')
    } else {
      await request.post(`/api/papers/${props.paperId}/references`, payload)
      ElMessage.success('文献已添加')
    }
    dialogVisible.value = false
    await loadRefs()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '文献保存失败')
  }
}

async function removeRef(item) {
  if (!props.paperId) {
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该文献？', '确认', { type: 'warning' })
    await request.delete(`/api/papers/${props.paperId}/references/${item.id}`)
    ElMessage.success('已删除')
    await loadRefs()
  } catch {}
}

async function citeRef(item) {
  if (!props.paperId) {
    return
  }
  try {
    const res = await request.get(`/api/papers/${props.paperId}/references/${item.id}/marker`)
    const marker = res?.data?.marker || `[${item.citationNo}]`
    emit('cite', {
      marker,
      displayLabel: res?.data?.displayLabel || (item.year ? `${item.citationNo} (${item.year})` : String(item.citationNo)),
      citationNo: item.citationNo,
      ref: item
    })
  } catch {
    ElMessage.error('获取引用标注失败')
  }
}
</script>

<template>
  <div class="ref-panel" v-loading="loading">
    <div class="ref-header">
      <div>
        <h3 class="panel-title">参考文献</h3>
        <p class="panel-tip">文献信息支持分步录入，至少填写一项即可保存；正文中可插入引用书签。</p>
      </div>
      <el-button type="primary" size="small" :icon="Plus" @click="openAdd">添加文献</el-button>
    </div>

    <div v-if="refs.length === 0" class="ref-empty">
      暂无参考文献，点击"添加文献"开始添加
    </div>

    <div v-else class="ref-list">
      <div v-for="ref in refs" :key="ref.id" class="ref-item">
        <span class="ref-idx">[{{ ref.citationNo }}]</span>
        <span class="ref-text">{{ ref.formattedText }}</span>
        <span class="ref-actions">
          <el-button size="small" text type="primary" @click="citeRef(ref)">引用</el-button>
          <el-button size="small" text :icon="Edit" @click="openEdit(ref)" />
          <el-button size="small" text :icon="Delete" @click="removeRef(ref)" style="color:var(--danger)" />
        </span>
      </div>
    </div>

    <!-- 编辑/添加对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑文献' : '添加文献'"
      width="580px"
      destroy-on-close
    >
      <div class="ref-form">
        <el-form label-width="80px" size="default">
          <el-form-item label="作者">
            <el-input v-model="form.authors" placeholder="张三, 李四" />
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="form.title" placeholder="论文或专著标题" />
          </el-form-item>
          <el-form-item label="刊物">
            <el-input v-model="form.journal" placeholder="发表刊物名称" />
          </el-form-item>
          <el-form-item label="年份">
            <el-input v-model="form.year" placeholder="2024" />
          </el-form-item>
          <el-form-item label="页码">
            <el-input v-model="form.pages" placeholder="1-10" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRef">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ref-panel { padding: 20px 24px; }
.ref-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.panel-title { font-family: var(--font-heading); font-size: 17px; margin: 0; color: var(--text-main); }
.panel-tip { margin: 6px 0 0; font-size: 12px; color: var(--text-dim); }
.ref-empty { text-align: center; color: var(--text-dim); padding: 40px 0; font-size: 13px; }
.ref-list { display: flex; flex-direction: column; gap: 4px; }
.ref-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 8px 10px; border-radius: 6px; font-size: 13px;
  transition: background .12s; border: 1px solid transparent;
}
.ref-item:hover { background: var(--bg-page); border-color: var(--border); }
.ref-idx { color: var(--primary); font-weight: 600; flex-shrink: 0; }
.ref-text { flex: 1; line-height: 1.6; color: var(--text-main); }
.ref-actions { display: flex; gap: 2px; flex-shrink: 0; opacity: 0; transition: opacity .12s; }
.ref-item:hover .ref-actions { opacity: 1; }
.ref-form { padding: 4px 0; }
</style>
