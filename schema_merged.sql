-- ============================================================
-- 脚本名: schema_merged.sql
-- 描述: 多集群容器管理平台 - 数据库完整初始化脚本(合并版)
-- 包含所有表结构、约束、索引及完整字段注释
-- 版本要求: PostgreSQL 15+
-- 幂等性: 所有语句支持重复执行(IF NOT EXISTS)
-- ============================================================

BEGIN;

-- 1. 公司表
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，公司唯一标识
    name VARCHAR(128) NOT NULL UNIQUE,                   -- 公司名称，全局唯一
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),         -- 创建时间
    updated_at TIMESTAMP                                 -- 更新时间
);
COMMENT ON TABLE companies IS '公司表，顶层组织单元';

-- 2. 部门表 (逻辑外键暂不建立物理约束)
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，部门唯一标识
    company_id BIGINT NOT NULL,                          -- 所属公司ID，逻辑外键关联companies.id
    name VARCHAR(128) NOT NULL,                          -- 部门名称，同一公司内唯一
    director_user_id BIGINT,                             -- 部门主管用户ID，逻辑外键关联users.id
    UNIQUE(company_id, name)
);
COMMENT ON TABLE departments IS '部门表，隶属于公司';

-- 3. 部门设置表
CREATE TABLE IF NOT EXISTS department_settings (
    department_id BIGINT PRIMARY KEY,                    -- 部门ID，主键，逻辑外键关联departments.id
    allow_ops_bypass_prod_scale BOOLEAN DEFAULT FALSE,   -- 是否允许运维工程师在生产环境免审批手动扩缩容
    updated_at TIMESTAMP                                 -- 更新时间
);
COMMENT ON TABLE department_settings IS '部门设置表，存储部门级开关配置';

-- 4. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，用户唯一标识
    name VARCHAR(64) NOT NULL,                           -- 用户姓名
    email VARCHAR(255) NOT NULL UNIQUE,                  -- 邮箱，用于登录和通知，全局唯一
    password_hash VARCHAR(255) NOT NULL,                 -- 密码哈希值（BCrypt或PBKDF2）
    totp_secret VARCHAR(255),                            -- TOTP二次验证密钥（加密存储）
    totp_enabled BOOLEAN DEFAULT FALSE,                  -- 是否已开启TOTP验证
    created_at TIMESTAMP NOT NULL                        -- 创建时间
);
COMMENT ON TABLE users IS '用户表，平台账户';

-- 5. 用户部门关联表
CREATE TABLE IF NOT EXISTS user_departments (
    user_id BIGINT NOT NULL,                             -- 用户ID，逻辑外键关联users.id
    department_id BIGINT NOT NULL,                       -- 部门ID，逻辑外键关联departments.id
    is_primary BOOLEAN DEFAULT FALSE,                    -- 是否为主部门，用户所属主要归属部门
    PRIMARY KEY (user_id, department_id)
);
COMMENT ON TABLE user_departments IS '用户-部门关联表，支持多部门';

-- 6. 角色表
CREATE TABLE IF NOT EXISTS roles (
    id SMALLINT PRIMARY KEY,                             -- 角色ID（1-5）
    name VARCHAR(32) NOT NULL UNIQUE,                    -- 角色名称：系统管理员/部门主管/运维工程师/开发工程师/审计员
    env_permission_mask INT                              -- 环境权限掩码（位掩码），如1=开发，2=测试，4=生产，NULL表示无限制
);
COMMENT ON TABLE roles IS '角色表，预置5种固定角色';

-- 插入基础角色数据（幂等）
INSERT INTO roles (id, name, env_permission_mask) VALUES
    (1, '系统管理员', NULL),
    (2, '部门主管', NULL),
    (3, '运维工程师', NULL),
    (4, '开发工程师', NULL),
    (5, '审计员', NULL)
ON CONFLICT (id) DO NOTHING;

-- 7. 用户角色分配表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,                             -- 用户ID，逻辑外键关联users.id
    role_id SMALLINT NOT NULL,                           -- 角色ID，逻辑外键关联roles.id
    env_type VARCHAR(20),                                -- 环境类型：dev, test, prod, all
    department_id BIGINT NOT NULL,                       -- 角色生效的部门ID，逻辑外键关联departments.id
    PRIMARY KEY (user_id, role_id, env_type, department_id)
);
COMMENT ON TABLE user_roles IS '用户角色分配表，支持不同环境不同角色';

-- 8. K8s 集群表
CREATE TABLE IF NOT EXISTS clusters (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，集群唯一标识
    name VARCHAR(64) NOT NULL UNIQUE,                    -- 集群名称，全局唯一
    env_type VARCHAR(20) NOT NULL CHECK (env_type IN ('dev','test','prod')), -- 环境类型：dev, test, prod
    api_endpoint VARCHAR(255) NOT NULL,                  -- 集群API Server地址，如 https://192.168.1.1:6443
    kubeconfig_encrypted TEXT NOT NULL,                  -- 加密后的kubeconfig内容（AES-256-GCM）
    ca_cert_encrypted TEXT,                              -- 加密后的CA证书（可选，部分kubeconfig内嵌）
    token_encrypted TEXT,                                -- 加密后的Token（可选，与证书二选一）
    status VARCHAR(20) DEFAULT 'offline',                -- 集群连通状态：online, offline, error
    last_heartbeat TIMESTAMP,                            -- 最后一次成功心跳时间
    created_at TIMESTAMP NOT NULL                        -- 创建时间
);
COMMENT ON TABLE clusters IS 'Kubernetes集群注册表';

-- 9. 服务实例表
CREATE TABLE IF NOT EXISTS service_instances (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，服务实例唯一标识
    name VARCHAR(128) NOT NULL,                          -- 服务名称（如order-service）
    department_id BIGINT NOT NULL,                       -- 归属部门ID，逻辑外键关联departments.id，用于审批链路
    cluster_id BIGINT NOT NULL,                          -- 所在集群ID，逻辑外键关联clusters.id
    namespace VARCHAR(64) NOT NULL,                      -- K8s命名空间
    workload_type VARCHAR(20) NOT NULL,                  -- 工作负载类型：Deployment 或 StatefulSet
    workload_name VARCHAR(128) NOT NULL,                 -- K8s资源名称
    replicas INT DEFAULT 1,                              -- 当前副本数（实时值）
    nacos_service_name VARCHAR(128),                     -- 在Nacos中注册的服务名（若与name不同）
    nacos_health_status VARCHAR(20) DEFAULT 'unknown',   -- Nacos注册健康状态：registered, unknown, unregistered
    env_type VARCHAR(20) NOT NULL,                       -- 环境类型：dev, test, prod（冗余，便于查询）
    UNIQUE(name, cluster_id),
    CONSTRAINT fk_service_instances_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE RESTRICT
);
COMMENT ON TABLE service_instances IS '服务实例表，一个逻辑服务在一个环境中的具体K8s工作负载';

-- 服务实例索引
CREATE INDEX IF NOT EXISTS idx_services_dept ON service_instances(department_id);
CREATE INDEX IF NOT EXISTS idx_services_cluster ON service_instances(cluster_id);
CREATE INDEX IF NOT EXISTS idx_service_instances_env_type ON service_instances(env_type);

-- 10. 部署任务表
CREATE TABLE IF NOT EXISTS deployment_tasks (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，任务唯一标识
    service_instance_id BIGINT NOT NULL,                 -- 关联的服务实例ID
    task_type VARCHAR(20) NOT NULL,                      -- 任务类型：canary, batch, scale
    status VARCHAR(20) NOT NULL,                         -- 任务状态：internal_test, waiting_approval, traffic_5, traffic_25, full, success, failed, paused, rolled_back 等
    current_stage INT,                                   -- 当前阶段（灰度阶段0-6或批次序号）
    strategy_json VARCHAR(4096),                         -- 策略配置JSON，如灰度批次参数、扩缩容目标等
    target_image VARCHAR(255),                           -- 目标镜像（用于发布）
    created_by BIGINT,                                   -- 创建人用户ID
    approved_by BIGINT,                                  -- 最终审批人用户ID
    approval_time TIMESTAMP,                             -- 审批通过时间
    approval_timeout_hours INT DEFAULT 24,               -- 审批超时小时数，默认24
    created_at TIMESTAMP NOT NULL,                       -- 任务创建时间
    completed_at TIMESTAMP                               -- 任务完成时间（成功/失败/回滚）
);
COMMENT ON TABLE deployment_tasks IS '部署任务表，统一记录灰度发布、批次发布、手动扩缩容等流程任务';

CREATE INDEX IF NOT EXISTS idx_tasks_service ON deployment_tasks(service_instance_id, status);

-- 11. 审批记录表
CREATE TABLE IF NOT EXISTS approvals (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，审批单唯一标识
    task_id BIGINT NOT NULL,                             -- 关联的部署任务ID
    approver_id BIGINT,                                  -- 审批人用户ID
    action VARCHAR(20) NOT NULL,                         -- 审批动作：approve, reject, transfer
    comment TEXT,                                        -- 审批备注
    status VARCHAR(20) DEFAULT 'pending',                -- 审批单状态：pending, approved, rejected, expired
    expires_at TIMESTAMP,                                -- 审批截止时间，超时自动拒绝
    created_at TIMESTAMP NOT NULL                        -- 审批单创建时间
);
COMMENT ON TABLE approvals IS '审批记录表，存储每个审批环节的详细信息';

CREATE INDEX IF NOT EXISTS idx_approvals_status ON approvals(status);
CREATE INDEX IF NOT EXISTS idx_approvals_expires_at ON approvals(expires_at) WHERE status = 'pending';

-- 12. 动态扩缩策略表
CREATE TABLE IF NOT EXISTS auto_scaling_policies (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，策略唯一标识
    service_instance_id BIGINT NOT NULL,                 -- 关联的服务实例ID
    enabled BOOLEAN DEFAULT TRUE,                        -- 是否启用该策略
    metric_type VARCHAR(20) NOT NULL,                    -- 指标类型：cpu, memory, qps, custom
    target_threshold INT NOT NULL,                       -- 目标阈值（例如CPU使用率百分比75）
    metric_query TEXT,                                   -- 自定义PromQL查询语句，当metric_type=custom时必填
    fallback_source VARCHAR(20) DEFAULT 'prometheus',    -- 降级指标源：prometheus, metrics_server
    min_replicas INT NOT NULL,                           -- 最小副本数
    max_replicas INT NOT NULL,                           -- 最大副本数
    cooldown_seconds INT DEFAULT 300,                    -- 冷却时间（秒），两次扩缩操作最小间隔
    scale_down_delay_minutes INT DEFAULT 10,             -- 缩容所需低负载持续分钟数（默认为10分钟）
    created_at TIMESTAMP NOT NULL,                       -- 策略创建时间
    updated_at TIMESTAMP                                 -- 策略更新时间
);
COMMENT ON TABLE auto_scaling_policies IS '动态扩缩容策略表，定义基于指标的自动扩缩规则';

-- 13. 审计日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，日志ID
    user_id BIGINT,                                      -- 操作用户ID
    operation VARCHAR(255) NOT NULL,                     -- 操作描述，如 POST /services/123/scale
    target_type VARCHAR(64),                             -- 操作目标类型，如 ServiceInstance, DeploymentTask
    target_id BIGINT,                                    -- 操作目标ID
    request_ip INET,                                     -- 请求IP地址
    user_agent TEXT,                                     -- 请求的User-Agent
    details VARCHAR(4096),                                       -- 操作详情JSON，存储请求参数等
    prev_hash VARCHAR(64),                               -- 前一条日志的SHA256哈希，用于链式校验
    created_at TIMESTAMP NOT NULL DEFAULT NOW()          -- 日志创建时间
);
COMMENT ON TABLE audit_logs IS '审计日志表，只追加，防篡改链式哈希';

CREATE INDEX IF NOT EXISTS idx_audit_user_time ON audit_logs(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_prev_hash ON audit_logs(prev_hash) WHERE prev_hash IS NOT NULL;

-- 14. 流水线定义表
CREATE TABLE IF NOT EXISTS pipelines (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，流水线ID
    name VARCHAR(128) NOT NULL,                          -- 流水线名称
    steps VARCHAR(4096),                                -- 步骤定义JSON，包含代码拉取、构建、部署等
    trigger_type VARCHAR(20),                            -- 触发方式：manual, webhook
    approval_timeout_hours INT DEFAULT 24,               -- 生产环境审批超时小时数，默认24
    created_by BIGINT,                                   -- 创建人用户ID
    created_at TIMESTAMP NOT NULL                        -- 创建时间
);
COMMENT ON TABLE pipelines IS '流水线定义表，存储CI/CD流水线配置';

-- 15. 流水线执行记录表
CREATE TABLE IF NOT EXISTS pipeline_runs (
    id BIGSERIAL PRIMARY KEY,                            -- 自增主键，执行记录ID
    pipeline_id BIGINT,                                  -- 关联的流水线定义ID
    status VARCHAR(20),                                  -- 执行状态：pending, running, success, failed, approved, rejected
    input_params VARCHAR(4096),                                  -- 输入参数JSON，如目标镜像、环境等
    approval_needed BOOLEAN,                             -- 是否需要审批（生产环境部署）
    approved_by BIGINT,                                  -- 审批人用户ID
    started_at TIMESTAMP,                                -- 开始执行时间
    finished_at TIMESTAMP                                -- 结束时间
);
COMMENT ON TABLE pipeline_runs IS '流水线执行记录表，记录每次运行状态';

COMMIT;