<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const router = useRouter()
const route = useRoute()

// 当前模式：login | register
const mode = ref('login')

const formRef = ref(null)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)

// 登录表单校验规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ]
}

// 注册表单校验规则（密码 + 确认密码）
const registerRules = {
  ...loginRules,
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 根据模式动态切换规则
const rules = computed(() => (mode.value === 'login' ? loginRules : registerRules))

// 切换登录/注册
function switchMode(val) {
  mode.value = val
  formRef.value?.resetFields()
}

// 提交表单
function handleSubmit() {
  formRef.value?.validate(async valid => {
    if (!valid) return

    loading.value = true
    try {
      if (mode.value === 'login') {
        await handleLogin()
      } else {
        await handleRegister()
      }
    } catch (e) {
      // 错误在拦截器或下方已处理
    } finally {
      loading.value = false
    }
  })
}

// 登录
async function handleLogin() {
  try {
    const res = await request.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })
    localStorage.setItem('token', res.data?.token || res.token)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || { name: 'Papers' }
    router.push(redirect)
  } catch (err) {
    const msg = err.response?.data?.message || '登录失败，请检查用户名和密码'
    ElMessage.error(msg)
  }
}

// 注册
async function handleRegister() {
  try {
    await request.post('/api/auth/register', {
      username: form.username,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    switchMode('login')
  } catch (err) {
    const msg = err.response?.data?.message || '注册失败，请稍后重试'
    ElMessage.error(msg)
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <!-- 标题 -->
      <h2 class="auth-title">{{ mode === 'login' ? '登录' : '注册' }}</h2>

      <!-- 切换选项卡 -->
      <div class="auth-tabs">
        <span
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >登录</span>
        <span
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >注册</span>
      </div>

      <!-- 表单 -->
      <el-form
        id="login-form"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item
          v-if="mode === 'register'"
          label="确认密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleSubmit"
          >
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-page);
  background-image:
    radial-gradient(ellipse at 20% 50%, rgba(91, 133, 119, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(91, 133, 119, 0.03) 0%, transparent 60%);
}

.auth-card {
  width: 420px;
  padding: 44px 40px 36px;
  background: var(--bg-card);
  border-radius: var(--r-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border);
}

.auth-title {
  text-align: center;
  margin: 0 0 28px;
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.3px;
}

.auth-tabs {
  display: flex;
  justify-content: center;
  gap: 36px;
  margin-bottom: 32px;
  border-bottom: 2px solid var(--border);
}

.auth-tabs span {
  padding: 0 4px 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-dim);
  cursor: pointer;
  transition: color var(--t-fast), border-color var(--t-fast);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.auth-tabs span.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.auth-tabs span:hover {
  color: var(--primary);
}

/* ID 选择器 —— 登录表单居中 */
#login-form {
  width: 300px;
  margin: 0 auto;
}

/* 覆盖 Element Plus 表单标签颜色 */
.auth-card :deep(.el-form-item__label) {
  color: var(--text-mute);
  font-weight: 500;
}

/* 覆盖 Element Plus 输入框样式 */
.auth-card :deep(.el-input__wrapper) {
  background: var(--bg-page);
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: var(--r);
}
.auth-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}
.auth-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset;
}
</style>
