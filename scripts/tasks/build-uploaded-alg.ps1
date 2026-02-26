param(
    [Parameter(Mandatory = $true)][string]$TaskId,
    [Parameter(Mandatory = $true)][string]$AlgId,
    [Parameter(Mandatory = $true)][string]$ServiceName,
    [Parameter(Mandatory = $true)][ValidateSet("java", "python")][string]$RuntimeType,
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$SourceZip,
    [Parameter(Mandatory = $true)][string]$ImageName,
    [Parameter(Mandatory = $true)][string]$ContainerName,
    [string]$WorkRoot = "temp/alg-build"
)

$ErrorActionPreference = "Stop"

function Fail([string]$msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
    exit 1
}

function Ensure-Cmd([string]$name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        Fail "未找到命令: $name"
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

Ensure-Cmd "docker"
if (-not (Test-Path $SourceZip)) {
    Fail "源码包不存在: $SourceZip"
}

$taskDir = Join-Path $WorkRoot $TaskId
$srcDir = Join-Path $taskDir "src"
if (Test-Path $srcDir) {
    Remove-Item -Recurse -Force $srcDir
}
New-Item -ItemType Directory -Path $srcDir -Force | Out-Null

Write-Host "[INFO] 解压源码包: $SourceZip -> $srcDir" -ForegroundColor Cyan
Expand-Archive -Path $SourceZip -DestinationPath $srcDir -Force

$metaPath = Join-Path $srcDir "exphlp-alg.json"
if (-not (Test-Path $metaPath)) {
    Fail "缺少 exphlp-alg.json"
}

$meta = Get-Content $metaPath -Raw | ConvertFrom-Json
if (-not $meta) {
    Fail "exphlp-alg.json 解析失败"
}
$metaRuntime = (($meta.runtimeType | Out-String).Trim().ToLower())
if ($metaRuntime -and $metaRuntime -ne $RuntimeType) {
    Fail "运行时不一致，元数据=$metaRuntime，请求=$RuntimeType"
}

$port = 18090
if ($meta.port) {
    $port = [int]$meta.port
}

$networkName = "docker_default"
$networkExists = docker network ls --format "{{.Name}}" | Select-String -SimpleMatch $networkName
if (-not $networkExists) {
    $networkName = ""
}

if ($RuntimeType -eq "java") {
    if (-not (Test-Path (Join-Path $srcDir "pom.xml"))) {
        Fail "Java 工程缺少 pom.xml"
    }
    $dockerfile = @"
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . /build
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
ENV SERVER_PORT=$port
ENTRYPOINT ["java","-jar","/app/app.jar"]
"@
    Set-Content -Path (Join-Path $srcDir "Dockerfile.generated") -Value $dockerfile -Encoding UTF8
} else {
    $entry = (($meta.entry | Out-String).Trim())
    if (-not $entry) {
        $entry = "main:app"
    }
    if (-not (Test-Path (Join-Path $srcDir "requirements.txt")) -and -not (Test-Path (Join-Path $srcDir "pyproject.toml"))) {
        Fail "Python 工程缺少 requirements.txt 或 pyproject.toml"
    }
    $runner = @"
import os
import socket
import threading
import time
import requests
import uvicorn

def local_ip():
    return socket.gethostbyname(socket.gethostname())

def register_nacos():
    nacos = os.getenv("NACOS_SERVER_ADDR", "host.docker.internal:8848")
    if not nacos.startswith("http"):
        nacos = "http://" + nacos
    service_name = os.getenv("ALG_SERVICE_NAME")
    ip = local_ip()
    port = int(os.getenv("ALG_PORT", "$port"))
    namespace_id = os.getenv("NACOS_NAMESPACE", "public")
    group_name = os.getenv("NACOS_GROUP", "DEFAULT_GROUP")
    requests.post(f"{nacos}/nacos/v1/ns/instance", data={
        "serviceName": service_name,
        "ip": ip,
        "port": port,
        "namespaceId": namespace_id,
        "groupName": group_name,
        "healthy": "true",
        "enabled": "true",
        "ephemeral": "true"
    }, timeout=5)
    while True:
        try:
            requests.put(f"{nacos}/nacos/v1/ns/instance/beat", data={
                "serviceName": service_name,
                "ip": ip,
                "port": port,
                "namespaceId": namespace_id,
                "groupName": group_name,
                "beat": '{"ip":"%s","port":%d,"healthy":true}' % (ip, port)
            }, timeout=5)
        except Exception:
            pass
        time.sleep(5)

if __name__ == "__main__":
    threading.Thread(target=register_nacos, daemon=True).start()
    uvicorn.run("$entry", host="0.0.0.0", port=int(os.getenv("ALG_PORT", "$port")))
"@
    Set-Content -Path (Join-Path $srcDir "start_with_nacos.py") -Value $runner -Encoding UTF8
    $dockerfile = @"
FROM python:3.11-slim
WORKDIR /app
COPY . /app
RUN pip install --no-cache-dir -r requirements.txt && pip install --no-cache-dir uvicorn fastapi requests
ENV ALG_PORT=$port
CMD ["python","start_with_nacos.py"]
"@
    Set-Content -Path (Join-Path $srcDir "Dockerfile.generated") -Value $dockerfile -Encoding UTF8
}

Write-Host "[INFO] 构建镜像: $ImageName" -ForegroundColor Cyan
docker build -t $ImageName -f (Join-Path $srcDir "Dockerfile.generated") $srcDir
if ($LASTEXITCODE -ne 0) {
    Fail "Docker build 失败"
}

Write-Host "[INFO] 移除旧容器: $ContainerName" -ForegroundColor Cyan
docker rm -f $ContainerName 2>$null | Out-Null

$args = @("run", "-d", "--name", $ContainerName,
    "-e", "ALG_PORT=$port",
    "-e", "ALG_SERVICE_NAME=$ServiceName",
    "-e", "NACOS_NAMESPACE=public",
    "-e", "NACOS_GROUP=DEFAULT_GROUP")
if ($networkName) {
    $args += @("-e", "NACOS_SERVER_ADDR=nacos:8848", "--network", $networkName)
} else {
    $args += @("-e", "NACOS_SERVER_ADDR=host.docker.internal:8848")
}
if ($RuntimeType -eq "java") {
    $args += @("-e", "SPRING_APPLICATION_NAME=$ServiceName")
}
$args += $ImageName

Write-Host "[INFO] 启动容器: docker $($args -join ' ')" -ForegroundColor Cyan
docker @args
if ($LASTEXITCODE -ne 0) {
    Fail "Docker run 失败"
}

Write-Host "[INFO] 等待 Nacos 注册: $ServiceName" -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    $healthy = Get-NacosHealthyCount -serviceName $ServiceName
    if ($healthy -gt 0) {
        Write-Host "[OK] 服务已注册到 Nacos，healthy=$healthy" -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 3
}

Fail "等待 Nacos 注册超时"
