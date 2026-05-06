import request from '../utils/request';

export interface Pipeline {
  id: number;
  name: string;
  steps?: any[];            // 步骤数组，具体结构由后端定义
  trigger_type?: string;     // 触发方式
  status?: string;           // 可能的状态
  created_by?: number;
  created_at?: string;
}

export interface PipelineCreateRequest {
  name: string;
  steps: any[];             // 至少为数组，内容格式自定
}

// 获取流水线列表
export const getPipelines = (): Promise<Pipeline[]> =>
  request.get('/pipelines');

// 创建流水线
export const createPipeline = (data: PipelineCreateRequest): Promise<any> =>
  request.post('/pipelines', data);

// 手动触发流水线
export const triggerPipeline = (pipelineId: number): Promise<any> =>
  request.post(`/pipelines/${pipelineId}/trigger`);