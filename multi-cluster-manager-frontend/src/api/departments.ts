// src/api/departments.ts
import request from '../utils/request'
export const getDepartments = (companyId?: string) => request.get('/departments', { params: { company_id: companyId } })
export const createDepartment = (data: any) => request.post('/departments', data)
export const getDepartmentSettings = (departmentId: string) => request.get(`/departments/${departmentId}/settings`)
export const updateDepartmentSettings = (departmentId: string, settings: any) => request.patch(`/departments/${departmentId}/settings`, settings)