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
$repoRoot = Resolve-Path (Join-Path $scriptDir "../..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile
$m2LocalRepo = Join-Path $repoRoot ".m2repo"

function Fail([string]$msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
    exit 1
}

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
        Fail "webapp 打包失败。"
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Fail "未找到 docker 命令，请先安装 Docker Desktop。"
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Fail "未找到 mvn 命令，请先安装 Maven 并配置 PATH。"
}
try {
    $null = docker version --format "{{.Server.Version}}" 2>$null
} catch {
    Fail "docker daemon 不可达，请确认 Docker Desktop 已启动且当前终端有访问权限。"
}

Write-Host "[INFO] bootstrap runtime start" -ForegroundColor Cyan
Write-Host "[INFO] repo=$repoRoot" -ForegroundColor Cyan

if (-not $SkipBaseBuild) {
    Write-Host "[INFO] 先打包 webapp（避免首机 bootstrap 缺少 JAR）" -ForegroundColor Cyan
    Invoke-MavenPackage -workDir (Join-Path $repoRoot "exphlp")
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
    $upScript = Join-Path $repoRoot "scripts/tasks/up-alg-services.ps1"
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
& powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/tasks/check-runtime-readiness.ps1")
if ($LASTEXITCODE -ne 0) {
    Fail "运行时健康检查失败。"
}

if (-not $SkipAlgorithmServices) {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $repoRoot "scripts/tasks/check-alg-services.ps1")
    if ($LASTEXITCODE -ne 0) {
        Fail "算法服务检查失败。"
    }
}

Write-Host "[OK] bootstrap runtime completed." -ForegroundColor Green
