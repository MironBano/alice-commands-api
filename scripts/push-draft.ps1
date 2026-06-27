#Requires -Version 5.1
param(
    [string]$BundleFile = "seed/full-catalog.json",
    [ValidateSet("merge", "replace")]
    [string]$Mode = "merge"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Load-ScriptEnv {
    $envFile = Join-Path $PSScriptRoot ".env"
    if (-not (Test-Path $envFile)) {
        throw "Missing scripts/.env — copy from scripts/.env.example"
    }
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $name, $value = $_ -split '=', 2
        Set-Item -Path "Env:$name" -Value $value.Trim()
    }
}

Load-ScriptEnv
$baseUrl = $env:STAGING_API_URL.TrimEnd('/')
$user = $env:ADMIN_USERNAME
$pass = $env:ADMIN_PASSWORD
$bundlePath = Join-Path $Root $BundleFile

if (-not (Test-Path $bundlePath)) {
    throw "Bundle not found: $bundlePath"
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{ username = $user; password = $pass } | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/admin/api/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $session | Out-Null

$json = Get-Content -Raw -Encoding UTF8 $bundlePath
$importUrl = "$baseUrl/admin/api/import/json?mode=$Mode"
Invoke-RestMethod -Uri $importUrl -Method Post -Body $json -ContentType "application/json; charset=utf-8" -WebSession $session | Out-Null

Write-Host "Draft import OK ($Mode): $BundleFile -> $baseUrl"
Write-Host "Next: open $baseUrl/admin -> Import review diff -> Publish manually"
