// src/api/canary.ts
import request from '../utils/request';

// 灰度策略
export interface CanaryStrategy {
  canary_replicas?: number;           // 默认 1
  auto_approve_traffic?: boolean;      // 默认 false
  auto_promote_steps?: number[];       // 可包含 5, 25
  rollback_on_error?: boolean;         // 默认 true
}

// 创建灰度任务请求体
export interface CanaryTaskCreateRequest {
  service_instance_id: number;
  target_image: string;
  strategy: CanaryStrategy;
}

// 灰度任务详情（对应 CanaryTaskDetail）
export interface CanaryTaskDetail {
  id: number;
  service_instance_id: number;
  status: 'internal_test' | 'waiting_approval' | 'traffic_5' | 'traffic_25' | 'full' | 'success' | 'failed' | 'paused' | 'rolled_back';
  current_stage: number;
  target_image: string;
  canary_weight: number;
  approval_id: number;
  approval_timeout_hours: number;
  created_at: string;
}

// 内部测试链接响应
export interface InternalEndpointResponse {
  internal_url: string;
  expires_in_seconds: number;
}

/**
 * 创建灰度发布任务
 * POST /canary/tasks
 */
export const createCanaryTask = (data: CanaryTaskCreateRequest): Promise<CanaryTaskDetail> =>
  request.post('/canary/tasks', data);

/**
 * 查询灰度任务详情
 * GET /canary/tasks/{task_id}
 */
export const getCanaryTask = (taskId: number): Promise<CanaryTaskDetail> =>
  request.get(`/canary/tasks/${taskId}`);

/**
 * 获取内部测试链接（仅在 internal_test 阶段有效）
 * GET /canary/tasks/{task_id}/internal-endpoint
 */
export const getInternalEndpoint = (taskId: number): Promise<InternalEndpointResponse> =>
  request.get(`/canary/tasks/${taskId}/internal-endpoint`);

/**
 * 推进灰度阶段
 * POST /canary/tasks/{task_id}/stage/{stage}
 * stage 可选值：internal_tested, request_traffic, promote_25, promote_100
 */
export const promoteStage = (taskId: number, stage: string): Promise<void> =>
  request.post(`/canary/tasks/${taskId}/stage/${stage}`);

/**
 * 回滚灰度任务
 * POST /canary/tasks/{task_id}/rollback
 */
export const rollbackCanary = (taskId: number): Promise<void> =>
  request.post(`/canary/tasks/${taskId}/rollback`);

/**
 * 恢复暂停的灰度任务
 * POST /canary/tasks/{task_id}/resume
 */
export const resumeCanary = (taskId: number): Promise<void> =>
  request.post(`/canary/tasks/${taskId}/resume`);