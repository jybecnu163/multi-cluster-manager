# 多集群容器管理平台
## 项目简介
本项目是一个基于 Kubernetes 的多集群容器管理平台，提供公司/部门/成员三级组织权限、多环境（生产/测试/开发）隔离访问，以及服务查看、灰度/批次发布、扩缩容、CI/CD 流水线审批等核心运维能力。

## 平台采用前后端分离架构：

后端：Java 21 + Spring Boot 3.2 + MyBatis-Plus + PostgreSQL + Redis + RabbitMQ

前端：React 18 + TypeScript + Vite + Ant Design

所有主键使用 BIGSERIAL（自增64位整数），移除外键约束，通过应用层维护关联关系。API 契约基于 OpenAPI 3.0，严格遵循 PRD v1.0 及补充说明 v1.1。

## 技术栈
层次	技术选型
前端框架	React 18 + TypeScript + Vite + Ant Design
后端框架	Java 21 + Spring Boot 3.2
持久层	MyBatis-Plus + PostgreSQL 15（JSONB 支持）
缓存	Redis 7 + Spring Data Redis
消息队列	RabbitMQ + Spring AMQP
安全	Spring Security 6 + JJWT (JWT) + TOTP (Google Auth)
Kubernetes 交互	Fabric8 Kubernetes Client
监控	Micrometer + Prometheus + metrics-server（降级）
分布式锁	Redisson
限流	Bucket4j
API 文档	OpenAPI 3.0（openapi.json）
服务发现	Nacos Client（只读）

## 快速开始
### 环境要求
后端

JDK 21+

Maven 3.8+

PostgreSQL 15+

Redis 7+

RabbitMQ 3.12+

（可选）Prometheus + metrics-server

前端

Node.js 18+

npm 9+ 或 yarn 1.22+

Docker（可选）

Docker Engine 20.10+

Docker Compose 2.0+

### 克隆项目
```bash
git clone <your-repo>
cd multi-cluster-manager
```
#### 后端启动
创建数据库并初始化表结构

```sql
CREATE DATABASE manager_db;
执行 src/main/resources/schema.sql（或使用架构设计文档中的完整建表语句）。
```

修改配置文件
编辑 src/main/resources/application.yml，设置数据库、Redis、RabbitMQ 连接信息及 JWT 密钥。

编译与运行

```bash
mvn clean compile
mvn spring-boot:run
```
应用默认监听 http://localhost:8080，所有 API 返回 501 Not Implemented（骨架阶段）。

#### 前端启动
进入前端目录（若前后端分离存放，请根据实际结构调整）

```bash
cd frontend
// 安装依赖

npm install
// 启动开发服务器

npm run dev
// 默认访问 http://localhost:3000，API 请求代理到后端 http://localhost:8080。
```

####    使用 Docker Compose 一键启动（含 PostgreSQL、Redis、RabbitMQ）
// 项目根目录提供 docker-compose.yml（需自行创建，内容示例见下文）。运行：

docker-compose up -d
// 示例 docker-compose.yml（请根据实际网络调整）：


```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: manager_user
      POSTGRES_PASSWORD: your_password
      POSTGRES_DB: manager_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
  redis:
    image: redis:7
    ports:
      - "6379:6379"
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
volumes:
  postgres_data:
 ```

## 项目结构
### 后端目录结构
```text
src/main/java/com/cloudplatform/manager/
├── ManageApplication.java          # 启动类
├── config/                         # 配置类（MyBatis-Plus、安全、WebSocket等）
├── controller/                     # REST控制器（所有API端点，返回501）
├── dto/                            # 请求/响应数据传输对象
├── exception/                      # 全局异常处理器
├── handler/                        # 自定义TypeHandler（如JsonbTypeHandler）
├── mapper/                         # MyBatis Mapper接口
├── model/entity/                   # 实体类（映射数据库表）
├── security/                       # 权限相关（PermissionEvaluator等）
├── service/                        # 业务逻辑接口及实现（空骨架）
├── util/                           # 工具类（JwtUtil、密码加密、TOTP等）
└── websocket/                      # WebSocket日志处理器
src/main/resources/
├── application.yml                 # 主配置文件
├── schema.sql                      # 建表SQL占位
└── mapper/                         # MyBatis XML映射文件
```

### 前端目录结构

```text
src/
├── api/               # API服务层（按模块拆分，含TypeScript类型）
├── components/        # 通用组件（AuthGuard、PageLoading等）
├── layouts/           # 布局组件（DashboardLayout）
├── pages/             # 页面组件（占位，显示“开发中”）
├── router/            # 路由配置（懒加载 + 受保护路由）
├── store/             # Zustand状态管理（认证状态）
├── hooks/             # 自定义Hook（useAuth）
├── utils/             # 工具函数（axios请求封装）
├── App.tsx            # 根组件
└── main.tsx           # 入口
```

## API 规范
基础路径：/api/v1

认证：Bearer JWT（除登录、2FA 设置外）

敏感操作二次验证：请求头 X-TOTP-Code

所有主键及外键字段类型：int64（JSON 中为数字）

### 统一错误响应：

```json
{ "code": "ERROR_CODE", "message": "错误描述" }
```

完整 API 定义见 openapi.json（与架构设计文档一致），或参考 接口规范文档。

主要模块路径：

模块	路径前缀
认证	/api/v1/auth
公司管理	/api/v1/companies
部门管理	/api/v1/departments
成员管理	/api/v1/users
集群管理	/api/v1/clusters
服务管理	/api/v1/services
手动扩缩容	/api/v1/services/{id}/scale
动态扩缩策略	/api/v1/autoscaling/policies
灰度发布	/api/v1/canary
批次发布	/api/v1/batch
流水线	/api/v1/pipelines
审批	/api/v1/approvals
审计日志	/api/v1/audit/logs
二次验证	/api/v1/2fa

##  配置说明
###  后端配置
主要配置文件：src/main/resources/application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/manager_db
    username: manager_user
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

app:
  jwt:
    secret: your-jwt-secret-key      # 至少256位
    expiration-ms: 28800000
  totp:
    issuer: MultiClusterManager
```

敏感信息建议使用环境变量或配置中心。

###  前端配置
环境变量文件：

.env.development：VITE_API_BASE_URL=/api/v1

.env.production：VITE_API_BASE_URL=https://api.manage-platform.com/api/v1

开发模式下，Vite 代理将 /api 转发到 http://localhost:8080。

##  开发约定
    契约先行：所有接口实现必须严格遵守 openapi.json，不得随意增减字段或修改路径。

    分层职责：Controller 只做参数校验和响应封装，业务逻辑放在 Service 层。

    权限控制：使用 Spring Security + @PreAuthorize + 自定义 PermissionEvaluator 实现环境级 RBAC。

    审计日志：敏感操作异步写入 audit_logs 表，并生成哈希链防篡改。

    异常处理：全局异常处理器返回标准错误格式。

    代码规范：后端遵循阿里巴巴 Java 规范，前端使用 ESLint + TypeScript 严格模式。

    数据库：无外键约束，关联查询在应用层通过二次查询实现。

##  部署与运维
###  Docker 镜像构建
```bash
// 后端
docker build -t multi-cluster-manager-backend:latest .

// 前端
cd frontend
npm run build
docker build -t multi-cluster-manager-frontend:latest .
```

###  高可用
    后端服务无状态：会话、JWT 黑名单、缓存统一存储在 Redis。

    定时任务（动态扩缩容、集群健康检查）使用 Redisson 分布式锁保证单实例执行。

    数据库使用主从或托管云数据库，RabbitMQ 部署集群。

###  监控与链路追踪
    后端暴露 /actuator/prometheus 端点，接入 Prometheus。

    每个请求自动生成 traceId（MDC），并传递到响应头 X-Trace-Id，便于日志串联。

##  当前状态与后续开发
###     已完成（骨架版本）
    ✅ 后端项目结构搭建，所有实体、Mapper、Controller、Service 空壳
    ✅ 前端项目结构搭建，所有 API 函数、路由、页面占位
    ✅ 数据库表结构 SQL（schema.sql）
    ✅ Docker 支持
    ✅ Spring Security 占位配置
    ✅ WebSocket 日志流占位实现
    ✅ JWT 认证与 TOTP 二次验证的具体实现
    ✅ 与 K8s API Server 的交互（fabric8 client）
    ✅ 前端页面真实 UI 与交互

###     待开发（由开发团队实现）
        各 Service 层的业务逻辑
        与 Nacos、Prometheus 的集成适配器
        灰度发布、批次发布、动态扩缩容的核心算法与流程
        审计日志哈希链计算与写入
        实时日志 WebSocket 客户端
        单元测试与集成测试

##  常见问题
    1. 后端启动时连接数据库失败？
    检查 application.yml 中的数据库 URL、用户名、密码是否正确，并且 PostgreSQL 服务已启动。

    2. 前端 npm run dev 后 API 请求 404？
    确保后端服务已启动在 8080 端口。若后端地址不同，请修改 vite.config.ts 中的代理目标。

    3. 如何注册本地 Docker Desktop Kubernetes 集群？
    API Endpoint：https://kubernetes.docker.internal:6443

    kubeconfig：~/.kube/config 或 %USERPROFILE%\.kube\config

    将其 Base64 编码后作为 kubeconfig 字段调用 POST /api/v1/clusters

    4. 如何验证所有接口已正确注册？
    后端启动后访问 http://localhost:8080/actuator/mappings 可查看所有注册的端点。

    5. 开发时如何绕过 JWT 认证？
    在 SecurityConfig 中临时放行所有路径（仅限开发环境）。

##  参考文档
    [产品需求规格说明书 v1.0](./PRD v1.0.md)
    PRD 补充说明 v1.1
    架构设计文档 v2.1-Java-MyBatis
    API 接口规范 v2.0.1
    后端 README（详细后端说明）
    前端 README（详细前端说明）