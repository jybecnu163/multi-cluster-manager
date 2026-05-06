import request from '../utils/request';

// 类型（在 types.ts 中统一，此处为引用）
export interface Cluster {
  id: number;
  name: string;
  env_type: 'dev' | 'test' | 'prod';
  api_endpoint: string;
  status: 'online' | 'offline' | 'error';
  last_heartbeat: string;
}

export interface ClusterRegisterRequest {
  name: string;
  env_type: string;
  api_endpoint: string;
  kubeconfig: string; // base64 编码的 kubeconfig 内容
}

export interface ClusterUpdateRequest {
  name?: string;
  env_type?: string;
  api_endpoint?: string;
  kubeconfig?: string; // base64 encoded
}

export const getClusters = (): Promise<Cluster[]> => request.get('/clusters');

export const registerCluster = (data: ClusterRegisterRequest): Promise<Cluster> =>
  request.post('/clusters', data);

export const getClusterHealth = (clusterId: number): Promise<{ status: 'online' }> =>
  request.get(`/clusters/${clusterId}/health`);

// 更新集群信息 (假设后端实现)
export const updateCluster = (clusterId: number, data: ClusterUpdateRequest): Promise<Cluster> =>
  request.patch(`/clusters/${clusterId}`, data);

// 删除集群 (假设后端实现)
export const deleteCluster = (clusterId: number): Promise<void> =>
  request.delete(`/clusters/${clusterId}`);
