// src/api/roles.ts
import request from '../utils/request'
export const getRoles = () => request.get('/roles')