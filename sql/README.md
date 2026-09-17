# MySQL 8 数据库初始化说明

本目录是 `industrial_maintenance` 数据库的唯一 SQL 初始化入口。脚本面向 MySQL 8.0.16 及以上版本，统一使用 InnoDB、`utf8mb4` 和 `utf8mb4_0900_ai_ci`。

## 执行顺序

| 顺序 | 文件 | 内容 |
|---:|---|---|
| 1 | `00_create_database.sql` | 创建数据库 |
| 2 | `01_schema_system.sql` | 用户、角色、菜单、关联关系、操作日志 |
| 3 | `02_schema_organization.sql` | 车间、产线、工位、班组、技能 |
| 4 | `03_schema_equipment.sql` | 设备类型、设备、状态履历、运行小时、附件 |
| 5 | `04_schema_work_order.sql` | 报修、工单、流转、派单、维修、附件、SLA |
| 6 | `05_schema_inventory.sql` | 仓库、供应商、备件、库存、流水、领用、预警 |
| 7 | `06_schema_maintenance.sql` | 保养计划、计划项目、执行结果 |
| 8 | `07_schema_knowledge.sql` | 故障知识库 |
| 9 | `08_schema_support.sql` | 站内消息 |
| 10 | `09_init_data.sql` | 五类角色和三条开发默认 SLA 规则 |
| 11 | `10_verify_database.sql` | 表数量、引擎、字符集、初始数据和重复业务键检查 |
| 12 | `11_dev_auth_seed.sql` | 可选：五种角色开发账号、禁用账号和认证阶段菜单权限 |
| 13 | `12_stage4_permissions.sql` | 第4阶段组织与设备菜单、按钮权限（现有库升级也需执行） |
| 14 | `13_stage4_demo_data.sql` | 可选：第4阶段组织、设备类型与设备演示数据 |
| 15 | `14_stage5_permissions.sql` | 第5阶段故障报修、工单菜单与按钮权限 |
| 16 | `15_stage7_permissions.sql` | 第7阶段仓库、备件、库存和工单备件权限 |
| 17 | `16_stage7_demo_data.sql` | 可选：第7阶段仓库、备件、期初库存和授权演示数据 |
| 18 | `17_stage8_permissions.sql` | 第8阶段预防性维护菜单与按钮权限 |
| 19 | `18_stage9_schema.sql` | 第9阶段技能映射、SLA 绑定与超时幂等增量结构 |
| 20 | `19_stage9_permissions.sql` | 第9阶段 SLA 与消息权限 |
| 21 | `20_stage9_demo_data.sql` | 可选：第9阶段技能映射演示数据 |
| 22 | `21_stage10_schema.sql` | 第10阶段知识维修结果、审核意见增量字段 |
| 23 | `22_stage10_permissions.sql` | 第10阶段知识库与相似工单权限 |

在 MySQL 客户端中依次执行：

```sql
SOURCE D:/BS/industrial-maintenance/sql/00_create_database.sql;
SOURCE D:/BS/industrial-maintenance/sql/01_schema_system.sql;
SOURCE D:/BS/industrial-maintenance/sql/02_schema_organization.sql;
SOURCE D:/BS/industrial-maintenance/sql/03_schema_equipment.sql;
SOURCE D:/BS/industrial-maintenance/sql/04_schema_work_order.sql;
SOURCE D:/BS/industrial-maintenance/sql/05_schema_inventory.sql;
SOURCE D:/BS/industrial-maintenance/sql/06_schema_maintenance.sql;
SOURCE D:/BS/industrial-maintenance/sql/07_schema_knowledge.sql;
SOURCE D:/BS/industrial-maintenance/sql/08_schema_support.sql;
SOURCE D:/BS/industrial-maintenance/sql/09_init_data.sql;
SOURCE D:/BS/industrial-maintenance/sql/10_verify_database.sql;
SOURCE D:/BS/industrial-maintenance/sql/12_stage4_permissions.sql;
SOURCE D:/BS/industrial-maintenance/sql/14_stage5_permissions.sql;
SOURCE D:/BS/industrial-maintenance/sql/15_stage7_permissions.sql;
SOURCE D:/BS/industrial-maintenance/sql/17_stage8_permissions.sql;
SOURCE D:/BS/industrial-maintenance/sql/18_stage9_schema.sql;
SOURCE D:/BS/industrial-maintenance/sql/19_stage9_permissions.sql;
SOURCE D:/BS/industrial-maintenance/sql/21_stage10_schema.sql;
SOURCE D:/BS/industrial-maintenance/sql/22_stage10_permissions.sql;
-- 仅本地开发/答辩演示时执行：
SOURCE D:/BS/industrial-maintenance/sql/11_dev_auth_seed.sql;
SOURCE D:/BS/industrial-maintenance/sql/13_stage4_demo_data.sql;
SOURCE D:/BS/industrial-maintenance/sql/16_stage7_demo_data.sql;
SOURCE D:/BS/industrial-maintenance/sql/20_stage9_demo_data.sql;
```

## 初始化与重复执行

- 建表脚本使用 `CREATE TABLE IF NOT EXISTS`，初始化数据使用业务唯一键进行幂等更新；同一结构可重复执行。
- 该方案用于从空库初始化，不承担已存在旧表的在线结构迁移。字段变更后应编写独立迁移脚本或在开发环境重建空库。
- 项目不会自动删除已有数据库。确需重建时，应由开发者确认目标是本地开发库后手动删除并重新执行全部脚本。
- `09_init_data.sql` 不创建管理员账号，也不保存默认明文密码。管理员账号应在认证模块阶段通过安全的开发初始化机制创建，并使用 BCrypt 哈希。
- `11_dev_auth_seed.sql` 只用于本地开发或答辩演示，统一初始密码为 `DevOnly@123`（数据库只保存 BCrypt Hash）；非开发环境必须删除测试账号或修改密码。
- 三条 SLA 时长是可修改的项目开发默认值，不是任务书或学校规定值。

## 连接配置

数据库脚本不创建 MySQL 登录用户。请为 Spring Boot 配置具有 `industrial_maintenance` 读写权限的本地开发账号：

```text
DB_URL=jdbc:mysql://localhost:3306/industrial_maintenance?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=maintenance_app
DB_PASSWORD=本地私有密码
DB_HEALTH_ENABLED=true
```

不要把真实数据库密码提交到 Git。

## 设计边界

- 共 38 张表，不设置 MySQL 物理外键；实体关系通过逻辑关联字段、索引和 Service 校验保证。
- 设备状态、工单状态、角色数据范围和库存流水类型使用英文编码，并由 MySQL `CHECK` 约束兜底。
- 设备状态履历、工单流转、库存流水、运行小时记录和操作日志不提供普通业务删除能力。
- 库存以 `(warehouse_id, spare_part_id)` 为唯一维度，所有数量变化必须同时生成 `inv_transaction`。

## 第11阶段增量

- `23_stage11_statistics.sql`：补充运行小时按统计日期查询所需索引，不新增汇总表，不保存派生 KPI。
- `24_stage11_permissions.sql`：增加统计分析菜单及 `statistics:view` 权限，授权管理员、运维主管、工程师和仓库管理员。
- KPI 与图表直接聚合设备状态履历、维修工单、运行小时和库存流水，口径见 `docs/第11阶段KPI统计口径设计.md`。
