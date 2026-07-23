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
function parseSectionData(content, fallback) {
  if (content && typeof content === 'object') return content
  try { return JSON.parse(content) } catch { return fallback }
}
function referenceText(reference) {
  if (reference.formattedText) return reference.formattedText
  return [reference.authors || reference.author, reference.title, reference.journal, reference.year]
    .filter(Boolean).join(' / ') || '未命名文献'
}
const paperSections = computed(() => {
  const raw = selected.value?.paper?.content
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map((section, index) => {
      const type = section.type || 'chapter'
      const item = {
        key: String(section.id || `section-${index + 1}`),
        title: section.title || `第${index + 1}部分`,
        type,
        level: Number(section.level) || 1,
        content: section.content || ''
      }
      if (type === 'cover' || type === 'abstract') {
        const data = parseSectionData(section.content, {})
        item.data = data && typeof data === 'object' && !Array.isArray(data) ? data : {}
      }
      if (type === 'references') {
        const references = parseSectionData(section.content, [])
        item.data = Array.isArray(references) ? references : []
      }
      return item
    }).filter(section => section.content || section.title)
  } catch {}
  return [{ key: 'body', title: '论文正文', type: 'chapter', level: 1, content: raw }]
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
  <section v-if="page==='students'" class="teacher-workspace"><div class="student-grid"><article class="tw-card create-card"><small>教师 · 学生绑定</small><h2>添加名下学生</h2><p>账号用户名为学号，初始密码统一为 123456。</p><label>学生姓名<input v-model="studentForm.name" placeholder="请输入学生姓名"></label><label>学生学号<input v-model="studentForm.studentNo" placeholder="作为学生登录账号"></label><button class="primary full" @click="addStudent">创建并绑定学生</button></article><article class="tw-card"><div class="tw-head"><div><h2>我的学生<p>  </p></h2></div><button @click="loadStudents">刷新</button></div><div v-if="loading" class="tw-empty">正在加载…</div><div v-else-if="!students.length" class="tw-empty">尚未添加学生</div><div v-else class="student-list"><div class="student-row row-head"><span>姓名</span><span>学号</span><span>账号状态</span><span>已提交论文</span></div><div v-for="student in students" :key="student.id" class="student-row"><b>{{student.name}}</b><span>{{student.studentNo}}</span><span>{{student.status===1?'正常':'停用'}}</span><strong>{{student.submittedPaperCount}} 篇</strong></div></div></article></div></section>

  <section v-else-if="page==='papers'" class="teacher-workspace"><article class="tw-card"><div class="tw-head"><div><small>DATABASE PAPERS</small><h2>学生提交论文</h2><p>   </p></div><button @click="loadPapers">刷新数据</button></div><div v-if="loading" class="tw-empty">正在从数据库读取…</div><div v-else-if="!papers.length" class="tw-empty"><b>暂无提交论文</b><span>学生提交后会自动出现在这里。</span></div><div v-else class="paper-list"><div class="paper-row row-head"><span>论文与学生</span><span>版本</span><span>状态</span><span>分数</span><span>操作</span></div><div v-for="item in papers" :key="item.paper.id" class="paper-row"><div><b>{{item.paper.title}}</b><small>{{item.studentName}} · {{item.studentNo}}</small></div><span>V{{item.paper.currentVersion}}</span><span><i class="status" :class="item.paper.status.toLowerCase()">{{statusLabels[item.paper.status]||item.paper.status}}</i></span><span>{{item.paper.score??'—'}}</span><button class="primary" @click="openPaper(item)">{{['SUBMITTED','REVIEWING'].includes(item.paper.status)?'进入批阅':'查看记录'}}</button></div></div></article></section>

  <section v-else class="teacher-workspace">
    <div v-if="!selected" class="tw-card tw-empty">
      <b>请先从“论文管理”选择一篇论文</b>
      <button class="primary" @click="$emit('navigate','papers')">前往论文管理</button>
    </div>
    <div v-else class="review-grid">
      <article class="tw-card original">
        <div class="paper-meta">
          <span>{{selected.studentName}} · {{selected.studentNo}} · V{{selected.paper.currentVersion}}</span>
          <i class="status" :class="selected.paper.status.toLowerCase()">{{statusLabels[selected.paper.status]}}</i>
        </div>
        <div v-if="!paperSections.length" class="tw-empty">论文正文为空</div>
        <div v-else class="paper-sheet">
          <section
            v-for="section in paperSections"
            :key="section.key"
            class="paper-section"
            :class="[{active:selectedSection?.key===section.key}, section.type==='cover'?'cover-page':'page-section']"
            @click="selectedSection=section"
          >
            <button class="comment-anchor" type="button" @click.stop="selectedSection=section">
              {{comments.filter(comment=>comment.sectionKey===section.key).length||'+'}}
            </button>

            <template v-if="section.type==='cover'">
              <div v-if="section.data.college" class="cover-school">{{section.data.college}}</div>
              <h1 class="cover-title">{{section.data.titleCn||selected.paper.title||'论文题目'}}</h1>
              <h2 v-if="section.data.titleEn" class="cover-title-sub">{{section.data.titleEn}}</h2>
              <div class="cover-meta">
                <div v-if="section.data.author||selected.studentName" class="cover-row">
                  <label>姓　　名</label><span>{{section.data.author||selected.studentName}}</span>
                </div>
                <div v-if="section.data.studentId||selected.studentNo" class="cover-row">
                  <label>学　　号</label><span>{{section.data.studentId||selected.studentNo}}</span>
                </div>
                <div v-if="section.data.college" class="cover-row">
                  <label>学　　院</label><span>{{section.data.college}}</span>
                </div>
                <div v-if="section.data.major" class="cover-row">
                  <label>专　　业</label><span>{{section.data.major}}</span>
                </div>
                <div v-if="section.data.advisor" class="cover-row">
                  <label>指导教师</label><span>{{section.data.advisor}} {{section.data.advisorTitle||''}}</span>
                </div>
                <div v-if="section.data.date" class="cover-row">
                  <label>日　　期</label><span>{{section.data.date}}</span>
                </div>
              </div>
            </template>

            <template v-else-if="section.type==='abstract'">
              <template v-if="section.data.abstractCn">
                <h2 class="section-heading">摘　要</h2>
                <div class="section-text" v-html="section.data.abstractCn"></div>
                <div v-if="section.data.keywordsCn?.length" class="keywords"><strong>关键词：</strong>{{section.data.keywordsCn.join('；')}}</div>
              </template>
              <template v-if="section.data.abstractEn">
                <h2 class="section-heading abstract-en-heading">Abstract</h2>
                <div class="section-text" v-html="section.data.abstractEn"></div>
                <div v-if="section.data.keywordsEn?.length" class="keywords"><strong>Keywords: </strong>{{section.data.keywordsEn.join('; ')}}</div>
              </template>
              <p v-if="!section.data.abstractCn&&!section.data.abstractEn" class="empty-section">（本节暂无文字内容）</p>
            </template>

            <template v-else-if="section.type==='references'">
              <h2 class="section-heading">参考文献</h2>
              <div v-if="section.data.length" class="ref-list">
                <div v-for="(reference,index) in section.data" :key="reference.id||index" class="ref-entry">
                  <span>[{{index+1}}]</span><span>{{referenceText(reference)}}</span>
                </div>
              </div>
              <p v-else class="empty-section">（本节暂无参考文献）</p>
            </template>

            <template v-else-if="section.type==='acknowledgment'">
              <h2 class="section-heading">致　谢</h2>
              <div v-if="section.content" class="section-text" v-html="section.content"></div>
              <p v-else class="empty-section">（本节暂无文字内容）</p>
            </template>

            <template v-else>
              <component :is="'h'+Math.min(section.level,4)" class="chapter-head">{{section.title}}</component>
              <div v-if="section.content" class="section-text" v-html="section.content"></div>
              <p v-else class="empty-section">（本节暂无文字内容）</p>
            </template>
          </section>
        </div>
      </article>
      <aside class="tools-column">
        <article class="tw-card"><h3>定位批注</h3><small>当前位置：{{selectedSection?.title||'请点击左侧章节'}}</small><textarea v-model="commentText" :disabled="!canReview" placeholder="输入对应位置的批注"></textarea><button class="primary full" :disabled="!canReview" @click="addComment">保存批注</button><div class="comment" v-for="comment in comments" :key="comment.id"><b>{{comment.anchorText||comment.sectionKey}} · V{{comment.paperVersion}}</b><p>{{comment.content}}</p></div></article>
        <article class="tw-card"><h3>评分与审核</h3><label>分数（0-100）<input v-model="reviewForm.score" type="number" min="0" max="100" :disabled="!canReview"></label><label>教师评语 / 修改意见<textarea v-model="reviewForm.summary" :disabled="!canReview"></textarea></label><div v-if="canReview" class="review-buttons"><button class="return" @click="finish('return')">驳回并解锁</button><button class="primary" @click="finish('approve')">审核通过</button></div><div v-else class="review-result"><b>{{statusLabels[selected.paper.status]}}</b><p>{{selected.paper.teacherSummary||'暂无评语'}}</p><span>{{selected.paper.score??'—'}} 分 · {{selected.paper.grade||'—'}}</span></div></article>
        <article class="tw-card"><h3>版本历史</h3><div v-for="version in versions" :key="version.id" class="version"><b>V{{version.versionNo}} · {{version.description}}</b><small>{{version.createdAt}}</small></div></article>
      </aside>
    </div>
  </section>
  <div v-if="notice" class="tw-toast">{{notice}}</div><div v-if="error" class="tw-error">{{error}}</div>
</template>

<style scoped>
.teacher-workspace{padding:24px 30px;max-width:1450px;margin:auto}.student-grid{display:grid;grid-template-columns:340px 1fr;gap:18px}.tw-card{background:#fff;border:1px solid #e4e3de;border-radius:13px;padding:20px}.tw-card h2,.tw-card h3{margin:5px 0}.tw-card p{color:#666}.tw-head,.paper-meta{display:flex;align-items:center;justify-content:space-between;gap:18px}.create-card label,.tools-column label{display:block;margin:15px 0;font-size:12px;font-weight:600;color:#60645f}.create-card input,.tools-column input,.tools-column textarea{width:100%;margin-top:6px}.tools-column textarea{min-height:85px}.full{width:100%}.tw-empty{display:grid;place-items:center;gap:10px;padding:50px;color:#858982}.student-row{display:grid;grid-template-columns:1.1fr 1.2fr .8fr .8fr;gap:12px;padding:14px;border-bottom:1px solid #eee}.paper-row{display:grid;grid-template-columns:minmax(260px,1.5fr) .45fr .7fr .45fr .65fr;gap:12px;padding:14px;align-items:center;border-bottom:1px solid #eee}.row-head{background:#f6f6f3;color:#888;font-size:11px;font-weight:700}.paper-row small{display:block;color:#888;margin-top:4px}.status{font-style:normal;padding:5px 9px;border-radius:999px;background:#e7eeea;color:#456b5f;font-size:11px}.status.returned{background:#f5e8d8;color:#996127}.status.approved{background:#e3eee9;color:#386858}.review-grid{display:grid;grid-template-columns:minmax(580px,1fr) 365px;gap:18px}.original{padding:18px;background:#efefec;overflow-x:auto}.paper-meta{position:sticky;left:0;padding:2px 2px 16px}.paper-sheet{width:794px;max-width:100%;margin:0 auto;background:#fff;box-shadow:0 2px 12px #00000018}.paper-section{position:relative;box-sizing:border-box;padding:50px 70px 40px;color:#242622}.paper-section+.paper-section{border-top:1px dashed #ddd}.paper-section.active{background:#eff5f2;outline:2px solid #7f9f94;outline-offset:-2px}.paper-section>.comment-anchor{position:absolute;z-index:2;top:16px;right:16px;width:30px;height:30px;border-radius:50%;padding:0;background:#fff;box-shadow:0 1px 5px #0002}.cover-page{text-align:center;min-height:1123px;padding:80px 70px 60px}.cover-school{font-size:22px;font-weight:600;margin-bottom:50px;letter-spacing:.1em}.cover-title{font-family:"Noto Serif SC","Microsoft YaHei",serif;font-size:30px;font-weight:700;color:#1a1a1a;margin:0 0 16px;line-height:1.4}.cover-title-sub{font-family:Georgia,serif;font-size:18px;font-weight:400;color:#555;margin:0 0 50px;font-style:italic}.cover-meta{display:inline-block;text-align:left;margin-top:30px}.cover-row{display:flex;gap:16px;padding:10px 0;font-size:12pt;border-bottom:1px solid #eee}.cover-row label{width:80px;color:#666;font-weight:500;flex-shrink:0}.cover-row span{color:#242622}.section-heading{text-align:center;font-family:"Noto Serif SC","Microsoft YaHei",serif;font-size:22px;margin:0 0 28px}.abstract-en-heading{margin-top:56px}.chapter-head{font-family:"Noto Serif SC","Microsoft YaHei",serif;color:#1a1a1a;margin:0 0 22px;line-height:1.45}.section-text{font-family:SimSun,"宋体",serif;font-size:12pt;line-height:1.9;text-align:justify}.section-text:deep(p){margin:0 0 1em}.section-text:deep(img){max-width:100%;height:auto}.section-text:deep(table){width:100%;border-collapse:collapse}.section-text:deep(td),.section-text:deep(th){border:1px solid #bbb;padding:6px}.keywords{margin-top:22px;font-family:SimSun,"宋体",serif;font-size:12pt;line-height:1.8}.ref-list{display:grid;gap:12px;font-family:SimSun,"宋体",serif;font-size:12pt;line-height:1.7}.ref-entry{display:grid;grid-template-columns:42px 1fr;gap:4px}.empty-section{line-height:1.9;text-align:center}.tools-column{display:block;background:transparent;border:0;padding:0}.tools-column .tw-card{margin-bottom:14px}.comment{border-left:3px solid #4f776a;background:#f4f6f3;padding:9px;margin-top:9px}.comment p{margin:4px 0}.review-buttons{display:grid;grid-template-columns:1fr 1fr;gap:8px}.return{color:#a8544d;border-color:#d3a5a0}.review-result{background:#f2f5f2;padding:12px;border-radius:8px}.version{display:grid;padding:9px 0;border-top:1px solid #eee}.version small{color:#888}.tw-toast,.tw-error{position:fixed;right:24px;bottom:24px;color:#fff;padding:11px 15px;border-radius:8px;z-index:30}.tw-toast{background:#315f51}.tw-error{background:#a8544d}@media(max-width:1050px){.student-grid,.review-grid{grid-template-columns:1fr}.paper-row{grid-template-columns:1fr}.paper-section>.comment-anchor{right:12px}}
</style>
