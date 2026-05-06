# 多集群容器管理平台 - 后端服务
## 项目简介
本项目是一个基于 Kubernetes 的多集群容器管理平台的后端服务，采用 Java 21 + Spring Boot 3.2 构建。平台支持公司/部门/成员三级组织权限、多环境（生产/测试/开发）隔离访问，提供服务查看、灰度/批次发布、扩缩容、CI/CD 流水线审批等核心运维能力。

本项目严格遵循 PRD v1.0 及补充说明 v1.1，API 契约以 openapi.json 为准，所有接口实现均需与契约保持一致。

## 技术栈
层次	技术选型
后端框架	Java 21 + Spring Boot 3.2
持久层	Spring Data JPA (Hibernate) + PostgreSQL 15
缓存	Redis 7 + Spring Data Redis
消息队列	RabbitMQ + Spring AMQP
安全框架	Spring Security 6 + JJWT (JWT) + TOTP (Google Auth)
Kubernetes 交互	Fabric8 Kubernetes Client
动态扩缩容指标	Micrometer + Prometheus + metrics-server（降级）
分布式锁	Redisson
限流	Bucket4j
API 文档	OpenAPI 3.0 (已提供 openapi.json)
服务发现与配置	Nacos Client (只读)

## 快速开始
## 环境要求
    JDK 21+
    Maven 3.8+
    PostgreSQL 15+
    RabbitMQ 3.12+
    Redis 7+
    （可选）Prometheus + metrics-server

## 配置数据库与中间件
创建 PostgreSQL 数据库：
> docker exec -it postgres psql -U root -d manager_db
# psql -U root -d manager_db < schema.sql


```sql
CREATE DATABASE manager_db;
```

执行架构文档中提供的 SQL 脚本初始化表结构（位于 架构设计文档-v2.1-java.md 第 4 节）。

修改 src/main/resources/application.yml 中的数据库、Redis、RabbitMQ 连接信息。

## 编译与运行
```bash
# 克隆项目
git clone <your-repo>
cd multi-cluster-manager

# 编译（当前骨架阶段，仅验证语法）
mvn clean compile

# 运行
mvn spring-boot:run
```
应用启动后，后端服务监听 http://localhost:8080。

## 验证
```bash
curl -X GET http://localhost:8080/api/v1/companies -H "Authorization: Bearer <placeholder-token>"
```
注意：当前版本为骨架（skeleton），所有接口均返回 501 Not Implemented，仅用于验证项目结构和路由。

## 项目目录结构 tree /F
```text
├─main
│  ├─java
│  │  └─com
│  │      └─cloudplatform
│  │          └─manager
│  │              │  ManageApplication.java
│  │              │  
│  │              ├─config
│  │              │      AsyncConfig.java
│  │              │      CorsConfig.java
│  │              │      JwtAuthenticationFilter.java
│  │              │      MybatisPlusConfig.java
│  │              │      RabbitMQConfig.java
│  │              │      RateLimitingFilter.java
│  │              │      RedisConfig.java
│  │              │      RedissonConfig.java
│  │              │      SecurityConfig.java
│  │              │      WebSocketConfig.java
│  │              │      
│  │              ├─controller
│  │              │      ApprovalController.java
│  │              │      AuditController.java
│  │              │      AuthController.java
│  │              │      AutoscalingController.java
│  │              │      BaseController.java
│  │              │      BatchController.java
│  │              │      CanaryController.java
│  │              │      ClusterController.java
│  │              │      CompanyController.java
│  │              │      DepartmentController.java
│  │              │      PipelineController.java
│  │              │      RoleController.java
│  │              │      ScalingController.java
│  │              │      ServiceController.java
│  │              │      TwoFactorController.java
│  │              │      UserController.java
│  │              │      
│  │              ├─dto
│  │              │      CompanyRequest.java
│  │              │      DepartmentRequest.java
│  │              │      ErrorResponse.java
│  │              │      LoginRequest.java
│  │              │      LoginResponse.java
│  │              │      RoleAssignmentRequest.java
│  │              │      UserRequest.java
│  │              │      
│  │              ├─exception
│  │              │      GlobalExceptionHandler.java
│  │              │      
│  │              ├─handler
│  │              │      JsonbTypeHandler.java
│  │              │      
│  │              ├─mapper
│  │              │      ApprovalMapper.java
│  │              │      AuditLogMapper.java
│  │              │      AutoScalingPolicyMapper.java
│  │              │      ClusterMapper.java
│  │              │      CompanyMapper.java
│  │              │      DepartmentMapper.java
│  │              │      DepartmentSettingsMapper.java
│  │              │      DeploymentTaskMapper.java
│  │              │      PipelineMapper.java
│  │              │      PipelineRunMapper.java
│  │              │      RoleMapper.java
│  │              │      ServiceInstanceMapper.java
│  │              │      UserDepartmentMapper.java
│  │              │      UserMapper.java
│  │              │      UserRoleMapper.java
│  │              │      
│  │              ├─model
│  │              │  └─entity
│  │              │          Approval.java
│  │              │          AuditLog.java
│  │              │          AutoScalingPolicy.java
│  │              │          Cluster.java
│  │              │          Company.java
│  │              │          Department.java
│  │              │          DepartmentSettings.java
│  │              │          DeploymentTask.java
│  │              │          Pipeline.java
│  │              │          PipelineRun.java
│  │              │          Role.java
│  │              │          ServiceInstance.java
│  │              │          User.java
│  │              │          UserDepartment.java
│  │              │          UserRole.java
│  │              │          
│  │              ├─security
│  │              │      CurrentUserDetails.java
│  │              │      PermissionEvaluatorImpl.java
│  │              │      
│  │              ├─service
│  │              │  │  ApprovalService.java
│  │              │  │  AuditService.java
│  │              │  │  AuthService.java
│  │              │  │  AutoscalingService.java
│  │              │  │  BatchService.java
│  │              │  │  CanaryService.java
│  │              │  │  ClusterService.java
│  │              │  │  CompanyService.java
│  │              │  │  DepartmentService.java
│  │              │  │  PipelineService.java
│  │              │  │  RoleService.java
│  │              │  │  ScalingService.java
│  │              │  │  ServiceService.java
│  │              │  │  TwoFactorService.java
│  │              │  │  UserService.java
│  │              │  │  
│  │              │  └─impl
│  │              │          AuthServiceImpl.java
│  │              │          CompanyServiceImpl.java
│  │              │          DepartmentServiceImpl.java
│  │              │          RoleServiceImpl.java
│  │              │          UserServiceImpl.java
│  │              │          
│  │              ├─util
│  │              │      JwtUtil.java
│  │              │      PasswordEncoder.java
│  │              │      RedisUtil.java
│  │              │      TotpUtil.java
│  │              │      
│  │              └─websocket
│  │                      LogWebSocketHandler.java
│  │                      
│  └─resources
│      │  application.yml
│      │  schema.sql
│      │  
│      └─mapper
│              DeploymentTaskMapper.xml
│              
└─test
    └─java
        └─com
            └─cloudplatform
                └─manager
                    └─service
                        └─impl
                                AuthServiceImplTest.java
```

## API 契约
所有 API 定义见项目根目录下的 openapi.json（与架构设计文档一致）。

## 认证方式
使用 Bearer JWT：在请求头中携带 Authorization: Bearer <token>。

登录接口：POST /api/v1/auth/login 获取 token。

敏感操作需要二次验证 TOTP，在请求头中添加 X-TOTP-Code。

## 主要模块及路径前缀
模块	路径前缀
认证	/api/v1/auth
公司管理	/api/v1/companies
部门管理	/api/v1/departments
成员管理	/api/v1/users
角色管理	/api/v1/roles
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

## 配置说明
主要配置文件：src/main/resources/application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/manager_db
    username: manager_user
    password: your_password
  jpa:
    hibernate.ddl-auto: validate   # 生产环境使用 validate
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

app:
  jwt:
    secret: your-jwt-secret-key      # 至少 256 位
    expiration-ms: 28800000          # 8 小时
  totp:
    issuer: MultiClusterManager
```
敏感信息（如数据库密码、JWT 密钥）请勿提交到代码仓库，建议使用环境变量或配置中心。

## 开发约定
契约先行：所有接口实现必须严格遵守 openapi.json，不得随意增减字段或修改路径。

模块划分：Controller 只负责参数校验和响应封装，业务逻辑放在 Service 层。

权限控制：使用 Spring Security + @PreAuthorize + 自定义 PermissionEvaluator 实现环境级 RBAC。

审计日志：所有敏感操作（发布、扩缩容、权限变更）异步写入 audit_logs 表，并生成哈希链防篡改。

异常处理：全局异常处理器返回标准错误格式 { "code": "...", "message": "...", "traceId": "..." }。

测试：单元测试使用 JUnit 5 + Mockito，集成测试使用 @SpringBootTest 与 Testcontainers。

## 部署与运维
### Docker 构建
项目根目录已提供 Dockerfile，可构建镜像：

```bash
docker build -t multi-cluster-manager:latest .
docker run -p 8080:8080 multi-cluster-manager:latest
```

### 高可用
管理服务实例必须无状态：会话、JWT 黑名单、缓存统一存储在 Redis。

定时任务（动态扩缩容巡检、集群健康检查）使用 Redisson 分布式锁保证单实例执行。

数据库使用主从或托管云数据库，RabbitMQ 部署集群。

### 监控与链路追踪
暴露 Micrometer 指标端点 /actuator/prometheus，接入 Prometheus。

每个请求自动生成 traceId（通过 MDC），并传递到响应头 X-Trace-Id，便于日志串联。

## 当前状态与后续开发
    当前代码为 骨架版本（skeleton），已完成：
        项目结构搭建
        数据模型（JPA 实体）
        所有 REST 控制器（端点返回 501）
        安全配置、WebSocket 配置、异步配置等占位实现
        Dockerfile

    待开发内容（由后端开发工程师实现）：
        各 Service 层的业务逻辑
        JWT 认证与 TOTP 二次验证的具体实现
        与 K8s API Server 的交互（fabric8 client）
        与 Nacos、Prometheus 的集成适配器
        灰度发布、批次发布、动态扩缩容的核心算法与流程
        审计日志哈希链计算与写入
        单元测试与集成测试

## 常见问题
1. 编译报错找不到符号？
确保使用 JDK 21，并执行 mvn clean compile。若使用 Lombok，请确保 IDE 已安装 Lombok 插件。

2. 启动时连接数据库失败？
检查 application.yml 中的数据库 URL、用户名、密码是否正确，以及 PostgreSQL 服务是否已启动。

3. 如何验证所有接口已正确注册？
启动应用后，访问 http://localhost:8080/actuator/mappings（需在配置中开放）可查看所有注册的端点。

4. 开发时如何绕过 JWT 认证？
在 SecurityConfig 中临时放行所有路径（仅开发环境），生产环境绝对禁用。

## 参考文档
产品需求规格说明书 v1.0
PRD 补充说明 v1.1
架构设计文档 v2.1-Java
OpenAPI 规范

## 许可证
[请根据实际情况填写，例如 MIT / 私有]

如有疑问，请联系项目技术经理（TPM）。