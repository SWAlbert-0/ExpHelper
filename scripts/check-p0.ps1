param(
    [switch]$SkipSmoke = $false,
    [string]$ApiBase = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Run-Step([string]$name, [scriptblock]$action) {
    Write-Host "==> $name" -ForegroundColor Cyan
    & $action
    Write-Host "[PASS] $name" -ForegroundColor Green
}

Run-Step "Backend tests (mvn -q test)" {
    Push-Location "exphlp"
    try {
        mvn -q test
    } finally {
        Pop-Location
    }
}

Run-Step "Frontend targeted eslint" {
    Push-Location "exphlp-front"
    try {
        npx eslint src/store/modules/permission.js src/router/index.js src/layout/components/Navbar.vue src/utils/request.js src/utils/errorCode.js src/views/platform/platformManage.vue src/views/profile/userInfo.vue src/api/auth.js
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
    } finally {
        Pop-Location
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
    }
}

Write-Host "P0 checks completed." -ForegroundColor Green
