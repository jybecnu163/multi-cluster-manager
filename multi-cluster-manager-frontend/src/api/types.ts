export interface ApiResponse<T = any> { code?: string; message?: string; data?: T }
export interface Company { id: string; name: string; created_at: string }
export interface User { id: string; name: string; email: string }
export interface LoginRequest { email: string; password: string }
export interface LoginResponse { access_token: string; token_type: string; expires_in: number }