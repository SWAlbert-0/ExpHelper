param(
    [ValidateSet("bootstrap", "deploy", "check", "alg-up", "alg-down", "alg-check")]
    [string]$Action = "bootstrap",
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

function Invoke-Script([string]$name, [string[]]$extraArgs) {
    $target = Join-Path $scriptDir $name
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
}
