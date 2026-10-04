# 2026-10-01 接口及行为增量

| 接口 | 权限 | 行为 |
|---|---|---|
| POST /api/repair-requests | repair:create | 创建SUBMITTED报修，workOrderId/workOrderNo为空；设备仍RUNNING |
| POST /api/repair-requests/{id}/confirm | repair:confirm | 仅管理员/授权主管，确认并创建唯一工单，转换报修、联动FAULT、绑定SLA及关联现场资料 |
| GET /api/repair-requests/{id}/attachments | repair:view | 按报修数据范围读取资料元数据 |
| POST /api/repair-requests/{id}/attachments | repair:create | multipart file，仅SUBMITTED，本人或授权管理人员可上传 |
| GET /api/repair-requests/{id}/attachments/{attachmentId} | repair:view | 下载；校验资料属于该报修且报修可见 |
| DELETE /api/users/{id} | system:user:delete | 逻辑删除并禁止登录，保留历史身份，不允许删除当前用户 |
| POST /api/work-orders/{id}/assign | workorder:assign | DispatchRequest增加可选recommended；true时核验当前Top3并保存评分；ASSIGNED未接单可人工改派，必须说明原因 |

工单附件上传仍使用原接口，补充角色、指派与阶段校验。已关闭/无法修复/取消工单不允许新增；故障资料限待派单或已派单的报修人/管理人员，维修资料限处理中的指派工程师，验收资料限待验收的报修人/管理人员。

密码重置递增password_version，旧版本访问与刷新令牌被拒绝。刷新消费使用Lua比较并删除，退出会话关联使用GETDEL，退出不能删除其他用户传入的刷新令牌。设备类型列表缓存TTL10分钟，类型写操作后清理。

KPI拒绝未来结束日期；可用率不计入当前时刻之后；MTBF分子分母采用同一设备授权范围；周转率使用历史流水边界，缺少边界或混合单位时available=false并返回原因。安全参数修改立即重算该备件库存告警，小时任务处理消费窗口变化。PM工单保存计划编号、名称快照，生成工单后禁止换设备及检查项。
