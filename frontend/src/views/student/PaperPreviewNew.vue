<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const paperId = route.params.id

const paper = ref(null)
const loading = ref(true)

const coverInfo = ref({})
const abstractData = ref({ abstractCn: '', abstractEn: '', keywordsCn: [], keywordsEn: [] })
const references = ref([])
const acknowledgment = ref('')
const chapters = ref([])

async function load() {
  try {
    const token = localStorage.getItem('token') || localStorage.getItem('paper-access-token')
    const res = await axios.get(`http://localhost:8080/api/papers/${paperId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    paper.value = res.data?.data || res.data
    const sections = paper.value?.sections || []

    // 解析各类型章节
    for (const s of sections) {
      switch (s.type) {
        case 'cover':
          try { coverInfo.value = JSON.parse(s.content) } catch { coverInfo.value = {} }
          break
        case 'abstract':
          try { abstractData.value = JSON.parse(s.content) } catch { abstractData.value = {} }
          break
        case 'references':
          try { references.value = JSON.parse(s.content) } catch { references.value = [] }
          break
        case 'acknowledgment':
          acknowledgment.value = s.content || ''
          break
        default:
          chapters.value.push(s)
      }
    }
    chapters.value.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch {
    ElMessage.error('加载论文失败')
  } finally {
    loading.value = false
  }
}

// 扁平化章节（用于目录）
const tocChapters = computed(() => {
  const result = []
  function walk(list, depth) {
    for (const ch of list.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))) {
      result.push({ ...ch, depth })
      const children = chapters.value.filter(c => c.parentId === ch.id)
      if (children.length > 0) walk(children, depth + 1)
    }
  }
  const roots = chapters.value.filter(c => !c.parentId)
  walk(roots, 1)
  return result
})

function goBack() { router.push({ name: 'Papers' }) }
function goEdit() { router.push({ name: 'EditPaper', params: { id: paperId } }) }

onMounted(load)
</script>

<template>
  <div class="preview-shell" v-loading="loading">
    <!-- 工具栏 -->
    <div class="preview-toolbar">
      <div class="pt-left">
        <el-button size="small" text @click="goBack">← 返回列表</el-button>
        <el-button size="small" @click="goEdit">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:3px"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"/></svg>
          编辑
        </el-button>
      </div>
      <div class="pt-right">
        <span class="pt-title">{{ paper?.title || '未命名论文' }}</span>
        <span class="pt-status" v-if="paper">{{ paper.status }}</span>
      </div>
    </div>

    <!-- 论文内容 -->
    <div class="preview-body" v-if="!loading">
      <div class="paper-a4">

        <!-- ========== 封面 ========== -->
        <div class="cover-page page-break">
          <div class="cover-school" v-if="coverInfo.college">{{ coverInfo.college }}</div>
          <h1 class="cover-title-main">{{ coverInfo.titleCn || paper?.title || '论文标题' }}</h1>
          <h2 class="cover-title-sub" v-if="coverInfo.titleEn">{{ coverInfo.titleEn }}</h2>

          <div class="cover-meta">
            <div class="cover-row" v-if="coverInfo.author"><label>作　者</label><span>{{ coverInfo.author }}</span></div>
            <div class="cover-row" v-if="coverInfo.studentId"><label>学　号</label><span>{{ coverInfo.studentId }}</span></div>
            <div class="cover-row" v-if="coverInfo.college"><label>学　院</label><span>{{ coverInfo.college }}</span></div>
            <div class="cover-row" v-if="coverInfo.major"><label>专　业</label><span>{{ coverInfo.major }}</span></div>
            <div class="cover-row" v-if="coverInfo.advisor"><label>导　师</label><span>{{ coverInfo.advisor }} {{ coverInfo.advisorTitle || '' }}</span></div>
            <div class="cover-row" v-if="coverInfo.date"><label>日　期</label><span>{{ coverInfo.date }}</span></div>
          </div>
        </div>

        <!-- ========== 中文摘要 ========== -->
        <div class="section-page page-break" v-if="abstractData.abstractCn">
          <h2 class="section-heading">摘　要</h2>
          <div class="section-text" v-html="abstractData.abstractCn" />
          <div class="keywords" v-if="abstractData.keywordsCn?.length">
            <strong>关键词：</strong>{{ (abstractData.keywordsCn || []).join('；') }}
          </div>
        </div>

        <!-- ========== 英文摘要 ========== -->
        <div class="section-page page-break" v-if="abstractData.abstractEn">
          <h2 class="section-heading">Abstract</h2>
          <div class="section-text" v-html="abstractData.abstractEn" />
          <div class="keywords" v-if="abstractData.keywordsEn?.length">
            <strong>Keywords: </strong>{{ (abstractData.keywordsEn || []).join('; ') }}
          </div>
        </div>

        <!-- ========== 目录 ========== -->
        <div class="section-page page-break" v-if="tocChapters.length > 0">
          <h2 class="section-heading">目　录</h2>
          <div class="toc-list">
            <div
              v-for="ch in tocChapters"
              :key="ch.id"
              class="toc-item"
              :style="{ paddingLeft: `${(ch.depth - 1) * 20}px` }"
            >
              <span class="toc-title">{{ ch.title }}</span>
              <span class="toc-dots" />
            </div>
          </div>
        </div>

        <!-- ========== 各章节正文 ========== -->
        <template v-for="ch in tocChapters" :key="ch.id">
          <div class="section-page" :class="{ 'page-break': ch.depth === 1 }">
            <component :is="`h${Math.min(ch.level || 1, 4)}`" class="chapter-head">
              {{ ch.title }}
            </component>
            <div class="section-text" v-html="ch.content" />
          </div>
        </template>

        <!-- ========== 参考文献 ========== -->
        <div class="section-page page-break" v-if="references.length > 0">
          <h2 class="section-heading">参考文献</h2>
          <ol class="ref-list">
            <li v-for="(ref, i) in references" :key="ref.id || i" class="ref-entry">
              <span v-if="ref.authors">{{ ref.authors }}. </span>
              <span v-if="ref.title">《{{ ref.title }}》[J]. </span>
              <span v-if="ref.journal">{{ ref.journal }}, </span>
              <span v-if="ref.year">{{ ref.year }}</span>
              <span v-if="ref.volume">, {{ ref.volume }}</span>
              <span v-if="ref.issue">({{ ref.issue }})</span>
              <span v-if="ref.pages">: {{ ref.pages }}</span>
              <span>.</span>
            </li>
          </ol>
        </div>

        <!-- ========== 致谢 ========== -->
        <div class="section-page page-break" v-if="acknowledgment">
          <h2 class="section-heading">致　谢</h2>
          <div class="section-text" v-html="acknowledgment" />
        </div>

      </div>

      <!-- 空状态 -->
      <div v-if="!paper" class="preview-empty">
        <el-empty description="论文数据加载失败" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 整体 ===== */
.preview-shell {
  display: flex; flex-direction: column;
  height: 100vh; background: #e8e7e2;
}

.preview-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  height: 50px; padding: 0 18px;
  background: #fff; border-bottom: 1px solid #d8d7d2;
  flex-shrink: 0; z-index: 5;
  box-shadow: 0 1px 3px rgba(0,0,0,.05);
}
.pt-left { display: flex; gap: 8px; }
.pt-right { display: flex; align-items: center; gap: 10px; }
.pt-title { font-weight: 600; font-size: 15px; color: #242622; max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pt-status { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: #EDF0EC; color: #5C605A; }

.preview-body { flex: 1; overflow-y: auto; padding: 20px 0 40px; }
.preview-body::-webkit-scrollbar { width: 8px; }
.preview-body::-webkit-scrollbar-thumb { background: #c0beb8; border-radius: 999px; }

.paper-a4 {
  width: 794px; /* A4 宽度 */
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,.08);
}

/* ===== 封面 ===== */
.cover-page {
  padding: 80px 70px 60px; text-align: center;
  min-height: 1123px; /* A4 高度 */
}
.cover-school {
  font-size: 22px; font-weight: 600; color: #242622;
  margin-bottom: 50px; letter-spacing: 0.1em;
}
.cover-title-main {
  font-family: "Noto Serif SC", "Microsoft YaHei", serif;
  font-size: 30px; font-weight: 700; color: #1a1a1a;
  margin: 0 0 16px; line-height: 1.4;
}
.cover-title-sub {
  font-family: "Noto Serif SC", serif;
  font-size: 18px; font-weight: 400; color: #555;
  margin: 0 0 50px; font-style: italic;
}
.cover-meta {
  display: inline-block; text-align: left; margin-top: 30px;
}
.cover-row {
  display: flex; gap: 16px; padding: 10px 0;
  font-size: 16px; border-bottom: 1px solid #eee;
}
.cover-row label { width: 70px; color: #666; font-weight: 500; flex-shrink: 0; }
.cover-row span { color: #242622; }

/* ===== 章节页 ===== */
.section-page {
  padding: 50px 70px 40px;
}
.page-break {
  border-top: 1px dashed #ddd;
  min-height: 600px;
}
.section-heading {
  font-family: "Noto Serif SC", serif;
  font-size: 22px; font-weight: 700; text-align: center;
  margin: 0 0 30px; color: #1a1a1a; letter-spacing: 0.15em;
}
.section-text {
  font-size: 15px; line-height: 1.9; color: #333; text-align: justify;
}
.section-text :deep(p) { text-indent: 2em; margin: 0.6em 0; }
.section-text :deep(h1), .section-text :deep(h2),
.section-text :deep(h3), .section-text :deep(h4) {
  font-family: "Noto Serif SC", serif; color: #1a1a1a;
  text-indent: 0; margin: 1em 0 0.4em;
}
.section-text :deep(img) { max-width: 100%; display: block; margin: 10px auto; }
.section-text :deep(table) { border-collapse: collapse; width: 100%; margin: 0.8em 0; }
.section-text :deep(th), .section-text :deep(td) { border: 1px solid #ccc; padding: 6px 10px; }
.section-text :deep(th) { background: #f7f7f5; font-weight: 600; }

.chapter-head {
  font-family: "Noto Serif SC", serif;
  color: #1a1a1a; margin: 0 0 18px;
}
h1.chapter-head { font-size: 20px; text-align: center; }
h2.chapter-head { font-size: 18px; }
h3.chapter-head { font-size: 16px; }
h4.chapter-head { font-size: 15px; }

.keywords { margin-top: 16px; font-size: 14px; color: #555; line-height: 1.6; }
.keywords strong { color: #242622; }

/* ===== 目录 ===== */
.toc-list { margin-top: 16px; }
.toc-item {
  display: flex; align-items: baseline; padding: 5px 0;
  font-size: 15px; color: #333; cursor: default;
}
.toc-title { flex-shrink: 0; }
.toc-dots { flex: 1; border-bottom: 1px dotted #ccc; margin: 0 6px; min-width: 20px; }

/* ===== 参考文献 ===== */
.ref-list { padding-left: 1.6em; }
.ref-entry { font-size: 13px; line-height: 1.8; color: #444; margin: 4px 0; }

.preview-empty { display: flex; justify-content: center; padding: 100px 0; }
</style>
