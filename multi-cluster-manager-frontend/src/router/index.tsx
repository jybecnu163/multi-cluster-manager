import { lazy, Suspense } from 'react'
import { PageLoading } from '../components/PageLoading'
const Dashboard = lazy(() => import('../pages/Dashboard'))
const CompanyList = lazy(() => import('../pages/CompanyList'))
const DepartmentList = lazy(() => import('../pages/DepartmentList'))
const UserList = lazy(() => import('../pages/UserList'))
const ClusterList = lazy(() => import('../pages/ClusterList'))
const ServiceList = lazy(() => import('../pages/ServiceList'))
const ServiceDetail = lazy(() => import('../pages/ServiceDetail'))
const CanaryTasks = lazy(() => import('../pages/CanaryTasks'))
const BatchTasks = lazy(() => import('../pages/BatchTasks'))
const AutoscalingPolicies = lazy(() => import('../pages/AutoscalingPolicies'))
const Pipelines = lazy(() => import('../pages/Pipelines'))
const Approvals = lazy(() => import('../pages/Approvals'))
const AuditLogs = lazy(() => import('../pages/AuditLogs'))
const withSuspense = (Comp: React.LazyExoticComponent<() => JSX.Element>) => <Suspense fallback={<PageLoading />}><Comp /></Suspense>
export const routerConfig = [
  { path: '/dashboard', element: withSuspense(Dashboard) },
  { path: '/companies', element: withSuspense(CompanyList) },
  { path: '/departments', element: withSuspense(DepartmentList) },
  { path: '/users', element: withSuspense(UserList) },
  { path: '/clusters', element: withSuspense(ClusterList) },
  { path: '/services', element: withSuspense(ServiceList) },
  { path: '/services/:id', element: withSuspense(ServiceDetail) },
  { path: '/canary', element: withSuspense(CanaryTasks) },
  { path: '/batch', element: withSuspense(BatchTasks) },
  { path: '/autoscaling', element: withSuspense(AutoscalingPolicies) },
  { path: '/pipelines', element: withSuspense(Pipelines) },
  { path: '/approvals', element: withSuspense(Approvals) },
  { path: '/audit', element: withSuspense(AuditLogs) }
]