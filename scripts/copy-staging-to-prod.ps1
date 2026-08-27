#Requires -Version 5.1
# Copy published catalog + smarthome devices from staging to production.
param(
    [switch]$SkipSmarthome,
    [ValidateSet("replace", "sync")]
    [string]$ImportMode = "replace",
    [switch]$RemoteOnVps
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot "_env.ps1")
. (Join-Path $PSScriptRoot "_staging-http.ps1")
. (Join-Path $PSScriptRoot "_prod-http.ps1")
Load-ScriptEnv

if (-not $env:PROD_API_URL) {
    $env:PROD_API_URL = "https://api.alicecommands.ru"
}

$sshKey = if ($env:SSH_KEY_PATH) { $env:SSH_KEY_PATH } else { "$env:USERPROFILE\.ssh\id_ed25519_selectel" }
$sshHost = if ($env:SSH_HOST) { $env:SSH_HOST } else { "root@161.104.46.92" }
$remoteDeploy = if ($env:DEPLOY_REMOTE_DEPLOY) { $env:DEPLOY_REMOTE_DEPLOY } else { "/opt/alice-api/deploy" }

if ($RemoteOnVps -or $env:COPY_PROD_VIA_SSH -eq '1' -or -not (Test-ProdDnsWorks)) {
    Write-Host "Copy via VPS localhost (recommended until DNS + TLS for api.*)"
    scp -i $sshKey (Join-Path $Root "deploy\copy-staging-to-prod-remote.sh") "${sshHost}:${remoteDeploy}/copy-staging-to-prod-remote.sh"
    scp -i $sshKey (Join-Path $Root "seed\smarthome-devices-full.json") "${sshHost}:/tmp/alice-smarthome-full.json"
    ssh -i $sshKey $sshHost "chmod +x ${remoteDeploy}/copy-staging-to-prod-remote.sh && bash ${remoteDeploy}/copy-staging-to-prod-remote.sh"
    Write-Host "Done. Run: .\scripts\verify-prod.ps1"
    exit 0
}

Write-Host "Staging: $($env:STAGING_API_URL)"
Write-Host "Prod:    $($env:PROD_API_URL)"

$gzipPath = Join-Path $env:TEMP "alice-staging-bundle-copy.gz"
$jsonPath = Join-Path $env:TEMP "alice-staging-bundle-copy.json"
Remove-Item $gzipPath, $jsonPath -Force -ErrorAction SilentlyContinue

Write-Host "== 1/4 Download staging published bundle =="
Invoke-StagingDownload -Path '/v1/content/bundle' -OutFile $gzipPath
$fs = [System.IO.File]::OpenRead($gzipPath)
try {
    $gzip = New-Object System.IO.Compression.GzipStream($fs, [System.IO.Compression.CompressionMode]::Decompress)
    $sr = New-Object System.IO.StreamReader($gzip)
    [IO.File]::WriteAllText($jsonPath, $sr.ReadToEnd(), [Text.UTF8Encoding]::new($false))
    $sr.Close()
} finally {
    $fs.Close()
}
Write-Host "Decompressed bundle -> $jsonPath"

$stagingCookie = Join-Path $env:TEMP "alice-copy-staging-cookies.txt"
$prodCookie = Join-Path $env:TEMP "alice-copy-prod-cookies.txt"
Remove-Item $stagingCookie, $prodCookie -Force -ErrorAction SilentlyContinue

try {
    New-StagingSession -CookieJar $stagingCookie
    New-ProdSession -CookieJar $prodCookie

    Write-Host "== 2/4 Import catalog to prod (mode=$ImportMode) =="
    Invoke-ProdJsonPost -Path "/admin/api/import/json?mode=$ImportMode" -CookieJar $prodCookie -BodyPath $jsonPath | Out-Null

    Write-Host "== 3/4 Publish content on prod =="
    $publishBody = '{"min_app_version":"1.0","notes":"copied from staging"}'
    $result = Invoke-ProdJsonPost -Path '/admin/api/publish' -CookieJar $prodCookie -BodyInline $publishBody
    Write-Host "Published content_version=$($result.content_version)"

    if (-not $SkipSmarthome) {
        Write-Host "== 4/4 Copy smarthome devices from staging =="
        $devices = Invoke-StagingJsonGet -Path '/v1/smarthome/devices' -CookieJar $stagingCookie
        $stagingPickIds = @($devices.picks | ForEach-Object { $_.id })

        Write-Host "Clear prod smarthome picks before guide import..."
        $prodPicksBefore = Invoke-ProdJsonGet -Path '/admin/api/smarthome/device-picks' -CookieJar $prodCookie
        foreach ($pick in @($prodPicksBefore)) {
            $delArgs = @(Get-ProdCurlArgs -Method Get -CookieJar $prodCookie) + @('-X', 'DELETE', "$($env:PROD_API_URL.TrimEnd('/'))/admin/api/smarthome/device-picks/$($pick.id)")
            Invoke-CurlUtf8Text -CurlArgs $delArgs | Out-Null
        }

        function Upsert-ProdGuide {
            param($Guide)
            $bodyFile = New-TemporaryFile
            try {
                $json = $Guide | ConvertTo-Json -Depth 20 -Compress
                [IO.File]::WriteAllText($bodyFile.FullName, $json, [Text.UTF8Encoding]::new($false))
                try {
                    Invoke-ProdJsonPut -Path "/admin/api/smarthome/device-guides/$($Guide.id)" -CookieJar $prodCookie -BodyPath $bodyFile.FullName | Out-Null
                } catch {
                    if ($_.Exception.Message -notmatch '\b404\b') { throw }
                    Invoke-ProdJsonPost -Path '/admin/api/smarthome/device-guides' -CookieJar $prodCookie -BodyPath $bodyFile.FullName | Out-Null
                }
            } finally {
                Remove-Item $bodyFile.FullName -Force -ErrorAction SilentlyContinue
            }
        }

        function Upsert-ProdPick {
            param($Pick)
            $bodyFile = New-TemporaryFile
            try {
                $json = $Pick | ConvertTo-Json -Depth 20 -Compress
                [IO.File]::WriteAllText($bodyFile.FullName, $json, [Text.UTF8Encoding]::new($false))
                try {
                    Invoke-ProdJsonPut -Path "/admin/api/smarthome/device-picks/$($Pick.id)" -CookieJar $prodCookie -BodyPath $bodyFile.FullName | Out-Null
                } catch {
                    if ($_.Exception.Message -notmatch '\b404\b') { throw }
                    Invoke-ProdJsonPost -Path '/admin/api/smarthome/device-picks' -CookieJar $prodCookie -BodyPath $bodyFile.FullName | Out-Null
                }
            } finally {
                Remove-Item $bodyFile.FullName -Force -ErrorAction SilentlyContinue
            }
        }

        foreach ($guide in @($devices.guides)) {
            $stub = $guide | ConvertTo-Json -Depth 20 | ConvertFrom-Json
            $stub.related_device_ids = @()
            Upsert-ProdGuide $stub
        }
        foreach ($guide in @($devices.guides)) {
            Upsert-ProdGuide $guide
        }
        foreach ($pick in @($devices.picks)) {
            Upsert-ProdPick $pick
        }

        $prodPicks = Invoke-ProdJsonGet -Path '/admin/api/smarthome/device-picks' -CookieJar $prodCookie
        foreach ($pick in @($prodPicks)) {
            if ($pick.id -notin $stagingPickIds) {
                Write-Host "Delete orphan pick $($pick.id)"
                $delArgs = @(Get-ProdCurlArgs -Method Get -CookieJar $prodCookie) + @('-X', 'DELETE', "$($env:PROD_API_URL.TrimEnd('/'))/admin/api/smarthome/device-picks/$($pick.id)")
                Invoke-CurlUtf8Text -CurlArgs $delArgs | Out-Null
            }
        }

        $public = Invoke-ProdJsonGet -Path '/v1/smarthome/devices' -CookieJar $prodCookie
        Write-Host "Smarthome: $($public.guides.Count) guides, $($public.picks.Count) picks (staging: $($devices.guides.Count)/$($devices.picks.Count))"
    } else {
        Write-Host "== 4/4 Skipped smarthome (-SkipSmarthome) =="
    }
} finally {
    Remove-Item $stagingCookie, $prodCookie, $gzipPath, $jsonPath -Force -ErrorAction SilentlyContinue
}

Write-Host "Done. Run: .\scripts\verify-prod.ps1"
