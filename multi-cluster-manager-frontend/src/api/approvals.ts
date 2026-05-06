import request from '../utils/request';

export interface ApprovalDetail {
  id: number;
  task_id: number;
  task_type: string;
  requester_name: string;
  action: string;
  comment: string;
  created_at: string;
  expires_at: string;       // 审批超时时间
}

export interface ApprovalRequest {
  action: 'approve' | 'reject';
  comment?: string;
}

// 获取待审批列表
export const getPendingApprovals = (): Promise<ApprovalDetail[]> =>
  request.get('/approvals/pending');

// 获取已审批历史（分页参数通过 query 传递 page）
export const getApprovalHistory = (page: number = 1): Promise<ApprovalDetail[]> =>
  request.get('/approvals/history', { params: { page } });

// 审批操作
export const handleApproval = (approvalId: number, data: ApprovalRequest): Promise<void> =>
  request.post(`/approvals/${approvalId}`, data);