// src/api/departments.ts
import request from '../utils/request';
import { Company, Department, DepartmentSettings } from './types';


export const getCompanies =  ():Promise<Company>=> request.get('/companies', { params: {  } });

export const getDepartments = (companyId?: number) => 
  request.get('/departments', { params: { company_id: companyId } });

export const createDepartment = (data: { 
  company_id: number; 
  name: string; 
  director_user_id?: number 
}): Promise<Department> => 
  request.post('/departments', data);

// 新增：部门设置
export const getDepartmentSettings = (departmentId: number): Promise<DepartmentSettings> => 
  request.get(`/departments/${departmentId}/settings`);

export const updateDepartmentSettings = (
  departmentId: number, 
  settings: Partial<DepartmentSettings> // 只需包含要修改的字段
): Promise<DepartmentSettings> => 
  request.patch(`/departments/${departmentId}/settings`, settings);