<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import request from '../api/request'

const props = defineProps({ page: { type: String, default: 'students' } })
const emit = defineEmits(['navigate'])
const students = ref([]), papers = ref([]), selected = ref(null), comments = ref([]), versions = ref([])
const studentForm = reactive({ name: '', studentNo: '' })
const reviewForm = reactive({ score: '', summary: '' })
const commentText = ref(''), selectedSection = ref(null), loading = ref(false), error = ref(''), notice = ref('')
const statusLabels = { SUBMITTED: '待批阅', REVIEWING: '批阅中', RETURNED: '已驳回', APPROVED: '审核通过' }

async function api(method, url, data) {
  const res = await request({ method, url, data })
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data
}
function flash(text) { notice.value = text; setTimeout(() => notice.value = '', 2300) }
function plainText(html) { return String(html || '').replace(/<style[\s\S]*?<\/style>/gi, ' ').replace(/<script[\s\S]*?<\/script>/gi, ' ').replace(/<[^>]+>/g, ' ').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/\s+/g, ' ').trim() }
const paperSections = computed(() => {
  const raw = selected.value?.paper?.content
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map((section, index) => ({ key: String(section.id || `section-${index + 1}`), title: section.title || `第${index + 1}部分`, content: plainText(section.content) })).filter(section => section.content || section.title)
  } catch {}
  return [{ key: 'body', title: '论文正文', content: plainText(raw) }]
})
const canReview = computed(() => ['SUBMITTED', 'REVIEWING'].includes(selected.value?.paper?.status))

async function loadStudents() { loading.value = true; error.value = ''; try { students.value = await api('get', '/api/teacher/students') } catch (e) { error.value = e.message } finally { loading.value = false } }
async function loadPapers() { loading.value = true; error.value = ''; try { papers.value = await api('get', '/api/teacher/papers') } catch (e) { error.value = e.message } finally { loading.value = false } }
async function addStudent() {
  if (!studentForm.name.trim() || !studentForm.studentNo.trim()) return error.value = '请填写学生姓名和学号'
  try { await api('post', '/api/teacher/students', studentForm); Object.assign(studentForm, { name: '', studentNo: '' }); await loadStudents(); flash('学生账号创建成功，初始密码为 123456') } catch (e) { error.value = e.message }
}
async function openPaper(item) {
  try {
    selected.value = await api('get', `/api/teacher/papers/${item.paper.id}`)
    reviewForm.score = selected.value.paper.score ?? ''; reviewForm.summary = selected.value.paper.teacherSummary || ''
    selectedSection.value = null; await loadReview(); emit('navigate', 'review')
  } catch (e) { error.value = e.message }
}
async function loadReview() {
  if (!selected.value) return
  const id = selected.value.paper.id
  ;[comments.value, versions.value] = await Promise.all([api('get', `/api/reviews/${id}/comments`), api('get', `/api/reviews/${id}/versions`)])
}
async function addComment() {
  if (!selectedSection.value) return error.value = '请先点击原文中的对应章节'
  if (!commentText.value.trim()) return error.value = '请输入批注内容'
  try { await api('post', `/api/reviews/${selected.value.paper.id}/comments`, { sectionKey: selectedSection.value.key, anchorText: selectedSection.value.title, content: commentText.value.trim() }); commentText.value = ''; selected.value.paper.status = 'REVIEWING'; await loadReview(); flash('批注已保存') } catch (e) { error.value = e.message }
}
async function finish(action) {
  if (!reviewForm.summary.trim()) return error.value = action === 'return' ? '驳回时必须填写修改意见' : '请填写审核评语'
  if (action === 'approve' && reviewForm.score === '') return error.value = '审核通过前请填写分数'
  try { selected.value.paper = await api('post', `/api/reviews/${selected.value.paper.id}/${action}`, { score: reviewForm.score === '' ? null : Number(reviewForm.score), summary: reviewForm.summary.trim() }); await loadPapers(); flash(action === 'return' ? '论文已驳回，学生端已解锁' : '论文审核通过并锁定') } catch (e) { error.value = e.message }
}

watch(() => props.page, page => { if (page === 'students') loadStudents(); if (page === 'papers') loadPapers() })
onMounted(() => Promise.all([loadStudents(), loadPapers()]))
</script>

<template>
  <section v-if="page==='students'" class="teacher-workspace"><div class="student-grid"><article class="tw-card create-card"><small>教师 · 学生绑定</small><h2>添加名下学生</h2><p>账号用户名为学号，初始密码统一为 123456。</p><label>学生姓名<input v-model="studentForm.name" placeholder="请输入学生姓名"></label><label>学生学号<input v-model="studentForm.studentNo" placeholder="作为学生登录账号"></label><button class="primary full" @click="addStudent">创建并绑定学生</button></article><article class="tw-card"><div class="tw-head"><div><h2>我的学生</h2><p>仅展示当前教师创建的学生</p></div><button @click="loadStudents">刷新</button></div><div v-if="loading" class="tw-empty">正在加载…</div><div v-else-if="!students.length" class="tw-empty">尚未添加学生</div><div v-else class="student-list"><div class="student-row row-head"><span>姓名</span><span>学号</span><span>账号状态</span><span>已提交论文</span></div><div v-for="student in students" :key="student.id" class="student-row"><b>{{student.name}}</b><span>{{student.studentNo}}</span><span>{{student.status===1?'正常':'停用'}}</span><strong>{{student.submittedPaperCount}} 篇</strong></div></div></article></div></section>

  <section v-else-if="page==='papers'" class="teacher-workspace"><article class="tw-card"><div class="tw-head"><div><small>DATABASE PAPERS</small><h2>学生真实提交论文</h2><p>仅展示当前教师名下学生已经提交到数据库的论文</p></div><button @click="loadPapers">刷新数据</button></div><div v-if="loading" class="tw-empty">正在从数据库读取…</div><div v-else-if="!papers.length" class="tw-empty"><b>暂无真实提交论文</b><span>学生提交后会自动出现在这里，本页不含虚拟模板论文。</span></div><div v-else class="paper-list"><div class="paper-row row-head"><span>论文与学生</span><span>版本</span><span>状态</span><span>分数</span><span>操作</span></div><div v-for="item in papers" :key="item.paper.id" class="paper-row"><div><b>{{item.paper.title}}</b><small>{{item.studentName}} · {{item.studentNo}}</small></div><span>V{{item.paper.currentVersion}}</span><span><i class="status" :class="item.paper.status.toLowerCase()">{{statusLabels[item.paper.status]||item.paper.status}}</i></span><span>{{item.paper.score??'—'}}</span><button class="primary" @click="openPaper(item)">{{['SUBMITTED','REVIEWING'].includes(item.paper.status)?'进入批阅':'查看记录'}}</button></div></div></article></section>

  <section v-else class="teacher-workspace"><div v-if="!selected" class="tw-card tw-empty"><b>请先从“论文管理”选择一篇真实论文</b><button class="primary" @click="$emit('navigate','papers')">前往论文管理</button></div><div v-else class="review-grid"><article class="tw-card original"><div class="paper-meta"><span>{{selected.studentName}} · {{selected.studentNo}} · V{{selected.paper.currentVersion}}</span><i class="status" :class="selected.paper.status.toLowerCase()">{{statusLabels[selected.paper.status]}}</i></div><h2>{{selected.paper.title}}</h2><div v-if="!paperSections.length" class="tw-empty">论文正文为空</div><section v-for="section in paperSections" :key="section.key" class="paper-section" :class="{active:selectedSection?.key===section.key}" @click="selectedSection=section"><button>{{comments.filter(comment=>comment.sectionKey===section.key).length||'+'}}</button><h3>{{section.title}}</h3><p>{{section.content||'（本节暂无文字内容）'}}</p></section></article><aside class="tools-column"><article class="tw-card"><h3>定位批注</h3><small>当前位置：{{selectedSection?.title||'请点击左侧章节'}}</small><textarea v-model="commentText" :disabled="!canReview" placeholder="输入对应位置的批注"></textarea><button class="primary full" :disabled="!canReview" @click="addComment">保存批注</button><div class="comment" v-for="comment in comments" :key="comment.id"><b>{{comment.anchorText||comment.sectionKey}} · V{{comment.paperVersion}}</b><p>{{comment.content}}</p></div></article><article class="tw-card"><h3>评分与审核</h3><label>分数（0-100）<input v-model="reviewForm.score" type="number" min="0" max="100" :disabled="!canReview"></label><label>教师评语 / 修改意见<textarea v-model="reviewForm.summary" :disabled="!canReview"></textarea></label><div v-if="canReview" class="review-buttons"><button class="return" @click="finish('return')">驳回并解锁</button><button class="primary" @click="finish('approve')">审核通过</button></div><div v-else class="review-result"><b>{{statusLabels[selected.paper.status]}}</b><p>{{selected.paper.teacherSummary||'暂无评语'}}</p><span>{{selected.paper.score??'—'}} 分 · {{selected.paper.grade||'—'}}</span></div></article><article class="tw-card"><h3>版本历史</h3><div v-for="version in versions" :key="version.id" class="version"><b>V{{version.versionNo}} · {{version.description}}</b><small>{{version.createdAt}}</small></div></article></aside></div></section>
  <div v-if="notice" class="tw-toast">{{notice}}</div><div v-if="error" class="tw-error">{{error}}</div>
</template>

<style scoped>
.teacher-workspace{padding:24px 30px;max-width:1450px;margin:auto}.student-grid{display:grid;grid-template-columns:340px 1fr;gap:18px}.tw-card{background:#fff;border:1px solid #e4e3de;border-radius:13px;padding:20px}.tw-card h2,.tw-card h3{margin:5px 0}.tw-card p{color:#666}.tw-head,.paper-meta{display:flex;align-items:center;justify-content:space-between;gap:18px}.create-card label,.tools-column label{display:block;margin:15px 0;font-size:12px;font-weight:600;color:#60645f}.create-card input,.tools-column input,.tools-column textarea{width:100%;margin-top:6px}.tools-column textarea{min-height:85px}.full{width:100%}.tw-empty{display:grid;place-items:center;gap:10px;padding:50px;color:#858982}.student-row{display:grid;grid-template-columns:1.1fr 1.2fr .8fr .8fr;gap:12px;padding:14px;border-bottom:1px solid #eee}.paper-row{display:grid;grid-template-columns:minmax(260px,1.5fr) .45fr .7fr .45fr .65fr;gap:12px;padding:14px;align-items:center;border-bottom:1px solid #eee}.row-head{background:#f6f6f3;color:#888;font-size:11px;font-weight:700}.paper-row small{display:block;color:#888;margin-top:4px}.status{font-style:normal;padding:5px 9px;border-radius:999px;background:#e7eeea;color:#456b5f;font-size:11px}.status.returned{background:#f5e8d8;color:#996127}.status.approved{background:#e3eee9;color:#386858}.review-grid{display:grid;grid-template-columns:minmax(580px,1fr) 365px;gap:18px}.original{padding:35px 50px}.original>h2{text-align:center;font-family:serif;margin:30px}.paper-section{position:relative;padding:10px 14px;border-radius:8px}.paper-section.active{background:#eff5f2;outline:2px solid #b9d0c7}.paper-section>button{position:absolute;right:-35px;width:28px;height:28px;border-radius:50%;padding:0}.paper-section p{line-height:1.9;text-align:justify}.tools-column{display:block;background:transparent;border:0;padding:0}.tools-column .tw-card{margin-bottom:14px}.comment{border-left:3px solid #4f776a;background:#f4f6f3;padding:9px;margin-top:9px}.comment p{margin:4px 0}.review-buttons{display:grid;grid-template-columns:1fr 1fr;gap:8px}.return{color:#a8544d;border-color:#d3a5a0}.review-result{background:#f2f5f2;padding:12px;border-radius:8px}.version{display:grid;padding:9px 0;border-top:1px solid #eee}.version small{color:#888}.tw-toast,.tw-error{position:fixed;right:24px;bottom:24px;color:#fff;padding:11px 15px;border-radius:8px;z-index:30}.tw-toast{background:#315f51}.tw-error{background:#a8544d}@media(max-width:1050px){.student-grid,.review-grid{grid-template-columns:1fr}.paper-row{grid-template-columns:1fr}.paper-section>button{right:0}}
</style>
