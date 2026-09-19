# 数据库备份与恢复脚本

`backup-mysql.ps1` 使用 `mysqldump --single-transaction` 生成 `industrial_maintenance_YYYYMMDD_HHmmss.sql`，密码仅从当前进程环境变量或安全输入读取，不写入仓库。默认输出目录为项目根目录下被 Git 忽略的 `backups/`。

```powershell
$env:DB_USERNAME='maintenance_app'
$env:DB_PASSWORD='仅设置在当前终端中的密码'
$env:MYSQLDUMP_PATH='D:\yyq\bin\mysqldump.exe'
.\scripts\backup-mysql.ps1
```

恢复必须指向独立测试数据库，禁止覆盖唯一开发库：

```powershell
$env:MYSQL_PATH='D:\yyq\bin\mysql.exe'
.\scripts\restore-mysql.ps1 -BackupFile .\backups\industrial_maintenance_时间戳.sql -Database industrial_maintenance_restore_test
```

定时执行可使用 Windows 任务计划程序，程序填写 `pwsh.exe`，参数填写 `-NoProfile -File D:\BS\industrial-maintenance\scripts\backup-mysql.ps1`。建议使用权限受限的 MySQL 备份账户，并通过任务账户的安全环境提供凭据。
