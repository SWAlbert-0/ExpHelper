param(
    [ValidateSet("bootstrap", "deploy", "check", "alg-up", "alg-down", "alg-check", "alg-build", "alg-start", "alg-restart", "alg-status", "gate", "onboarding")]
    [string]$Action = "bootstrap",
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml",
    [switch]$SkipBaseBuild,
    [switch]$SkipAlgorithmServices,
    [switch]$SkipLocalPackage,
    [switch]$SkipSmoke,
    [switch]$SkipE2E,
    [switch]$SkipAlgCheck,
    [switch]$SkipRuntime,
    [ValidateSet("auto", "online", "offline")]
    [string]$Mode = "auto"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-Script([string]$name, [string[]]$extraArgs) {
    $target = Join-Path (Join-Path $scriptDir "tasks") $name
    if (-not (Test-Path $target)) {
        Write-Host "[FAIL] script not found: $target" -ForegroundColor Red
        exit 1
    }
    $args = @("-ExecutionPolicy", "Bypass", "-File", $target) + $extraArgs
    & powershell @args
    exit $LASTEXITCODE
}

switch ($Action) {
    "bootstrap" {
        $args = @("-EnvFile", $EnvFile, "-ComposeFile", $ComposeFile, "-Mode", $Mode)
        if ($SkipBaseBuild) { $args += "-SkipBaseBuild" }
        if ($SkipAlgorithmServices) { $args += "-SkipAlgorithmServices" }
        if ($SkipLocalPackage) { $args += "-SkipLocalPackage" }
        Invoke-Script -name "bootstrap-runtime.ps1" -extraArgs $args
    }
    "deploy" {
        $args = @("-EnvFile", $EnvFile, "-ComposeFile", $ComposeFile)
        Invoke-Script -name "deploy-runtime.ps1" -extraArgs $args
    }
    "check" {
        Invoke-Script -name "check-runtime-readiness.ps1" -extraArgs @()
    }
    "alg-up" {
        $args = @("-EnvFile", $EnvFile, "-ComposeFile", $ComposeFile, "-Mode", $Mode)
        if ($SkipLocalPackage) { $args += "-SkipLocalPackage" }
        Invoke-Script -name "up-alg-services.ps1" -extraArgs $args
    }
    "alg-down" {
        $args = @("-EnvFile", $EnvFile, "-ComposeFile", $ComposeFile)
        Invoke-Script -name "down-alg-services.ps1" -extraArgs $args
    }
    "alg-check" {
        Invoke-Script -name "check-alg-services.ps1" -extraArgs @()
    }
    "alg-build" {
        Write-Host "[INFO] alg-build 当前由后端接口触发 scripts/tasks/build-uploaded-alg.ps1 执行。" -ForegroundColor Cyan
        exit 0
    }
    "alg-start" {
        Write-Host "[INFO] alg-start 当前由后端接口触发 scripts/tasks/build-uploaded-alg.ps1 执行。" -ForegroundColor Cyan
        exit 0
    }
    "alg-restart" {
        Write-Host "[INFO] alg-restart 当前由后端接口触发 scripts/tasks/build-uploaded-alg.ps1 执行。" -ForegroundColor Cyan
        exit 0
    }
    "alg-status" {
        Invoke-Script -name "check-alg-services.ps1" -extraArgs @()
    }
    "gate" {
        $target = Join-Path (Join-Path $scriptDir "pipelines") "gate-runtime.ps1"
        if (-not (Test-Path $target)) {
            Write-Host "[FAIL] script not found: $target" -ForegroundColor Red
            exit 1
        }
        $args = @("-ExecutionPolicy", "Bypass", "-File", $target, "-EnvFile", $EnvFile, "-ComposeFile", $ComposeFile)
        if ($SkipSmoke) { $args += "-SkipSmoke" }
        if ($SkipE2E) { $args += "-SkipE2E" }
        if ($SkipAlgCheck) { $args += "-SkipAlgCheck" }
        if ($SkipRuntime) { $args += "-SkipRuntime" }
        & powershell @args
        exit $LASTEXITCODE
    }
    "onboarding" {
        Invoke-Script -name "open-onboarding-links.ps1" -extraArgs @()
    }
}
