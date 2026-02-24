param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml"
)

$ErrorActionPreference = "Stop"

function Write-Ok([string]$msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Info([string]$msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Fail([string]$msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile
$services = @("webapp", "exphlp_front")
$webappDir = Join-Path $repoRoot "exphlp"
$webappJar = Join-Path $repoRoot "exphlp/api/webApp/target/webApp-1.0-SNAPSHOT.jar"

Write-Info "Repo: $repoRoot"
Write-Info "Compose: $composePath"
Write-Info "Env: $envPath"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fail "docker 未安装或不在 PATH 中。"
    exit 1
}

if (-not (Test-Path $envPath)) {
    Write-Fail "未找到环境文件: $envPath"
    exit 1
}

if (-not (Test-Path $composePath)) {
    Write-Fail "未找到 compose 文件: $composePath"
    exit 1
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Fail "未找到 mvn 命令，请先安装 Maven 并配置 PATH。"
    exit 1
}

try {
    $null = docker version --format "{{.Server.Version}}" 2>$null
} catch {
    Write-Fail "docker daemon 不可达，请先启动 Docker Desktop。"
    exit 1
}

Write-Info "先构建 webapp JAR（避免旧包被重复打进镜像）"
Push-Location $webappDir
try {
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "webapp 打包失败。"
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
if (-not (Test-Path $webappJar)) {
    Write-Fail "未找到打包产物: $webappJar"
    exit 1
}
$jarInfo = Get-Item $webappJar
Write-Ok ("webapp JAR 就绪: {0} | LastWriteTime={1} | Size={2}" -f $jarInfo.FullName, $jarInfo.LastWriteTime, $jarInfo.Length)

Write-Info "开始重建镜像: $($services -join ", ")"
docker compose --env-file $envPath -f $composePath build @services
if ($LASTEXITCODE -ne 0) {
    Write-Fail "镜像重建失败。"
    exit $LASTEXITCODE
}
Write-Ok "镜像重建完成。"

Write-Info "启动容器: $($services -join ", ")"
docker compose --env-file $envPath -f $composePath up -d @services
if ($LASTEXITCODE -ne 0) {
    Write-Fail "容器启动失败。"
    exit $LASTEXITCODE
}
Write-Ok "容器启动完成。"

Write-Info "当前容器状态"
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | Select-String -Pattern "c_exphlp_webapp|c_exphlp_vue_front|NAMES"
