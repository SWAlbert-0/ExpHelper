$ErrorActionPreference = "Stop"
$root = Resolve-Path "docs/cases/moo-python-moead-lsmop/algorithm_service"
$outDir = Resolve-Path "docs/cases/moo-python-moead-lsmop"
$zip = Join-Path $outDir "py-moead-lsmop-ls-source.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $root "*") -DestinationPath $zip -Force
Write-Host "[OK] 已生成源码包: $zip" -ForegroundColor Green
