param(
    [string]$NacosBase = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "[FAIL] 未找到 docker 命令，请先安装 Docker Desktop。" -ForegroundColor Red
    exit 1
}
try {
    $null = docker version --format "{{.Server.Version}}" 2>$null
} catch {
    Write-Host "[FAIL] docker daemon 不可达，请确认 Docker Desktop 已启动且当前终端有访问权限。" -ForegroundColor Red
    exit 1
}

function Get-Count([string]$serviceName) {
    try {
        $uri = "$NacosBase/nacos/v1/ns/instance/list?serviceName=$serviceName&groupName=DEFAULT_GROUP&namespaceId=public"
        $resp = Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 5
        if ($resp -and $resp.hosts) {
            $healthy = @($resp.hosts | Where-Object { $_.healthy -eq $true }).Count
            $total = @($resp.hosts).Count
            return @{ total = $total; healthy = $healthy }
        }
    } catch {
        return @{ total = -1; healthy = -1 }
    }
    return @{ total = 0; healthy = 0 }
}

$targets = @(
    @{ service = "nsga2-zdt1-ls"; container = "c_nsga2_zdt1_example" },
    @{ service = "moead-lsmop-ls"; container = "c_moead_lsmop_example" },
    @{ service = "smpso-lsmop-ls"; container = "c_smpso_lsmop_example" },
    @{ service = "spea2-lsmop-ls"; container = "c_spea2_lsmop_example" }
)

Write-Host "=== 容器状态 ===" -ForegroundColor Cyan
docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String -Pattern "c_nsga2_zdt1_example|c_moead_lsmop_example|c_smpso_lsmop_example|c_spea2_lsmop_example|NAMES"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] 无法读取容器状态（docker ps 失败）。" -ForegroundColor Red
    exit 1
}
Write-Host "=== Nacos 实例状态 ===" -ForegroundColor Cyan

foreach ($item in $targets) {
    $count = Get-Count $item.service
    if ($count.total -lt 0) {
        Write-Host "[FAIL] $($item.service): Nacos 不可达" -ForegroundColor Red
        continue
    }
    if ($count.healthy -gt 0) {
        Write-Host "[OK] $($item.service): total=$($count.total), healthy=$($count.healthy)" -ForegroundColor Green
    } else {
        Write-Host "[WARN] $($item.service): total=$($count.total), healthy=$($count.healthy)" -ForegroundColor Yellow
    }
}
