# 多集群容器管理平台 API 接口规范
版本：2.0.1
对应架构文档：架构设计文档-v2.1-java-mybatis.md
重要变更：所有主键 id 及关联字段 *_id 类型均为 整数（int64），不再是 UUID 字符串。路径参数中的 {id} 同样为整数。

## 通用说明
基础路径：/api/v1

## 认证方式：Bearer JWT（除登录、2FA 设置外所有接口需要）
请求头：Authorization: Bearer <access_token>

敏感操作二次验证：需要 TOTP 码，请求头 X-TOTP-Code

## 统一错误响应格式：

```json
{
"code": "ERROR_CODE",
"message": "错误描述"
}
```
成功响应：根据接口返回对应数据结构或空响应（204）。

## 1. 认证模块
###    1.1 用户登录
   路径：POST /auth/login

请求体：

字段	类型	必填	说明
email	string	是	用户邮箱，格式 email
password	string	是	密码，最小长度 8
响应体（200 OK）：

字段	类型	说明
access_token	string	JWT 令牌
token_type	string	固定 Bearer
expires_in	int	有效时间（秒）
### 1.2 用户注销
路径：POST /auth/logout

响应：204 No Content

### 1.3 获取 TOTP 绑定二维码
路径：POST /auth/2fa/setup

响应体（200 OK）：

字段	类型	说明
provisioning_uri	string	TOTP 配置 URI
qr_code_url	string	二维码图片 URL
## 2. 公司管理
###    2.1 获取公司列表
   路径：GET /companies

响应体：数组，元素为 Company

### 2.2 创建公司
路径：POST /companies

请求体：

字段	类型	必填	说明
name	string	是	公司名称
响应体（201 Created）：Company

### 2.3 删除公司
路径：DELETE /companies/{company_id}

路径参数：company_id (int64)

响应：204 No Content 或 409 Conflict（存在依赖资源）

## 3. 部门管理
###    3.1 获取部门列表
   路径：GET /departments

查询参数：company_id (int64, 可选)

响应体：数组，元素为 Department

### 3.2 创建部门
路径：POST /departments

请求体：

字段	类型	必填	说明
company_id	int64	是	所属公司ID
name	string	是	部门名称
director_user_id	int64	否	部门主管用户ID
响应体（201 Created）：Department

### 3.3 获取部门设置
路径：GET /departments/{department_id}/settings

响应体：DepartmentSettings

### 3.4 修改部门设置
路径：PATCH /departments/{department_id}/settings

请求体：DepartmentSettings（只包含要修改的字段）

响应体：修改后的 DepartmentSettings

## 4. 成员管理
###    4.1 获取成员列表
   路径：GET /users

查询参数：department_id (int64, 可选)

响应体：数组，元素为 User

### 4.2 创建成员
路径：POST /users

请求体：

字段	类型	必填	说明
name	string	是	姓名
email	string	是	邮箱，唯一
password	string	是	密码，最小长度 8
department_ids	int64[]	否	所属部门ID列表
primary_department_id	int64	否	主部门ID
响应体（201 Created）：User

### 4.3 分配角色
路径：PUT /users/{user_id}/roles

路径参数：user_id (int64)

请求体：

字段	类型	必填	说明
role_id	int	是	角色ID（1~5）
env_type	string	是	环境类型：dev/test/prod/all
department_id	int64	是	部门ID（角色作用域）
响应：200 OK

## 5. 角色权限
###   5.1 获取角色列表
   路径：GET /roles

响应体：数组，元素为 Role

## 6. 集群管理
###   6.1 获取集群列表
   路径：GET /clusters

响应体：数组，元素为 Cluster

### 6.2 注册集群
路径：POST /clusters

请求体：

字段	类型	必填	说明
name	string	是	集群名称，唯一
env_type	string	是	环境：dev/test/prod
api_endpoint	string	是	API Server 地址
kubeconfig	string	是	base64 编码的 kubeconfig 内容
响应体（201 Created）：Cluster

### 6.3 测试集群连通性
路径：GET /clusters/{cluster_id}/health

响应体：{ "status": "online" }

## 7. 服务管理
###    7.1 服务列表
   路径：GET /services

查询参数：

参数名	类型	必填	说明
department_id	int64	否	部门ID
env_type	string	否	环境：dev/test/prod
name	string	否	服务名模糊匹配
page	int	否	页码，默认 1
page_size	int	否	每页条数，默认 20
响应体：分页数据，列表项为 ServiceInstance（包含实时状态）

### 7.2 服务详情
路径：GET /services/{service_id}

响应体：ServiceDetail

### 7.3 获取服务资源使用趋势
路径：GET /services/{service_id}/metrics

查询参数：

参数名	类型	必填	说明
metric	string	是	指标类型：cpu 或 memory
range	string	否	时间范围：1h/6h/24h，默认 1h
响应体：MetricTimeSeries

### 7.4 实时日志（WebSocket）
路径：GET /services/{service_id}/logs

查询参数：

参数名	类型	必填	说明
pod_name	string	是	Pod 名称
container	string	否	容器名称
协议：升级为 WebSocket，流式推送日志行。

### 7.5 导出历史报表 CSV
路径：GET /services/{service_id}/reports/export

查询参数：

参数名	类型	必填	说明
time_range	string	是	聚合粒度：day/week/month
start_date	string	否	开始日期 YYYY-MM-DD
end_date	string	否	结束日期 YYYY-MM-DD
响应：200 OK，Content-Type: text/csv，文件下载。

## 8. 手动扩缩容
###    8.1 手动扩缩容
   路径：POST /services/{service_id}/scale

路径参数：service_id (int64)

请求体：ManualScaleRequest

响应体（202 Accepted）：

```json
{
"task_id": 12345,
"requires_approval": true
}
```

## 9. 动态扩缩策略
###    9.1 获取策略列表
   路径：GET /autoscaling/policies

响应体：数组，元素为 AutoScalingPolicy

### 9.2 创建或更新策略
路径：POST /autoscaling/policies

请求体：AutoScalingPolicy（id 字段存在时为更新）

响应体：保存后的策略对象

## 10. 灰度发布
### 10.1 创建灰度任务
    路径：POST /canary/tasks

请求体：CanaryTaskCreateRequest

响应体（201 Created）：CanaryTaskDetail

### 10.2 查询灰度任务详情
路径：GET /canary/tasks/{task_id}

响应体：CanaryTaskDetail

### 10.3 获取内部测试链接
路径：GET /canary/tasks/{task_id}/internal-endpoint

响应体：

```json
{
"internal_url": "https://...",
"expires_in_seconds": 3600
}
```

### 10.4 推进灰度阶段
路径：POST /canary/tasks/{task_id}/stage/{stage}

路径参数：stage 可选值：internal_tested, request_traffic, promote_25, promote_100

响应：200 OK

### 10.5 回滚灰度任务
路径：POST /canary/tasks/{task_id}/rollback

响应：200 OK

### 10.6 恢复暂停的灰度任务
路径：POST /canary/tasks/{task_id}/resume

响应：200 OK 或 400 BadRequest

## 11. 批次发布
### 11.1 创建批次任务
    路径：POST /batch/tasks

请求体：BatchTaskCreateRequest

响应体：BatchTaskDetail

### 11.2 查询批次任务详情
路径：GET /batch/tasks/{task_id}

响应体：BatchTaskDetail

### 11.3 手动执行下一批
路径：POST /batch/tasks/{task_id}/next

响应：200 OK

### 11.4 回滚批次发布
路径：POST /batch/tasks/{task_id}/rollback

响应：200 OK

### 11.5 恢复暂停的批次任务
路径：POST /batch/tasks/{task_id}/resume

响应：200 OK 或 400 BadRequest

## 12. CI/CD 流水线
### 12.1 获取流水线列表
    路径：GET /pipelines

响应体：数组，元素为流水线概要（无严格 schema）

### 12.2 创建流水线
路径：POST /pipelines

请求体：

```json
{
"name": "string",
"steps": [ ... ]
}
```
响应：201 Created

### 12.3 手动触发流水线
路径：POST /pipelines/{pipeline_id}/trigger

响应：202 Accepted

## 13. 审批
###     13.1 待审批任务列表
    路径：GET /approvals/pending

响应体：数组，元素为 ApprovalDetail

### 13.2 已审批历史
路径：GET /approvals/history

查询参数：page (int)

响应体：分页的 ApprovalDetail 数组

### 13.3 审批操作
路径：POST /approvals/{approval_id}

请求体：ApprovalRequest

响应：200 OK

## 14. 审计日志
    14.1 查询审计日志
    路径：GET /audit/logs

查询参数：

参数名	类型	必填	说明
start_time	string	否	ISO 时间起始
end_time	string	否	ISO 时间结束
operation	string	否	操作类型过滤
page	int	否	页码
响应体：分页的审计日志条目

## 附录：数据模型 Schemas（类型已更新为整数主键）
Company
字段	类型	说明
id	int64	公司ID（自增）
name	string	名称
created_at	string	ISO 时间
Department
字段	类型	说明
id	int64	部门ID
company_id	int64	所属公司ID
name	string	部门名称
director_user_id	int64	主管用户ID
DepartmentSettings
字段	类型	说明
department_id	int64	部门ID（关联）
allow_ops_bypass_prod_scale	boolean	是否允许运维免批生产
updated_at	string	ISO 时间
User
字段	类型	说明
id	int64	用户ID
name	string	姓名
email	string	邮箱
primary_department_id	int64	主部门ID
department_ids	int64[]	所有部门ID列表
Role
字段	类型	说明
id	int	角色ID（1~5）
name	string	系统管理员/部门主管/...
Cluster
字段	类型	说明
id	int64	集群ID
name	string	名称
env_type	string	dev/test/prod
api_endpoint	string	API Server 地址
status	string	online/offline/error
last_heartbeat	string	ISO 时间
ServiceInstance
字段	类型	说明
id	int64	服务实例ID
name	string	服务名
department_id	int64	所属部门
cluster_id	int64	所在集群
namespace	string	K8s 命名空间
workload_type	string	Deployment / StatefulSet
workload_name	string	工作负载名称
replicas	int	当前副本数
nacos_service_name	string	Nacos 注册名
nacos_health_status	string	registered/unknown/unregistered
ServiceDetail
包含 ServiceInstance 所有字段，额外增加：

字段	类型	说明
startup_command	string	启动命令
env_variables	object	环境变量键值对
cpu_request	string	CPU 请求量
memory_request	string	内存请求量
pods	Pod[]	Pod 列表
Pod 对象：

字段	类型	说明
name	string	Pod名称
status	string	状态
restart_count	int	重启次数
ip	string	IP地址
MetricTimeSeries
字段	类型	说明
timestamps	string[]	时间点数组
values	number[]	对应指标值数组
ManualScaleRequest
字段	类型	必填	说明
target_replicas	int	是	目标副本数
reason	string	是	操作原因
ignore_approval	boolean	否	是否跳过审批（需特定权限）
AutoScalingPolicy
字段	类型	必填	说明
id	int64	否	策略ID（更新时提供）
service_instance_id	int64	是	关联服务
enabled	boolean	否	是否启用
metric_type	string	是	cpu/memory/qps/custom
target_threshold	int	是	目标阈值（百分比或绝对值）
metric_query	string	条件	自定义 PromQL
fallback_source	string	否	prometheus / metrics_server
min_replicas	int	是	最小副本数
max_replicas	int	是	最大副本数
cooldown_seconds	int	否	冷却时间，默认 300
scale_down_delay_minutes	int	否	缩容延迟，默认 10
CanaryTaskCreateRequest
字段	类型	必填	说明
service_instance_id	int64	是	服务实例ID
target_image	string	是	新镜像地址
strategy	object	是	策略配置
strategy 字段：

字段	类型	默认	说明
canary_replicas	int	1	金丝雀实例数
auto_approve_traffic	boolean	false	是否自动审批流量接入
auto_promote_steps	int[]	[]	自动放大的流量百分比
rollback_on_error	boolean	true	出错是否自动回滚
CanaryTaskDetail
字段	类型	说明
id	int64	任务ID
service_instance_id	int64	服务实例ID
status	string	internal_test/waiting_approval/traffic_5/traffic_25/full/success/failed/paused/rolled_back
current_stage	int	0~6
target_image	string	目标镜像
canary_weight	int	当前金丝雀流量权重（0~100）
approval_id	int64	关联审批单ID（若有）
approval_timeout_hours	int	审批超时小时数
created_at	string	ISO 时间
BatchTaskCreateRequest
字段	类型	必填	说明
service_instance_id	int64	是	服务实例ID
target_image	string	是	新镜像地址
batch_config	object	是	分批配置
batch_config 字段：

字段	类型	必填	说明
batch_size_type	string	是	count/percentage
batch_value	int	是	每批实例数或百分比
interval_seconds	int	是	批次间隔秒数
require_confirmation	boolean	是	是否需要人工确认
statefulset_partition_mode	boolean	否	StatefulSet 分区模式
failure_condition	object	否	失败回滚条件
failure_condition：

字段	类型	默认	说明
min_ready_percent	int	80	就绪比例低于该值回滚
max_error_log_spike	number	2.0	错误日志增幅倍数
min_absolute_error_increment	int	10	绝对增量（条/分钟）
BatchTaskDetail
字段	类型	说明
id	int64	任务ID
status	string	状态
total_batches	int	总批次数
current_batch	int	当前批次索引
batch_statuses	array	每批状态详情
ApprovalRequest
字段	类型	必填	说明
action	string	是	approve / reject
comment	string	否	审批意见
ApprovalDetail
字段	类型	说明
id	int64	审批单ID
task_id	int64	关联任务ID
task_type	string	任务类型
requester_name	string	请求人姓名
action	string	最终审批动作
comment	string	审批意见
created_at	string	创建时间
expires_at	string	超时时间

### 版本历史：

2026-05-01 v2.0.1：所有主键及关联字段类型从 UUID 字符串改为 int64；路径参数 {id} 均为整数。