// src/api/auth.ts
import request from '../utils/request';
import { LoginRequest, LoginResponse } from './types';

// 登录
export const login = (data: LoginRequest): Promise<LoginResponse> => 
  request.post('/auth/login', data);

// 注销
export const logout = (): Promise<void> => 
  request.post('/auth/logout');

// 获取 TOTP 绑定二维码
export const setup2FA = (): Promise<{ provisioning_uri: string; qr_code_url: string }> => 
  request.post('/auth/2fa/setup');