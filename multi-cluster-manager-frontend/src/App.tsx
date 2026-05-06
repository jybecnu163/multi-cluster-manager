// src/App.tsx
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import {ConfigProvider} from 'antd';
import zhCN from 'antd/locale/zh_CN';
import {AuthGuard} from './components/AuthGuard';
import DashboardLayout from './layouts/DashboardLayout';
import {routerConfig} from './router';
import Login from './pages/Login';

function App() {
    return (
        <ConfigProvider locale={zhCN}>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<Login/>}/>
                    <Route element={<AuthGuard/>}>
                        <Route element={<DashboardLayout/>}>
                            {routerConfig.map((route) => (
                                <Route key={route.path} path={route.path} element={route.element}/>
                            ))}
                        </Route>
                    </Route>
                    <Route path="*" element={<Navigate to="/dashboard" replace/>}/>
                </Routes>
            </BrowserRouter>
        </ConfigProvider>
    );
}

export default App;