import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Papers',
    component: () => import('../views/student/Papers.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/edit/:id?',
    name: 'EditPaper',
    component: () => import('../views/EditPaper.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function isAuthenticated() {
  // App.vue 登录后写 sessionStorage + localStorage
  const session = sessionStorage.getItem('paper-user-session')
  const token = localStorage.getItem('paper-access-token') || localStorage.getItem('token')
  return !!(session && token)
}

// 全局导航守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    // 未登录 → 回到根路径，App.vue 会显示登录页
    next('/')
  } else {
    next()
  }
})

export default router
