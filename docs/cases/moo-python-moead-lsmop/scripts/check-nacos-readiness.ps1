param(
  [string]$ServiceName = "py-moead-lsmop-ls",
  [string]$AlgHttpBase = "http://localhost:18087",
  [string]$NacosBase = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

Write-Host "==> 检查 Nacos 服务实例: $ServiceName" -ForegroundColor Cyan
$u = "$NacosBase/nacos/v1/ns/instance/list?serviceName=$ServiceName&groupName=DEFAULT_GROUP&namespaceId=public"
$r = Invoke-RestMethod -Method Get -Uri $u -TimeoutSec 8
$healthy = @($r.hosts | Where-Object { $_.healthy -eq $true -and $_.enabled -ne $false }).Count
if ($healthy -le 0) { throw "Nacos中无可用实例: $ServiceName" }
Write-Host "[OK] Nacos healthy实例=$healthy" -ForegroundColor Green

Write-Host "==> 检查算法健康接口: $AlgHttpBase/myAlg/" -ForegroundColor Cyan
$h = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$AlgHttpBase/myAlg/" -TimeoutSec 8
if ($h.StatusCode -ne 200) { throw "算法健康接口异常: $($h.StatusCode)" }
Write-Host "[OK] 算法健康接口可访问" -ForegroundColor Green
