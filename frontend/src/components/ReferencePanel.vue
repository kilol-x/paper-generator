<script setup>
import { reactive, watch, ref } from 'vue'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const refs = ref([])

watch(() => props.modelValue, (v) => {
  refs.value = Array.isArray(v) ? [...v] : []
}, { immediate: true, deep: true })

function emitChange() {
  emit('update:modelValue', [...refs.value])
}

// 表单对话框
const dialogVisible = ref(false)
const editingIdx = ref(-1)

const emptyRef = () => ({
  id: Date.now(),
  authors: '',
  title: '',
  journal: '',
  year: '',
  volume: '',
  issue: '',
  pages: ''
})

const form = reactive(emptyRef())

function openAdd() {
  editingIdx.value = -1
  Object.assign(form, emptyRef())
  dialogVisible.value = true
}

function openEdit(idx) {
  editingIdx.value = idx
  Object.assign(form, { ...refs.value[idx] })
  dialogVisible.value = true
}

function saveRef() {
  if (!form.title.trim()) {
    ElMessage.warning('文献标题不能为空')
    return
  }
  if (editingIdx.value >= 0) {
    refs.value[editingIdx.value] = { ...form, id: refs.value[editingIdx.value].id }
  } else {
    refs.value.push({ ...form, id: Date.now() })
  }
  dialogVisible.value = false
  emitChange()
  ElMessage.success('已保存')
}

async function removeRef(idx) {
  try {
    await ElMessageBox.confirm('确定删除该文献？', '确认', { type: 'warning' })
    refs.value.splice(idx, 1)
    emitChange()
    ElMessage.success('已删除')
  } catch {}
}

// 格式化引用文本
function formatRefText(ref) {
  const parts = []
  if (ref.authors) parts.push(ref.authors)
  if (ref.title) parts.push(`《${ref.title}》`)
  if (ref.journal) parts.push(ref.journal)
  if (ref.year) parts.push(`${ref.year}`)
  if (ref.volume) parts.push(`${ref.volume}(${ref.issue || ''})`)
  if (ref.pages) parts.push(`:${ref.pages}`)
  return parts.join('. ') || '未命名文献'
}
</script>

<template>
  <div class="ref-panel">
    <div class="ref-header">
      <h3 class="panel-title">参考文献</h3>
      <el-button type="primary" size="small" :icon="Plus" @click="openAdd">添加文献</el-button>
    </div>

    <div v-if="refs.length === 0" class="ref-empty">
      暂无参考文献，点击"添加文献"开始添加
    </div>

    <div v-else class="ref-list">
      <div
        v-for="(ref, idx) in refs"
        :key="ref.id"
        class="ref-item"
      >
        <span class="ref-idx">[{{ idx + 1 }}]</span>
        <span class="ref-text">{{ formatRefText(ref) }}</span>
        <span class="ref-actions">
          <el-button size="small" text :icon="Edit" @click="openEdit(idx)" />
          <el-button size="small" text :icon="Delete" @click="removeRef(idx)" style="color:var(--danger)" />
        </span>
      </div>
    </div>

    <!-- 编辑/添加对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingIdx >= 0 ? '编辑文献' : '添加文献'"
      width="580px"
      destroy-on-close
    >
      <div class="ref-form">
        <el-form label-width="80px" size="default">
          <el-form-item label="作者">
            <el-input v-model="form.authors" placeholder="张三, 李四" />
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" placeholder="论文或专著标题" />
          </el-form-item>
          <el-form-item label="刊物">
            <el-input v-model="form.journal" placeholder="刊物名称" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="年份" label-width="50px">
                <el-input v-model="form.year" placeholder="2024" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="卷" label-width="40px">
                <el-input v-model="form.volume" placeholder="卷" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="期" label-width="40px">
                <el-input v-model="form.issue" placeholder="期" />
              </el-form-item>
            </el-col>
          </el-row>
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
