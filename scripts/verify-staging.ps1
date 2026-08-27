#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_staging-http.ps1")
Load-ScriptEnv

function Read-GzipJson {
    param([Parameter(Mandatory)][string]$Path)
    $fs = [IO.File]::OpenRead($Path)
    try {
        $gz = New-Object IO.Compression.GzipStream($fs, [IO.Compression.CompressionMode]::Decompress)
        try {
            $sr = New-Object IO.StreamReader($gz, [Text.UTF8Encoding]::new($false))
            try {
                return $sr.ReadToEnd() | ConvertFrom-Json
            } finally { $sr.Dispose() }
        } finally { $gz.Dispose() }
    } finally { $fs.Dispose() }
}

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

    $bundle = Read-GzipJson -Path $gzipPath
    $cmds = @($bundle.commands)
    $schema = $bundle.schema_version
    $catCount = @($bundle.categories).Count
    $groupCount = @($bundle.command_groups).Count
    Write-Host "Published bundle: schema=$schema $catCount categories, $groupCount groups, $($cmds.Count) commands"
    if ($schema -ne 2) { Write-Warning "Expected schema_version=2 for command groups release" }
    $cod = $bundle.command_of_day
    if (-not $cod) {
        Write-Warning 'command_of_day missing in bundle (old publish or feature disabled)'
    } else {
        Write-Host "Command of day OK: mode=$($cod.mode) command_id=$($cod.command_id)"
    }
    $sampleCommandId = if ($cmds.Count -gt 0) { $cmds[0].id } else { $null }
} finally {
    Remove-Item $gzipPath -Force -ErrorAction SilentlyContinue
}

$cookieJar = Join-Path $env:TEMP "alice-verify-cookies.txt"
Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
try {
    New-StagingSession -CookieJar $cookieJar
    $status = Invoke-StagingJsonGet -Path '/admin/api/content/pipeline' -CookieJar $cookieJar
    if ($null -eq $status.draft) {
        throw 'Content status API missing draft block'
    }
    Write-Host "Draft status OK: $($status.draft.commandsCount) commands, unpublished=$($status.hasUnpublishedChanges)"

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
    throw "Admin API check failed: $_"
} finally {
    Remove-Item $cookieJar -Force -ErrorAction SilentlyContinue
}
