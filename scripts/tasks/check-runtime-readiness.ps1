param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$FrontBase = "http://localhost:8086",
    [string]$NacosBase = "http://localhost:8848",
    [string]$ServiceName = "nsga2-zdt1-ls"
)

$ErrorActionPreference = "Stop"

function Step([string]$name, [scriptblock]$action) {
    Write-Host "==> $name" -ForegroundColor Cyan
    try {
        & $action
        Write-Host "[PASS] $name" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

function Warn([string]$msg) {
    Write-Host "[WARN] $msg" -ForegroundColor Yellow
}

Step "Docker daemon / containers" {
    $table = docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    if ($LASTEXITCODE -ne 0) {
        throw "docker daemon 不可用"
    }
    $table | Select-String -Pattern "c_exphlp_webapp|c_exphlp_vue_front|c_exphlp_nacos|c_exphlp_mongo|c_exphlp_rabbitmq|NAMES"
}

Step "WebApp healthz" {
    $resp = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/auth/healthz" -TimeoutSec 8
    if (-not $resp -or $resp.code -ne 200) {
        throw "healthz 返回异常"
    }
    $data = $resp.data
    Write-Host ("status={0}, version={1}, buildTime={2}" -f $data.status, $data.artifactVersion, $data.buildTime)
}

Step "Front page availability" {
    $resp = Invoke-WebRequest -Method Get -Uri $FrontBase -UseBasicParsing -TimeoutSec 8
    if (-not $resp -or ($resp.StatusCode -lt 200 -or $resp.StatusCode -ge 400)) {
        throw "前端不可访问"
    }
    Write-Host ("front status={0}" -f $resp.StatusCode)
}

Step "Nacos service instances" {
    $uri = "$NacosBase/nacos/v1/ns/instance/list?serviceName=$ServiceName&groupName=DEFAULT_GROUP"
    $resp = Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 8
    $hosts = @()
    if ($resp -and $resp.hosts) {
        $hosts = @($resp.hosts)
    }
    $healthy = @($hosts | Where-Object { $_.healthy -eq $true -and $_.enabled -ne $false })
    Write-Host ("service={0}, total={1}, healthy={2}" -f $ServiceName, $hosts.Count, $healthy.Count)
    if ($healthy.Count -le 0) {
        throw "Nacos中无可用实例: $ServiceName"
    }
}

Step "Notification profile API (auth required)" {
    Warn "该接口需登录态校验。建议在浏览器登录后，从页面“个人中心->通知设置”点击刷新。"
}

Write-Host "Runtime readiness check completed." -ForegroundColor Green
