import { getApiBaseUrl } from '../config'

let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  
  failedQueue = []
}

const apiRequest = async (url, options = {}) => {
  const accessToken = localStorage.getItem('accessToken')
  const refreshToken = localStorage.getItem('refreshToken')
  
  const defaultHeaders = {
    'Content-Type': 'application/json',
  }
  
  if (accessToken) {
    defaultHeaders.Authorization = `Bearer ${accessToken}`
  }
  
  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }
  
  const apiBaseUrl = await getApiBaseUrl()
  const response = await fetch(`${apiBaseUrl}${url}`, config)
  
  if ((response.status === 401 || response.status === 403) && refreshToken && !url.includes('/auth/')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          config.headers.Authorization = `Bearer ${token}`
          return fetch(`${apiBaseUrl}${url}`, config)
        })
      }
      
      isRefreshing = true
      
      try {
        const refreshResponse = await fetch(`${apiBaseUrl}/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
        
        if (refreshResponse.ok) {
          const data = await refreshResponse.json()
          localStorage.setItem('accessToken', data.accessToken)
          localStorage.setItem('refreshToken', data.refreshToken)
          
          config.headers.Authorization = `Bearer ${data.accessToken}`
          processQueue(null, data.accessToken)
          
          return fetch(`${apiBaseUrl}${url}`, config)
        } else {
          processQueue(new Error('Token refresh failed'))
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('user')
          window.location.href = '/login'
          throw new Error('Token refresh failed')
        }
      } catch (refreshError) {
        processQueue(refreshError)
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.href = '/login'
        throw refreshError
      } finally {
        isRefreshing = false
      }
    }
  
  return response
}

export const get = (url, options = {}) => {
  return apiRequest(url, { ...options, method: 'GET' })
}

export const post = (url, data, options = {}) => {
  return apiRequest(url, {
    ...options,
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export const put = (url, data, options = {}) => {
  return apiRequest(url, {
    ...options,
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export const patch = (url, data, options = {}) => {
  return apiRequest(url, {
    ...options,
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}

export const del = (url, options = {}) => {
  return apiRequest(url, { ...options, method: 'DELETE' })
}

export default apiRequest
