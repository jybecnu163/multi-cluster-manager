// src/api/auth.ts
import request from '../utils/request'
import { LoginRequest, LoginResponse } from './types'
export const login = (data: LoginRequest): Promise<LoginResponse> => request.post('/auth/login', data)
export const logout = (): Promise<void> => request.post('/auth/logout')
export const setup2FA = (): Promise<{ provisioning_uri: string; qr_code_url: string }> => request.post('/auth/2fa/setup')