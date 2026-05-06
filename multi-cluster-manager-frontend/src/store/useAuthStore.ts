// src/store/authStore.ts
import {create} from 'zustand';

// 简化的用户权限信息，真实场景应从 JWT 解析或通过 /users/me 接口获得
interface AuthUser {
    id: number;
    name: string;
    email: string;
    roles: string[]; // 角色名列表，如 ['系统管理员', '部门主管']
}

interface AuthState {
    user: AuthUser | null;
    token: string | null;
    isAuthenticated: boolean;

    setToken: (token: string | null) => void;
    setUser: (user: AuthUser | null) => void;
    loginSuccess: (token: string, user: AuthUser) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    user: null,
    token: localStorage.getItem('access_token'),
    isAuthenticated: !!localStorage.getItem('access_token'),

    setToken: (token) => {
        if (token) {
            localStorage.setItem('access_token', token);
        } else {
            localStorage.removeItem('access_token');
        }
        set({token, isAuthenticated: !!token});
    },

    setUser: (user) => set({user}),

    loginSuccess: (token, user) => {
        localStorage.setItem('access_token', token);
        set({token, isAuthenticated: true, user});
    },

    logout: () => {
        localStorage.removeItem('access_token');
        set({token: null, isAuthenticated: false, user: null});
    }
}));