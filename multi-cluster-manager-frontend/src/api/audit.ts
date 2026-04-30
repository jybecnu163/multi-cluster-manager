// src/api/audit.ts
import request from '../utils/request'
export const getAuditLogs = (params: any) => request.get('/audit/logs', { params })