$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$docPath = Join-Path $repoRoot "docs/dev/配置清单.md"

if (-not (Test-Path $docPath)) {
  Write-Error "缺少配置清单文档: docs/dev/配置清单.md"
}

$docContent = Get-Content -Path $docPath -Raw -Encoding UTF8

function To-RepoRelativePath([string]$fullPath) {
  $full = [System.IO.Path]::GetFullPath($fullPath)
  $root = [System.IO.Path]::GetFullPath($repoRoot.Path)
  if (-not $full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
    return $null
  }
  $rel = $full.Substring($root.Length).TrimStart('\', '/')
  return $rel -replace "\\", "/"
}

function Collect-Candidates {
  $result = New-Object System.Collections.Generic.List[string]

  $explicit = @(
    "docker/.env",
    "docker/docker-compose.yml",
    "docker/alg-runner/Dockerfile",
    "docker/alg-runner/entrypoint.sh",
    "docker/docker-compose.local-front.yml",
    "docker/frontend/nginx.conf",
    "docker/mongo/mongo-init.js",
    "scripts/ops.ps1",
    "scripts/README.md",
    "scripts/pipelines/gate-runtime.ps1",
    "scripts/tasks/bootstrap-runtime.ps1",
    "scripts/tasks/deploy-runtime.ps1",
    "scripts/tasks/up-alg-services.ps1",
    "scripts/tasks/down-alg-services.ps1",
    "scripts/tasks/check-alg-services.ps1",
    "scripts/tasks/check-runtime-readiness.ps1",
    "scripts/tasks/check-p0.ps1",
    "scripts/tasks/open-onboarding-links.ps1",
    "exphlp-front/.env.development",
    "exphlp-front/.env.staging",
    "exphlp-front/.env.production",
    "exphlp-front/vue.config.js",
    "docs/cases/moo-nsga2-zdt1/algorithm_service/src/main/resources/application.yml",
    "docs/cases/moo-moead-lsmop/algorithm_service/src/main/resources/application.yml",
    "docs/cases/moo-smpso-lsmop/algorithm_service/src/main/resources/application.yml",
    "docs/cases/moo-spea2-lsmop/algorithm_service/src/main/resources/application.yml",
    "docs/cases/moo-nsga2-zdt1/scripts/start-alg-with-nacos.ps1",
    "docs/cases/moo-nsga2-zdt1/scripts/check-nacos-readiness.ps1"
  )
  foreach ($item in $explicit) {
    if (Test-Path (Join-Path $repoRoot $item)) {
      $result.Add($item)
    }
  }

  $resourceFiles = Get-ChildItem -Path (Join-Path $repoRoot "exphlp") -Recurse -File |
    Where-Object {
      $_.FullName -match "src\\main\\resources\\application([-.].+)?\.(yml|yaml|properties)$"
    }
  foreach ($f in $resourceFiles) {
    $rel = To-RepoRelativePath $f.FullName
    if ($null -ne $rel) {
      $result.Add($rel)
    }
  }

  return $result | Sort-Object -Unique
}

$candidates = Collect-Candidates
$missing = New-Object System.Collections.Generic.List[string]

foreach ($path in $candidates) {
  $needle = [regex]::Escape(([char]96 + $path + [char]96))
  if ($docContent -notmatch $needle) {
    $missing.Add($path)
  }
}

if ($missing.Count -gt 0) {
  Write-Host "以下配置文件未记录到 docs/dev/配置清单.md：" -ForegroundColor Yellow
  $missing | ForEach-Object { Write-Host " - $_" }
  exit 1
}

Write-Host "配置清单校验通过，共校验 $($candidates.Count) 项。" -ForegroundColor Green
exit 0

