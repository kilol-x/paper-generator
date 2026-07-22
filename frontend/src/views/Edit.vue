<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PaperEditor from '../components/PaperEditor.vue'

const router = useRouter()

const content = ref('')

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('paper-access-token')
  sessionStorage.removeItem('paper-user-session')
  window.location.href = '/'
}
</script>

<template>
  <div class="edit-container">
    <header class="edit-header">
      <h2>论文编辑</h2>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </header>
    <main class="edit-content">
      <PaperEditor v-model="content" placeholder="请输入论文内容…" />
    </main>
  </div>
</template>

<style scoped>
.edit-container {
  min-height: 100vh;
  background: var(--bg-page);
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.edit-header h2 {
  margin: 0;
  font-size: 18px;
  font-family: var(--font-heading);
  color: var(--text-main);
}

.logout-btn {
  padding: 7px 18px;
  background: var(--danger);
  color: #fff;
  border: none;
  border-radius: var(--r);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--t-fast);
}

.logout-btn:hover {
  background: #A0534D;
}

.edit-content {
  max-width: 900px;
  margin: 28px auto;
  padding: 0 16px;
}
</style>
