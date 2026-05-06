import request from '../utils/request';

export interface AutoScalingPolicy {
  id?: number;                  // 创建时不传，更新时必传
  service_instance_id: number;
  enabled?: boolean;
  metric_type: 'cpu' | 'memory' | 'qps' | 'custom';
  target_threshold: number;
  metric_query?: string;       // metric_type='custom' 时必填
  fallback_source?: 'prometheus' | 'metrics_server';
  min_replicas: number;
  max_replicas: number;
  cooldown_seconds?: number;
  scale_down_delay_minutes?: number;
}

// 获取所有策略
export const getPolicies = (): Promise<AutoScalingPolicy[]> =>
  request.get('/autoscaling/policies');

// 创建或更新策略（id 存在时更新）
export const createOrUpdatePolicy = (policy: AutoScalingPolicy): Promise<AutoScalingPolicy> =>
  request.post('/autoscaling/policies', policy);