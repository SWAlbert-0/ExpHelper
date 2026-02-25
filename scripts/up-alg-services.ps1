param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipLocalPackage,
    [ValidateSet("auto", "online", "offline")]
    [string]$Mode = "auto",
    [int]$WaitNacosSec = 120
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile
$m2LocalRepo = (Join-Path $repoRoot ".m2repo")
$services = @("nsga2_zdt1_example", "moead_lsmop_example", "smpso_lsmop_example", "spea2_lsmop_example")
$serviceNameMap = @{
    "c_nsga2_zdt1_example" = "nsga2-zdt1-ls"
    "c_moead_lsmop_example" = "moead-lsmop-ls"
    "c_smpso_lsmop_example" = "smpso-lsmop-ls"
    "c_spea2_lsmop_example" = "spea2-lsmop-ls"
}
$algServiceDirs = @(
    @{ dir = "docs/cases/moo-nsga2-zdt1/algorithm_service"; jar = "nsga2-zdt1-ls-1.0.0.jar" },
    @{ dir = "docs/cases/moo-moead-lsmop/algorithm_service"; jar = "moead-lsmop-ls-1.0.0.jar" },
    @{ dir = "docs/cases/moo-smpso-lsmop/algorithm_service"; jar = "smpso-lsmop-ls-1.0.0.jar" },
    @{ dir = "docs/cases/moo-spea2-lsmop/algorithm_service"; jar = "spea2-lsmop-ls-1.0.0.jar" }
)

function Fail([string]$msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
    exit 1
}

function Ensure-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Fail "未找到 docker 命令，请先安装 Docker Desktop。"
    }
    try {
        $null = docker version --format "{{.Server.Version}}" 2>$null
    } catch {
        Fail "docker daemon 不可达，请确认 Docker Desktop 已启动且当前终端有访问权限。"
    }
}

function Ensure-Maven {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue) -and -not (Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
        Fail "未找到 mvn 命令，请先安装 Maven 并配置 PATH。"
    }
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
        Write-Host "[FAIL] 打包失败目录: $workDir" -ForegroundColor Red
        Write-Host ("[FAIL] 执行参数: mvn {0}" -f ($argList -join " ")) -ForegroundColor Red
        exit $proc.ExitCode
    }
}

function Ensure-LocalJarExists([string]$workDir, [string]$jarName) {
    $jarPath = Join-Path (Join-Path $workDir "target") $jarName
    if (-not (Test-Path $jarPath)) {
        Fail "未找到本地 JAR：$jarPath（offline 模式要求先本机打包）"
    }
}

function Get-NacosHealthyCount([string]$serviceName) {
    try {
        $uri = "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=$serviceName&groupName=DEFAULT_GROUP&namespaceId=public"
        $resp = Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 5
        if ($resp -and $resp.hosts) {
            return @($resp.hosts | Where-Object { $_.healthy -eq $true -and $_.enabled -ne $false }).Count
        }
    } catch {
        return 0
    }
    return 0
}

Ensure-Docker

$resolvedMode = $Mode
if ($resolvedMode -eq "auto") {
    $resolvedMode = "online"
    if (-not $SkipLocalPackage) {
        $resolvedMode = "offline"
    }
}

if ($resolvedMode -eq "offline") {
    Ensure-Maven
    Write-Host "[INFO] 先在本机打包算法服务 JAR（规避 Docker Hub 拉取 maven 镜像失败）" -ForegroundColor Cyan
    foreach ($item in $algServiceDirs) {
        $fullDir = Join-Path $repoRoot $item.dir
        if (-not (Test-Path $fullDir)) {
            Fail "目录不存在: $fullDir"
        }
        Write-Host ("  - packaging: {0}" -f $item.dir)
        Invoke-MavenPackage -workDir $fullDir
        Ensure-LocalJarExists -workDir $fullDir -jarName $item.jar
    }
} elseif ($resolvedMode -eq "online") {
    if (-not $SkipLocalPackage) {
        Ensure-Maven
        Write-Host "[INFO] online 模式仍执行本机打包（可用 -SkipLocalPackage 跳过）" -ForegroundColor Cyan
        foreach ($item in $algServiceDirs) {
            $fullDir = Join-Path $repoRoot $item.dir
            if (-not (Test-Path $fullDir)) {
                Fail "目录不存在: $fullDir"
            }
            Write-Host ("  - packaging: {0}" -f $item.dir)
            Invoke-MavenPackage -workDir $fullDir
            Ensure-LocalJarExists -workDir $fullDir -jarName $item.jar
        }
    }
}

Write-Host "[INFO] 启动算法服务容器（后台常驻）: $($services -join ', '), mode=$resolvedMode" -ForegroundColor Cyan
if ($resolvedMode -eq "offline") {
    docker compose --env-file $envPath -f $composePath up -d --build @services
} else {
    docker compose --env-file $envPath -f $composePath up -d --build @services
}
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] 算法服务容器启动失败。" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[OK] 算法服务容器启动完成。" -ForegroundColor Green
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Select-String -Pattern "c_nsga2_zdt1_example|c_moead_lsmop_example|c_smpso_lsmop_example|c_spea2_lsmop_example|NAMES"

Write-Host "[INFO] 等待算法服务在 Nacos 注册完成（timeout=${WaitNacosSec}s）" -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds($WaitNacosSec)
$allReady = $false
while ((Get-Date) -lt $deadline) {
    $readyCount = 0
    foreach ($k in $serviceNameMap.Keys) {
        $healthy = Get-NacosHealthyCount -serviceName $serviceNameMap[$k]
        if ($healthy -gt 0) { $readyCount++ }
    }
    if ($readyCount -eq $serviceNameMap.Count) {
        $allReady = $true
        break
    }
    Start-Sleep -Seconds 3
}

if (-not $allReady) {
    Write-Host "[FAIL] 等待 Nacos 注册超时，输出容器状态与日志尾部用于排查。" -ForegroundColor Red
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Select-String -Pattern "c_nsga2_zdt1_example|c_moead_lsmop_example|c_smpso_lsmop_example|c_spea2_lsmop_example|NAMES"
    foreach ($k in $serviceNameMap.Keys) {
        Write-Host "---- logs: $k ----" -ForegroundColor Yellow
        docker logs --tail 60 $k
    }
    exit 1
}

foreach ($k in $serviceNameMap.Keys) {
    $svc = $serviceNameMap[$k]
    $healthy = Get-NacosHealthyCount -serviceName $svc
    Write-Host ("[OK] nacos service={0}, healthy={1}" -f $svc, $healthy) -ForegroundColor Green
}
Write-Host "[INFO] 可在 Nacos 检查服务是否健康: http://localhost:8848/nacos" -ForegroundColor Cyan
