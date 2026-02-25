param(
    [switch]$SkipSmoke = $false,
    [switch]$SkipE2E = $false,
    [string]$ApiBase = "http://localhost:8080",
    [string]$EnvFile = "docker/.env",
    [string]$ComposeFile = "docker/docker-compose.yml"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$m2LocalRepo = Join-Path $repoRoot ".m2repo"
$envPath = Join-Path $repoRoot $EnvFile
$composePath = Join-Path $repoRoot $ComposeFile

function Invoke-Maven([string[]]$mvnArgs, [string]$workingDir) {
    if (-not (Test-Path $m2LocalRepo)) {
        New-Item -Path $m2LocalRepo -ItemType Directory -Force | Out-Null
    }
    $repoArg = "-Dmaven.repo.local=$($m2LocalRepo -replace '\\','/')"
    $argList = @($repoArg) + $mvnArgs
    $mvnCmd = "mvn"
    if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) {
        $mvnCmd = "mvn.cmd"
    }
    $proc = Start-Process -FilePath $mvnCmd -ArgumentList $argList -WorkingDirectory $workingDir -Wait -PassThru -NoNewWindow
    return $proc.ExitCode
}

function Run-Step([string]$name, [scriptblock]$action) {
    Write-Host "==> $name" -ForegroundColor Cyan
    try {
        & $action
        Write-Host "[PASS] $name" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

function Invoke-FrontDockerBuildFallback {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "frontend build failed and docker is unavailable for fallback."
    }
    if (-not (Test-Path $envPath)) {
        throw "frontend build failed and env file not found for fallback: $envPath"
    }
    if (-not (Test-Path $composePath)) {
        throw "frontend build failed and compose file not found for fallback: $composePath"
    }
    Write-Host "[WARN] 启用 Docker 前端构建回退（本机 Node 进程权限受限）" -ForegroundColor Yellow
    docker compose --env-file $envPath -f $composePath build exphlp_front
    if ($LASTEXITCODE -ne 0) {
        throw "frontend docker build fallback failed with exit code $LASTEXITCODE"
    }
}

Run-Step "Backend tests (mvn -q test)" {
    $backendDir = Join-Path $repoRoot "exphlp"
    $code = Invoke-Maven -mvnArgs @("-q", "test") -workingDir $backendDir
    if ($code -ne 0) {
        throw "mvn test failed with exit code $code"
    }
}

Run-Step "Frontend targeted eslint" {
    Push-Location "exphlp-front"
    try {
        npx eslint src/store/modules/permission.js src/router/index.js src/layout/components/Navbar.vue src/utils/request.js src/utils/errorCode.js src/views/platform/platformManage.vue src/views/profile/userInfo.vue src/api/auth.js
        if ($LASTEXITCODE -ne 0) {
            throw "eslint failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

Run-Step "Frontend legacy admin baseline" {
    Push-Location "exphlp-front"
    try {
        $adminMatches = @()
        $raw = rg -n "/admin/" src
        if ($LASTEXITCODE -eq 0) {
            $adminMatches = $raw
        } elseif ($LASTEXITCODE -ne 1) {
            throw "Failed to scan /admin/ baseline."
        }
        $allowed = @("src\utils\request.js:")
        foreach ($line in $adminMatches) {
            $isAllowed = $false
            foreach ($prefix in $allowed) {
                if ($line.StartsWith($prefix)) {
                    $isAllowed = $true
                    break
                }
            }
            if (-not $isAllowed) {
                throw "Unexpected /admin/ reference: $line"
            }
        }
    } finally {
        Pop-Location
    }
}

Run-Step "Frontend legacy api-import baseline" {
    Push-Location "exphlp-front"
    try {
        $legacyImports = @()
        $raw = rg -n "@/api/vadmin" src
        if ($LASTEXITCODE -eq 0) {
            $legacyImports = $raw
        } elseif ($LASTEXITCODE -ne 1) {
            throw "Failed to scan legacy api imports."
        }
        foreach ($line in $legacyImports) {
            throw "Unexpected legacy api import: $line"
        }
    } finally {
        Pop-Location
    }
}

Run-Step "Frontend production build" {
    Push-Location "exphlp-front"
    try {
        npm run build:prod
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[WARN] 首次前端构建失败，重试并禁用 terser 并行子进程。" -ForegroundColor Yellow
            $oldTerserParallel = $env:TERSER_PARALLEL
            try {
                $env:TERSER_PARALLEL = "false"
                npm run build:prod
                if ($LASTEXITCODE -ne 0) {
                    Invoke-FrontDockerBuildFallback
                }
            } finally {
                $env:TERSER_PARALLEL = $oldTerserParallel
            }
        }
    } finally {
        Pop-Location
    }
}

if (-not $SkipE2E) {
    Run-Step "Playwright core e2e" {
        Push-Location "exphlp-front"
        try {
            npx playwright test tests/e2e/auth-login.spec.js tests/e2e/plan-manage.spec.js
            if ($LASTEXITCODE -ne 0) {
                throw "playwright e2e failed with exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }
}

if (-not $SkipSmoke) {
    Run-Step "Smoke test" {
        try {
            $health = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/auth/healthz" -TimeoutSec 5
            if (-not $health -or $health.code -ne 200) {
                throw "healthz check failed"
            }
        } catch {
            throw "Smoke test requires running backend at $ApiBase. Start webApp first or rerun with -SkipSmoke."
        }
        powershell -ExecutionPolicy Bypass -File "docker/scripts/smoke.ps1" -ApiBase $ApiBase
        if ($LASTEXITCODE -ne 0) {
            throw "smoke script failed with exit code $LASTEXITCODE"
        }
    }
}

Write-Host "P0 checks completed." -ForegroundColor Green
