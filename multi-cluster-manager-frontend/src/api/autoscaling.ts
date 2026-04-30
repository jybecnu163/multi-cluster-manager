// src/api/autoscaling.ts
import request from '../utils/request'
export const getPolicies = () => request.get('/autoscaling/policies')
export const createOrUpdatePolicy = (data: any) => request.post('/autoscaling/policies', data)