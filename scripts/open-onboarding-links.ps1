param(
    [string]$FrontUrl = "http://localhost:8086",
    [string]$NacosUrl = "http://localhost:8848/nacos",
    [string]$DocPath = "docs/user/快速上手-从部署到执行.md"
)

$ErrorActionPreference = "Stop"

Write-Host "Opening key onboarding links..." -ForegroundColor Cyan
Start-Process $FrontUrl
Start-Process $NacosUrl

$fullDoc = Resolve-Path $DocPath
Start-Process $fullDoc.Path

Write-Host "Opened front + nacos + quickstart doc." -ForegroundColor Green
