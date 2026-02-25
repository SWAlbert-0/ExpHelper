param(
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipLocalCacheClean,
    [switch]$SkipFrontNoCacheBuild
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Join-Path $scriptDir "deploy-runtime.ps1"

Write-Host "[WARN] scripts/rebuild-webapp-front.ps1 已进入兼容模式，请迁移到 scripts/deploy-runtime.ps1" -ForegroundColor Yellow
$args = @(
    "-ExecutionPolicy", "Bypass",
    "-File", $target,
    "-EnvFile", $EnvFile,
    "-ComposeFile", $ComposeFile
)
if ($SkipLocalCacheClean) {
    $args += "-SkipLocalCacheClean"
}
if ($SkipFrontNoCacheBuild) {
    $args += "-SkipFrontNoCacheBuild"
}

& powershell @args
exit $LASTEXITCODE
