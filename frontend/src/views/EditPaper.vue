<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, DocumentAdd, EditPen, CirclePlus } from '@element-plus/icons-vue'
import PaperEditor from '@/components/PaperEditor.vue'
import request from '@/api/request'

// ==================== 论文标题 ====================
const paperId = ref(null)
const title = ref('')
const saving = ref(false)
const loading = ref(false)

// ==================== 章节管理 ====================
let nextId = 2

const chapters = ref([
  { id: 1, name: '第一章', content: '' }
])

const activeChapterId = ref(1)

const activeChapter = computed(() =>
  chapters.value.find(c => c.id === activeChapterId.value)
)

const activeContent = computed({
  get() {
    return activeChapter.value?.content ?? ''
  },
  set(val) {
    const ch = chapters.value.find(c => c.id === activeChapterId.value)
    if (ch) ch.content = val
  }
})

// ==================== 章节操作 ====================
function addChapter() {
  const id = nextId++
  const name = `第${chapters.value.length + 1}章`
  chapters.value.push({ id, name, content: '' })
  activeChapterId.value = id
  ElMessage.success(`已添加「${name}」`)
}

function removeChapter(id) {
  if (chapters.value.length <= 1) {
    ElMessage.warning('至少保留一个章节')
    return
  }
  const ch = chapters.value.find(c => c.id === id)
  ElMessageBox.confirm(
    `确定删除「${ch?.name}」吗？该章节内容将丢失。`,
    '删除章节',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const idx = chapters.value.findIndex(c => c.id === id)
    chapters.value.splice(idx, 1)
    if (activeChapterId.value === id) {
      activeChapterId.value = chapters.value[0].id
    }
    ElMessage.success('章节已删除')
  }).catch(() => {})
}

function renameChapter(id) {
  const ch = chapters.value.find(c => c.id === id)
  if (!ch) return
  ElMessageBox.prompt('请输入新的章节名称', '重命名', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: ch.name
  }).then(({ value }) => {
    if (value && value.trim()) {
      ch.name = value.trim()
    }
  }).catch(() => {})
}

// ==================== 加载论文 ====================
async function loadPaper(id) {
  loading.value = true
  try {
    const res = await request.get(`/api/papers/${id}`)
    const paper = res.data
    title.value = paper.title || ''
    if (paper.chapters && paper.chapters.length > 0) {
      chapters.value = paper.chapters.map(ch => ({
        id: ch.id,
        name: ch.name,
        content: ch.content
      }))
      nextId = Math.max(...chapters.value.map(c => c.id), 0) + 1
      activeChapterId.value = chapters.value[0].id
    }
    paperId.value = id
  } catch {
    ElMessage.error('加载论文失败')
  } finally {
    loading.value = false
  }
}

// ==================== 保存 ====================
async function savePaper() {
  if (!title.value.trim()) {
    ElMessage.warning('请输入论文标题')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: title.value.trim(),
      chapters: chapters.value.map(ch => ({
        id: ch.id,
        name: ch.name,
        content: ch.content
      }))
    }
    const res = await request.post('/api/papers', payload)
    // 保存成功后记录返回的论文 ID，便于后续更新
    if (res.data?.id) {
      paperId.value = res.data.id
    }
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ==================== 键盘快捷键 ====================
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    savePaper()
  }
}
</script>

<template>
  <div class="edit-paper" @keydown="onKeydown">
    <!-- ==================== 顶部标题栏 ==================== -->
    <el-header class="header" height="64px">
      <div class="header-left">
        <el-input
          v-model="title"
          placeholder="请输入论文标题"
          size="large"
          class="title-input"
          clearable
        />
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          size="large"
          :icon="DocumentAdd"
          :loading="saving"
          @click="savePaper"
        >
          保存论文
        </el-button>
      </div>
    </el-header>

    <!-- ==================== 主体：大纲 + 编辑器 ==================== -->
    <el-container class="body-container">
      <!-- 左侧章节大纲 -->
      <el-aside class="aside" width="260px">
        <div class="aside-header">
          <span class="aside-title">章节大纲</span>
          <el-button
            type="primary"
            size="small"
            :icon="CirclePlus"
            text
            @click="addChapter"
          >
            添加章节
          </el-button>
        </div>

        <div class="chapter-list">
          <div
            v-for="(ch, index) in chapters"
            :key="ch.id"
            class="chapter-item"
            :class="{ active: ch.id === activeChapterId }"
            @click="activeChapterId = ch.id"
          >
            <el-icon class="chapter-drag-handle"><Plus /></el-icon>
            <div class="chapter-info">
              <span class="chapter-name">{{ ch.name }}</span>
              <span class="chapter-meta">
                {{ index + 1 }} · {{ ch.content ? '已有内容' : '空章节' }}
              </span>
            </div>
            <div class="chapter-actions" @click.stop>
              <el-button
                :icon="EditPen"
                size="small"
                text
                title="重命名"
                @click="renameChapter(ch.id)"
              />
              <el-button
                :icon="Delete"
                size="small"
                text
                type="danger"
                title="删除"
                @click="removeChapter(ch.id)"
              />
            </div>
          </div>

          <el-empty
            v-if="chapters.length === 0"
            description="暂无章节"
            :image-size="60"
          />
        </div>
      </el-aside>

      <!-- 右侧编辑器 -->
      <el-main class="main-content">
        <div class="editor-wrapper">
          <div class="chapter-breadcrumb" v-if="activeChapter">
            <el-icon><EditPen /></el-icon>
            <span>当前编辑：{{ activeChapter.name }}</span>
          </div>
          <PaperEditor
            v-model:content="activeContent"
            :key="activeChapterId"
          />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<style scoped>
.edit-paper {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 600px;
  background: #f5f5f5;
}

/* ========== 顶部标题栏 ========== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.header-left {
  flex: 1;
  max-width: 640px;
}

.title-input :deep(.el-input__inner) {
  font-size: 20px;
  font-weight: 600;
  border: none;
  padding-left: 0;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding-left: 0;
}

.title-input :deep(.el-input__inner::placeholder) {
  font-weight: 400;
  color: #c0c4cc;
}

.header-right {
  flex-shrink: 0;
}

/* ========== 主体布局 ========== */
.body-container {
  flex: 1;
  overflow: hidden;
}

/* ========== 左侧章节大纲 ========== */
.aside {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow: hidden;
}

.aside-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.aside-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chapter-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.chapter-item:hover {
  background: #f5f7fa;
  border-color: #e8e8e8;
}

.chapter-item.active {
  background: #ecf5ff;
  border-color: #a0cfff;
}

.chapter-item.active .chapter-name {
  color: #1677ff;
}

.chapter-drag-handle {
  flex-shrink: 0;
  color: #c0c4cc;
  font-size: 14px;
  cursor: grab;
}

.chapter-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chapter-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chapter-meta {
  font-size: 12px;
  color: #909399;
}

.chapter-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.chapter-item:hover .chapter-actions {
  opacity: 1;
}

/* ========== 右侧编辑器 ========== */
.main-content {
  padding: 24px;
  overflow-y: auto;
}

.editor-wrapper {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chapter-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 20px;
  font-size: 14px;
  color: #606266;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.chapter-breadcrumb .el-icon {
  color: #1677ff;
}
</style>
