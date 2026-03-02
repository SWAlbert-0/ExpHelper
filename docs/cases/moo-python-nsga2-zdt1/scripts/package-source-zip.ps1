$ErrorActionPreference = "Stop"
$root = Resolve-Path "docs/cases/moo-python-nsga2-zdt1/algorithm_service"
$outDir = Resolve-Path "docs/cases/moo-python-nsga2-zdt1"
$zip = Join-Path $outDir "py-nsga2-zdt1-ls-source.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $root "*") -DestinationPath $zip -Force
Write-Host "[OK] 已生成源码包: $zip" -ForegroundColor Green
