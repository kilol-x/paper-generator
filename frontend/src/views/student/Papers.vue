<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Document, Upload } from '@element-plus/icons-vue'
import request from '../../api/request'
import TemplatePickerDialog from '../../components/TemplatePickerDialog.vue'

const router = useRouter()

const papers = ref([])
const loading = ref(false)
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchKeyword = ref('')
const filterStatus = ref('')

const showTemplatePicker = ref(false)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '批阅中', value: 'REVIEWING' },
  { label: '已退回', value: 'RETURNED' },
  { label: '已评分', value: 'GRADED' }
]

const statusMap = {
  DRAFT:    { label: '草稿',   cls: 's-draft' },
  SUBMITTED:{ label: '已提交', cls: 's-submitted' },
  REVIEWING:{ label: '批阅中', cls: 's-reviewing' },
  RETURNED: { label: '已退回', cls: 's-returned' },
  GRADED:   { label: '已评分', cls: 's-graded' }
}

async function loadPapers() {
  loading.value = true
  try {
    const params = { page: currentPage.value - 1, size: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    const res = await request.get('/api/papers', { params })
    const data = res.data || res
    papers.value = data.content || []
    totalElements.value = data.totalElements || 0
  } catch {
    ElMessage.error('加载论文列表失败')
  } finally {
    loading.value = false
  }
}

function editPaper(id) {
  router.push({ name: 'EditPaper', params: { id } })
}

function createPaper() {
  showTemplatePicker.value = true
}

async function onTemplatePicked(templateData) {
  const tpl = templateData.template
  const cfg = templateData.config || {}
  try {
    const res = await request.post('/api/papers', {
      title: `${tpl.name} - 未命名论文`,
      sections: [],
      templateId: tpl.id,
      templateSnapshot: JSON.stringify({
        templateId: tpl.id,
        templateName: tpl.name,
        templateType: tpl.type,
        structureJson: cfg.structureJson || '',
        formatJson: cfg.formatJson || '',
        coverFields: cfg.coverFields || '',
      })
    })
    const newId = res?.id || res?.data?.id
    ElMessage.success('论文已创建，正在进入编辑器…')
    router.push({
      name: 'EditPaper',
      params: { id: newId },
      state: { templateData }
    })
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '创建论文失败')
  }
}

async function submitPaper(paper) {
  try {
    await ElMessageBox.confirm(
      `确定提交「${paper.title}」吗？提交后将锁定论文，等待教师批阅。`,
      '提交确认',
      { confirmButtonText: '确定提交', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    // 先通过更新接口设置状态为 SUBMITTED 并锁定
    await request.put(`/api/papers/${paper.id}`, {
      title: paper.title,
      status: 'SUBMITTED'
    })
    // 再调用 submit 接口创建版本记录
    try {
      await request.post(`/api/reviews/${paper.id}/submit`)
    } catch { /* submit 接口可能还未完善 */ }
    ElMessage.success('论文已提交')
    await loadPapers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '提交失败')
  }
}

function deletePaper(paper) {
  ElMessageBox.confirm(
    `确定删除「${paper.title}」？删除后不可恢复。`,
    '删除确认',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await request.delete(`/api/papers/${paper.id}`)
      ElMessage.success('已删除')
      if (papers.value.length === 1 && currentPage.value > 1) currentPage.value--
      await loadPapers()
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '删除失败')
    }
  }).catch(() => {})
}

function onSearch() { currentPage.value = 1; loadPapers() }
function onStatusChange() { currentPage.value = 1; loadPapers() }
function onSizeChange(size) { pageSize.value = size; currentPage.value = 1; loadPapers() }
function onPageChange(page) { currentPage.value = page; loadPapers() }

function fmtDate(str) {
  if (!str) return '—'
  return str.replace('T', ' ').slice(0, 16)
}

function canEdit(paper) {
  return paper.status === 'DRAFT' || paper.status === 'RETURNED'
}

function canSubmit(paper) {
  return paper.status === 'DRAFT'
}

onMounted(() => loadPapers())
</script>

<template>
  <div class="papers-content">
    <!-- 工具栏 -->
    <div class="papers-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索论文标题…"
          :prefix-icon="Search"
          clearable
          style="width: 260px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select
          v-model="filterStatus"
          placeholder="状态筛选"
          style="width: 130px"
          @change="onStatusChange"
        >
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button @click="onSearch">搜索</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="createPaper">新建论文</el-button>
    </div>

    <!-- 表格 -->
    <div class="papers-table-wrap">
      <el-table
        v-loading="loading"
        :data="papers"
        stripe
        empty-text="暂无论文，点击右上角「新建论文」开始创作"
        @row-dblclick="(row) => canEdit(row) && editPaper(row.id)"
      >
        <el-table-column prop="title" label="论文标题" min-width="260">
          <template #default="{ row }">
            <div
              class="title-cell"
              :class="{ locked: !canEdit(row) }"
              @click="canEdit(row) ? editPaper(row.id) : null"
            >
              <el-icon><Document /></el-icon>
              <span>{{ row.title || '未命名论文' }}</span>
              <el-tag v-if="row.locked" size="small" type="info" class="lock-tag">🔒 已锁定</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <span v-if="statusMap[row.status]" class="status-dot" :class="statusMap[row.status].cls">
              {{ statusMap[row.status].label }}
            </span>
            <span v-else class="status-dot s-draft">{{ row.status || '草稿' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.currentVersion || 1 }}</template>
        </el-table-column>

        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmtDate(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="更新时间" width="160" align="center">
          <template #default="{ row }">{{ fmtDate(row.updatedAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button
                v-if="canEdit(row)"
                size="small" type="primary" link :icon="Edit"
                @click="editPaper(row.id)"
              >编辑</el-button>
              <el-button
                v-if="canSubmit(row)"
                size="small" type="success" link :icon="Upload"
                @click="submitPaper(row)"
              >提交</el-button>
              <el-button
                v-if="canEdit(row)"
                size="small" type="danger" link :icon="Delete"
                @click="deletePaper(row)"
              >删除</el-button>
              <span v-if="!canEdit(row)" class="readonly-hint">只读</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="totalElements > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <!-- 模板选择弹窗 -->
    <TemplatePickerDialog
      v-model="showTemplatePicker"
      @confirm="onTemplatePicked"
    />
  </div>
</template>

<style scoped>
.papers-content {
  --el-color-primary: #4f776a;
  --el-color-primary-light-3: #729487;
  --el-color-primary-light-5: #90aca2;
  --el-color-primary-light-7: #b8cec6;
  --el-color-primary-light-8: #d0dfd9;
  --el-color-primary-light-9: #e8efec;
  --el-color-primary-dark-2: #3f5f55;
  padding: 4px 0;
}

.papers-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
}

.papers-table-wrap {
  --el-color-primary: #4f776a;
  --el-color-primary-light-3: #729487;
  --el-color-primary-light-5: #90aca2;
  --el-color-primary-light-7: #b8cec6;
  --el-color-primary-light-8: #d0dfd9;
  --el-color-primary-light-9: #e8efec;
  --el-color-primary-dark-2: #3f5f55;
  --el-fill-color-blank: #fff;
  --el-border-radius-base: 8px;
  background: #fff;
  border: 1px solid #e4e3de;
  border-radius: 13px;
  overflow: hidden;
}

.papers-table-wrap :deep(.el-pager li.is-active) {
  background-color: #4f776a !important;
  color: #fff !important;
}
.papers-table-wrap :deep(.el-pager li:hover) { color: #4f776a; }
.papers-table-wrap :deep(.el-pagination .btn-prev:hover),
.papers-table-wrap :deep(.el-pagination .btn-next:hover) { color: #4f776a; }

.title-cell {
  display: flex; align-items: center; gap: 7px;
  cursor: pointer; color: #242622; font-weight: 500;
}
.title-cell:hover { color: #4F776A; }
.title-cell.locked { cursor: default; opacity: 0.7; }
.title-cell.locked:hover { color: #242622; }
.lock-tag { margin-left: 6px; opacity: 0.8; }

.status-dot {
  display: inline-block; padding: 3px 10px;
  border-radius: 999px; font-size: 12px; font-weight: 600;
}
.s-draft      { background: #EDF0EC; color: #5C605A; }
.s-submitted  { background: #E6EDF5; color: #506D8F; }
.s-reviewing  { background: #E8EEF2; color: #4F6D7A; }
.s-returned   { background: #F5E8D8; color: #996127; }
.s-graded     { background: #E3EEE9; color: #386858; }

.action-btns { display: flex; gap: 4px; justify-content: center; }
.readonly-hint { font-size: 12px; color: var(--text-dim); }

.pagination-wrap {
  display: flex; justify-content: center;
  padding: 14px 18px; border-top: 1px solid #e4e3de;
}
</style>
