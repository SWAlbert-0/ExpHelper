param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "123456",
    [string]$ProbPrefix = "spea2-lsmop-",
    [string]$AlgPrefix = "spea2-lsmop-ls-",
    [string]$PlanPrefix = "plan-spea2-lsmop-"
)

$ErrorActionPreference = "Stop"

function Login([string]$ApiBase, [string]$Username, [string]$Password) {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/auth/login" -ContentType "application/json" -Body $body
    if (-not $resp -or $resp.code -ne 200 -or -not $resp.data.token) {
        throw "login failed"
    }
    return @{ Authorization = "Bearer $($resp.data.token)" }
}

$headers = Login -ApiBase $ApiBase -Username $Username -Password $Password

Write-Host "==> cleanup plans"
for ($page = 1; $page -le 20; $page++) {
    $plans = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/ExePlanController/getExePlans?pageNum=$page&pageSize=50" -Headers $headers
    if (-not $plans -or $plans.Count -eq 0) { break }
    foreach ($p in $plans) {
        if ($p.planName -like "$PlanPrefix*") {
            try {
                Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ExePlanController/deleteExePlanById?planId=$($p.planId)" -Headers $headers | Out-Null
                Write-Host "deleted plan: $($p.planName)"
            } catch {
                Write-Host "skip plan delete: $($p.planName)"
            }
        }
    }
}

Write-Host "==> cleanup algorithms"
for ($page = 1; $page -le 20; $page++) {
    $algs = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/AlgController/getAlgs?pageNum=$page&pageSize=50" -Headers $headers
    if (-not $algs -or $algs.Count -eq 0) { break }
    foreach ($a in $algs) {
        if ($a.algName -like "$AlgPrefix*") {
            try {
                Invoke-RestMethod -Method Post -Uri "$ApiBase/api/AlgController/deleteAlgById?algId=$($a.algId)" -Headers $headers | Out-Null
                Write-Host "deleted alg: $($a.algName)"
            } catch {
                Write-Host "skip alg delete: $($a.algName)"
            }
        }
    }
}

Write-Host "==> cleanup problems"
for ($page = 1; $page -le 20; $page++) {
    $probs = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/ProbController/getProblems?pageNum=$page&pageSize=50" -Headers $headers
    if (-not $probs -or $probs.Count -eq 0) { break }
    foreach ($p in $probs) {
        if ($p.instName -like "$ProbPrefix*") {
            try {
                Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ProbController/deleteProblemById?proId=$($p.instId)" -Headers $headers | Out-Null
                Write-Host "deleted prob: $($p.instName)"
            } catch {
                Write-Host "skip prob delete: $($p.instName)"
            }
        }
    }
}

Write-Host "cleanup completed."

