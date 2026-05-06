import request from '../utils/request';
import { AuditLog, PaginatedResponse } from './types';

interface AuditLogQuery {
  start_time?: string;
  end_time?: string;
  operation?: string;
  page?: number;
  page_size?: number;
}

// 分页获取审计日志
export const getAuditLogs = (
  params: AuditLogQuery
): Promise<PaginatedResponse<AuditLog>> =>
  request.get('/audit/logs', { params });

// 导出全部日志为 CSV Blob（通过大 page_size 获取全量数据）
export const exportAuditLogs = async (
  params: Omit<AuditLogQuery, 'page' | 'page_size'>
): Promise<Blob> => {
  // 请求第一页，并设置足够大的 page_size 以获取全部记录
  const res = await request.get('/audit/logs', {
    params: { ...params, page: 1, page_size: 9999 },
  });
  const records: AuditLog[] = res.data;//.records;
  // 生成 CSV 字符串（带 BOM 防止中文乱码）
  const headers = [
    'ID', '用户ID', '操作', '目标类型', '目标ID', '请求IP', 'User-Agent', '详情', '前一哈希', '时间'
  ];
  const rows = records.map(log =>
    [
      log.id,
      log.user_id,
      log.operation,
      log.target_type,
      log.target_id,
      log.request_ip,
      log.user_agent,
      JSON.stringify(log.details),
      log.prev_hash,
      log.created_at,
    ].map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')
  );
  const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n');
  return new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
};