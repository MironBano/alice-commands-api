#Requires -Version 5.1
param(
    [switch]$SkipFetch,
    [switch]$ForceFetch,
    [switch]$SkipPush,
    [ValidateSet("sync", "merge", "replace")]
    [string]$ImportMode = "sync"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_python.ps1")

$editorialPath = Join-Path $Root "seed\data\editorial.json"
if (-not (Test-Path $editorialPath)) {
    Write-Host "== 0/4 Bootstrap editorial (first run) =="
    $bootArgs = @("--bootstrap")
    if ($SkipFetch) { $bootArgs += "--skip-fetch" }
    Invoke-PythonScript "tools/content/pipeline_run.py" @bootArgs
}

Write-Host "== 1/4 Pipeline (inventory -> sync -> catalog) =="
$buildArgs = @()
if ($SkipFetch) { $buildArgs += "--skip-fetch" }
if ($ForceFetch) { $buildArgs += "--force-fetch" }
Invoke-PythonScript "tools/content/pipeline_run.py" @buildArgs

Write-Host "== 2/4 Validate schema =="
& .\gradlew.bat ":server:validateContent" "-PcontentFile=seed/full-catalog.json" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($SkipPush) {
    Write-Host "Skip push (--SkipPush). Done."
    exit 0
}

Write-Host "== 3/4 Push draft to staging =="
& "$PSScriptRoot\push-draft.ps1" -BundleFile "seed/full-catalog.json" -Mode $ImportMode
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 4/4 Verify staging manifest =="
& "$PSScriptRoot\verify-staging.ps1"
exit $LASTEXITCODE
