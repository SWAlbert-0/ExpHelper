param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$User = "admin",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Write-Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }

function Invoke-MultipartUpload([string]$uri, [hashtable]$headers, [string]$fieldName, [string]$filePath, [string]$fileName, [string]$contentType = "image/png") {
    $boundary = [System.Guid]::NewGuid().ToString("N")
    $newLine = "`r`n"
    $fileBytes = [System.IO.File]::ReadAllBytes($filePath)

    $preamble = "--$boundary$newLine" +
        "Content-Disposition: form-data; name=`"$fieldName`"; filename=`"$fileName`"$newLine" +
        "Content-Type: $contentType$newLine$newLine"
    $epilogue = "$newLine--$boundary--$newLine"

    $preambleBytes = [System.Text.Encoding]::UTF8.GetBytes($preamble)
    $epilogueBytes = [System.Text.Encoding]::UTF8.GetBytes($epilogue)

    $body = New-Object byte[] ($preambleBytes.Length + $fileBytes.Length + $epilogueBytes.Length)
    [System.Array]::Copy($preambleBytes, 0, $body, 0, $preambleBytes.Length)
    [System.Array]::Copy($fileBytes, 0, $body, $preambleBytes.Length, $fileBytes.Length)
    [System.Array]::Copy($epilogueBytes, 0, $body, $preambleBytes.Length + $fileBytes.Length, $epilogueBytes.Length)

    Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "multipart/form-data; boundary=$boundary" -Body $body
}

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
    $allPass = (Test-Endpoint "GET /api/auth/me" {
        $headers = @{ Authorization = "Bearer $token" }
        $r = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/auth/me" -Headers $headers
        if (-not $r -or $r.code -ne 200 -or -not $r.data.user) { throw "unexpected response" }
    }) -and $allPass

    $allPass = (Test-Endpoint "POST /api/auth/avatar" {
        $tmpFile = [System.IO.Path]::GetTempFileName() + ".png"
        [byte[]]$png = 137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,15,4,0,9,251,3,253,167,90,142,199,0,0,0,0,73,69,78,68,174,66,96,130
        [System.IO.File]::WriteAllBytes($tmpFile, $png)
        try {
            $headers = @{ Authorization = "Bearer $token" }
            $r = Invoke-MultipartUpload -uri "$ApiBase/api/auth/avatar" -headers $headers -fieldName "file" -filePath $tmpFile -fileName "avatar.png"
            if (-not $r -or $r.code -ne 200 -or -not $r.data.avatar) { throw "unexpected response" }
        } finally {
            Remove-Item -ErrorAction SilentlyContinue $tmpFile
        }
    }) -and $allPass

    $allPass = (Test-Endpoint "POST /api/ExePlanController/execute?planId=bad-id" {
        $headers = @{ Authorization = "Bearer $token" }
        $r = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ExePlanController/execute?planId=bad-id" -Headers $headers
        if (-not $r -or -not $r.code) { throw "unexpected response" }
        if ($r.code -eq 200 -or $r.code -lt 400) {
            throw "expected invalid planId to return client/server error code"
        }
    }) -and $allPass
}

if (-not $allPass) {
    exit 1
}

Write-Ok "Smoke check passed."
