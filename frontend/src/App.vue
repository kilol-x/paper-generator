<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AdminSystem from './components/AdminSystem.vue'
import TeacherWorkspace from './components/TeacherWorkspace.vue'
import StudentFeedback from './components/StudentFeedback.vue'

const router = useRouter()
const route = useRoute()

const loginAccounts={
  STUDENT:{label:'学生',icon:'学',username:'',password:'',note:'生成自己的论文与批阅反馈'},
  TEACHER:{label:'教师',icon:'师',username:'teacher',password:'teacher123',note:'管理名下学生与批阅真实论文'},
  ADMIN:{label:'管理员',icon:'管',username:'admin',password:'admin123',note:'管理学院与论文模板'}
}
const registerRoles=['TEACHER','ADMIN']
function readSession(){try{const saved=JSON.parse(sessionStorage.getItem('paper-user-session')||'null');return ['STUDENT','TEACHER','ADMIN'].includes(saved?.role)?saved:null}catch{return null}}
const session=ref(readSession()),role=computed(()=>session.value?.role?.toLowerCase()||''),page=ref(role.value==='teacher'?'students':'overview')
const authMode=ref('login'),loginForm=reactive({role:'STUDENT',username:'',password:''}),loginError=ref(''),loginSuccess=ref(''),loginLoading=ref(false)
const registerForm=reactive({role:'TEACHER',nickname:'',username:'',password:'',confirmPassword:''}),registerError=ref(''),registerLoading=ref(false)
function chooseLoginRole(nextRole){loginForm.role=nextRole;loginError.value='';loginSuccess.value=''}
function openRegister(){authMode.value='register';registerForm.role=loginForm.role==='ADMIN'?'ADMIN':'TEACHER';registerError.value='';loginSuccess.value=''}
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
    session.value=result.data;sessionStorage.setItem('paper-user-session',JSON.stringify(result.data));localStorage.setItem('paper-access-token',result.data.token);localStorage.setItem('token',result.data.token);page.value=result.data.role==='TEACHER'?'students':'overview'
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
    Object.assign(registerForm,{role:'TEACHER',nickname:'',username:'',password:'',confirmPassword:''})
  }catch(e){registerError.value=e.message==='Failed to fetch'?'后端服务未启动，请先运行 Spring Boot':e.message}finally{registerLoading.value=false}
}
function logout(){session.value=null;sessionStorage.removeItem('paper-user-session');localStorage.removeItem('paper-access-token');localStorage.removeItem('token');loginError.value=''}

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
<form v-if="authMode==='login'" class="login-card" @submit.prevent="submitLogin"><div class="login-heading"><small>欢迎使用</small><h2>请选择登录身份</h2><p>账号身份必须与所选入口一致</p></div><div class="role-grid"><button v-for="(item,key) in loginAccounts" :key="key" type="button" :class="{selected:loginForm.role===key}" @click="chooseLoginRole(key)"><i>{{item.icon}}</i><b>{{item.label}}</b><small>{{item.note}}</small></button></div><div v-if="loginForm.role==='STUDENT'" class="account-tip">学生账号由教师创建，用户名为学号，初始密码为 123456。</div><label>用户名<input v-model="loginForm.username" autocomplete="username" placeholder="请输入用户名"></label><label>密码<input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码"></label><div v-if="loginSuccess" class="login-success">{{loginSuccess}}</div><div v-if="loginError" class="login-error">{{loginError}}</div><button class="login-submit" :disabled="loginLoading" type="submit">{{loginLoading?'正在验证…':`登录${loginAccounts[loginForm.role].label}端`}}</button><button class="auth-switch" type="button" @click="openRegister">注册教师或管理员账号</button></form>
<form v-else class="login-card register-card" @submit.prevent="submitRegister"><div class="login-heading"><small>创建账号</small><h2>注册教师 / 管理员账号</h2><p>学生账号只能由教师在“学生管理”中创建</p></div><div class="role-grid register-roles"><button v-for="key in registerRoles" :key="key" type="button" :class="{selected:registerForm.role===key}" @click="chooseRegisterRole(key)"><i>{{loginAccounts[key].icon}}</i><b>{{loginAccounts[key].label}}</b><small>{{loginAccounts[key].note}}</small></button></div><div class="register-fields"><label>姓名 / 昵称<input v-model="registerForm.nickname" autocomplete="name" placeholder="请输入姓名或昵称"></label><label>用户名<input v-model="registerForm.username" autocomplete="username" placeholder="设置登录用户名"></label><label>密码<input v-model="registerForm.password" type="password" autocomplete="new-password" placeholder="至少 6 位密码"></label><label>确认密码<input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入密码"></label></div><div v-if="registerError" class="login-error">{{registerError}}</div><button class="login-submit" :disabled="registerLoading" type="submit">{{registerLoading?'正在注册…':`注册${loginAccounts[registerForm.role].label}账号`}}</button><button class="auth-switch" type="button" @click="backToLogin">已有账号？返回登录</button></form>
</section></main>

  <!-- ==================== 外壳（学生列表 / 教师 / 管理员） ==================== -->
  <div v-else class="shell"><aside><div class="brand"><i><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg></i><div><b>论文生成系统</b><small>一体化论文工作平台</small></div></div><div class="account-role"><small>当前身份</small><b>{{loginAccounts[session.role].label}}端</b></div>
<nav v-if="role==='admin'"><button class="on">▦ 管理员系统</button></nav>
<nav v-else-if="role==='student'">
  <button :class="{on:route.name==='Papers' && page==='overview'}" @click="router.push({name:'Papers'}); page='overview'">▦ 我的论文</button>
  <button :class="{on:page==='feedback'}" @click="page='feedback'">◷ 批阅反馈</button>
</nav>
<nav v-else><button :class="{on:page==='students'}" @click="page='students'">▦ 学生管理</button><button :class="{on:page==='papers'}" @click="page='papers'">▤ 论文管理</button><button :class="{on:page==='review'}" @click="page='review'">✎ 论文批阅</button></nav><div class="user">{{session.nickname||session.username}} · {{session.role}}</div></aside><main><header><h1>{{role==='admin'?'管理员系统':role==='student'?(page==='overview'?'我的论文':'教师批阅反馈'):(page==='students'?'学生管理':page==='papers'?'学生论文管理':'论文批阅工作台')}}</h1><div class="header-actions"><button class="logout" @click="logout">退出登录</button></div></header><div class="content" :class="{flush:role==='admin'||(role==='student'&&page==='overview')}">
  <AdminSystem v-if="role==='admin'" />

  <!-- === 学生端：router-view === -->
  <router-view v-else-if="role==='student'&&page==='overview'" />

  <!-- === 学生端：真实批阅反馈与退回修改 === -->
  <StudentFeedback v-else-if="role==='student'" @back="page='overview'" />

  <!-- === 教师端：数据库学生与真实论文 === -->
  <TeacherWorkspace v-else :page="page" @navigate="page=$event" />
</div></main></div>
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
