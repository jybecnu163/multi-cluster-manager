import request from '../utils/request';
import { ServiceInstance, ServiceDetail, MetricTimeSeries } from './types';
// 服务详情
export const getServiceDetail = (serviceId: number): Promise<ServiceDetail> =>
  request.get(`/services/${serviceId}`);

// 资源使用趋势
export const getServiceMetrics = (
  serviceId: number,
  metric: 'cpu' | 'memory',
  range: '1h' | '6h' | '24h' = '1h'
): Promise<MetricTimeSeries> =>
  request.get(`/services/${serviceId}/metrics`, { params: { metric, range } });

// 导出历史报表 CSV
export const exportServiceReport = (
  serviceId: number,
  timeRange: 'day' | 'week' | 'month',
  startDate?: string,
  endDate?: string
): Promise<Blob> =>
  request.get(`/services/${serviceId}/reports/export`, {
    params: {
      time_range: timeRange,
      start_date: startDate,
      end_date: endDate,
    },
    responseType: 'blob',
  });

  // 手动扩缩容
export const manualScale = (
  serviceId: number,
  data: { target_replicas: number; reason: string; ignore_approval?: boolean }
): Promise<{ task_id: number; requires_approval: boolean }> =>
  request.post(`/services/${serviceId}/scale`, data);



export const getServices = async (params: any[]): Promise<{ records: ServiceInstance[]; total: number; current: number; size: number }> => {
    const res = await request.get('/services', { params }) as any; // 先断言为 any，再处理
    const data: any = res; // res 实际上是后端 JSON 数据
    return {
        records: data.items,
        total: data.total,
        current: data.page,
        size: data.page_size,
    };
};

// // 将后端分页格式转换为统一格式 (records, current, size)
// const toPaginated = <T>(res: PaginatedResponse<T>) => ({
//     records: res.items,
//     total: res.total,
//     current: res.page,
//     size: res.page_size,
// });

// export const getServices = (params: {
//     department_id?: number;
//     env_type?: string;
//     name?: string;
//     page?: number;
//     page_size?: number;
// }) =>
//     request
//         .get('/services', { params })
//         .then((res: PaginatedResponse<ServiceInstance>) => toPaginated(res));

// 服务列表（分页）
// export const getServices = (params: {
//   department_id?: number;
//   env_type?: string;
//   name?: string;
//   page?: number;
//   page_size?: number;
// }): Promise<PaginatedResponse<ServiceInstance>> =>
//   request.get('/services', { params });