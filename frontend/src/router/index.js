import { createRouter, createWebHistory } from 'vue-router'


const routes = [
  {
    path: '/',
    name: 'Papers',
    component: () => import('../views/student/Papers.vue'),
    // 统一登录页由 App.vue 在根路由展示，未登录时必须允许进入 /
    meta: { requiresAuth: false }
  },
  {
    path: '/edit/:id?',
    name: 'EditPaper',
    component: () => import('../views/EditPaper.vue'),
    meta: { requiresAuth: true }
  },

  // ========== 馃憞 鏂板锛氳鏂囬瑙堣矾鐢憋紙浜哄憳5 娣诲姞锛?==========
  {
    path: '/preview/:id',
    name: 'PaperPreview',
    component: () => import('../views/student/PaperPreviewNew.vue'),
    meta: { requiresAuth: true }
  },
  // ========== 鍏ㄦ柊棰勮椤?==========
  {
    path: '/full-preview/:id',
    name: 'PaperFullPreview',
    component: () => import('../views/student/PaperPreviewNew.vue'),
    meta: { requiresAuth: true }
  },

  // ========== 404 鍏滃簳璺敱锛堟斁鍦ㄦ渶鍚庯級 ==========
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

// 鍏ㄥ眬瀵艰埅瀹堝崼
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/')
  } else {
    next()
  }
})

export default router
