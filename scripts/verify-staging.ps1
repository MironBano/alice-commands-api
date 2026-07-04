#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_python.ps1")
. (Join-Path $PSScriptRoot "_staging-http.ps1")
Load-ScriptEnv

$manifest = Invoke-StagingJsonGet -Path '/v1/content/manifest'
Write-Host "Manifest: content_version=$($manifest.content_version), schema=$($manifest.schema_version)"

if ($manifest.content_version -eq 0 -or -not $manifest.content_version) {
    Write-Host "No published content yet (expected before first Publish)"
    exit 0
}

$gzipPath = Join-Path $env:TEMP "alice-verify-bundle.gz"
try {
    Invoke-StagingDownload -Path '/v1/content/bundle' -OutFile $gzipPath

    $sha = (Get-FileHash -Path $gzipPath -Algorithm SHA256).Hash.ToLower()
    if ($sha -ne $manifest.bundle_sha256) {
        Write-Warning "SHA256 mismatch: manifest=$($manifest.bundle_sha256) actual=$sha"
    } else {
        Write-Host "Bundle SHA256 OK ($($manifest.bundle_size_bytes) bytes gzip)"
    }

    $pyCode = @"
import gzip,json,sys
b=json.load(gzip.open(sys.argv[1],'rt',encoding='utf-8'))
cmds=b.get('commands',[])
cod=b.get('command_of_day')
print(b.get('schema_version',1), len(b.get('categories',[])), len(b.get('command_groups',[])), len(cmds), cmds[0]['id'] if cmds else '')
if not cod:
    print('COD_MISSING')
    sys.exit(0)
pool=sorted([c for c in cmds if c.get('category_id')==cod.get('auto_category_id')], key=lambda c:(c.get('sort_order') if c.get('sort_order') is not None else 2147483647, c['id']))
from datetime import date
d=date.fromisoformat(cod['resolved_date'])
ed=d.toordinal()-date(1970,1,1).toordinal()
seed=cod.get('auto_seed') or 31
idx=((ed*seed)+len(pool))%len(pool) if pool else -1
expected=pool[idx]['id'] if pool and idx>=0 else ''
ok=cod['command_id']==expected if cod.get('mode')=='auto' else True
print('COD', cod.get('mode'), cod.get('auto_category_id'), cod.get('command_id'), 'pool', len(pool), 'resolver_ok', ok)
if cod.get('mode')=='auto' and not ok:
    sys.exit(2)
"@
    $stats = Invoke-PythonCode -Code $pyCode $gzipPath
    $lines = ($stats | Out-String).Trim() -split "`n"
    $parts = $lines[0].Trim() -split '\s+'
    if ($parts.Count -ge 4) {
        Write-Host "Published bundle: schema=$($parts[0]) $($parts[1]) categories, $($parts[2]) groups, $($parts[3]) commands"
        if ($parts[0] -ne '2') { Write-Warning "Expected schema_version=2 for command groups release" }
    }
    $codLine = $lines | Where-Object { $_ -match '^COD' } | Select-Object -First 1
    if ($codLine -eq 'COD_MISSING') {
        Write-Warning 'command_of_day missing in bundle (old publish or feature disabled)'
    } elseif ($codLine) {
        Write-Host "Command of day OK: $codLine"
        if ($codLine -notmatch 'resolver_ok True') { throw "command_of_day resolver mismatch: $codLine" }
    }
    $sampleCommandId = if ($parts.Count -ge 5) { $parts[4] } else { $null }
} finally {
    Remove-Item $gzipPath -Force -ErrorAction SilentlyContinue
}

$cookieJar = Join-Path $env:TEMP "alice-verify-cookies.txt"
Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
try {
    New-StagingSession -CookieJar $cookieJar
    $pipeline = Invoke-StagingJsonGet -Path '/admin/api/content/pipeline' -CookieJar $cookieJar
    if ($pipeline.pipeline) {
        Write-Host "Pipeline API OK: inventory=$($pipeline.pipeline.inventory_count) queue=$($pipeline.pipeline.open_queue)"
    } else {
        throw 'Pipeline API missing (old backend). Run deploy-staging.ps1'
    }

    $feedbackBody = '{"message":"staging verify feedback","app_version":"verify","platform":"script"}'
    $feedback = Invoke-StagingJsonPost -Path '/v1/feedback' -CookieJar $cookieJar -BodyInline $feedbackBody
    if (-not $feedback.id) { throw 'POST /v1/feedback did not return id' }
    Write-Host "Feedback API OK: id=$($feedback.id)"

    $dashboard = Invoke-StagingJsonGet -Path '/admin/api/dashboard' -CookieJar $cookieJar
    if ($null -eq $dashboard.inbox) {
        throw 'Dashboard missing inbox counts (old backend). Run deploy-staging.ps1'
    }
    Write-Host "Inbox API OK: open_feedback=$($dashboard.inbox.open_feedback) open_command_reports=$($dashboard.inbox.open_command_reports)"

    $feedbackList = Invoke-StagingJsonGet -Path '/admin/api/feedback?status=open' -CookieJar $cookieJar
    if (-not ($feedbackList | Where-Object { $_.id -eq $feedback.id })) {
        throw 'Submitted feedback not visible in admin inbox'
    }
    Write-Host 'Admin feedback inbox OK'

    $codAdmin = Invoke-StagingJsonGet -Path '/admin/api/command-of-day' -CookieJar $cookieJar
    if (-not $codAdmin.settings.mode) { throw 'Admin command-of-day settings missing' }
    $codSummary = "mode=$($codAdmin.settings.mode)"
    if ($codAdmin.settings.auto_category_id) { $codSummary += " category=$($codAdmin.settings.auto_category_id) pool=$($codAdmin.pool_size)" }
    if ($codAdmin.preview_today.command_id) { $codSummary += " today=$($codAdmin.preview_today.command_id)" }
    Write-Host "Command of day admin OK: $codSummary"

    if ($sampleCommandId) {
        $reportBody = (@{
            issue_type = 'other'
            message = 'staging verify command report'
            content_version = $manifest.content_version
        } | ConvertTo-Json -Compress)
        $report = Invoke-StagingJsonPost -Path "/v1/commands/$sampleCommandId/report" -CookieJar $cookieJar -BodyInline $reportBody
        if (-not $report.id) { throw "POST /v1/commands/$sampleCommandId/report did not return id" }
        Write-Host "Command report API OK: id=$($report.id) command=$sampleCommandId"
    } else {
        Write-Warning 'No command in bundle — skipped command report smoke test'
    }
} catch {
    throw "Pipeline API check failed: $_"
} finally {
    Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
}
