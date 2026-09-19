[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [Parameter(Mandatory=$true)][string]$AccessToken,
    [int]$RequestsPerEndpoint = 20,
    [int]$Concurrency = 5
)
$ErrorActionPreference = 'Stop'
$endpoints = [ordered]@{
    currentUser = '/api/auth/me'
    equipment = '/api/equipment?page=1&size=10'
    workOrders = '/api/work-orders?page=1&size=10'
    inventory = '/api/inventory/stocks?page=1&size=10'
    statistics = '/api/statistics/kpis?startDate=2026-01-01&endDate=2026-09-18'
    operationLogs = '/api/operation-logs?page=1&size=10'
}
foreach ($entry in $endpoints.GetEnumerator()) {
    $target = $BaseUrl + $entry.Value
    $started = [Diagnostics.Stopwatch]::StartNew()
    $samples = 1..$RequestsPerEndpoint | ForEach-Object -Parallel {
        $watch = [Diagnostics.Stopwatch]::StartNew()
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $using:target -Headers @{ Authorization = 'Bearer ' + $using:AccessToken }
            $status = $response.StatusCode
        } catch {
            $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        }
        $watch.Stop()
        [pscustomobject]@{ Milliseconds = $watch.Elapsed.TotalMilliseconds; Status = $status }
    } -ThrottleLimit $Concurrency
    $started.Stop()
    $times = @($samples.Milliseconds | Sort-Object)
    $p95Index = [Math]::Max(0, [Math]::Ceiling($times.Count * 0.95) - 1)
    [pscustomobject]@{
        Endpoint = $entry.Key
        Requests = $times.Count
        Concurrency = $Concurrency
        AverageMs = [Math]::Round(($times | Measure-Object -Average).Average, 2)
        P95Ms = [Math]::Round($times[$p95Index], 2)
        MaxMs = [Math]::Round(($times | Measure-Object -Maximum).Maximum, 2)
        ThroughputPerSecond = [Math]::Round($times.Count / $started.Elapsed.TotalSeconds, 2)
        Errors = @($samples | Where-Object Status -ne 200).Count
    }
}
