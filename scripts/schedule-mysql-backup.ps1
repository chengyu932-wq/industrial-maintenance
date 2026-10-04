[CmdletBinding()]
param(
    [ValidateSet('Run','Register','Unregister')][string]$Action = 'Run',
    [string]$TaskName = 'IndustrialMaintenanceDailyBackup',
    [string]$DailyAt = '02:00',
    [string]$Database = 'industrial_maintenance',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Username = 'maintenance_app',
    [string]$MySqlDump = 'mysqldump.exe',
    [string]$CredentialFile = (Join-Path $PSScriptRoot '..\data\backup-credential.xml'),
    [string]$BackupDirectory = (Join-Path $PSScriptRoot '..\backups'),
    [ValidateRange(1,3650)][int]$RetentionDays = 30
)
$ErrorActionPreference = 'Stop'
if ($Action -eq 'Unregister') {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    return
}
if ($Action -eq 'Register') {
    if (-not (Test-Path -LiteralPath $CredentialFile)) {
        throw '先使用 Get-Credential | Export-Clixml 保存 MySQL 凭据。凭据只能由保存它的 Windows 用户在本机解密。'
    }
    $shellPath = (Get-Process -Id $PID).Path
    $arguments = @('-NoProfile','-NonInteractive','-File', ('"{0}"' -f $PSCommandPath),'-Action','Run',
        '-Database', ('"{0}"' -f $Database),'-HostName', ('"{0}"' -f $HostName),'-Port',$Port,
        '-Username', ('"{0}"' -f $Username),'-MySqlDump', ('"{0}"' -f $MySqlDump),
        '-CredentialFile', ('"{0}"' -f ([IO.Path]::GetFullPath($CredentialFile))),
        '-BackupDirectory', ('"{0}"' -f ([IO.Path]::GetFullPath($BackupDirectory))),'-RetentionDays',$RetentionDays) -join ' '
    $trigger = New-ScheduledTaskTrigger -Daily -At $DailyAt
    $taskAction = New-ScheduledTaskAction -Execute $shellPath -Argument $arguments -WorkingDirectory $PSScriptRoot
    $principal = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew
    Register-ScheduledTask -TaskName $TaskName -Action $taskAction -Trigger $trigger -Principal $principal -Settings $settings -Force | Out-Null
    Write-Output "已注册每日 $DailyAt 全量备份（当前 Windows 用户登录时执行）：$TaskName"
    return
}
$credential = Import-Clixml -LiteralPath $CredentialFile
if ($credential -isnot [PSCredential]) { throw '凭据文件必须包含 PSCredential' }
$passwordValue = $credential.GetNetworkCredential().Password
try {
    & (Join-Path $PSScriptRoot 'backup-mysql.ps1') -Database $Database -HostName $HostName -Port $Port -Username $Username -Password $passwordValue -MySqlDump $MySqlDump -BackupDirectory $BackupDirectory
} finally { $passwordValue = $null }
# Prune only successful database backups inside the explicitly resolved directory.
$resolvedDirectory = [IO.Path]::GetFullPath($BackupDirectory)
$filePattern = '^{0}_\d{{8}}_\d{{6}}\.sql$' -f [regex]::Escape($Database)
Get-ChildItem -LiteralPath $resolvedDirectory -File | Where-Object { $_.Name -match $filePattern -and $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
