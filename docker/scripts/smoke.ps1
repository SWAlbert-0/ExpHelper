param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$User = "admin",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Write-Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }

function Test-Endpoint([string]$name, [scriptblock]$action) {
    try {
        & $action
        Write-Ok $name
        return $true
    } catch {
        Write-Fail "$name -> $($_.Exception.Message)"
        return $false
    }
}

$allPass = $true

$allPass = (Test-Endpoint "GET /api/auth/healthz" {
    $r = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/auth/healthz"
    if (-not $r -or $r.code -ne 200) { throw "unexpected response" }
}) -and $allPass

$allPass = (Test-Endpoint "GET /api/auth/captcha" {
    $r = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/auth/captcha"
    if (-not $r -or $r.code -ne 200) { throw "unexpected response" }
}) -and $allPass

$token = $null
$allPass = (Test-Endpoint "POST /api/auth/login" {
    $body = @{ username = $User; password = $Password } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/auth/login" -ContentType "application/json" -Body $body
    if (-not $r -or $r.code -ne 200 -or -not $r.data.token) { throw "login failed" }
    $script:token = $r.data.token
}) -and $allPass

if ($token) {
    $allPass = (Test-Endpoint "POST /api/ExePlanController/execute?planId=bad-id" {
        $headers = @{ Authorization = "Bearer $token" }
        $r = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ExePlanController/execute?planId=bad-id" -Headers $headers
        if (-not $r -or -not $r.code) { throw "unexpected response" }
        if ($r.code -eq 200) {
            Write-Warn "bad-id returned code=200 (accepted=$($r.data.accepted))"
        }
    }) -and $allPass
}

if (-not $allPass) {
    exit 1
}

Write-Ok "Smoke check passed."
