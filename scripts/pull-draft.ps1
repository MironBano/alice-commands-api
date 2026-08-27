#Requires -Version 5.1
# Download staging draft bundle into seed/catalog-audit-fixed.json (safe reverse of push-draft).
# Use after point-edits in Admin so the repo file matches draft and next push-draft won't wipe them.
param(
    [string]$OutFile = "seed/catalog-audit-fixed.json",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_staging-http.ps1")
Load-ScriptEnv

$outPath = Join-Path $Root $OutFile
if ((Test-Path $outPath) -and -not $Force) {
    $answer = Read-Host "Overwrite $OutFile with staging draft? [y/N]"
    if ($answer -notmatch '^[yY]') {
        Write-Host "Cancelled."
        exit 0
    }
}

$cookieJar = Join-Path $env:TEMP "alice-pull-cookies.txt"
$tmpRaw = Join-Path $env:TEMP "alice-pull-draft-raw.json"
Remove-Item $cookieJar, $tmpRaw -Force -ErrorAction SilentlyContinue
try {
    New-StagingSession -CookieJar $cookieJar
    # Raw download keeps exact JSON (PowerShell ConvertTo-Json can mangle arrays).
    $url = "$($env:STAGING_API_URL.TrimEnd('/'))/admin/api/preview/bundle"
    $curlArgs = @(Get-StagingCurlArgs -Method Get -CookieJar $cookieJar) + @('-o', $tmpRaw, $url)
    & curl.exe @curlArgs
    if ($LASTEXITCODE -ne 0) { throw "Failed to download draft preview (exit $LASTEXITCODE)" }

    $raw = [IO.File]::ReadAllText($tmpRaw, [Text.UTF8Encoding]::new($false))
    $parsed = $raw | ConvertFrom-Json
    $cmdCount = @($parsed.commands).Count
    if ($cmdCount -lt 1) { throw "Draft preview has no commands — aborting write to $OutFile" }

    Copy-Item -Force $tmpRaw $outPath
    Write-Host "Wrote $OutFile ($cmdCount commands) from staging draft"
    Write-Host "Next: validateContent, then git commit; Publish in admin for app users"
} finally {
    Remove-Item $cookieJar, $tmpRaw -Force -ErrorAction SilentlyContinue
}
