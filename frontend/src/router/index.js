import { createRouter, createWebHashHistory } from 'vue-router'
import EditPaper from '@/views/EditPaper.vue'
import Login from '@/views/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/edit',
    name: 'EditPaper',
    component: EditPaper,
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/edit'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 访问需要登录的页面，但未登录 → 跳转登录页
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login' })
    return
  }

  // 已登录时访问登录页 → 跳转编辑页
  if (to.name === 'Login' && token) {
    next({ name: 'EditPaper' })
    return
  }

  next()
})

export default router
