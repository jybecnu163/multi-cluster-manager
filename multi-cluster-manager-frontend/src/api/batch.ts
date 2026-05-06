import request from '../utils/request';

// 失败回滚条件
export interface FailureCondition {
  min_ready_percent?: number;
  max_error_log_spike?: number;
  min_absolute_error_increment?: number;
}

// 分批配置
export interface BatchConfig {
  batch_size_type: 'count' | 'percentage';
  batch_value: number;
  interval_seconds: number;
  require_confirmation: boolean;
  statefulset_partition_mode?: boolean;
  failure_condition?: FailureCondition;
}

// 创建请求
export interface BatchTaskCreateRequest {
  service_instance_id: number;
  target_image: string;
  batch_config: BatchConfig;
}

// 任务详情
export interface BatchTaskDetail {
  id: number;
  status: string;
  total_batches: number;
  current_batch: number;
  batch_statuses: Array<{
    batch_index?: number;
    status?: string;
    message?: string;
    [key: string]: any;
  }>;
  // 以下字段假设后端可能返回，便于前端获取确认模式（如未返回则始终弹出确认）
  batch_config?: BatchConfig;
}

// 创建批次任务
export const createBatchTask = (data: BatchTaskCreateRequest): Promise<BatchTaskDetail> =>
  request.post('/batch/tasks', data);

// 查询任务详情
export const getBatchTask = (taskId: number): Promise<BatchTaskDetail> =>
  request.get(`/batch/tasks/${taskId}`);

// 执行下一批
export const nextBatch = (taskId: number): Promise<void> =>
  request.post(`/batch/tasks/${taskId}/next`);

// 回滚
export const rollbackBatch = (taskId: number): Promise<void> =>
  request.post(`/batch/tasks/${taskId}/rollback`);

// 恢复暂停的任务
export const resumeBatch = (taskId: number): Promise<void> =>
  request.post(`/batch/tasks/${taskId}/resume`);

// 暂停（后端若未实现，调用会失败，前端将提示）
export const pauseBatch = (taskId: number): Promise<void> =>
  request.post(`/batch/tasks/${taskId}/pause`);