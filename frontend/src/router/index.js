import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import EditPaper from '../views/EditPaper.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresGuest: true }
  },
  {
    path: '/edit/:id?',
    name: 'EditPaper',
    component: EditPaper,
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 检查用户是否已登录
function isAuthenticated() {
  return !!localStorage.getItem('token')
}

// 全局导航守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    // 需要登录但未登录，跳转到登录页
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresGuest && isAuthenticated()) {
    // 已登录用户访问登录页，重定向到编辑页
    next({ name: 'EditPaper' })
  } else {
    next()
  }
})

export default router
