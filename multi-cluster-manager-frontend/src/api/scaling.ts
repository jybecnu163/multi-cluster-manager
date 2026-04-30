// src/api/scaling.ts
import request from '../utils/request'
export const manualScale = (serviceId: string, data: { target_replicas: number; reason: string; ignore_approval?: boolean }) => request.post(`/services/${serviceId}/scale`, data)