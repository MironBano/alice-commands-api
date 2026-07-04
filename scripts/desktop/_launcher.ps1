#Requires -Version 5.1
param(
    [Parameter(Mandatory)]
    [ValidateSet('update', 'build-local', 'force-fetch', 'verify', 'open-admin', 'install-shortcuts', 'deploy')]
    [string]$Action
)

$ErrorActionPreference = 'Stop'
$ScriptsDir = Split-Path -Parent $PSScriptRoot
$Root = Split-Path -Parent $ScriptsDir
Set-Location $Root
. (Join-Path $ScriptsDir '_env.ps1')

function Wait-ForKey {
    if ($Host.Name -eq 'ConsoleHost') {
        Write-Host ''
        Read-Host 'Done. Press Enter to close'
    }
}

try {
    switch ($Action) {
        'update' {
            Write-Host '== Update catalog (build + validate + push + verify) ==' -ForegroundColor Cyan
            & (Join-Path $ScriptsDir 'update-content.ps1')
        }
        'build-local' {
            Write-Host '== Build locally (no push) ==' -ForegroundColor Cyan
            & (Join-Path $ScriptsDir 'update-content.ps1') -SkipPush
        }
        'force-fetch' {
            Write-Host '== Force fetch from Yandex docs ==' -ForegroundColor Cyan
            & (Join-Path $ScriptsDir 'update-content.ps1') -ForceFetch
        }
        'verify' {
            Write-Host '== Verify staging manifest ==' -ForegroundColor Cyan
            & (Join-Path $ScriptsDir 'verify-staging.ps1')
        }
        'open-admin' {
            Load-ScriptEnv
            $url = "$($env:STAGING_API_URL.TrimEnd('/'))/admin"
            Write-Host "Opening admin: $url" -ForegroundColor Cyan
            Start-Process $url
            Start-Sleep -Seconds 2
        }
        'install-shortcuts' {
            & (Join-Path $PSScriptRoot 'Install-Desktop-Shortcuts.ps1')
        }
        'deploy' {
            Write-Host '== Deploy backend to staging VPS ==' -ForegroundColor Cyan
            & (Join-Path $ScriptsDir 'deploy-staging.ps1')
        }
    }
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
} finally {
    if ($Action -ne 'open-admin') {
        Wait-ForKey
    }
}
