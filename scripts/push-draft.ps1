#Requires -Version 5.1
param(
    [string]$BundleFile = "seed/catalog-audit-fixed.json",
    [ValidateSet("replace")]
    [string]$Mode = "replace",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_staging-http.ps1")
Load-ScriptEnv

$bundlePath = Join-Path $Root $BundleFile
if (-not (Test-Path $bundlePath)) {
    throw "Bundle not found: $bundlePath"
}

$cookieJar = Join-Path $env:TEMP "alice-push-cookies.txt"
Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
try {
    try {
        New-StagingSession -CookieJar $cookieJar
    } catch {
        throw @"
Login failed for user '$($env:ADMIN_USERNAME)' at $($env:STAGING_API_URL)
Check ADMIN_USERNAME / ADMIN_PASSWORD in repo .env (root) or scripts/.env
Admin UI password must match staging server /opt/alice-api/.env
$_
"@
    }

    $status = Invoke-StagingJsonGet -Path '/admin/api/content/pipeline' -CookieJar $cookieJar
    if ($status.hasUnpublishedChanges -and -not $Force) {
        throw @"
Staging draft отличается от live (есть неопубликованные изменения).
push-draft REPLACE перезапишет draft файлом $BundleFile.

Безопасные варианты:
  1) Если правили в админке и хотите сохранить их в файл:
       .\scripts\pull-draft.ps1
     затем Publish в админке (app увидит правки). Push не обязателен.
  2) Если файл уже актуален и вы сознательно затираете draft:
       .\scripts\push-draft.ps1 -Force
  3) Если сначала опубликовали draft → live, hasUnpublished исчезнет — обычный push без -Force снова работает.
"@
    }

    Invoke-StagingJsonPost -Path "/admin/api/import/json?mode=$Mode" -CookieJar $cookieJar -BodyPath $bundlePath | Out-Null

    Write-Host "Draft import OK ($Mode): $BundleFile -> $($env:STAGING_API_URL)"
    Write-Host "Next: open $($env:STAGING_API_URL.TrimEnd('/'))/admin -> Content -> diff -> Publish"
} finally {
    Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
}
