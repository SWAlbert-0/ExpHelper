param(
    [string]$EnvFile = ".env",
    [switch]$SkipImageCheck
)

$ErrorActionPreference = "Stop"

function Write-Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }

function Test-CommandExists([string]$name) {
    $null -ne (Get-Command $name -ErrorAction SilentlyContinue)
}

function Test-PortInUse([int]$port) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$dockerDir = Split-Path -Parent $scriptDir
$repoDir = Resolve-Path (Join-Path $dockerDir "..")
$envPath = Join-Path $dockerDir $EnvFile
$jarPath = Join-Path $repoDir "exphlp/api/webApp/target/webApp-1.0-SNAPSHOT.jar"

Write-Host "== ExpHelper preflight check ==" -ForegroundColor Cyan
Write-Host "Repo: $repoDir"
Write-Host "Env : $envPath"

if (-not (Test-CommandExists "docker")) {
    Write-Fail "docker not found in PATH."
    exit 1
}
Write-Ok "docker command is available."

try {
    $null = docker version --format "{{.Server.Version}}" 2>$null
    Write-Ok "docker daemon is reachable."
} catch {
    Write-Fail "docker daemon is not reachable."
    exit 1
}

if (-not (Test-Path $envPath)) {
    Write-Fail "$EnvFile not found under docker/."
    Write-Host "Run: copy .env.example .env" -ForegroundColor DarkGray
    exit 1
}
Write-Ok "$EnvFile exists."

if (-not (Test-Path $jarPath)) {
    Write-Fail "webapp jar missing: $jarPath"
    Write-Host "Run: cd exphlp && mvn -pl api/webApp -am clean package -DskipTests" -ForegroundColor DarkGray
    exit 1
}
Write-Ok "webapp jar exists."

$requiredKeys = @(
    "WEBAPP_BASE_IMAGE",
    "FRONT_NODE_IMAGE",
    "FRONT_NGINX_IMAGE",
    "MONGO_IMAGE",
    "RABBITMQ_IMAGE",
    "NACOS_IMAGE",
    "WEBAPP_PORT",
    "FRONT_PORT",
    "MONGO_PORT",
    "RABBITMQ_PORT",
    "RABBITMQ_MGMT_PORT",
    "NACOS_PORT",
    "APP_MONGO_DB",
    "APP_MONGO_USER",
    "APP_MONGO_PASSWORD",
    "RABBITMQ_USERNAME",
    "RABBITMQ_PASSWORD"
)

$envMap = @{}
Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -le 0) { return }
    $key = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    $envMap[$key] = $value
}

$missing = @()
foreach ($key in $requiredKeys) {
    if (-not $envMap.ContainsKey($key)) { $missing += $key }
}
if ($missing.Count -gt 0) {
    Write-Fail "Missing keys in ${EnvFile}: $($missing -join ", ")"
    exit 1
}
Write-Ok "Required env keys are present."

$ports = @("WEBAPP_PORT", "FRONT_PORT", "MONGO_PORT", "RABBITMQ_PORT", "RABBITMQ_MGMT_PORT", "NACOS_PORT")
foreach ($name in $ports) {
    $value = $envMap[$name]
    [int]$port = 0
    if (-not [int]::TryParse($value, [ref]$port)) {
        Write-Fail "$name is not a valid integer: $value"
        exit 1
    }
    if (Test-PortInUse $port) {
        Write-Warn "$name=$port is already in use."
    } else {
        Write-Ok "$name=$port is available."
    }
}

if (-not $SkipImageCheck) {
    $imageVars = @("MONGO_IMAGE", "RABBITMQ_IMAGE", "NACOS_IMAGE", "FRONT_NODE_IMAGE", "FRONT_NGINX_IMAGE", "WEBAPP_BASE_IMAGE")
    foreach ($name in $imageVars) {
        $img = $envMap[$name]
        if ([string]::IsNullOrWhiteSpace($img)) {
            Write-Fail "$name is empty."
            exit 1
        }
        $exists = docker image inspect $img 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "$name image exists locally: $img"
        } else {
            Write-Warn "$name image not found locally: $img"
        }
    }
}

Write-Host ""
Write-Host "Preflight passed. Suggested next step:" -ForegroundColor Cyan
Write-Host "docker compose --env-file .env up -d --build" -ForegroundColor DarkGray
