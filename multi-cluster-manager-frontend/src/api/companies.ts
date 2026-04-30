// src/api/companies.ts
import request from '../utils/request'
import { Company } from './types'
export const getCompanies = (): Promise<Company[]> => request.get('/companies')
export const createCompany = (data: { name: string }): Promise<Company> => request.post('/companies', data)
export const deleteCompany = (companyId: string): Promise<void> => request.delete(`/companies/${companyId}`)