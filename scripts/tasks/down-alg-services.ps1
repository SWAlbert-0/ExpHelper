param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "../..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile
$services = @("nsga2_zdt1_example", "moead_lsmop_example", "smpso_lsmop_example", "spea2_lsmop_example")

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

Write-Host "[INFO] 停止算法服务容器: $($services -join ', ')" -ForegroundColor Cyan
docker compose --env-file $envPath -f $composePath stop @services
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] 算法服务容器停止失败。" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[OK] 算法服务容器已停止。" -ForegroundColor Green
