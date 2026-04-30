// src/api/pipelines.ts
import request from '../utils/request'
export const getPipelines = () => request.get('/pipelines')
export const createPipeline = (data: any) => request.post('/pipelines', data)
export const triggerPipeline = (pipelineId: string) => request.post(`/pipelines/${pipelineId}/trigger`)