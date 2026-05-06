// src/api/types.ts
// 对齐 interface-strict.md v2.0.1: 所有主键及关联字段均为 int64 整数

export interface ApiResponse<T = any> { 
  code?: string; 
  message?: string; 
  data?: T 
}
// 分页响应
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  page_size: number;
}
// --- 组织架构 ---
export interface Company {
  id: number;  // int64
  name: string;
  created_at: string;
}

export interface Department {
  id: number;
  company_id: number;
  name: string;
  director_user_id: number | null;
}

export interface DepartmentSettings {
  department_id: number;
  allow_ops_bypass_prod_scale: boolean;
  updated_at: string;
}

// --- 用户与角色 ---
export interface User {
  id: number;
  name: string;
  email: string;
  primary_department_id: number | null;
  department_ids: number[];
  // 角色信息可能在关联后获取，这里定义基础字段
}

export interface Role {
  id: number; // 1~5
  name: string;
  // env_permission_mask?: number; // 根据架构文档，角色表有此字段，但 openapi 未暴露，前端不强行使用
}

// 分配给用户的角色与环境的关联
export interface UserRoleAssignment {
  role_id: number;
  env_type: 'dev' | 'test' | 'prod' | 'all';
  department_id: number;
}

// --- 认证 ---
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}

// --- 服务相关 ---
export interface ServiceInstance {
  id: number;
  name: string;
  department_id: number;
  cluster_id: number;
  namespace: string;
  workload_type: 'Deployment' | 'StatefulSet';
  workload_name: string;
  replicas: number;
  nacos_service_name: string;
  nacos_health_status: 'registered' | 'unknown' | 'unregistered';
}

export interface PodInfo {
  name: string;
  status: string;
  restart_count: number;
  ip: string;
}

export interface ServiceDetail extends ServiceInstance {
  startup_command: string;
  env_variables: Record<string, string>;
  cpu_request: string;
  memory_request: string;
  pods: PodInfo[];
}

export interface MetricTimeSeries {
  timestamps: string[];
  values: number[];
}

// --- 审批（用于菜单徽标等） ---
export interface ApprovalDetail {
  id: number;
  task_id: number;
  task_type: string;
  requester_name: string;
  action: string;
  comment: string;
  created_at: string;
  expires_at: string;
}

// 审计日志条目
export interface AuditLog {
  id: number;
  user_id: number;
  operation: string;
  target_type: string;
  target_id: number;
  request_ip: string;
  user_agent: string;
  details: any;          // JSONB 对象
  prev_hash: string;
  created_at: string;
}