import axios from 'axios'

// ==================== 轻量提示工具（可替换为 element-plus 等 UI 库） ====================
const showMessage = {
  error(msg) {
    // 替换为: ElMessage.error(msg)  或其他 UI 库的提示方法
    console.error(`[Request Error] ${msg}`)
  },
  success(msg) {
    console.log(`[Request Success] ${msg}`)
  }
}

const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ========== 请求拦截器 ==========
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// ========== 响应拦截器 ==========
service.interceptors.response.use(
  response => {
    const res = response.data

    // 如果是文件下载等返回二进制的情况，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    // 根据后端约定的 code 判断是否成功
    // 这里假设后端返回 { code: 200, data: ..., message: ... } 格式
    if (res.code !== 200 && res.code !== undefined) {
      showMessage.error(res.message || '请求失败')

      // Token 过期，清除登录状态并跳转登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case 400:
          showMessage.error(data?.message || '请求参数错误')
          break
        case 401:
          showMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          showMessage.error('没有访问权限')
          break
        case 404:
          showMessage.error('请求的资源不存在')
          break
        case 500:
          showMessage.error('服务器内部错误')
          break
        default:
          showMessage.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.message.includes('timeout')) {
      showMessage.error('请求超时，请稍后重试')
    } else if (error.message.includes('Network Error')) {
      showMessage.error('网络连接失败，请检查网络')
    } else {
      showMessage.error('请求异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default service
