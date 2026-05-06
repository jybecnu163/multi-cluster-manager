# 多集群容器管理平台 - 前端服务
## 项目简介
本项目为多集群容器管理平台的前端应用，基于 React 18 + TypeScript + Vite + Ant Design 构建。平台提供公司/部门/成员三级组织管理、多环境（开发/测试/生产）服务查看、灰度/批次发布、扩缩容、CI/CD 流水线审批等运维操作的可视化界面。

前端代码严格遵循后端的 OpenAPI 契约，通过 API 服务层与后端交互，不包含任何业务逻辑，仅负责界面渲染、用户交互和安全凭证存储。

## 技术栈
类别	技术选型
框架	React 18 + TypeScript（严格模式）
构建工具	Vite 5
UI 组件库	Ant Design 5
路由	React Router 6
HTTP 客户端	Axios（封装在 src/utils/request.ts）
状态管理	Zustand（轻量级，仅用于认证状态）
代码规范	ESLint + TypeScript ESLint
开发服务器	默认端口 3000，支持 API 代理到后端 8080 端口

## 快速开始
### 环境要求
Node.js 18+

npm 9+ 或 yarn 1.22+

### 安装与运行
```bash
# 克隆项目
git clone <your-repo>
cd multi-cluster-manager-frontend

# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:3000）
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```
### 代理配置
开发环境下，Vite 会将 /api 前缀的请求代理到 http://localhost:8080（后端默认地址）。若后端地址不同，请修改 vite.config.ts 中的 proxy.target。

## 项目目录结构
```text
src/
├── api/                        # API 服务层（按模块拆分）
│   ├── types.ts                # 公共 TypeScript 类型（源自 OpenAPI）
│   ├── auth.ts                 # 认证相关（登录、注销、2FA）
│   ├── companies.ts            # 公司管理
│   ├── departments.ts          # 部门管理
│   ├── users.ts                # 成员管理
│   ├── roles.ts                # 角色管理
│   ├── clusters.ts             # 集群管理
│   ├── services.ts             # 服务查看、指标、报表导出
│   ├── scaling.ts              # 手动扩缩容
│   ├── autoscaling.ts          # 动态扩缩策略
│   ├── canary.ts               # 灰度发布
│   ├── batch.ts                # 批次发布
│   ├── pipelines.ts            # CI/CD 流水线
│   ├── approvals.ts            # 审批
│   └── audit.ts                # 审计日志
├── components/                 # 通用组件（空骨架）
│   ├── AuthGuard.tsx           # 路由守卫（未登录跳转）
│   ├── PageLoading.tsx         # 加载占位
│   └── ErrorBoundary.tsx       # 错误边界
├── layouts/                    # 布局组件
│   └── DashboardLayout.tsx     # 主布局（侧边栏+头部+内容区）
├── pages/                      # 页面组件（按功能拆分）
│   ├── Dashboard/              # 仪表盘占位
│   ├── CompanyList/            # 公司列表页
│   ├── DepartmentList/         # 部门列表页
│   ├── UserList/               # 成员列表页
│   ├── ClusterList/            # 集群列表页
│   ├── ServiceList/            # 服务列表页
│   ├── ServiceDetail/          # 服务详情页
│   ├── CanaryTasks/            # 灰度任务页
│   ├── BatchTasks/             # 批次任务页
│   ├── AutoscalingPolicies/    # 动态扩缩策略页
│   ├── Pipelines/              # 流水线管理页
│   ├── Approvals/              # 待审批页
│   └── AuditLogs/              # 审计日志页
├── router/                     # 路由配置
│   └── index.tsx               # 路由表（懒加载 + 受保护路由）
├── store/                      # 状态管理
│   └── useAuthStore.ts         # 认证状态（Zustand）
├── hooks/                      # 自定义 Hook
│   └── useAuth.ts              # 认证 Hook
├── utils/                      # 工具函数
│   └── request.ts              # Axios 实例（拦截器、JWT 注入）
├── App.tsx                     # 根组件（路由容器）
├── main.tsx                    # 入口文件
└── vite-env.d.ts               # Vite 类型声明
```

## 主要功能模块
模块	路径	说明
登录/注销	/login	获取 JWT，存储到 localStorage
仪表盘	/dashboard	概览信息占位
公司管理	/companies	增删公司
部门管理	/departments	增删部门，配置部门设置
成员管理	/users	创建成员，分配角色和环境权限
集群管理	/clusters	注册 K8s 集群，查看健康状态
服务管理	/services	服务列表+分页/搜索，服务详情
服务详情	/services/:id	资源趋势、实时日志、启动命令等
手动扩缩容	（在服务详情页操作）	修改副本数，生产环境可触发审批
动态扩缩策略	/autoscaling	配置基于 CPU/内存/QPS 的策略
灰度发布	/canary	创建灰度任务，推进阶段/回滚
批次发布	/batch	分批更新，自动/手动确认
流水线	/pipelines	定义流水线，触发部署
待审批	/approvals	查看/审批生产环境变更任务
审计日志	/audit	查询操作记录，支持链式校验

### API 服务层说明
所有后端接口调用均通过 src/api/*.ts 中的函数完成，每个函数：

使用统一的 request 实例（已配置 baseURL、拦截器）。

具有完整的 TypeScript 类型（参数、返回值）。

仅负责发送请求并返回 Promise，不处理业务逻辑。

示例：

```typescript
// src/api/companies.ts
import request from '../utils/request'
import { Company } from './types'

export const getCompanies = (): Promise<Company[]> => request.get('/companies')
```

## JWT 管理
登录成功后，后端返回 access_token，前端存储在 localStorage 中。

request.ts 的请求拦截器自动在 Authorization 头中添加 Bearer <token>。

若收到 401 响应，自动清除 token 并跳转到登录页。

## 路由与权限控制
    - 公开路由：/login（其他路由均需认证）。
    - 受保护路由：由 AuthGuard 组件包裹，检查 localStorage 中是否存在 access_token，若无则重定向到 /login。
    - 路由表：定义在 src/router/index.tsx，使用 lazy() 实现代码分割。
    - 菜单：DashboardLayout 中的侧边菜单与路由路径一一对应。

注意：前端仅做路由守卫，真正的权限校验在后端。前端可根据用户角色隐藏部分按钮，但敏感操作后端必须再次校验。

## 状态管理
使用 Zustand 管理全局认证状态，避免 prop drilling。

```typescript
// src/store/useAuthStore.ts
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('access_token'),
  setToken: (token) => { ... },
  setUser: (user) => { ... },
  logout: () => { ... }
}))
```

自定义 Hook useAuth 提供了简化的登录/登出方法。

## 样式与 UI 规范
全局样式重置：Ant Design 的 reset.css。

布局：响应式侧边栏 + 内容区，支持暗色/亮色主题（Ant Design 默认）。

所有页面组件统一使用 <Card> 包裹，并展示标题和开发中占位文本。

## 开发约定
- 目录与命名：
    页面组件文件夹使用 PascalCase（如 ServiceList）。
    API 函数文件名使用 camelCase（如 services.ts）。
    每个文件顶部必须有注释说明路径和职责（已通过代码生成脚本自动添加）。

- TypeScript 严格模式：
    禁止使用 any，除非与第三方库交互且无法推导。
    所有 API 响应必须定义类型（定义在 types.ts 中或按模块扩展）。

- 代码格式化：
    项目未强制 Prettier，但建议使用 ESLint 规则。
    提交前运行 npm run build 确保无 TS 错误。

- 环境变量：
    开发环境：.env.development 中配置 VITE_API_BASE_URL=/api/v1。
    生产环境：.env.production 中配置真实 API 地址。
    在代码中通过 import.meta.env.VITE_API_BASE_URL 访问。

- 与后端协作：
    API 变更必须同步更新 openapi.json 并重新生成类型。
    联调前确保后端服务已启动，且代理配置正确。

## 当前状态与后续开发
- 当前代码为 骨架版本（skeleton），已完成：
    项目初始化（Vite + React + TS + Ant Design）
    目录结构搭建
    所有 API 函数定义（占位实现，实际请求会发送到后端）
    完整的路由配置（含懒加载）
    布局组件（DashboardLayout）与侧边菜单
    所有页面占位组件（显示“该页面功能正在开发中...”）
    认证状态管理与路由守卫

- 待开发内容（由前端开发工程师实现）：
    各页面的具体 UI 和交互逻辑（表格、表单、图表等）
    与 API 函数对接，实现数据的真实获取和提交
    实时日志 WebSocket 连接与展示
    灰度发布、批次发布的流程可视化组件
    动态扩缩容策略的配置表单
    服务详情的资源趋势图表（ECharts 或 Ant Design Charts）
    表单验证、错误提示、加载状态等用户体验细节

## 常见问题
    - 1. 安装依赖失败？
    尝试删除 node_modules 和 package-lock.json，重新执行 npm install。

    确保 Node.js 版本 ≥ 18。

    - 2. 启动后访问 /api 接口 404？
    检查后端服务是否运行在 http://localhost:8080。

    若后端地址不同，修改 vite.config.ts 中的 proxy.target 并重启。

    - 3. 登录后 token 未携带到请求头？
    检查 src/utils/request.ts 中的拦截器是否正确从 localStorage 读取 access_token。

    确保登录成功后调用了 useAuthStore.getState().setToken(token)。

    - 4. 页面显示空白？
    打开浏览器控制台查看是否有报错（通常是路由匹配问题或组件导入错误）。

    确认所有页面组件文件存在且默认导出正确。

    - 5. 如何添加新的 API 函数？
    在 src/api/ 下对应的模块文件中添加导出函数，使用 request 实例发送请求。

若响应数据结构未定义，先在 src/api/types.ts 中添加类型。

## 部署与构建
### 开发环境
```bash
npm run dev   # 访问 http://localhost:3000
```
### 生产构建
```bash
npm run build   # 输出到 dist/ 目录
```
构建产物可部署到任何静态文件服务器（Nginx、CDN 等）。注意需要配置反向代理将 /api 请求指向后端服务。

### Nginx 配置示例
```nginx
server {
    listen 80;
    server_name example.com;
    root /path/to/dist;
    index index.html;

    location /api/ {
        proxy_pass http://backend-server:8080/api/;
        proxy_set_header Host $host;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 环境变量
生产环境下，需要在内置环境变量或构建时指定 VITE_API_BASE_URL：

```bash
VITE_API_BASE_URL=https://api.manage-platform.com/api/v1 npm run build
```

## 参考文档
产品需求规格说明书 v1.0
PRD 补充说明 v1.1
后端架构设计文档 (Java)
OpenAPI 规范

## 许可证
[请根据实际情况填写]

如有疑问，请联系项目技术经理（TPM）。