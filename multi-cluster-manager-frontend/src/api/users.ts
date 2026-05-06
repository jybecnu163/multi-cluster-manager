// src/api/users.ts
import request from '../utils/request';
import { User, UserRoleAssignment } from './types';

export const getUsers = (departmentId?: number): Promise<User[]> => 
  request.get('/users', { params: { department_id: departmentId } });

export const createUser = (data: {
  name: string;
  email: string;
  password: string;
  department_ids?: number[];
  primary_department_id?: number;
}): Promise<User> => 
  request.post('/users', data);

// 新增：分配角色
export const assignRole = (userId: number, roleAssignment: UserRoleAssignment): Promise<void> => 
  request.put(`/users/${userId}/roles`, roleAssignment);