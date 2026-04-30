// src/api/canary.ts
import request from '../utils/request'
export const createCanaryTask = (data: any) => request.post('/canary/tasks', data)
export const getCanaryTask = (taskId: string) => request.get(`/canary/tasks/${taskId}`)
export const getInternalEndpoint = (taskId: string) => request.get(`/canary/tasks/${taskId}/internal-endpoint`)
export const promoteStage = (taskId: string, stage: string) => request.post(`/canary/tasks/${taskId}/stage/${stage}`)
export const rollbackCanary = (taskId: string) => request.post(`/canary/tasks/${taskId}/rollback`)
export const resumeCanary = (taskId: string) => request.post(`/canary/tasks/${taskId}/resume`)