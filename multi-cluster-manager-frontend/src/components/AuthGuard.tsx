import { Navigate, Outlet } from 'react-router-dom'
export const AuthGuard = () => { const token = localStorage.getItem('access_token'); return token ? <Outlet /> : <Navigate to="/login" replace /> }