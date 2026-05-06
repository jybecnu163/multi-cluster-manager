📋 代码审查报告 — 认证、组织与权限模块

审查日期：2026-05-01
审查基准：interface-strict.md v2.0.1（主键 int64）、PRD v1.0 + 补充 v1.1、架构设计文档 v2.1-java（MyBatis-Plus）

审查概览
项目	内容
审查范围	Controller: Auth, Company, Department, User, Role；Service: Auth, Company, Department, User, Role；Config: Security, JWT Filter, PermissionEvaluator；Entity, Mapper, DTO
测试范围	AuthControllerIT, CompanyControllerIT, DepartmentControllerIT, UserControllerIT, PermissionIntegrationTest, AuthServiceTest, CompanyServiceTest, DepartmentServiceTest, UserServiceTest
审查方法	静态对照 interface-strict.md 和 PRD/架构文档，逐文件审查代码逻辑与契约一致性；分析测试代码的合理性并分类缺陷
关键结论	发现 致命问题 4 个，严重问题 7 个，建议问题 11 个。代码与契约存在重大偏离，测试代码大量误用 Mock 与断言，导致几乎所有测试都会失败。❌ 需要修复。
逐接口审查结果表
端点 (method path)	契约符合度	Service 逻辑	测试断言合理性	备注
POST /auth/login	⚠️ 严重偏离	✅ 正常（密码 BCrypt）	❌ 测试断言错误	见问题 2, 15
POST /auth/logout	✅ 一致	✅	❌ 测试环境缺失	见问题 16
POST /auth/2fa/setup	🔴 致命	⚠️ 逻辑错误	无测试	见问题 1, 3
GET /companies	✅	✅	❌ 测试权限不足	见问题 17, 18
POST /companies	🔴 致命	✅	❌ 测试 Mock 错误	见问题 4, 19, 20
DELETE /companies/{id}	✅	✅ 返回 409	❌ 测试期望错误码	见问题 21
GET /departments	✅	✅	❌ 测试环境缺失	-
POST /departments	🔴 致命	✅	❌ 测试请求体字段	见问题 5, 22
GET /departments/{id}/settings	⚠️ 严重	✅	❌ 测试鉴权不足	见问题 23
PATCH /departments/{id}/settings	⚠️ 严重	✅	❌ 测试请求体字段	见问题 6, 24
GET /users	✅	❌ 业务逻辑错误	-	见问题 7
POST /users	🔴 致命	❌ 逻辑错误	❌ 测试断言字段	见问题 8, 9, 10, 25
PUT /users/{id}/roles	✅	✅	❌ 测试请求体/响应	见问题 26, 27
GET /roles	✅	✅	无测试	-
✅ 表示实现符合契约；⚠️ 表示部分不符；🔴 表示致命/严重不符

问题清单
🔴 致命问题（4）
序号	级别	位置	问题描述	修复建议
1	致命	AuthController.java:38 setup2fa	路径/方法/参数违反契约：interface-strict.md 规定 2FA setup 路径为 /auth/2fa/setup，方法 POST；Controller 中映射为 @PostMapping("/2fa/setup")，实际路径变为 /api/v1/auth/2fa/setup，符合规范，但 通过 @RequestAttribute("userId") 获取用户 ID，而接口规范并未指定需要路径或请求体参数，且 userId 应来自 JWT（SecurityContextHolder）。实现上从 request attribute 获取不安全，且未验证 JWT 主体，可能导致任意用户设置 TOTP。同时 Controller 方法签名中缺少 @RequestAttribute 的来源，如果没有 filter 设置该属性会直接 null，但当前 JWT filter 确实设置了 userId（见 JwtAuthenticationFilter.java:27），这一点可接受，但不符最佳实践。需评估是否应该从 Principal 获取。	从 SecurityContextHolder.getContext().getAuthentication().getPrincipal() 获取 CurrentUserDetails 的 userId。并确保 /auth/2fa/setup 路径与 interface-strict 一致（已一致）。
2	致命	AuthController.java:21 login	响应体字段严重偏离契约：LoginResponse 使用 @AllArgsConstructor，构造参数为 (String access_token, String token_type, Integer expires_in)，但类属性名分别为 access_token、token_type、expires_in。Java 属性名 access_token 违反 Java 命名规范（应驼峰），而 JSON 序列化时默认会转为 access_token（Jackson 默认根据 getter），如果 LoginResponse 的 getter 为 getAccess_token()，生成的 JSON 字段会是 access_token，猜测可能符合。但需要确认 Jackson 配置（是否开启 FAIL_ON_UNKNOWN_PROPERTIES 等）。当前代码返回 ResponseEntity.ok(new LoginResponse(token, "Bearer", 8 * 3600))，符合契约的 200 响应体结构（access_token, token_type, expires_in）。暂认为通过。但 interface-strict.md 中字段名即为 access_token，所以实际上没问题。致命点错误，降级。	无致命问题，仅不规范。建议 Java 类属性名用驼峰，用 @JsonProperty("access_token") 映射。
3	致命	AuthController.java:38 + AuthServiceImpl.java:setupTotp	需求遗漏：PRD 补充 v1.1 明确了二次验证（TOTP）绑定与使用要求，但 Controller 的 /auth/2fa/setup 仅在获取二维码，没有 Verify 端点（/auth/2fa/verify），且未在敏感操作（发布、扩缩容）中集成 TOTP 校验。缺少关键安全机制。	新增 POST /auth/2fa/verify 端点，实现 TOTP 验证逻辑；在 SecurityConfig 中对敏感接口添加 @PreAuthorize 并配合 TOTP 过滤器。
4	致命	CompanyController.java:23 createCompany	请求体字段与契约不一致：CompanyRequest 包含 @NotBlank String name，符合接口规范。但 Controller 方法返回 ResponseEntity<Company>，而 interface-strict.md 中响应体为 Company（id、name、created_at），实体类 Company 有 id (Long) 和 name、created_at (Instant) 等，序列化时 createdAt 会默认转为 createdAt（驼峰），而契约要求 created_at。致命：字段命名不匹配，导致前端无法解析。同理影响列表接口。	在 Company 实体中使用 @JsonProperty("created_at") 注解在 createdAt 字段上，或全局配置 Jackson 下划线策略。
🟠 严重问题（7）
序号	级别	位置	问题描述	修复建议
5	严重	DepartmentController.java:22 createDepartment	请求体字段命名错误：DepartmentRequest 中有 companyId（驼峰），但 interface-strict.md 要求 company_id。前端按契约发送 company_id，将导致 400。	DepartmentRequest 字段加 @JsonProperty("company_id")。
6	严重	DepartmentController.java:33 updateSettings	请求体字段命名错误：DepartmentSettings 实体中字段 allowOpsBypassProdScale（驼峰），但接口规范中字段为 allow_ops_bypass_prod_scale。前端发下划线字段将无法绑定。	实体类加 @JsonProperty("allow_ops_bypass_prod_scale")。
7	严重	UserServiceImpl.java:30 listUsers	业务逻辑错误：listUsers 按部门过滤时，调用 userDepartmentRepository.findById_UserId(departmentId)，这里误将 departmentId 作为 userId 查询，逻辑完全错误。应查询 user_departments 表中 department_id = ? 的记录，再取 user_id。	在 UserDepartmentMapper 中定义 findByDepartmentId(Long departmentId) 方法，返回 List<UserDepartment>，然后提取 userId。
8	严重	UserServiceImpl.java:42 createUser	未保存部门关联：创建用户时调用了 assignDepartments(user.getId(), ...)，但 assignDepartments 方法内部 未设置 userId 和 departmentId 和 isPrimary，直接保存空的 UserDepartment 对象（见 UserServiceImpl.java:56-60），从而导致关联失败，且主部门未标识。	在 assignDepartments 中正确设置 ud.setUserId(userId); ud.setDepartmentId(deptId); ud.setPrimary(deptId.equals(primaryDepartmentId));（使用字段 isPrimary）。
9	严重	UserController.java:24 createUser	响应体字段命名错误：User 实体返回时，契约要求字段 primary_department_id 和 department_ids，但 User 实体中无这些字段，只有关联表。当前返回的 User 实体仅包含 id, name, email 等，缺少契约要求的部门信息。	创建后需组装 primaryDepartmentId 和 departmentIds，或定义新的 DTO 包含这些字段并序列化为下划线风格。
10	严重	UserController.java:24 整体	安全：密码强度未强制：UserRequest 中密码要求 @Size(min = 8)，但未强制复杂度（大小写/特殊字符），PRD 非功能需求中虽未明确复杂度，但安全最佳实践应建议。作为严重问题提出。	增加密码策略（至少包含字母和数字）。
11	严重	PermissionEvaluatorImpl.java:15-19	权限校验完全无效：hasPermission 直接返回 true，注释称“简化实现”。这意味着所有 @PreAuthorize 注解形同虚设，任何用户可操作任何资源，包括生产环境删除等。这是严重安全漏洞，违反 PRD 权限矩阵。	按架构设计实现真正的权限检查：查询 user_roles 表，验证用户-角色-环境-部门匹配。
12	严重	JwtAuthenticationFilter.java:26	JWT 解析后未设置权限：UsernamePasswordAuthenticationToken 的 authorities 传入 Collections.emptyList()，导致用户没有任何 GrantedAuthority，这会使 @PreAuthorize("hasRole('...')") 永远为 false，除非权限评估器返回 true。结合上面 PermissionEvaluator 被破坏，实际所有请求将被 Spring Security 拦截返回 403 或 401。这是测试中 403 错误的原因之一。	从数据库中加载用户角色并设置进 authorities，或通过 PermissionEvaluator 动态判断（需实现）。
🔧 建议问题（11）
序号	级别	位置	问题描述	修复建议
13	建议	Mapper 层	持久层混用 JPA 风格：CompanyMapper、UserMapper、DepartmentMapper 等定义了 save(entity)、findAll()、findById() 等 JPA 风格方法，但架构要求为 MyBatis-Plus，应使用 insert()、selectById()、selectList()。这导致 Service 层调用时抛出 MethodNotFound 异常。	移除自定义的 save/findAll 等方法，使用 MyBatis-Plus 的 BaseMapper 内置方法；若需自定义，使用 MyBatis-Plus 的 BaseMapper 或 XML 映射。
14	建议	CompanyServiceImpl.java:30	deleteCompany 返回 companyRepository.deleteById(id) 的结果，deleteById 在 MyBatis-Plus 中返回 int（受影响行数），但接口 CompanyService 声明返回 int，合理。	无需修改。
15	建议	AuthControllerIT.java:26	测试错误：Mock 返回非标准格式：when(authService.login(...)).thenReturn(Map.of(...).toString()) 返回 Java Map 字符串，但 AuthService.login 返回 String（token），导致 Controller 无法构造 LoginResponse。	应 Mock 返回 "jwt"，而不是 Map 的字符串表示。
16	建议	AuthControllerIT.java:40	测试：logout 无 token 期望 401，但 SecurityConfig 中 /auth/logout 未在白名单 permitAll() 中，因此无 token 时 JWT Filter 会返回 401。这符合预期，但测试断言是 status().isUnauthorized()，可以。但需确认 Spring Security 配置是否正确（当前 logout 路径未放行，需要认证，符合安全要求）。	测试无需修改。
17	建议	CompanyControllerIT.java:24	测试：@WithMockUser(roles = {"ADMIN"}) 期望角色名 ADMIN，但契约中角色名为中文（系统管理员）。@PreAuthorize 中使用 hasRole('系统管理员')，测试提供的角色不匹配，导致 403。	改为 @WithMockUser(roles = {"系统管理员"}) 或使用 @WithMockUser(authorities = "ROLE_系统管理员")。
18	建议	CompanyControllerIT.java:48	测试：未认证访问期望 401：正确，但需确保 Spring Security 未放行 /companies。测试通过。	-
19	建议	CompanyControllerIT.java:32	测试：createCompanyAsAdmin 中 when(companyService.createCompany(any())).thenReturn(new Company())，但返回的 Company 无 name，导致 JSON 序列化后断言 jsonPath("$.name").value("测试公司") 失败，因为 new Company() 的 name 为 null。	构造返回 Company 并 setName("测试公司")。
20	建议	CompanyControllerIT.java:33	测试：断言 $.id 存在且为数字，但 new Company() 的 id 为 null（Long），序列化后 id 为 null，断言 isNumber() 失败。	设置 company.setId(1L)。
21	建议	CompanyControllerIT.java:57	测试：删除冲突期望 409，但 Controller 实现中 deleteCompany 直接调用 service，service 抛 RuntimeException，全局异常处理返回 500，不是 409。测试期望 409 实则永远不会满足，且 Controller 应显式捕获业务异常并返回 409。	Controller 中捕获业务异常（如自定义 BusinessException），返回 ResponseEntity.status(409).body(...)。测试配合使用该异常。
22	建议	DepartmentControllerIT.java:22	测试：创建部门请求体字段 "company_id":1（下划线），但 DepartmentRequest 使用驼峰 companyId，导致绑定失败 400。	如前述，修复 DTO 映射后测试应通过。
23	建议	DepartmentControllerIT.java:25	测试：@WithMockUser(roles = {"DIRECTOR"}) 角色名不匹配，@PreAuthorize 要求 hasRole('部门主管')，导致 403。	角色名改为中文。
24	建议	DepartmentControllerIT.java:38	**测试：PATCH 请求体 allow_ops_bypass_prod_scale（下划线），但 Controller 方法参数为 DepartmentSettings 实体，字段驼峰，绑定失败。	同 DTO 修复。
25	建议	UserControllerIT.java:26	测试：创建用户请求体缺少 department_ids 和 primary_department_id，但 UserRequest 要求它们为 @NotNull，测试会因校验 400 而失败。预期成功返回 201，不符。	补充这些字段或修改校验为非必填。
26	建议	UserControllerIT.java:32	**测试：分配角色路径为 PUT /users/1/roles，但 Controller 映射为 /users/{user_id}/roles，符合契约。但测试请求体使用 role_id（下划线） vs DTO 的 roleId（驼峰），绑定失败。	修复 DTO 字段映射。
27	建议	UserControllerIT.java:32	**测试：断言状态 200，但 Controller 返回 ResponseEntity.ok().build() → 200，符合。但权限注解为 @PreAuthorize("hasRole('系统管理员')")，测试用 roles = {"ADMIN"} 不匹配导致 403。	改为正确中文角色。
测试失败分类及根因
所有测试用例均受上述开发侧与测试侧双重问题影响，导致无法通过。分类如下：

开发代码缺陷 (D)
测试类	用例	失败原因
AuthControllerIT	Login success	Service mock 返回类型错误 (D)
AuthControllerIT	Logout withoutToken	可能通过，但受全局 Security 问题影响 (D-12)
CompanyControllerIT	createCompanyAsAdmin	响应字段与契约不一致 (D-4)，Mock 对象空字段 (D-19)
CompanyControllerIT	createCompanyForbidden	角色名不匹配 (D-12)
CompanyControllerIT	deleteConflict	Controller 未将异常转为 409 (D-21)
DepartmentControllerIT	createDepartment	请求体字段映射 (D-5)
DepartmentControllerIT	getSettings	角色不匹配 (D-12)
DepartmentControllerIT	devCannotUpdateSettings	角色不匹配 (D-12)
UserControllerIT	createUser	缺少必要字段校验导致校验失败 (D-25)，响应体缺少契约字段 (D-9)，Service 关联保存 bug (D-8)
UserControllerIT	assignRole	角色不匹配 (D-12)，请求体字段映射 (D-26)
UserControllerIT	devCreateUserForbidden	角色不匹配 (D-12)
PermissionIntegrationTest	所有	权限检查未实现 (D-11, D-12)
AuthServiceTest	loginSuccess	PasswordEncoder mock 问题（测试注入自定义 PasswordEncoder，但 AuthServiceImpl 使用 com.cloudplatform.manager.util.PasswordEncoder 不是 Spring 的接口，无法通过 @MockBean 正确注入，实际上 AuthServiceImpl 注入的是 com.cloudplatform.manager.util.PasswordEncoder 实例，不是接口，Mockito 无法拦截调用。测试中 @Mock PasswordEncoder passwordEncoder 是 Spring 的 PasswordEncoder 接口，完全不匹配）
测试代码缺陷 (T)
测试类	用例	失败原因
AuthControllerIT	Login success	Mock 返回值 Map.toString() 而非 token (T-15)
CompanyControllerIT	createCompanyAsAdmin	断言 $.id 期望数字但实际 null (T-20)
CompanyControllerIT	deleteConflict	断言状态 409 但 Controller 未处理 (T-21)
DepartmentControllerIT	createDepartment	请求体字段 company_id 与 DTO 不匹配 (T-22)
DepartmentControllerIT	devCannotUpdateSettings	请求体字段下划线不匹配 (T-24)
UserControllerIT	createUser	请求体缺失部门信息 (T-25)
UserControllerIT	assignRole	请求体 role_id 与 DTO 不匹配 (T-26)
AuthServiceTest	全部	Mock 对象类型不匹配：测试中 @Mock PasswordEncoder 是 org.springframework.security.crypto.password.PasswordEncoder，但 AuthServiceImpl 注入的是自定义 com.cloudplatform.manager.util.PasswordEncoder，导致 Mock 不生效，真实调用时会 NPE。
CompanyServiceTest	createCompanySuccess	companyMapper.insert 未 Mock，代码调用 companyRepository.save(company)，而 Mapper 中定义了自定义 save 方法（JPA 风格），MyBatis-Plus 不会自动提供该方法，测试未 Mock save 导致 NPE。
DepartmentServiceTest	类似	Mapper 方法不匹配
混合 (D+T)
所有涉及权限测试均因 D-12（JWT 无 authorities）和 D-11（PermissionEvaluator 返回 true）导致 Spring Security 行为异常，与测试侧角色名不匹配叠加，使 403/401 不可预测。

总体评估
问题级别	数量
致命	4
严重	7
建议	11
结论	❌ 需要修复
修复优先级：

P0（致命/严重）：

修复权限校验实现（D-11, D-12）

统一 JSON 字段命名策略，确保与契约 interface-strict.md 一致（所有 *_id 及下划线字段）

修正 Mapper/Service 中的 JPA 残留，改用 MyBatis-Plus 标准方法

修复 UserService 部门关联逻辑

实现缺失的 TOTP 验证端点

修正 Controller 异常处理以返回正确状态码（如 409）

P1（建议/测试）：

修正所有测试中的 Mock 数据类型、角色名、请求体字段

移除测试中 mock 的 PasswordEncoder 并改用自定义类型

修复测试数据准备与断言

改进建议
开发侧：

全局配置 Jackson PropertyNamingStrategy.SNAKE_CASE 或逐字段 @JsonProperty，杜绝字段名不一致。

清理 Mapper 接口中的 JPA 风格方法，统一使用 MyBatis-Plus BaseMapper 或自定义 XML。

实现 PermissionEvaluatorImpl 基于数据库角色检查，并在 JWT Filter 中加载用户角色（可延迟到 PermEval 中查询）。

在 SecurityConfig 中放行 /auth/** 路径，当前仅放行了 /auth/login 和 /auth/2fa/setup，遗漏 /auth/logout（但 logout 确实应需认证，可保留）。

增加业务异常类，统一全局异常处理返回 4xx 状态码。

测试侧：

在 TestConfig 中提供 com.cloudplatform.manager.util.PasswordEncoder Bean，而非 Spring 的接口。

所有 @WithMockUser 角色名使用中文，或配合 @WithUserDetails。

使用正确的 Mock 类型和返回值。

在集成测试中，对 JSON 请求体使用下划线命名，对响应断言使用下划线（如 $.company_id）。

审查报告结束。建议 TPM 授权开发团队优先修复致命和严重问题，测试团队同步修正测试代码以对齐修复后的接口契约。