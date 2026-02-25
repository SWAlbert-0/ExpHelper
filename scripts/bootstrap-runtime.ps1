param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipBaseBuild,
    [switch]$SkipAlgorithmServices,
    [switch]$SkipLocalPackage,
    [ValidateSet("auto", "online", "offline")]
    [string]$Mode = "auto"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile

function Fail([string]$msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
    exit 1
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Fail "未找到 docker 命令，请先安装 Docker Desktop。"
}
try {
    $null = docker version --format "{{.Server.Version}}" 2>$null
} catch {
    Fail "docker daemon 不可达，请确认 Docker Desktop 已启动且当前终端有访问权限。"
}

Write-Host "[INFO] bootstrap runtime start" -ForegroundColor Cyan
Write-Host "[INFO] repo=$repoRoot" -ForegroundColor Cyan

if (-not $SkipBaseBuild) {
    Write-Host "[INFO] 启动基础容器（mongo/rabbitmq/nacos/webapp/front）" -ForegroundColor Cyan
    docker compose --env-file $envPath -f $composePath up -d --build mongodb rabbitmq nacos webapp exphlp_front
    if ($LASTEXITCODE -ne 0) {
        Fail "基础容器启动失败。"
    }
} else {
    Write-Host "[INFO] 已跳过基础容器重建（-SkipBaseBuild）" -ForegroundColor Yellow
}

if (-not $SkipAlgorithmServices) {
    Write-Host "[INFO] 启动算法容器" -ForegroundColor Cyan
    $upScript = Join-Path $repoRoot "scripts/up-alg-services.ps1"
    $args = @(
        "-ExecutionPolicy", "Bypass",
        "-File", $upScript,
        "-EnvFile", $EnvFile,
        "-ComposeFile", $ComposeFile,
        "-Mode", $Mode
    )
    if ($SkipLocalPackage) {
        $args += "-SkipLocalPackage"
    }
    & powershell @args
    if ($LASTEXITCODE -ne 0) {
        Fail "算法容器启动失败。"
    }
} else {
    Write-Host "[INFO] 已跳过算法容器启动（-SkipAlgorithmServices）" -ForegroundColor Yellow
}

Write-Host "[INFO] 执行运行时健康检查" -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/check-runtime-readiness.ps1")
if ($LASTEXITCODE -ne 0) {
    Fail "运行时健康检查失败。"
}

if (-not $SkipAlgorithmServices) {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/check-alg-services.ps1")
    if ($LASTEXITCODE -ne 0) {
        Fail "算法服务检查失败。"
    }
}

Write-Host "[OK] bootstrap runtime completed." -ForegroundColor Green
