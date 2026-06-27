#Requires -Version 5.1
param(
    [switch]$SkipFetch,
    [switch]$ForceFetch,
    [switch]$SkipPush,
    [ValidateSet("merge", "replace")]
    [string]$ImportMode = "merge"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "== 1/4 Build bundle =="
$python = if (Get-Command python -ErrorAction SilentlyContinue) { "python" } else { "py -3" }
$buildArgs = @("tools/content/build_bundle.py")
if ($SkipFetch) { $buildArgs += "--skip-fetch" }
if ($ForceFetch) { $buildArgs += "--force-fetch" }
Invoke-Expression "$python $($buildArgs -join ' ')"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

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
