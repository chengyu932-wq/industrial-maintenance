[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$BackupFile,
    [Parameter(Mandatory=$true)][string]$Database,
    [string]$HostName = $(if ($env:DB_HOST) { $env:DB_HOST } else { '127.0.0.1' }),
    [int]$Port = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$Username = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'maintenance_app' }),
    [string]$Password = $env:DB_PASSWORD,
    [string]$MySql = $(if ($env:MYSQL_PATH) { $env:MYSQL_PATH } else { 'mysql.exe' })
)
$ErrorActionPreference='Stop'
$source=(Resolve-Path -LiteralPath $BackupFile).Path
if(-not $Password){$Password=(Read-Host 'MySQL password' -AsSecureString | ConvertFrom-SecureString -AsPlainText)}
$psi=[System.Diagnostics.ProcessStartInfo]::new();$psi.FileName=$MySql;$psi.UseShellExecute=$false;$psi.RedirectStandardInput=$true;$psi.RedirectStandardError=$true
$psi.ArgumentList.Add("--host=$HostName");$psi.ArgumentList.Add("--port=$Port");$psi.ArgumentList.Add("--user=$Username");$psi.ArgumentList.Add('--default-character-set=utf8mb4');$psi.ArgumentList.Add($Database);$psi.Environment['MYSQL_PWD']=$Password
$process=[System.Diagnostics.Process]::new();$process.StartInfo=$psi
try{if(-not $process.Start()){throw 'mysql failed to start'};$file=[System.IO.File]::OpenRead($source);try{$file.CopyTo($process.StandardInput.BaseStream)}finally{$file.Dispose();$process.StandardInput.Close()};$stderr=$process.StandardError.ReadToEnd();$process.WaitForExit();if($process.ExitCode -ne 0){throw "mysql exit $($process.ExitCode): $stderr"};Write-Output "Restored $source to $Database"}finally{$psi.Environment.Remove('MYSQL_PWD') | Out-Null}
