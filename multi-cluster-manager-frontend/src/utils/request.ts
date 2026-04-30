import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
const request: AxiosInstance = axios.create({ baseURL: '/api/v1', timeout: 30000 })
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
}, (error) => Promise.reject(error))
request.interceptors.response.use((response: AxiosResponse) => response.data, (error) => {
  if (error.response?.status === 401) { localStorage.removeItem('access_token'); window.location.href = '/login' }
  return Promise.reject(error)
})
export default request