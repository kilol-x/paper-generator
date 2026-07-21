<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AdminSystem from './components/AdminSystem.vue'

const router = useRouter()
const route = useRoute()

const loginAccounts={
  STUDENT:{label:'学生',icon:'学',username:'student',password:'student123',note:'生成自己的论文与批阅反馈'},
  TEACHER:{label:'教师',icon:'师',username:'teacher',password:'teacher123',note:'批注、退回、评分与版本记录'},
  ADMIN:{label:'管理员',icon:'管',username:'admin',password:'admin123',note:'管理学院与论文模板'}
}
function readSession(){try{const saved=JSON.parse(sessionStorage.getItem('paper-user-session')||'null');return ['STUDENT','TEACHER','ADMIN'].includes(saved?.role)?saved:null}catch{return null}}
const session=ref(readSession()),role=computed(()=>session.value?.role?.toLowerCase()||''),page=ref(role.value==='teacher'?'queue':'overview'),selected=ref('abstract'),search=ref(''),commentText=ref(''),toast=ref('')
const authMode=ref('login'),loginForm=reactive({role:'STUDENT',username:'student',password:'student123'}),loginError=ref(''),loginSuccess=ref(''),loginLoading=ref(false)
const registerForm=reactive({role:'STUDENT',nickname:'',username:'',password:'',confirmPassword:''}),registerError=ref(''),registerLoading=ref(false)
const initial={status:'SUBMITTED',version:1,comments:[],scores:{structure:0,content:0,method:0,writing:0},summary:'',history:[{action:'提交',text:'学生提交论文 V1，系统自动锁定文档',time:'2026-07-20 09:30',version:1}]}
const state=reactive(JSON.parse(localStorage.getItem('vue3-review-state')||'null')||initial)
const sections=[['abstract','摘要','本文分析生成式人工智能在高校教学中的典型应用，从教学设计、学习支持和评价反馈三个维度讨论其价值，并关注内容可信度、学术诚信和数据安全等风险。'],['intro','一、引言','大语言模型正在改变知识获取和内容生产方式。高校需要重新审视教学目标，将知识记忆转向问题定义、证据判断和创造性解决问题等高阶能力。'],['method','二、研究方法','研究采用文献分析与半结构化访谈方法，选取三所高校的教师和学生作为观察对象，对工具使用场景、学习成效以及潜在风险进行归纳编码。'],['finding','三、研究发现','生成式人工智能能够提升备课效率并缩短形成性反馈周期；若缺乏过程监管，学生可能产生依赖，并面临虚构引用和隐私泄露问题。'],['conclusion','四、结论与建议','高校应建立分级使用规范，并通过口头答辩、过程日志与版本追踪，使人工智能成为思维脚手架而非答案替代品。']]
const dims=[['structure','结构与逻辑',20],['content','内容与观点',35],['method','方法与论证',25],['writing','规范与表达',20]]
const total=computed(()=>Object.values(state.scores).reduce((a,b)=>a+(+b||0),0)),grade=computed(()=>total.value>=90?'A':total.value>=80?'B':total.value>=70?'C':total.value>=60?'D':total.value?'F':'—')
const statusMap={SUBMITTED:['已提交 · 文档锁定','blue'],REVIEWING:['批阅中 · 文档锁定','blue'],RETURNED:['已退回 · 可修改','orange'],GRADED:['已评分 · 已归档','green']}
watch(state,v=>localStorage.setItem('vue3-review-state',JSON.stringify(v)),{deep:true})
function chooseLoginRole(nextRole){loginForm.role=nextRole;loginForm.username=loginAccounts[nextRole].username;loginForm.password=loginAccounts[nextRole].password;loginError.value='';loginSuccess.value=''}
function openRegister(){authMode.value='register';registerForm.role=loginForm.role;registerError.value='';loginSuccess.value=''}
function backToLogin(){authMode.value='login';registerError.value=''}
function chooseRegisterRole(nextRole){registerForm.role=nextRole;registerError.value=''}
async function submitLogin(){
  if(!loginForm.username.trim()||!loginForm.password)return loginError.value='请输入用户名和密码'
  loginLoading.value=true;loginError.value=''
  try{
    const response=await fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:loginForm.username.trim(),password:loginForm.password,role:loginForm.role})})
    const result=await response.json()
    if(result.code!==200)throw new Error(result.message||'登录失败')
    if(String(result.data.role).toUpperCase()!==loginForm.role)throw new Error('所选身份与该账号的角色不匹配')
    session.value=result.data;sessionStorage.setItem('paper-user-session',JSON.stringify(result.data));localStorage.setItem('paper-access-token',result.data.token);localStorage.setItem('token',result.data.token);page.value=result.data.role==='TEACHER'?'queue':'overview'
  }catch(e){loginError.value=e.message==='Failed to fetch'?'后端服务未启动，请先运行 Spring Boot':e.message}finally{loginLoading.value=false}
}
async function submitRegister(){
  if(!registerForm.nickname.trim()||!registerForm.username.trim()||!registerForm.password)return registerError.value='请完整填写注册信息'
  if(registerForm.password.length<6)return registerError.value='密码至少需要 6 位'
  if(registerForm.password!==registerForm.confirmPassword)return registerError.value='两次输入的密码不一致'
  registerLoading.value=true;registerError.value=''
  try{
    const response=await fetch('/api/auth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({nickname:registerForm.nickname.trim(),username:registerForm.username.trim(),password:registerForm.password,role:registerForm.role})})
    const result=await response.json()
    if(result.code!==200)throw new Error(result.message||'注册失败')
    const registeredRole=registerForm.role;loginForm.role=registeredRole;loginForm.username=registerForm.username.trim();loginForm.password=registerForm.password;authMode.value='login';loginSuccess.value=`${loginAccounts[registeredRole].label}账号注册成功，请登录`
    Object.assign(registerForm,{role:'STUDENT',nickname:'',username:'',password:'',confirmPassword:''})
  }catch(e){registerError.value=e.message==='Failed to fetch'?'后端服务未启动，请先运行 Spring Boot':e.message}finally{registerLoading.value=false}
}
function logout(){session.value=null;sessionStorage.removeItem('paper-user-session');localStorage.removeItem('paper-access-token');localStorage.removeItem('token');loginError.value=''}
function msg(t){toast.value=t;setTimeout(()=>toast.value='',1900)}function now(){return new Date().toLocaleString('zh-CN',{hour12:false})}function record(action,text){state.history.push({action,text,time:now(),version:state.version})}
function addComment(){if(!commentText.value.trim())return msg('请输入批注内容');state.comments.push({section:selected.value,text:commentText.value.trim(),time:now(),version:state.version});state.status='REVIEWING';record('批注',`教师在"${sections.find(s=>s[0]===selected.value)[1]}"添加定位评语`);commentText.value='';msg('批注已添加到对应位置')}
function saveReview(){state.status='REVIEWING';record('保存批阅','教师保存当前批注、评分和总评草稿');msg('批阅进度已保存')}
function returnPaper(){if(!state.comments.length&&!state.summary.trim())return msg('退回前请填写修改意见');state.status='RETURNED';record('退回修改','教师退回论文，学生端解除锁定');msg('论文已退回，学生可修改')}
function finalize(){if(!total.value||!state.summary.trim())return msg('请完成评分并填写总评');state.status='GRADED';record('完成评分',`成绩 ${total.value} 分（${grade.value}）已归档`);msg('评分已完成')}
function resubmit(){if(state.status!=='RETURNED')return msg('仅退回状态可以重新提交');state.version++;state.status='SUBMITTED';state.scores={structure:0,content:0,method:0,writing:0};state.summary='';record('重新提交',`学生修订后提交 V${state.version}，文档再次锁定`);msg('新版本已提交并锁定')}

// 判断当前是否在编辑页（编辑页全屏，不显示外壳）
const isEditing = computed(() => route.name === 'EditPaper')
</script>

<template>
  <!-- ==================== 编辑器全屏模式 ==================== -->
  <div v-if="session && role==='student' && isEditing" class="editor-fullscreen">
    <router-view />
  </div>

  <!-- ==================== 登录页 ==================== -->
  <main v-else-if="!session" class="login-page">
<section class="login-intro"><div class="login-brand"><i><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg></i><span>在线论文文档自动生成系统</span></div><div class="intro-copy"><span class="eyebrow">ONLINE PAPER DOCUMENT SYSTEM</span><h1>在线论文文档<br>自动生成系统</h1><p>面向学生、教师和管理员的一体化论文平台，支持论文生成、提交锁定、教师批阅与全过程记录。</p></div><div class="flow"><span>论文生成</span><i>→</i><span>提交锁定</span><i>→</i><span>教师批阅</span></div></section>
<section class="login-panel">
<form v-if="authMode==='login'" class="login-card" @submit.prevent="submitLogin"><div class="login-heading"><small>欢迎使用</small><h2>请选择登录身份</h2><p>账号身份必须与所选入口一致</p></div><div class="role-grid"><button v-for="(item,key) in loginAccounts" :key="key" type="button" :class="{selected:loginForm.role===key}" @click="chooseLoginRole(key)"><i>{{item.icon}}</i><b>{{item.label}}</b><small>{{item.note}}</small></button></div><label>用户名<input v-model="loginForm.username" autocomplete="username" placeholder="请输入用户名"></label><label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码"></label><div v-if="loginSuccess" class="login-success">{{loginSuccess}}</div><div v-if="loginError" class="login-error">{{loginError}}</div><button class="login-submit" :disabled="loginLoading" type="submit">{{loginLoading?'正在验证…':`登录${loginAccounts[loginForm.role].label}端`}}</button><button class="auth-switch" type="button" @click="openRegister">没有账号？注册新账号</button><p class="demo-account">演示账号：{{loginAccounts[loginForm.role].username}} / {{loginAccounts[loginForm.role].password}}</p></form>
<form v-else class="login-card register-card" @submit.prevent="submitRegister"><div class="login-heading"><small>创建账号</small><h2>注册系统账号</h2><p>请选择学生、教师或管理员身份</p></div><div class="role-grid"><button v-for="(item,key) in loginAccounts" :key="key" type="button" :class="{selected:registerForm.role===key}" @click="chooseRegisterRole(key)"><i>{{item.icon}}</i><b>{{item.label}}</b><small>{{item.note}}</small></button></div><div class="register-fields"><label>姓名 / 昵称<input v-model="registerForm.nickname" autocomplete="name" placeholder="请输入姓名或昵称"></label><label>用户名<input v-model="registerForm.username" autocomplete="username" placeholder="设置登录用户名"></label><label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="至少 6 位密码"></label><label>确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码"></label></div><div v-if="registerError" class="login-error">{{registerError}}</div><button class="login-submit" :disabled="registerLoading" type="submit">{{registerLoading?'正在注册…':`注册${loginAccounts[registerForm.role].label}账号`}}</button><button class="auth-switch" type="button" @click="backToLogin">已有账号？返回登录</button></form>
</section></main>

  <!-- ==================== 外壳（学生列表 / 教师 / 管理员） ==================== -->
  <div v-else class="shell"><aside><div class="brand"><i><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg></i><div><b>论文生成系统</b><small>一体化论文工作平台</small></div></div><div class="account-role"><small>当前身份</small><b>{{loginAccounts[session.role].label}}端</b></div>
<nav v-if="role==='admin'"><button class="on">▦ 管理员系统</button></nav>
<nav v-else-if="role==='student'">
  <button :class="{on:route.name==='Papers' && page==='overview'}" @click="router.push({name:'Papers'}); page='overview'">▦ 我的论文</button>
  <button :class="{on:page==='feedback'}" @click="page='feedback'">◷ 批阅反馈</button>
</nav>
<nav v-else><button :class="{on:page==='queue'}" @click="page='queue'">▦ 待批阅论文</button><button :class="{on:page==='review'}" @click="page='review'">✎ 当前批阅</button><button :class="{on:page==='history'}" @click="page='history'">◷ 版本历史</button></nav><div class="user">{{session.nickname||session.username}} · {{session.role}}</div></aside><main><header><h1>{{role==='admin'?'管理员系统':role==='student'?(page==='overview'?'我的论文':'教师批阅反馈'):(page==='queue'?'待批阅论文':page==='review'?'论文批阅工作台':'版本历史记录')}}</h1><div class="header-actions"><div v-if="role!=='admin'" class="state" :class="statusMap[state.status][1]">{{statusMap[state.status][0]}}　{{state.status==='RETURNED'?'🔓':'🔒'}}</div><button class="logout" @click="logout">退出登录</button></div></header><div class="content" :class="{flush:role==='admin'||(role==='student'&&page==='overview')}">
  <AdminSystem v-if="role==='admin'" />

  <!-- === 学生端：router-view === -->
  <router-view v-else-if="role==='student'&&page==='overview'" />

  <!-- === 学生端：批阅反馈（保持原样） === -->
  <template v-else-if="role==='student'">
    <section class="card history"><div class="cardhead"><b>教师批注与版本历史</b><button @click="page='overview'">返回我的论文</button></div><div v-if="state.comments.length" class="feedback-list"><div v-for="(c,i) in state.comments" :key="i"><b>批注 #{{i+1}} · {{sections.find(s=>s[0]===c.section)[1]}}</b><p>{{c.text}}</p><small>V{{c.version}} · {{c.time}}</small></div></div><div v-else class="empty-state">暂无教师批注</div><div class="event" v-for="(h,i) in [...state.history].reverse()" :key="i"><i>{{state.history.length-i}}</i><div><b>{{h.action}} · V{{h.version}}</b><p>{{h.text}}</p><small>{{h.time}}</small></div></div></section>
  </template>

  <!-- === 教师端 === -->
  <template v-else><section v-if="page==='queue'"><div class="metrics"><article><small>待批阅</small><strong>{{['SUBMITTED','REVIEWING'].includes(state.status)?1:0}}</strong></article><article><small>已退回</small><strong>{{state.status==='RETURNED'?1:0}}</strong></article><article><small>已评分</small><strong>{{state.status==='GRADED'?1:0}}</strong></article><article><small>定位批注</small><strong>{{state.comments.length}}</strong></article></div><div class="card"><div class="cardhead"><b>课程论文提交列表</b><input v-model="search" placeholder="搜索学生或论文标题"></div><div class="row" v-if="'张明 生成式人工智能在高校教学中的应用研究'.includes(search)"><div><b>生成式人工智能在高校教学中的应用研究</b><small>张明 · 2023110208 · V{{state.version}}</small></div><span>课程论文<small>类型</small></span><span>{{state.comments.length}} 条<small>批注</small></span><span>{{total}} 分<small>评分</small></span><button class="primary" @click="page='review'">进入批阅</button></div></div></section><section v-else-if="page==='review'" class="review"><article class="paper card"><div class="notice">{{state.status==='RETURNED'?'论文已退回，当前批阅只读。':state.status==='GRADED'?'评分已归档，当前批阅只读。':'学生提交后文档已锁定，教师可批注但不能改写原文。'}}</div><div class="title"><h2>生成式人工智能在高校教学中的应用研究</h2><p>张明 · 2023110208　|　课程论文</p></div><section v-for="s in sections" :key="s[0]" class="para" :class="{selected:selected===s[0]}" @click="selected=s[0]"><button :class="{has:state.comments.some(c=>c.section===s[0])}">{{state.comments.filter(c=>c.section===s[0]).length||'+'}}</button><h3>{{s[1]}}</h3><p>{{s[2]}}</p></section></article><aside class="tools"><div class="box"><h3>定位批注</h3><small>当前位置：{{sections.find(s=>s[0]===selected)[1]}}</small><textarea v-model="commentText" placeholder="输入该位置的评语"></textarea><button class="primary" :disabled="['RETURNED','GRADED'].includes(state.status)" @click="addComment">添加批注</button><div class="comments"><div v-for="(c,i) in state.comments" :key="i"><b>#{{i+1}} {{sections.find(s=>s[0]===c.section)[1]}}</b><p>{{c.text}}</p><small>V{{c.version}} · {{c.time}}</small></div></div></div><div class="box"><h3>评分与总评</h3><label v-for="d in dims" :key="d[0]">{{d[1]}}（{{d[2]}}分）<input v-model.number="state.scores[d[0]]" type="number" min="0" :max="d[2]" :disabled="['RETURNED','GRADED'].includes(state.status)"></label><div class="total">总分 / 等级 <strong>{{total}} / {{grade}}</strong></div><textarea v-model="state.summary" placeholder="教师总评"></textarea><div class="actions"><button @click="saveReview">保存</button><button class="danger" @click="returnPaper">退回修改</button><button class="primary" @click="finalize">完成评分</button></div></div></aside></section><section v-else class="card history"><div class="cardhead"><b>完整版本与操作留痕</b></div><div class="event" v-for="(h,i) in [...state.history].reverse()" :key="i"><i>{{state.history.length-i}}</i><div><b>{{h.action}} · V{{h.version}}</b><p>{{h.text}}</p><small>{{h.time}}</small></div></div></section></template>
</div></main><div class="toast" v-if="toast">{{toast}}</div></div>
</template>

<style>
/* ============================================================
   Notion 莫兰迪风格 CSS 设计令牌（Design Tokens）
   供全局组件和页面使用
   ============================================================ */
:root {
  --primary:       #4F776A;
  --primary-tint:  #E8F0EC;
  --primary-hover: #3E5F54;
  --bg-page:       #F5F4F0;
  --bg-card:       #FFFFFF;
  --bg-hover:      #EDF0EC;
  --border:        #E4E3DE;
  --border-strong: #C8C7C2;
  --text-main:     #242622;
  --text-mute:     #5C605A;
  --text-dim:      #858982;
  --success:       #386858;
  --warning:       #B8893E;
  --danger:        #A8544D;
  --danger-bg:     #FAEAE7;
  --font-heading:  "Noto Serif SC", "Microsoft YaHei", serif;
  --r-xs:  4px;
  --r-sm:  6px;
  --r:     8px;
  --r-lg:  12px;
  --r-xl:  16px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,.04);
  --shadow-lg: 0 8px 24px rgba(0,0,0,.06);
  --t-fast: .18s ease;

  /* ===== Element Plus 主题色映射到项目莫兰迪绿 ===== */
  --el-color-primary:       #4F776A;
  --el-color-primary-light-3: #729487;
  --el-color-primary-light-5: #90ACA2;
  --el-color-primary-light-7: #B8CEC6;
  --el-color-primary-light-8: #D0DFD9;
  --el-color-primary-light-9: #E8EFEC;
  --el-color-primary-dark-2:  #3F5F55;
  --el-color-success:       #386858;
  --el-color-warning:       #B8893E;
  --el-color-danger:        #A8544D;
}

/* 编辑器全屏模式 */
.editor-fullscreen {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
</style>
