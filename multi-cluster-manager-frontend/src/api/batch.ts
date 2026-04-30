// src/api/batch.ts
import request from '../utils/request'
export const createBatchTask = (data: any) => request.post('/batch/tasks', data)
export const getBatchTask = (taskId: string) => request.get(`/batch/tasks/${taskId}`)
export const nextBatch = (taskId: string) => request.post(`/batch/tasks/${taskId}/next`)
export const rollbackBatch = (taskId: string) => request.post(`/batch/tasks/${taskId}/rollback`)
export const resumeBatch = (taskId: string) => request.post(`/batch/tasks/${taskId}/resume`)