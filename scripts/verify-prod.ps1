#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_prod-http.ps1")
Load-ScriptEnv

if (-not $env:PROD_API_URL) {
    $env:PROD_API_URL = "https://api.alicecommands.ru"
}

$manifest = Invoke-ProdJsonGet -Path '/v1/content/manifest'
Write-Host "Manifest: content_version=$($manifest.content_version), schema=$($manifest.schema_version)"

if ($manifest.content_version -eq 0 -or -not $manifest.content_version) {
    Write-Host "No published content on prod yet - run copy-staging-to-prod.ps1"
    exit 1
}

$gzipPath = Join-Path $env:TEMP "alice-verify-prod-bundle.gz"
try {
    Invoke-ProdDownload -Path '/v1/content/bundle' -OutFile $gzipPath

    $sha = (Get-FileHash -Path $gzipPath -Algorithm SHA256).Hash.ToLower()
    if ($sha -ne $manifest.bundle_sha256) {
        Write-Warning "SHA256 mismatch: manifest=$($manifest.bundle_sha256) actual=$sha"
    } else {
        $sizeBytes = $manifest.bundle_size_bytes
        Write-Host "Bundle SHA256 OK ($sizeBytes bytes gzip)"
    }

    $fs = [System.IO.File]::OpenRead($gzipPath)
  try {
        $gzip = New-Object System.IO.Compression.GzipStream($fs, [System.IO.Compression.CompressionMode]::Decompress)
        $sr = New-Object System.IO.StreamReader($gzip)
        $obj = $sr.ReadToEnd() | ConvertFrom-Json
        $sr.Close()
    } finally {
        $fs.Close()
    }

    $catCount = @($obj.categories).Count
    $cmdCount = @($obj.commands).Count
    $groupCount = @($obj.command_groups).Count
    $script:sampleCommandId = @($obj.commands | Select-Object -First 1).id
    Write-Host "Published bundle: schema=$($obj.schema_version) $catCount categories, $groupCount groups, $cmdCount commands"
    if ($catCount -lt 13) { Write-Warning "Expected >= 13 categories" }
    if ($cmdCount -lt 300) { Write-Warning "Expected >= 300 commands" }
    if ($obj.schema_version -ne 2) { Write-Warning "Expected schema_version=2" }
} finally {
    Remove-Item $gzipPath -Force -ErrorAction SilentlyContinue
}

$devices = Invoke-ProdJsonGet -Path '/v1/smarthome/devices'
$guideCount = @($devices.guides).Count
$pickCount = @($devices.picks).Count
Write-Host "Smarthome devices: $guideCount guides, $pickCount picks"

$iconCode = curl.exe -sS -o NUL -w "%{http_code}" --max-time 20 "https://cdn.alicecommands.ru/icons/v1/music_note.svg"
if ($iconCode -eq "200") {
    Write-Host "CDN icon smoke OK: music_note.svg -> 200"
} else {
    Write-Warning "CDN icon smoke failed ($iconCode) - try setup-cdn.ps1 or staging-api mirror"
    $stagingIcon = curl.exe -sS -o NUL -w "%{http_code}" --max-time 20 "$($env:PROD_API_URL.TrimEnd('/'))/icons/v1/music_note.svg"
    Write-Host "Prod API icon mirror: $stagingIcon"
}

# Analytics: only probe that the route rejects invalid payload (no insert / no KPI noise).
$analyticsProbeBody = '{"events":[]}'
$analyticsTmp = New-TemporaryFile
try {
    [IO.File]::WriteAllText($analyticsTmp.FullName, $analyticsProbeBody, [Text.UTF8Encoding]::new($false))
    $analyticsUrl = "$($env:PROD_API_URL.TrimEnd('/'))/v1/analytics/events/batch"
    # Drop --fail: we intentionally expect HTTP 400.
    $analyticsCurlArgs = @(Get-ProdCurlArgs -Method Post -BodyPath $analyticsTmp.FullName | Where-Object { $_ -ne '--fail' })
    $analyticsCode = & curl.exe @($analyticsCurlArgs + @('-o', 'NUL', '-w', '%{http_code}', $analyticsUrl))
    if ($LASTEXITCODE -ne 0) { throw "analytics probe request failed (exit $LASTEXITCODE)" }
    if ($analyticsCode -ne "400") {
        throw "analytics probe expected HTTP 400 for empty events, got $analyticsCode"
    }
    Write-Host "Analytics route probe OK (400 empty batch, no events written)"
} finally {
    Remove-Item $analyticsTmp.FullName -Force -ErrorAction SilentlyContinue
}

$feedbackBody = '{"message":"prod verify feedback","app_version":"verify","platform":"script"}'
$feedbackTmp = New-TemporaryFile
try {
    [IO.File]::WriteAllText($feedbackTmp.FullName, $feedbackBody, [Text.UTF8Encoding]::new($false))
    $feedbackArgs = @(Get-ProdCurlArgs -Method Post -BodyPath $feedbackTmp.FullName) + @("$($env:PROD_API_URL.TrimEnd('/'))/v1/feedback")
    $feedbackJson = Invoke-CurlUtf8Text -CurlArgs $feedbackArgs
    $feedback = $feedbackJson | ConvertFrom-Json
    if (-not $feedback.id) { throw "POST /v1/feedback did not return id" }
    Write-Host "Feedback smoke OK: id=$($feedback.id)"
} finally {
    Remove-Item $feedbackTmp.FullName -Force -ErrorAction SilentlyContinue
}

$sampleCommandId = $script:sampleCommandId
if ($sampleCommandId) {
    $reportBody = "{`"issue_type`":`"other`",`"message`":`"prod verify report`",`"app_version`":`"verify`",`"platform`":`"script`",`"content_version`":$($manifest.content_version)}"
    $reportTmp = New-TemporaryFile
    try {
        [IO.File]::WriteAllText($reportTmp.FullName, $reportBody, [Text.UTF8Encoding]::new($false))
        $reportArgs = @(Get-ProdCurlArgs -Method Post -BodyPath $reportTmp.FullName) + @("$($env:PROD_API_URL.TrimEnd('/'))/v1/commands/$sampleCommandId/report")
        $reportJson = Invoke-CurlUtf8Text -CurlArgs $reportArgs
        $report = $reportJson | ConvertFrom-Json
        if (-not $report.id) { throw "POST /v1/commands/$sampleCommandId/report did not return id" }
        Write-Host "Command report smoke OK: id=$($report.id) command=$sampleCommandId"
    } finally {
        Remove-Item $reportTmp.FullName -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Warning "Command report smoke skipped: no sample command_id from bundle"
}

Write-Host "verify-prod.ps1 OK"
