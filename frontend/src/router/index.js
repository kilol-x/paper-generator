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

  // ========== 👇 新增：论文预览路由（人员5 添加） ==========
  {
    path: '/preview/:id',
    name: 'PaperPreview',
    component: () => import('../views/student/PaperPreview.vue'),
    meta: { requiresAuth: true }
  },
  // ========== 全新预览页 ==========
  {
    path: '/full-preview/:id',
    name: 'PaperFullPreview',
    component: () => import('../views/student/PaperPreviewNew.vue'),
    meta: { requiresAuth: true }
  },

  // ========== 404 兜底路由（放在最后） ==========
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
  const session = sessionStorage.getItem('paper-user-session')
  const token = localStorage.getItem('paper-access-token') || localStorage.getItem('token')
  return !!(session && token)
}

// 全局导航守卫
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/')
  } else {
    next()
  }
})

export default router