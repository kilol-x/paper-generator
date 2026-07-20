<template>
  <div class="preview-container">
    <!-- 顶部操作栏 -->
    <div class="preview-toolbar">
      <el-button @click="goBack" type="default" :icon="ArrowLeft">
        返回编辑
      </el-button>
      <el-button type="primary" @click="exportDocx" :icon="Download">
        导出 Word
      </el-button>
      <el-button type="success" @click="exportPdf" :icon="Download">
        导出 PDF
      </el-button>
      <el-button @click="toggleFullscreen" :icon="FullScreen">
        全屏预览
      </el-button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
    </div>

    <!-- 分页控制器 -->
    <div class="page-controls">
      <el-button @click="prevPage" :disabled="currentPage <= 1">
        上一页
      </el-button>
      <el-pagination
        small
        :page-size="1"
        :total="totalPages"
        :current-page="currentPage"
        @current-change="goToPage"
        layout="prev, pager, next"
      />
      <el-button @click="nextPage" :disabled="currentPage >= totalPages">
        下一页
      </el-button>
    </div>

    <!-- 论文预览 - 模拟A4纸 -->
    <div class="paper-wrapper" ref="paperWrapper">
      <div
        class="paper-page"
        v-for="(page, index) in pages"
        v-show="index + 1 === currentPage"
        :key="index"
      >
        <div v-html="page"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download, FullScreen } from '@element-plus/icons-vue'
import axios from 'axios'

// ========== 路由 ==========
const route = useRoute()
const router = useRouter()
const paperId = ref(route.params.id)

// ========== 数据 ==========
const paperData = ref(null)
const pages = ref([])
const currentPage = ref(1)
const totalPages = ref(0)
const paperWrapper = ref(null)

// ========== 获取论文数据 ==========
const fetchPaperData = async () => {
  try {
    const res = await axios.get(`/api/student/papers/${paperId.value}/preview`)
    paperData.value = res.data
    pages.value = splitIntoPages(res.data.fullHtml)
    totalPages.value = pages.value.length
  } catch (error) {
    ElMessage.error('加载论文数据失败')
    console.error(error)
  }
}

// ========== 分页 ==========
const splitIntoPages = (html) => {
  if (!html) return ['<div style="text-align:center;padding:100px 0;">暂无内容</div>']
  const pageSize = 800
  const result = []
  for (let i = 0; i < html.length; i += pageSize) {
    result.push(html.substring(i, i + pageSize))
  }
  return result.length > 0 ? result : ['<div>暂无内容</div>']
}

// ========== 翻页 ==========
const prevPage = () => { if (currentPage.value > 1) currentPage.value-- }
const nextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }
const goToPage = (page) => { currentPage.value = page }

// ========== 返回 ==========
const goBack = () => {
  router.push(`/student/paper/edit/${paperId.value}`)
}

// ========== 全屏 ==========
const toggleFullscreen = () => {
  const el = paperWrapper.value
  if (!document.fullscreenElement) {
    el?.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

// ========== 导出 Word ==========
const exportDocx = async () => {
  try {
    ElMessage.info('正在生成 Word 文档，请稍候...')
    const response = await axios({
      url: `/api/student/papers/${paperId.value}/export/docx`,
      method: 'GET',
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = `${paperData.value?.title || '论文'}.docx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Word 文档导出成功')
  } catch (error) {
    ElMessage.error('导出 Word 失败')
    console.error(error)
  }
}

// ========== 导出 PDF ==========
const exportPdf = async () => {
  try {
    ElMessage.info('正在生成 PDF 文档，请稍候...')
    const response = await axios({
      url: `/api/student/papers/${paperId.value}/export/pdf`,
      method: 'GET',
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = `${paperData.value?.title || '论文'}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF 文档导出成功')
  } catch (error) {
    ElMessage.error('导出 PDF 失败')
    console.error(error)
  }
}

onMounted(() => {
  fetchPaperData()
})
</script>

<style scoped>
.preview-container {
  padding: 20px;
  background: #f0f2f5;
  min-height: 100vh;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-info {
  margin-left: auto;
  color: #666;
  font-size: 14px;
}

.page-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.paper-wrapper {
  display: flex;
  justify-content: center;
  background: #e8e8e8;
  padding: 30px 20px;
  border-radius: 8px;
  min-height: 600px;
}

.paper-page {
  width: 210mm;
  min-height: 297mm;
  background: #ffffff;
  padding: 2.54cm 3.17cm;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  font-size: 12pt;
  line-height: 1.5;
  font-family: 'Times New Roman', SimSun, serif;
  box-sizing: border-box;
  overflow: hidden;
}

/* 论文内部样式 */
.paper-page :deep(h1) {
  font-size: 18pt;
  font-weight: bold;
  text-align: center;
  margin: 20px 0;
}
.paper-page :deep(h2) {
  font-size: 16pt;
  font-weight: bold;
  margin: 16px 0;
}
.paper-page :deep(h3) {
  font-size: 14pt;
  font-weight: bold;
  margin: 12px 0;
}
.paper-page :deep(p) {
  text-indent: 2em;
  margin: 0.5em 0;
}
.paper-page :deep(table) {
  border-collapse: collapse;
  margin: 12px auto;
  width: 100%;
}
.paper-page :deep(th),
.paper-page :deep(td) {
  border: 1px solid #333;
  padding: 6px 10px;
  text-align: center;
}
.paper-page :deep(img) {
  max-width: 100%;
  display: block;
  margin: 10px auto;
}
.paper-page :deep(.table-three-line th) {
  border-top: 1.5pt solid #000;
  border-bottom: 1pt solid #000;
  padding: 6px 10px;
}
.paper-page :deep(.table-three-line td) {
  border: none;
  padding: 6px 10px;
}
.paper-page :deep(.table-three-line tr:last-child td) {
  border-bottom: 1.5pt solid #000;
}
</style>