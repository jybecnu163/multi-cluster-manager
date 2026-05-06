import {lazy, Suspense} from 'react';
import {PageLoading} from '../components/PageLoading';
import {useAuthStore} from '../store/useAuthStore';
import {Button, Result} from 'antd';

// ========== 懒加载导入 ==========
const Dashboard = lazy(() => import('../pages/Dashboard'));
const CompanyList = lazy(() => import('../pages/CompanyList'));
const DepartmentList = lazy(() => import('../pages/DepartmentList'));
const UserList = lazy(() => import('../pages/UserList'));
const ClusterList = lazy(() => import('../pages/ClusterList'));
const ServiceList = lazy(() => import('../pages/ServiceList'));
const ServiceDetail = lazy(() => import('../pages/ServiceDetail'));
const ServiceLogs = lazy(() => import('../pages/ServiceLogs'));
const CanaryTasks = lazy(() => import('../pages/CanaryTasks'));
const CanaryTaskCreate = lazy(() => import('../pages/CanaryTaskCreate'));
const CanaryTaskDetail = lazy(() => import('../pages/CanaryTaskDetail'));
const BatchTasks = lazy(() => import('../pages/BatchTasks'));
const BatchTaskCreate = lazy(() => import('../pages/BatchTaskCreate'));
const BatchTaskDetail = lazy(() => import('../pages/BatchTaskDetail'));
const AutoscalingPolicies = lazy(() => import('../pages/AutoscalingPolicies'));
const Pipelines = lazy(() => import('../pages/Pipelines'));
const PipelineDetail = lazy(() => import('../pages/PipelineDetail'));
const Approvals = lazy(() => import('../pages/Approvals'));
const AuditLogs = lazy(() => import('../pages/AuditLogs'));

// ========== 通用工具 ==========
const withSuspense = (Comp: React.LazyExoticComponent<() => JSX.Element>) => (
    <Suspense fallback={<PageLoading/>}>
        <Comp/>
    </Suspense>
);

// // ========== 认证守卫（已在 App.tsx 外层使用，此处保留用于嵌套） ==========
// const RequireAuth = ({ children }: { children: JSX.Element }) => {
//     const { isAuthenticated } = useAuthStore();
//     const location = useLocation();
//     if (!isAuthenticated) {
//         return <Navigate to="/login" state={{ from: location }} replace />;
//     }
//     return children;
// };

// ========== 权限守卫 ==========
const RequireRole = ({allowedRoles, children}: { allowedRoles: string[]; children: JSX.Element }) => {
    const {user} = useAuthStore();
    if (!user || !user.roles.some((role) => allowedRoles.includes(role))) {
        return (
            <Result
                status="403"
                title="403"
                subTitle="抱歉，你没有权限访问此页面。"
                extra={
                    <Button type="primary" onClick={() => (window.location.href = '/dashboard')}>
                        回到仪表盘
                    </Button>
                }
            />
        );
    }
    return children;
};

// ========== 路由表（仅包含需要 DashboardLayout 的子路由） ==========
export const routerConfig = [
    {path: '/dashboard', element: withSuspense(Dashboard)},
    {
        path: '/companies',
        element: (
            <RequireRole allowedRoles={['系统管理员']}>
                {withSuspense(CompanyList)}
            </RequireRole>
        ),
    },
    {path: '/departments', element: withSuspense(DepartmentList)},
    {path: '/users', element: withSuspense(UserList)},
    {path: '/clusters', element: withSuspense(ClusterList)},
    {path: '/services', element: withSuspense(ServiceList)},
    {path: '/services/:id', element: withSuspense(ServiceDetail)},
    {path: '/services/:id/logs', element: withSuspense(ServiceLogs)},
    {path: '/canary', element: withSuspense(CanaryTasks)},
    {path: '/canary/create', element: withSuspense(CanaryTaskCreate)},
    { path: '/canary/:taskId', element: withSuspense(CanaryTaskDetail) },
    {path: '/batch', element: withSuspense(BatchTasks)},
    {path: '/batch/create', element: withSuspense(BatchTaskCreate)},
    {path: '/batch/:taskId', element: withSuspense(BatchTaskDetail)},
    {path: '/autoscaling', element: withSuspense(AutoscalingPolicies)},
    {path: '/pipelines', element: withSuspense(Pipelines)},
    {path: '/pipelines/:id', element: withSuspense(PipelineDetail)},
    {path: '/approvals', element: withSuspense(Approvals)},
    {path: '/audit', element: withSuspense(AuditLogs)},
];