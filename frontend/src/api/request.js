import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
})

// ========== 请求拦截器 ==========
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// ========== 响应拦截器 ==========
request.interceptors.response.use(
  // 统一剥离外层 data
  response => response.data,
  error => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          // Token 失效，清除并跳转登录
          localStorage.removeItem('token')
          import('../router').then(({ default: router }) => {
            router.push({
              name: 'Login',
              query: { redirect: router.currentRoute.value.fullPath }
            })
          })
          break
        case 403:
          console.error('没有权限访问该资源')
          break
        case 500:
          console.error('服务器内部错误')
          break
        default:
          break
      }
    }
    return Promise.reject(error)
  }
)

export default request
