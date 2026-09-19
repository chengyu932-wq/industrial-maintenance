# 第11阶段 KPI 统计口径设计

## 1. 统一统计边界

- 时间参数由后端统一解析，支持 `LAST_7_DAYS`、`LAST_30_DAYS`、`LAST_90_DAYS`、`CUSTOM`。预设区间以服务器当前日期为结束日，采用左闭右开区间 `[startDate 00:00:00, endDate + 1 day 00:00:00)`；自定义区间必须同时提供 `startDate`、`endDate`，且开始日期不晚于结束日期。
- 维修类指标仅统计 `mnt_work_order.work_order_type = 'REPAIR'`；取消工单不进入完成类指标。
- 统计范围复用现有 RBAC 数据范围：管理员为全部；运维主管为所属车间或负责班组；工程师为本人或所属班组关联工单；仓库管理员仅统计授权仓库；无相应业务范围的数据返回 0 或“数据不足”，不绕过权限查询全库。
- 百分比保留两位小数，时长类指标保留两位小数。分母为 0 时不返回虚构的 0%，而返回不可计算状态和原因。

## 2. 六项 KPI 冻结口径

| 指标 | 业务含义 | 计算公式 | 数据来源与字段 | 时间范围 | 过滤条件与异常处理 |
|---|---|---|---|---|---|
| MTBF | 有真实运行记录的设备平均每发生一次已完成故障维修前累计的运行小时 | `统计期运行小时增量合计 / 统计期完成维修工单数` | `eqp_runtime_record.running_hours_increment, record_date, equipment_id`；`mnt_work_order.work_order_type, status, completed_at, equipment_id` | 运行记录按 `record_date`，故障次数按 `completed_at` 落入统一区间 | 仅 `REPAIR + COMPLETED` 计故障次数；运行增量必须非负；故障数或运行小时为 0 时标记“数据不足”。不使用自然日 × 24 小时推算 |
| MTTR | 已进入实际维修且最终完成的维修工单平均修复历时 | `Σ(completed_at - started_at) / 有效维修次数` | `mnt_work_order.started_at, completed_at, status, work_order_type` | `completed_at` 落入统一区间 | 仅 `REPAIR + COMPLETED`，要求起止时间均非空且 `completed_at >= started_at`；取消、未完成、时间倒置数据排除并在样本数中体现 |
| 工单按时关闭率 | 绑定了解决 SLA 的已完成维修工单中，在截止时间前完成的比例 | `completed_at <= sla_resolve_deadline 的工单数 / sla_resolve_deadline 非空的已完成维修工单数 × 100%` | `mnt_work_order.completed_at, sla_resolve_deadline, status, work_order_type` | `completed_at` 落入统一区间 | 分母只包含具有解决截止时间的 `REPAIR + COMPLETED` 工单；无 SLA 样本时标记“数据不足” |
| 一次修复率（首次验收通过） | 已完成维修工单首次提交后直接通过验收的比例 | `acceptance_return_count = 0 的已完成维修工单数 / 已完成维修工单数 × 100%` | `mnt_work_order.acceptance_return_count, work_order_type, status, completed_at` | 按 `completed_at` 落入统一统计区间 | 这是可由现有业务记录审计的“首次验收通过率”，不等同于“7 天同类故障未复发率”；系统不使用自由文本推断故障类别 |
| 设备综合可用率 | 在状态履历能够覆盖的观测时间内，设备处于运行状态的比例 | `RUNNING 状态秒数 / (RUNNING + FAULT + REPAIRING + STOPPED 状态秒数) × 100%` | `eqp_status_log.equipment_id, to_status, changed_at`；`eqp_equipment` 及组织字段用于权限过滤 | 状态区间与统一统计区间求交集 | 不把 `PENDING`、`SCRAPPED` 计入可用时间或观测时间；只计算状态履历实际覆盖的区间，不把无履历时段默认成运行；观测秒数为 0 时标记“数据不足” |
| 备件周转率 | 授权仓库内，统计期备件净消耗量相对于平均库存量的比例 | `净消耗量 / ((期初库存量 + 期末库存量) / 2)`；净消耗量为 `OUTBOUND + ISSUE - RETURN` 的绝对数量 | `inv_transaction.transaction_type, qty_change, qty_before, qty_after, created_at, warehouse_id, spare_part_id`；`inv_stock.current_qty` 用于无期内流水时回推边界 | `inv_transaction.created_at` 落入统一区间 | 排除仓库间调拨、盘点与报废；按仓库-备件维度推导期初/期末库存后汇总；平均库存为 0 时标记“数据不足”。该数量口径适合本系统演示，不宣称财务金额周转率 |

## 3. 安全库存口径

安全库存已由库存模块真实实现并纳入看板的“库存预警”展示，但不作为第七项 KPI。单个仓库-备件的建议安全库存为：

`近 30 天净消耗量 / 30 × safety_days`

补货点为：

`近 30 天日均净消耗量 × (safety_days + lead_time_days)`

数据来自 `inv_transaction`、`inv_spare_part.safety_days` 与 `lead_time_days`，看板展示当前真实开放预警数量及预警明细，不生成预测数据。

## 4. 图表口径

| 图表 | 接口数据 | 查询字段 | 说明 |
|---|---|---|---|
| 设备状态分布 | `/api/statistics/charts` | `eqp_equipment.status` | 按当前用户设备数据范围聚合当前状态 |
| 工单状态分布 | `/api/statistics/charts` | `mnt_work_order.status, created_at` | 统计创建时间落入区间的可见工单 |
| 故障设备类型排行 | `/api/statistics/charts` | `eqp_type.type_name, mnt_work_order.completed_at` | 用真实设备类型替代不存在的标准故障类型，统计完成维修次数 Top 5 |
| 维修趋势 | `/api/statistics/charts` | `mnt_work_order.completed_at` | 按日统计已完成维修工单；缺失日期由服务层补 0，不生成业务数据 |
| 备件消耗排行 | `/api/statistics/charts` | `inv_transaction.qty_change, transaction_type, spare_part_id` | 授权仓库内 `OUTBOUND + ISSUE - RETURN` 净消耗 Top 5 |
| SLA 完成情况 | `/api/statistics/charts` | `completed_at, sla_resolve_deadline` | 已完成且有 SLA 的维修工单分为按时/超时 |
| 库存预警 | `/api/statistics/overview` | `inv_warning.status, warehouse_id` | 当前开放预警数量，按授权仓库过滤 |

## 5. 数据缺口声明

1. `eqp_runtime_record` 虽已建表，但现有页面没有运行小时录入流程；因此数据库没有记录时 MTBF 必须显示“运行小时记录不足”，不能按 24 小时运行推算。
2. 一次修复率采用首次验收通过口径；如论文讨论“7 天同类故障未复发率”，必须明确该扩展口径因缺少标准故障分类而未实现。
3. 设备可用率仅对状态履历实际覆盖时段负责；测试数据规模有限时，结果只能说明当前测试库，不代表企业生产效果。
