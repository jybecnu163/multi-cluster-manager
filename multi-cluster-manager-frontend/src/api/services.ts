// src/api/services.ts
import request from '../utils/request'
export const getServices = (params: any) => request.get('/services', { params })
export const getServiceDetail = (serviceId: string) => request.get(`/services/${serviceId}`)
export const getServiceMetrics = (serviceId: string, metric: string, range: string = '1h') => request.get(`/services/${serviceId}/metrics`, { params: { metric, range } })
export const exportServiceReport = (serviceId: string, timeRange: string, startDate?: string, endDate?: string) => request.get(`/services/${serviceId}/reports/export`, { params: { time_range: timeRange, start_date: startDate, end_date: endDate }, responseType: 'blob' })