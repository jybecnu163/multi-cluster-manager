import { create } from 'zustand'
interface AuthState { user: any | null; token: string | null; setToken: (token: string | null) => void; setUser: (user: any) => void; logout: () => void }
export const useAuthStore = create<AuthState>((set) => ({
  user: null, token: localStorage.getItem('access_token'),
  setToken: (token) => { if (token) localStorage.setItem('access_token', token); else localStorage.removeItem('access_token'); set({ token }) },
  setUser: (user) => set({ user }),
  logout: () => { localStorage.removeItem('access_token'); set({ token: null, user: null }) }
}))