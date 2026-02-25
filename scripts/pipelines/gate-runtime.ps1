param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipSmoke = $false,
    [switch]$SkipE2E = $false,
    [switch]$SkipAlgCheck = $false,
    [switch]$SkipRuntime = $false
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "../..")

function Run-Step([string]$name, [scriptblock]$action) {
    Write-Host "==> $name" -ForegroundColor Cyan
    try {
        & $action
        Write-Host "[PASS] $name" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

Run-Step "配置清单校验" {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/check-config-doc.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "scripts/check-config-doc.ps1 failed."
    }
}

Run-Step "代码与测试门禁（后端/前端/e2e）" {
    $args = @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $repoRoot "scripts/tasks/check-p0.ps1"))
    if ($SkipSmoke) { $args += "-SkipSmoke" }
    if ($SkipE2E) { $args += "-SkipE2E" }
    & powershell @args
    if ($LASTEXITCODE -ne 0) {
        throw "scripts/tasks/check-p0.ps1 failed."
    }
}

if (-not $SkipRuntime) {
    Run-Step "运行时健康检查" {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/tasks/check-runtime-readiness.ps1")
        if ($LASTEXITCODE -ne 0) {
            throw "scripts/tasks/check-runtime-readiness.ps1 failed."
        }
    }
}

if (-not $SkipAlgCheck) {
    Run-Step "算法服务检查" {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/tasks/check-alg-services.ps1")
        if ($LASTEXITCODE -ne 0) {
            throw "scripts/tasks/check-alg-services.ps1 failed."
        }
    }
}

Write-Host "[OK] Gate completed." -ForegroundColor Green
