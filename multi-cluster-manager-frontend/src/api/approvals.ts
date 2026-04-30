// src/api/approvals.ts
import request from '../utils/request'
export const getPendingApprovals = () => request.get('/approvals/pending')
export const getApprovalHistory = (page: number = 1) => request.get('/approvals/history', { params: { page } })
export const handleApproval = (approvalId: string, action: string, comment?: string) => request.post(`/approvals/${approvalId}`, { action, comment })