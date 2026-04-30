// src/api/users.ts
import request from '../utils/request'
import { User } from './types'
export const getUsers = (departmentId?: string): Promise<User[]> => request.get('/users', { params: { department_id: departmentId } })
export const createUser = (data: any): Promise<User> => request.post('/users', data)
export const assignRole = (userId: string, roleAssignment: any): Promise<void> => request.put(`/users/${userId}/roles`, roleAssignment)