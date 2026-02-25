param(
    [string]$NacosBase = "http://localhost:8848",
    [string]$ApiBase = "http://localhost:8080",
    [string]$ServiceName = "smpso-lsmop-ls",
    [string]$AlgHttpBase = "http://localhost:18084",
    [string]$PlanId = ""
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
    Write-Host ""
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Safe-InvokeJson([string]$uri) {
    try {
        return Invoke-RestMethod -Method Get -Uri $uri
    } catch {
        return $null
    }
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port
    )
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(1500, $false)
        if (-not $ok) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

Write-Step "检查 Nacos 服务列表"
$svcList = Safe-InvokeJson "$NacosBase/nacos/v1/ns/service/list?pageNo=1&pageSize=100&namespaceId=public"
if (-not $svcList) {
    Write-Host "Nacos 不可达: $NacosBase" -ForegroundColor Red
    exit 2
}
$doms = @()
if ($svcList.doms) { $doms = @($svcList.doms) }
Write-Host "Nacos 服务数: $($svcList.count)"
Write-Host "服务列表: $($doms -join ', ')"

$baseUri = [Uri]$NacosBase
$grpcPort = $baseUri.Port + 1000
Write-Host "Nacos gRPC端口($grpcPort)可达: $(Test-TcpPort -HostName $baseUri.Host -Port $grpcPort)"

Write-Step "检查目标算法服务实例"
$inst = Safe-InvokeJson "$NacosBase/nacos/v1/ns/instance/list?serviceName=$ServiceName&groupName=DEFAULT_GROUP&namespaceId=public"
if (-not $inst) {
    Write-Host "无法查询服务实例: $ServiceName" -ForegroundColor Yellow
} else {
    $hosts = @()
    if ($inst.hosts) { $hosts = @($inst.hosts) }
    Write-Host "目标服务: $ServiceName"
    Write-Host "实例数: $($hosts.Count)"
    if ($hosts.Count -gt 0) {
        $hosts | ForEach-Object {
            Write-Host ("  - {0}:{1} healthy={2} enabled={3}" -f $_.ip, $_.port, $_.healthy, $_.enabled)
        }
    }
}

Write-Step "检查算法服务 HTTP 活性"
try {
    $resp = Invoke-WebRequest -Method Get -Uri "$AlgHttpBase/myAlg/" -UseBasicParsing
    Write-Host "算法服务可达，HTTP: $($resp.StatusCode)"
} catch {
    if ($_.Exception.Response) {
        Write-Host "算法服务可达，HTTP: $([int]$_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
    } else {
        Write-Host "算法服务不可达: $AlgHttpBase" -ForegroundColor Red
    }
}

if ($PlanId -ne "") {
    Write-Step "检查执行计划 preCheck 接口"
    $pre = Safe-InvokeJson "$ApiBase/api/ExePlanController/preCheck?planId=$PlanId"
    if ($pre) {
        Write-Host ($pre | ConvertTo-Json -Depth 8)
    } else {
        Write-Host "preCheck 调用失败（可能需要登录 token）" -ForegroundColor Yellow
    }
}

Write-Step "结论"
$instanceCount = 0
if ($inst -and $inst.hosts) { $instanceCount = @($inst.hosts).Count }
if ($instanceCount -gt 0) {
    Write-Host "PASS: Nacos 已发现 $ServiceName 可用实例，可执行计划。" -ForegroundColor Green
    exit 0
}
Write-Host "FAIL: Nacos 未发现 $ServiceName 可用实例。" -ForegroundColor Red
Write-Host "建议: 1) 确认算法服务已启动 2) 确认 spring.application.name 与 serviceName 一致 3) 检查 NACOS_SERVER_ADDR 是否指向同一 Nacos"
exit 1

