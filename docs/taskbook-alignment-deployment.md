# 任务书与开题约束对齐部署说明

本轮保持任务书、开题报告原件不变。数据库升级、代码发布应一起进行。

## 数据库升级

1. 使用 `scripts/backup-mysql.ps1` 备份现有数据库，保存可验证的 SQL 备份。
2. 对已有且已完成 00—26 号脚本的数据库执行 `sql/27_taskbook_alignment.sql` 一次。
3. 全新部署按 00—27 号顺序执行 SQL；11、13、16、20 为开发演示数据，不应在生产环境反复执行。
4. 重启后端，重新登录获取新增的 `repair:confirm` 和 `system:user:delete` 权限。

迁移新增报修附件表、PM 工单计划名称/编号快照及用户逻辑删除字段。已有维修工单保持原状态；迁移后的新报修先待确认，再由授权主管生成唯一维修工单。历史 PM 快照由迁移时的计划信息回填，不能复原已被修改前的原名称。

## 每日全量备份

使用 PowerShell 7，在计划任务执行用户的会话中先创建 `data` 目录，再保存凭据（输入 MySQL 密码，用户名填写实际 MySQL 用户）：

```powershell
Get-Credential | Export-Clixml -LiteralPath .\data\backup-credential.xml
.\scripts\schedule-mysql-backup.ps1 -Action Register -Username maintenance_app -MySqlDump '实际路径\mysqldump.exe' -DailyAt 02:00
.\scripts\schedule-mysql-backup.ps1 -Action Run -Username maintenance_app -MySqlDump '实际路径\mysqldump.exe'
```

凭据使用 Windows DPAPI，绑定保存它的 Windows 用户和本机；密码不写入计划任务参数。任务默认只在该用户登录时执行，错过时间后补执行。需要无人值守服务器备份时，应由部署管理员配置专用运行身份。备份成功后只清理该数据库超过 30 天的备份，失败不清理。日志为 `backups/backup.log`。恢复使用 `scripts/restore-mysql.ps1`，先在独立数据库验证备份可恢复，再恢复目标数据库。

## 实现边界

日历 PM 支持周/月/季；人工累计运行小时字段已维护，运行小时阈值自动触发作为扩展，当前不宣称已实现。站内信已实现，邮件和企业微信仍为扩展。算法结果来自测试/模拟样本，不能推断真实工厂效率收益。
