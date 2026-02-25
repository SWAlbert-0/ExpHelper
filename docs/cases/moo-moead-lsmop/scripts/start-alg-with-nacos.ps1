param(
    [string]$NacosBase = "http://localhost:8848",
    [string]$ServiceName = "moead-lsmop-ls",
    [int]$AlgPort = 18083,
    [int]$TimeoutSec = 90,
    [string]$NacosNamespace = "public",
    [string]$NacosGroup = "DEFAULT_GROUP",
    [string]$MavenSettings = ""
)

$ErrorActionPreference = "Stop"

function Get-ServiceInstanceCount {
    param(
        [string]$Base,
        [string]$Svc,
        [string]$Ns,
        [string]$Grp
    )
    try {
        $uri = "$Base/nacos/v1/ns/instance/list?serviceName=$Svc&groupName=$Grp&namespaceId=$Ns"
        $resp = Invoke-RestMethod -Method Get -Uri $uri
        if ($resp -and $resp.hosts) {
            return @($resp.hosts).Count
        }
        return 0
    } catch {
        return -1
    }
}

function Test-AlgHttp {
    param([int]$Port)
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:$Port/myAlg/" -UseBasicParsing -TimeoutSec 3
        return $true
    } catch {
        if ($_.Exception.Response) {
            return $true
        }
        return $false
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

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$algDir = Resolve-Path (Join-Path $scriptDir "../algorithm_service")
$logDir = Resolve-Path (Join-Path $scriptDir "../../../../")
$logFile = Join-Path $logDir "temp/moead-lsmop-ls-start.log"
$errFile = Join-Path $logDir "temp/moead-lsmop-ls-start.err.log"

Write-Host "==> 检查 Nacos 可达性: $NacosBase"
$count = Get-ServiceInstanceCount -Base $NacosBase -Svc $ServiceName -Ns $NacosNamespace -Grp $NacosGroup
if ($count -lt 0) {
    Write-Host "Nacos 不可达，请先启动 Nacos。" -ForegroundColor Red
    exit 2
}

$baseUri = [Uri]$NacosBase
$grpcPort = $baseUri.Port + 1000
if (-not (Test-TcpPort -HostName $baseUri.Host -Port $grpcPort)) {
    Write-Host "Nacos gRPC 端口不可达: $($baseUri.Host):$grpcPort" -ForegroundColor Red
    Write-Host "请确认 Nacos 2.x 已开放 9848/9849（docker compose 需映射这两个端口）。" -ForegroundColor Yellow
    exit 2
}

if ($count -gt 0) {
    Write-Host "服务[$ServiceName]已存在 $count 个实例，无需重复启动。" -ForegroundColor Green
    exit 0
}

$portOccupied = $false
try {
    $listener = Get-NetTCPConnection -LocalPort $AlgPort -State Listen -ErrorAction Stop
    if ($listener) {
        $portOccupied = $true
    }
} catch {
    $portOccupied = $false
}

if ($portOccupied -and -not (Test-AlgHttp -Port $AlgPort)) {
    Write-Host "端口 $AlgPort 已被占用且不是可用算法服务，请先释放端口后重试。" -ForegroundColor Red
    exit 3
}

Write-Host "==> 启动算法服务并等待注册: $ServiceName"
if (Test-Path $logFile) {
    Remove-Item $logFile -Force
}
if (Test-Path $errFile) {
    Remove-Item $errFile -Force
}

$oldNacos = $env:NACOS_SERVER_ADDR
$oldSvc = $env:ALG_SERVICE_NAME
$oldPort = $env:ALG_PORT
$oldNs = $env:NACOS_NAMESPACE
$oldGrp = $env:NACOS_GROUP

$nacosHost = ($NacosBase -replace "^https?://", "")
$env:NACOS_SERVER_ADDR = $nacosHost
$env:ALG_SERVICE_NAME = $ServiceName
$env:ALG_PORT = [string]$AlgPort
$env:NACOS_NAMESPACE = $NacosNamespace
$env:NACOS_GROUP = $NacosGroup

$mvnArgs = @("spring-boot:run")
if ($MavenSettings -ne "") {
    $settingsPath = $MavenSettings
    if (-not [System.IO.Path]::IsPathRooted($settingsPath)) {
        $settingsPath = Resolve-Path (Join-Path $logDir $MavenSettings)
    }
    $mvnArgs = @("-s", [string]$settingsPath, "spring-boot:run")
}

$proc = Start-Process -FilePath "mvn" `
    -ArgumentList $mvnArgs `
    -WorkingDirectory $algDir `
    -RedirectStandardOutput $logFile `
    -RedirectStandardError $errFile `
    -PassThru

$deadline = (Get-Date).AddSeconds($TimeoutSec)
$registered = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if ($proc.HasExited) {
        break
    }
    $count = Get-ServiceInstanceCount -Base $NacosBase -Svc $ServiceName -Ns $NacosNamespace -Grp $NacosGroup
    if ($count -gt 0) {
        $registered = $true
        break
    }
}

$env:NACOS_SERVER_ADDR = $oldNacos
$env:ALG_SERVICE_NAME = $oldSvc
$env:ALG_PORT = $oldPort
$env:NACOS_NAMESPACE = $oldNs
$env:NACOS_GROUP = $oldGrp

if ($registered) {
    Write-Host "注册成功: 服务[$ServiceName]已出现在 Nacos，进程PID=$($proc.Id)" -ForegroundColor Green
    Write-Host "日志: $logFile"
    exit 0
}

Write-Host "注册失败或超时，最近日志如下:" -ForegroundColor Red
if (Test-Path $logFile) {
    Get-Content $logFile -Tail 120
}
if (Test-Path $errFile) {
    Get-Content $errFile -Tail 80
}
if (-not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force
}
exit 1

