param(
    [string]$ApiBase = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Assert-Code200($resp, $name) {
    if (-not $resp -or $resp.code -ne 200) {
        throw "$name failed: $($resp | ConvertTo-Json -Depth 8)"
    }
}

function To-Body($obj, [int]$depth = 8) {
    return ($obj | ConvertTo-Json -Depth $depth)
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Resolve-Path (Join-Path $scriptDir "..")
$caseAPath = Join-Path $rootDir "problem_instances/zdt1-ls100-caseA.json"

Write-Host "==> login"
$loginBody = To-Body @{ username = $Username; password = $Password }
$login = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/auth/login" -ContentType "application/json" -Body $loginBody
Assert-Code200 $login "login"
$token = $login.data.token
if (-not $token) { throw "login token is empty" }
$headers = @{ Authorization = "Bearer $token" }

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$probName = "zdt1-ls100-$suffix"
$algName = "nsga2-zdt1-ls-$suffix"
$planName = "plan-zdt1-ls100-$suffix"

Write-Host "==> add problem"
$caseA = Get-Content $caseAPath -Raw | ConvertFrom-Json
$probBody = @{
    categoryName = $caseA.categoryName
    instName = $probName
    machineIp = $caseA.machineIp
    dirName = $caseA.dirName
    machineName = $caseA.machineName
    description = $caseA.description
}
Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ProbController/addProblem" -Headers $headers -ContentType "application/json" -Body (To-Body $probBody) | Out-Null
$probList = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/ProbController/getProblemsByName?probName=$probName&pageNum=1&pageSize=10" -Headers $headers
$prob = $probList | Where-Object { $_.instName -eq $probName } | Select-Object -First 1
if (-not $prob) { throw "problem not found: $probName" }
$probId = $prob.instId

Write-Host "==> add algorithm"
$algBody = @{
    algName = $algName
    serviceName = "nsga2-zdt1-ls"
    description = "NSGA-II large-scale ZDT1 example"
    defParas = @(
        @{ paraId = 1; paraName = "nVars"; paraType = "int"; paraValue = "100"; description = "number of decision variables" },
        @{ paraId = 2; paraName = "populationSize"; paraType = "int"; paraValue = "200"; description = "population size" },
        @{ paraId = 3; paraName = "maxGenerations"; paraType = "int"; paraValue = "250"; description = "max generations" },
        @{ paraId = 4; paraName = "crossoverProbability"; paraType = "double"; paraValue = "0.9"; description = "SBX probability" },
        @{ paraId = 5; paraName = "mutationProbability"; paraType = "double"; paraValue = "0.01"; description = "mutation probability" },
        @{ paraId = 6; paraName = "seed"; paraType = "long"; paraValue = "20260223"; description = "random seed" }
    )
}
Invoke-RestMethod -Method Post -Uri "$ApiBase/api/AlgController/addAlg" -Headers $headers -ContentType "application/json" -Body (To-Body $algBody) | Out-Null
$algList = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/AlgController/getAlgsByName?algName=$algName&pageNum=1&pageSize=10" -Headers $headers
$alg = $algList | Where-Object { $_.algName -eq $algName } | Select-Object -First 1
if (-not $alg) { throw "algorithm not found: $algName" }

Write-Host "==> add plan"
$planBody = @{
    planName = $planName
    probInstIds = @($probId)
    algRunInfos = @(
        @{
            algRunInfoId = "1"
            algId = $alg.algId
            algName = $alg.algName
            serviceName = $alg.serviceName
            runNum = 1
            runParas = $alg.defParas
        }
    )
    userIds = @()
    exeStartTime = 0
    exeEndTime = 0
    exeState = 1
    description = "NSGA-II + ZDT1 large-scale test plan"
}

$planId = $null
try {
    $planId = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ExePlanController/addExePlan" -Headers $headers -ContentType "application/json" -Body (To-Body $planBody)
} catch {
    Write-Host "addExePlan failed, fallback to get by name"
}
if (-not $planId) {
    $plan = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/ExePlanController/getExePlanByName?planName=$planName" -Headers $headers
    $planId = $plan.planId
}
if (-not $planId) { throw "planId is empty" }

Write-Host "==> execute plan: $planId"
$exec = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/ExePlanController/execute?planId=$planId" -Headers $headers
Assert-Code200 $exec "execute"
if (-not $exec.data.accepted) {
    throw "plan not accepted: $($exec | ConvertTo-Json -Depth 8)"
}

Write-Host "==> poll plan state"
$finalPlan = $null
for ($i = 0; $i -lt 60; $i++) {
    $finalPlan = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/ExePlanController/getExePlanByName?planName=$planName" -Headers $headers
    Write-Host "state=$($finalPlan.exeState), lastError=$($finalPlan.lastError)"
    if ($finalPlan.exeState -eq 3 -or $finalPlan.exeState -eq 4) { break }
    Start-Sleep -Seconds 2
}
if (-not $finalPlan -or $finalPlan.exeState -ne 4) {
    throw "plan not finished successfully: $($finalPlan | ConvertTo-Json -Depth 8)"
}

Write-Host "==> query result"
$result = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/AlgRltSaveController/getAlgSaveByAlgName?planId=$planId&algId=$($alg.algId)&algName=$($alg.algName)-1" -Headers $headers
if (-not $result -or $result.Count -eq 0) {
    throw "empty result set"
}

$keys = @($result | ForEach-Object { $_.key })
foreach ($required in @("paretoSize", "bestF1", "bestF2", "runtimeMs")) {
    if ($keys -notcontains $required) {
        throw "missing summary key: $required"
    }
}

Write-Host ""
Write-Host "===== SUCCESS =====" -ForegroundColor Green
Write-Host "probName = $probName"
Write-Host "algName  = $algName"
Write-Host "planName = $planName"
Write-Host "planId   = $planId"
