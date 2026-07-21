<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const emit = defineEmits(['back'])
const router = useRouter()
const papers = ref([])
const selected = ref(null)
const comments = ref([])
const versions = ref([])
const loading = ref(false)
const error = ref('')

const statusLabels = {
  DRAFT: '草稿',
  SUBMITTED: '已提交，等待教师批阅',
  REVIEWING: '教师批阅中',
  RETURNED: '已驳回，可修改后重新提交',
  APPROVED: '审核通过，论文已锁定'
}

async function api(method, url, data, params) {
  const res = await request({ method, url, data, params })
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data
}

async function loadPapers() {
  loading.value = true
  error.value = ''
  try {
    const data = await api('get', '/api/papers', null, { page: 0, size: 100 })
    papers.value = data.content || []
    const firstReviewed = papers.value.find(paper => paper.status !== 'DRAFT')
    if (firstReviewed && !selected.value) await choosePaper(firstReviewed)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function choosePaper(paper) {
  selected.value = paper
  comments.value = []
  versions.value = []
  error.value = ''
  try {
    ;[comments.value, versions.value] = await Promise.all([
      api('get', `/api/reviews/${paper.id}/comments`),
      api('get', `/api/reviews/${paper.id}/versions`)
    ])
  } catch (e) {
    error.value = e.message
  }
}

function editReturnedPaper() {
  if (selected.value?.status !== 'RETURNED') return
  emit('back')
  router.push({ name: 'EditPaper', params: { id: selected.value.id } })
}

const reviewedPapers = computed(() => papers.value.filter(paper => paper.status !== 'DRAFT'))
function fmtDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }

onMounted(loadPapers)
</script>

<template>
  <section class="feedback-page">
    <aside class="feedback-list">
      <div class="feedback-head">
        <div><small>TEACHER FEEDBACK</small><h2>论文与批阅反馈</h2></div>
        <button @click="loadPapers">刷新</button>
      </div>
      <div v-if="loading" class="empty">正在读取…</div>
      <div v-else-if="!reviewedPapers.length" class="empty">尚无已提交论文</div>
      <button
        v-for="paper in reviewedPapers"
        :key="paper.id"
        class="paper-item"
        :class="{ active: selected?.id === paper.id }"
        @click="choosePaper(paper)"
      >
        <b>{{ paper.title }}</b>
        <span>{{ statusLabels[paper.status] || paper.status }}</span>
        <small>V{{ paper.currentVersion || 1 }} · {{ fmtDate(paper.updatedAt) }}</small>
      </button>
    </aside>

    <main class="feedback-detail">
      <div v-if="!selected" class="empty detail-empty">请选择一篇已提交论文</div>
      <template v-else>
        <article class="result-card" :class="selected.status.toLowerCase()">
          <div><small>当前审核结果</small><h2>{{ statusLabels[selected.status] || selected.status }}</h2></div>
          <div class="score"><strong>{{ selected.score ?? '—' }}</strong><span>分 · {{ selected.grade || '—' }}</span></div>
          <button v-if="selected.status === 'RETURNED'" class="edit-button" @click="editReturnedPaper">返回修改论文</button>
        </article>

        <article class="detail-card">
          <h3>教师总评 / 修改意见</h3>
          <p class="summary">{{ selected.teacherSummary || '教师尚未填写总评' }}</p>
        </article>

        <article class="detail-card">
          <h3>对应位置批注</h3>
          <div v-if="!comments.length" class="empty">暂无批注</div>
          <div v-for="comment in comments" :key="comment.id" class="comment">
            <b>{{ comment.anchorText || comment.sectionKey || '论文正文' }}</b>
            <p>{{ comment.content }}</p>
            <small>V{{ comment.paperVersion }} · {{ fmtDate(comment.createdAt) }}</small>
          </div>
        </article>

        <article class="detail-card">
          <h3>版本历史记录</h3>
          <div v-if="!versions.length" class="empty">暂无版本记录</div>
          <div v-for="version in versions" :key="version.id" class="version">
            <i>V{{ version.versionNo }}</i>
            <div><b>{{ version.description || '论文版本' }}</b><small>{{ fmtDate(version.createdAt) }}</small></div>
          </div>
        </article>
      </template>
      <div v-if="error" class="error">{{ error }}</div>
    </main>
  </section>
</template>

<style scoped>
.feedback-page{display:grid;grid-template-columns:330px minmax(0,1fr);gap:18px;max-width:1250px;margin:auto;padding:24px 30px}.feedback-list,.detail-card,.result-card{background:#fff;border:1px solid #e4e3de;border-radius:13px}.feedback-list{padding:16px;height:max-content}.feedback-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.feedback-head h2{margin:4px 0}.paper-item{display:grid;width:100%;text-align:left;padding:13px;margin:7px 0;border:1px solid transparent;background:#f7f7f4}.paper-item.active{border-color:#86a89b;background:#edf4f1}.paper-item span,.paper-item small{color:#757a74;margin-top:5px}.feedback-detail{display:grid;gap:14px}.result-card{display:flex;align-items:center;gap:24px;padding:20px 24px;border-left:5px solid #789589}.result-card.returned{border-left-color:#b8893e}.result-card.approved{border-left-color:#386858}.result-card h2{margin:4px 0}.score{margin-left:auto;display:grid;text-align:right}.score strong{font-size:30px}.edit-button{background:#4f776a;color:#fff;border:0;padding:10px 15px}.detail-card{padding:20px 24px}.detail-card h3{margin-top:0}.summary{line-height:1.8;white-space:pre-wrap}.comment{border-left:3px solid #4f776a;background:#f5f6f3;padding:12px;margin:9px 0}.comment p{margin:7px 0;line-height:1.7}.comment small,.version small{color:#858982}.version{display:flex;gap:12px;padding:12px 0;border-top:1px solid #eee}.version i{font-style:normal;font-weight:700;color:#4f776a}.version div{display:grid;gap:4px}.empty{padding:28px;text-align:center;color:#858982}.detail-empty{background:#fff;border:1px solid #e4e3de;border-radius:13px}.error{position:fixed;right:24px;bottom:24px;background:#a8544d;color:#fff;padding:11px 15px;border-radius:8px}@media(max-width:900px){.feedback-page{grid-template-columns:1fr}.result-card{align-items:flex-start;flex-wrap:wrap}.score{margin-left:0}}
</style>
