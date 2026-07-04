#Requires -Version 5.1
param(
    [string]$BundleFile = "seed/full-catalog.json",
    [ValidateSet("sync", "merge", "replace")]
    [string]$Mode = "sync",
    [switch]$SkipPipelineSync
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

    if (-not $SkipPipelineSync) {
        $dataDir = Join-Path $Root "seed/data"
        $snapshotPath = Join-Path $dataDir "inventory_snapshot.json"
        $editorialPath = Join-Path $dataDir "editorial.json"
        $queuePath = Join-Path $dataDir "queue.json"

        if ((Test-Path $snapshotPath) -and (Test-Path $editorialPath)) {
            Write-Host "Sync pipeline state (inventory + editorial + queue)..."
            $snapshot = Get-Content -Raw -Encoding UTF8 $snapshotPath | ConvertFrom-Json
            $editorialDoc = Get-Content -Raw -Encoding UTF8 $editorialPath | ConvertFrom-Json
            $queueItems = @()
            if (Test-Path $queuePath) {
                $queueDoc = Get-Content -Raw -Encoding UTF8 $queuePath | ConvertFrom-Json
                if ($queueDoc.items) { $queueItems = @($queueDoc.items) }
            }
            $editorialRecords = @()
            if ($editorialDoc.records) {
                foreach ($prop in $editorialDoc.records.PSObject.Properties) {
                    $editorialRecords += $prop.Value
                }
            }
            $payload = @{
                inventory = @($snapshot.items)
                editorial = $editorialRecords
                queue     = $queueItems
            }
        $payloadFile = Join-Path $env:TEMP "alice-pipeline-sync.json"
        [IO.File]::WriteAllText($payloadFile, ($payload | ConvertTo-Json -Depth 20 -Compress), [Text.UTF8Encoding]::new($false))
            try {
                Invoke-StagingJsonPost -Path '/admin/api/content/pipeline-sync' -CookieJar $cookieJar -BodyPath $payloadFile | Out-Null
                Write-Host "Pipeline sync OK"
            } catch {
                throw @"
Pipeline sync failed at $($env:STAGING_API_URL)/admin/api/content/pipeline-sync
Deploy new backend first: .\scripts\deploy-staging.ps1 (or desktop shortcut 6)
$_
"@
            } finally {
                Remove-Item $payloadFile -Force -ErrorAction SilentlyContinue
            }
        } else {
            throw "Missing seed/data/inventory_snapshot.json or editorial.json - run pipeline first (shortcut 1)"
        }
    }

    # Order: pipeline-sync (state) -> import sync (catalog JSON) -> rebuild-draft (canonical draft from pipeline DB)
    try {
        Invoke-StagingJsonPost -Path "/admin/api/import/json?mode=$Mode" -CookieJar $cookieJar -BodyPath $bundlePath | Out-Null
    } catch {
        throw "Import failed (mode=$Mode): $_"
    }

    try {
        $rebuild = Invoke-StagingJsonPost -Path '/admin/api/content/rebuild-draft' -CookieJar $cookieJar -BodyInline '{}'
        Write-Host "Draft rebuild from pipeline: $($rebuild.commands_updated) commands"
    } catch {
        Write-Warning "rebuild-draft skipped (deploy new backend?): $_"
    }

    Write-Host "Draft import OK ($Mode): $BundleFile -> $($env:STAGING_API_URL)"
    Write-Host "Next: open $($env:STAGING_API_URL.TrimEnd('/'))/admin -> Content -> queue + diff -> Publish"
} finally {
    Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
}
