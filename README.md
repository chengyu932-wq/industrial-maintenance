# 基于 Spring Boot 的工业设备运维管理系统

本项目面向制造业设备运维管理场景，采用 B/S 架构和前后端分离模式，围绕设备全生命周期和维修工单闭环，提供设备台账、故障报修、维修工单、预防性维护、备件库存、智能派单、SLA、知识库、KPI 统计、权限控制及系统支撑能力。

## 当前阶段

已进入第 12 阶段最终验收。系统已形成认证与 RBAC、组织与设备、故障维修工单、备件库存、预防性维护、智能派单与 SLA、知识沉淀与相似工单推荐、六项 KPI 与 ECharts 看板闭环，并补充用户管理/导入、运行小时登记、设备/工单附件、工单 PDF、AOP 审计和 MySQL 备份恢复。最终完成度与未完成项以 [第12阶段最终验收报告](docs/第12阶段最终验收报告.md) 为准；当前本机验收通过，但尚缺第二台全新主机部署证据，因此不标记为最终冻结版。

## 技术栈

### 后端

- JDK 17
- Spring Boot 3.5.16
- Maven 3.9.11 Wrapper
- Spring Web、Validation、Security
- MyBatis-Plus 3.5.17
- MySQL Connector/J（由 Spring Boot 依赖管理）
- Spring Data Redis
- JJWT 0.12.6
- Lombok、Spring AOP、Actuator、Spring Test

### 前端

- Vue 3.5.42
- Vite 7.3.6
- Vue Router 4.6.4
- Pinia 3.0.4
- Axios 1.20.0
- Element Plus 2.14.5
- ECharts 6.1.0
- npm 10.8.2

## 项目结构

```text
industrial-maintenance/
├─ backend/      Spring Boot 后端
├─ frontend/     Vue 3 前端
├─ sql/          数据库初始化入口
├─ docs/         项目文档、测试与验收证据
├─ scripts/      备份恢复与性能 Smoke Test
├─ .gitignore
└─ README.md
```

## 开发环境

- JDK 17
- Node.js 20.19.5 或兼容版本
- npm 10.x
- MySQL 8.0
- Redis

数据库默认名称为 `industrial_maintenance`。当前骨架允许在 MySQL、Redis 尚未启动时启动应用；开发环境默认关闭数据库和 Redis 的 Actuator 健康探测，接入基础设施后可通过环境变量开启。

## 后端启动

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

默认端口：`8080`。

可用环境变量：

```text
SERVER_PORT
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
DB_HEALTH_ENABLED
REDIS_HEALTH_ENABLED
CORS_ALLOWED_ORIGINS
JWT_SECRET（必填，至少 32 字节）
ACCESS_TOKEN_TTL
REFRESH_TOKEN_TTL
CAPTCHA_TTL
LOGIN_FAILURE_WINDOW
LOGIN_LOCK_TTL
MAX_LOGIN_FAILURES
PUBLIC_BASE_URL（二维码中的前端公开地址）
```

健康检查：

```text
GET http://localhost:8080/api/health
GET http://localhost:8080/actuator/health
```

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

默认端口：`5173`。开发服务器把 `/api` 请求代理到 `http://localhost:8080`。

## 数据库初始化

使用 MySQL 8.0 执行：

```text
sql/00_create_database.sql
```

按 [sql/README.md](sql/README.md) 中的顺序执行全部正式脚本。认证联调时可额外执行 `sql/11_dev_auth_seed.sql`、`sql/12_stage4_permissions.sql`、`sql/13_stage4_demo_data.sql`、`sql/14_stage5_permissions.sql`、`sql/15_stage7_permissions.sql` 和 `sql/16_stage7_demo_data.sql`；五种角色测试账号统一使用仅供开发演示的密码 `DevOnly@123`。

## 最终验证

```powershell
cd backend
.\mvnw.cmd '-Dstage4.db-tests=true' '-Dstage5.db-tests=true' '-Dstage6.db-tests=true' '-Dstage7.db-tests=true' '-Dstage8.db-tests=true' '-Dstage9.db-tests=true' '-Dstage10.db-tests=true' '-Dstage11.db-tests=true' '-Dstage12.db-tests=true' test
.\mvnw.cmd -DskipTests package

cd ..\frontend
npm run build
npm run test:e2e
```

真实数据库测试使用被 Git 忽略的 `application-local.yml`。Playwright 还需通过环境变量提供 Redis CLI 路径和仅供开发演示的账号密码。不要把本机凭据、Token 或备份文件提交到 Git。

备份恢复与定时任务配置见 [scripts/README.md](scripts/README.md)，SQL 从零初始化顺序见 [sql/README.md](sql/README.md)。最终资料入口包括：[任务书完成矩阵](docs/任务书功能完成矩阵.md)、[最终测试报告](docs/最终测试报告.md)、[论文事实基线](docs/论文事实基线.md)、[答辩事实基线](docs/答辩事实基线.md)和[截图映射](docs/论文截图映射.md)。

真实数据库测试使用被 Git 忽略的 `application-local.yml` 提供本机凭据，不应提交该文件。浏览器联调截图保存在 `docs/evidence/`。

## 第 7 阶段验证

```powershell
cd backend
.\mvnw.cmd '-Dstage4.db-tests=true' '-Dstage5.db-tests=true' '-Dstage7.db-tests=true' test

cd ..\frontend
npm run build
```

第 7 阶段的库存一致性设计、API、38 项新增测试与浏览器证据见 [第7阶段实现与测试报告](docs/第7阶段实现与测试报告.md)。

## 第 11 阶段验证

```powershell
cd backend
.\mvnw.cmd '-Dstage4.db-tests=true' '-Dstage5.db-tests=true' '-Dstage7.db-tests=true' '-Dstage8.db-tests=true' '-Dstage9.db-tests=true' '-Dstage10.db-tests=true' '-Dstage11.db-tests=true' test

cd ..\frontend
npm run build
npx playwright test tests/e2e/stage11.spec.js
```

统计接口为 `GET /api/statistics/overview`、`GET /api/statistics/kpis` 和 `GET /api/statistics/charts`，统一支持近 7/30/90 天与自定义日期。完整公式、数据权限、测试结果和截图见 [第11阶段实现与测试报告](docs/第11阶段实现与测试报告.md)。

## 认证说明

- Access Token 默认 30 分钟，Refresh Token 默认 7 天；刷新接口采用 Refresh Token 轮换，旧 Token 立即从 Redis 删除。
- 验证码默认 2 分钟且每次校验后删除；验证码错误不计入密码失败次数。
- 连续 5 次账号密码错误后锁定 10 分钟；计数与锁均有 TTL。
- 退出登录会将 Access Token 按剩余有效期加入黑名单，并删除关联 Refresh Token。
- 为便于本科项目演示，前端把双 Token 保存到 `localStorage`，刷新页面后可恢复登录。该方案存在 XSS 读取风险，生产系统应结合严格 CSP、依赖审计，并优先评估 HttpOnly Secure Cookie。
