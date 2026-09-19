[CmdletBinding()]
param(
    [string]$Database = $(if ($env:DB_NAME) { $env:DB_NAME } else { 'industrial_maintenance' }),
    [string]$HostName = $(if ($env:DB_HOST) { $env:DB_HOST } else { '127.0.0.1' }),
    [int]$Port = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$Username = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'maintenance_app' }),
    [string]$Password = $env:DB_PASSWORD,
    [string]$MySqlDump = $(if ($env:MYSQLDUMP_PATH) { $env:MYSQLDUMP_PATH } else { 'mysqldump.exe' }),
    [string]$BackupDirectory = (Join-Path $PSScriptRoot '..\backups')
)
$ErrorActionPreference = 'Stop'
if (-not $Password) { $Password = (Read-Host 'MySQL password' -AsSecureString | ConvertFrom-SecureString -AsPlainText) }
$resolvedBackup = [System.IO.Path]::GetFullPath($BackupDirectory)
[System.IO.Directory]::CreateDirectory($resolvedBackup) | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$target = Join-Path $resolvedBackup ("{0}_{1}.sql" -f $Database,$stamp)
$logFile = Join-Path $resolvedBackup 'backup.log'
$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = $MySqlDump
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.ArgumentList.Add("--host=$HostName");$psi.ArgumentList.Add("--port=$Port");$psi.ArgumentList.Add("--user=$Username")
$psi.ArgumentList.Add('--single-transaction');$psi.ArgumentList.Add('--routines');$psi.ArgumentList.Add('--events');$psi.ArgumentList.Add('--triggers');$psi.ArgumentList.Add('--set-gtid-purged=OFF');$psi.ArgumentList.Add('--default-character-set=utf8mb4');$psi.ArgumentList.Add($Database)
$psi.Environment['MYSQL_PWD']=$Password
$process=[System.Diagnostics.Process]::new();$process.StartInfo=$psi
try {
    if(-not $process.Start()){throw 'mysqldump failed to start'}
    $file=[System.IO.File]::Create($target)
    try{$process.StandardOutput.BaseStream.CopyTo($file)}finally{$file.Dispose()}
    $stderr=$process.StandardError.ReadToEnd();$process.WaitForExit()
    if($process.ExitCode -ne 0){Remove-Item -LiteralPath $target -Force -ErrorAction SilentlyContinue;throw "mysqldump exit $($process.ExitCode): $stderr"}
    if((Get-Item -LiteralPath $target).Length -eq 0){Remove-Item -LiteralPath $target -Force;throw 'Backup file is empty'}
    "$(Get-Date -Format s) SUCCESS $target" | Add-Content -LiteralPath $logFile -Encoding utf8
    Write-Output $target
} catch { "$(Get-Date -Format s) FAIL $($_.Exception.Message)" | Add-Content -LiteralPath $logFile -Encoding utf8; throw } finally { $psi.Environment.Remove('MYSQL_PWD') | Out-Null }
