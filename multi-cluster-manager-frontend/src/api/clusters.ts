// src/api/clusters.ts
import request from '../utils/request'
export const getClusters = () => request.get('/clusters')
export const registerCluster = (data: any) => request.post('/clusters', data)
export const getClusterHealth = (clusterId: string) => request.get(`/clusters/${clusterId}/health`)