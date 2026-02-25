param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipLocalCacheClean,
    [switch]$SkipFrontNoCacheBuild
)

$ErrorActionPreference = "Stop"

function Write-Ok([string]$msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Info([string]$msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Fail([string]$msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }
function Invoke-MavenPackage([string]$workDir) {
    if (-not (Test-Path $m2LocalRepo)) {
        New-Item -Path $m2LocalRepo -ItemType Directory -Force | Out-Null
    }
    $repoArg = "-Dmaven.repo.local=$($m2LocalRepo -replace '\\','/')"
    $argList = @("-q", $repoArg, "-DskipTests", "package")
    $mvnCmd = "mvn"
    if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) {
        $mvnCmd = "mvn.cmd"
    }
    $proc = Start-Process -FilePath $mvnCmd -ArgumentList $argList -WorkingDirectory $workDir -Wait -PassThru -NoNewWindow
    if ($proc.ExitCode -ne 0) {
        Write-Fail "webapp 打包失败。"
        exit $proc.ExitCode
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "../..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile
$services = @("webapp", "exphlp_front")
$webappDir = Join-Path $repoRoot "exphlp"
$webappJar = Join-Path $repoRoot "exphlp/api/webApp/target/webApp-1.0-SNAPSHOT.jar"
$frontDir = Join-Path $repoRoot "exphlp-front"
$frontDist = Join-Path $frontDir "dist"
$frontCache = Join-Path $frontDir "node_modules/.cache"
$repoNpmCache = Join-Path $repoRoot ".npm-cache"
$m2LocalRepo = Join-Path $repoRoot ".m2repo"

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

if (-not $SkipLocalCacheClean) {
    Write-Info "清理本地前端缓存与构建产物（用于强制页面刷新）"
    if (Test-Path $frontDist) { Remove-Item -Recurse -Force $frontDist }
    if (Test-Path $frontCache) { Remove-Item -Recurse -Force $frontCache }
    if (Test-Path $repoNpmCache) { Remove-Item -Recurse -Force $repoNpmCache }
    Write-Ok "本地缓存清理完成。"
} else {
    Write-Info "已跳过本地缓存清理（-SkipLocalCacheClean）"
}

Write-Info "先构建 webapp JAR（避免旧包被重复打进镜像）"
Invoke-MavenPackage -workDir $webappDir
if (-not (Test-Path $webappJar)) {
    Write-Fail "未找到打包产物: $webappJar"
    exit 1
}
$jarInfo = Get-Item $webappJar
Write-Ok ("webapp JAR 就绪: {0} | LastWriteTime={1} | Size={2}" -f $jarInfo.FullName, $jarInfo.LastWriteTime, $jarInfo.Length)

Write-Info "重建 webapp 镜像"
docker compose --env-file $envPath -f $composePath build webapp
if ($LASTEXITCODE -ne 0) {
    Write-Fail "镜像重建失败。"
    exit $LASTEXITCODE
}
Write-Ok "webapp 镜像重建完成。"

if ($SkipFrontNoCacheBuild) {
    Write-Info "重建 exphlp_front 镜像（允许使用缓存）"
    docker compose --env-file $envPath -f $composePath build exphlp_front
} else {
    Write-Info "重建 exphlp_front 镜像（--no-cache，强制刷新页面静态资源）"
    docker compose --env-file $envPath -f $composePath build --no-cache exphlp_front
}
if ($LASTEXITCODE -ne 0) {
    Write-Fail "前端镜像重建失败。"
    exit $LASTEXITCODE
}
Write-Ok "前端镜像重建完成。"

Write-Info "启动并强制重建容器实例: $($services -join ", ")"
docker compose --env-file $envPath -f $composePath up -d --force-recreate @services
if ($LASTEXITCODE -ne 0) {
    Write-Fail "容器启动失败。"
    exit $LASTEXITCODE
}
Write-Ok "容器启动完成。"

Write-Info "当前容器状态"
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" | Select-String -Pattern "c_exphlp_webapp|c_exphlp_vue_front|NAMES"

Write-Info "校验 webapp/front 健康状态（带重试）"
$webappOk = $false
for ($i = 1; $i -le 20; $i++) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/healthz" -Method Get -TimeoutSec 8
        Write-Ok ("webapp healthz: status={0}, version={1}, buildTime={2}" -f $health.status, $health.artifactVersion, $health.buildTime)
        $webappOk = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $webappOk) {
    Write-Fail "webapp healthz 检查失败，请查看 c_exphlp_webapp 日志。"
    exit 1
}

$frontOk = $false
for ($i = 1; $i -le 10; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8086/healthz" -UseBasicParsing -TimeoutSec 8
        if ($resp.StatusCode -eq 200) {
            Write-Ok "front healthz: status=ok"
            $frontOk = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $frontOk) {
    Write-Fail "front healthz 检查失败，请查看 c_exphlp_vue_front 日志。"
    exit 1
}

$cacheBust = [DateTimeOffset]::Now.ToUnixTimeSeconds()
Write-Info ("建议打开（带缓存穿透参数）: http://localhost:8086/?v={0}" -f $cacheBust)
