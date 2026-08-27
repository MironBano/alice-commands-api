#Requires -Version 5.1
param(
    [Parameter(Mandatory)]
    [ValidateSet('push-catalog', 'pull-catalog', 'validate-catalog', 'verify', 'open-admin', 'install-shortcuts', 'deploy')]
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
        'push-catalog' {
            Write-Host '== Validate + push catalog-audit-fixed.json to staging ==' -ForegroundColor Cyan
            & (Join-Path $Root 'gradlew.bat') ':server:validateContent' '-PcontentFile=seed/catalog-audit-fixed.json'
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            & (Join-Path $ScriptsDir 'push-draft.ps1')
        }
        'pull-catalog' {
            Write-Host '== Pull staging draft -> seed/catalog-audit-fixed.json ==' -ForegroundColor Cyan
            Write-Host 'Внимание: локальный seed/catalog-audit-fixed.json будет перезаписан draft с сервера.' -ForegroundColor Yellow
            & (Join-Path $ScriptsDir 'pull-draft.ps1') -Force
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            & (Join-Path $Root 'gradlew.bat') ':server:validateContent' '-PcontentFile=seed/catalog-audit-fixed.json'
        }
        'validate-catalog' {
            Write-Host '== Validate catalog-audit-fixed.json ==' -ForegroundColor Cyan
            & (Join-Path $Root 'gradlew.bat') ':server:validateContent' '-PcontentFile=seed/catalog-audit-fixed.json'
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
