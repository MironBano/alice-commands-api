#Requires -Version 5.1
# Import smarthome guides + picks from UTF-8 JSON (no PowerShell stdout mojibake).
# Guides: two-pass (stub without related_device_ids, then full) so FK validation can pass.
param(
    [ValidateSet('staging', 'prod', 'both')]
    [string]$Target = 'staging',
    [string]$PayloadPath = ''
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot '_env.ps1')
. (Join-Path $PSScriptRoot '_staging-http.ps1')
. (Join-Path $PSScriptRoot '_prod-http.ps1')
Load-ScriptEnv

if (-not $PayloadPath) {
    $PayloadPath = Join-Path $Root 'seed\smarthome-devices-full.json'
}
if (-not (Test-Path -LiteralPath $PayloadPath)) {
    throw "Payload not found: $PayloadPath"
}

function Write-Utf8JsonBodyFile {
    param($Object)
    $tmp = New-TemporaryFile
    $json = $Object | ConvertTo-Json -Depth 20 -Compress
    [IO.File]::WriteAllText($tmp.FullName, $json, [Text.UTF8Encoding]::new($false))
    return $tmp
}

function Test-HttpNotFoundError {
    param($ErrorRecord)
    $msg = [string]$ErrorRecord
    if ($ErrorRecord.Exception) { $msg += ' ' + [string]$ErrorRecord.Exception.Message }
    return ($msg -match '\b404\b')
}

function Import-SmarthomeToTarget {
    param(
        [ValidateSet('staging', 'prod')][string]$EnvName,
        $Payload
    )
    $cookie = Join-Path $env:TEMP "alice-import-sh-$EnvName-cookies.txt"
    Remove-Item $cookie -Force -ErrorAction SilentlyContinue
    try {
        if ($EnvName -eq 'staging') {
            New-StagingSession -CookieJar $cookie
            $put = { param($Path, $BodyPath) Invoke-StagingJsonPut -Path $Path -CookieJar $cookie -BodyPath $BodyPath }
            $post = { param($Path, $BodyPath) Invoke-StagingJsonPost -Path $Path -CookieJar $cookie -BodyPath $BodyPath }
            $getPublic = { Invoke-StagingJsonGet -Path '/v1/smarthome/devices' -CookieJar $cookie }
        } else {
            if (-not $env:PROD_API_URL) { $env:PROD_API_URL = 'https://api.alicecommands.ru' }
            New-ProdSession -CookieJar $cookie
            $put = { param($Path, $BodyPath) Invoke-ProdJsonPut -Path $Path -CookieJar $cookie -BodyPath $BodyPath }
            $post = { param($Path, $BodyPath) Invoke-ProdJsonPost -Path $Path -CookieJar $cookie -BodyPath $BodyPath }
            $getPublic = { Invoke-ProdJsonGet -Path '/v1/smarthome/devices' -CookieJar $cookie }
        }

        function Upsert-Entity {
            param(
                [string]$PutPath,
                [string]$PostPath,
                $Body,
                [string]$Label
            )
            $bodyFile = Write-Utf8JsonBodyFile $Body
            try {
                try {
                    & $put $PutPath $bodyFile.FullName | Out-Null
                } catch {
                    if (-not (Test-HttpNotFoundError $_)) { throw }
                    & $post $PostPath $bodyFile.FullName | Out-Null
                }
            } finally {
                Remove-Item $bodyFile.FullName -Force -ErrorAction SilentlyContinue
            }
            Write-Host "[$EnvName] $Label"
        }

        # Pass 1: create/update all guides without related_device_ids (FK may not exist yet).
        foreach ($guide in @($Payload.guides)) {
            $stub = $guide.PSObject.Copy()
            $stub.related_device_ids = @()
            Upsert-Entity -PutPath "/admin/api/smarthome/device-guides/$($guide.id)" `
                -PostPath '/admin/api/smarthome/device-guides' -Body $stub -Label "guide-stub $($guide.id)"
        }

        # Pass 2: full guides with related_device_ids.
        foreach ($guide in @($Payload.guides)) {
            Upsert-Entity -PutPath "/admin/api/smarthome/device-guides/$($guide.id)" `
                -PostPath '/admin/api/smarthome/device-guides' -Body $guide -Label "guide $($guide.id)"
        }

        foreach ($pick in @($Payload.picks)) {
            Upsert-Entity -PutPath "/admin/api/smarthome/device-picks/$($pick.id)" `
                -PostPath '/admin/api/smarthome/device-picks' -Body $pick -Label "pick $($pick.id)"
        }

        $public = & $getPublic
        $guideCount = @($public.guides).Count
        if ($guideCount -eq 0) {
            throw "[$EnvName] Public snapshot has 0 guides"
        }
        $title = $public.guides[0].title_ru
        $withPlacements = @($public.picks | Where-Object { $_.placements -and $_.placements.Count -gt 0 }).Count
        Write-Host "[$EnvName] verify guides=$guideCount title='$title' picks_with_placements=$withPlacements/$($public.picks.Count)"
        if ($guideCount -lt @($Payload.guides).Count) {
            throw "[$EnvName] Expected $(@($Payload.guides).Count) guides, got $guideCount"
        }
        if ($title -notmatch '[\u0400-\u04FF]') {
            throw "[$EnvName] Cyrillic check failed for guide title: $title"
        }
        if ($withPlacements -lt $public.picks.Count) {
            throw "[$EnvName] Not all picks have placements"
        }
        foreach ($expected in @($Payload.guides)) {
            $got = @($public.guides | Where-Object { $_.id -eq $expected.id } | Select-Object -First 1)
            if (-not $got) {
                throw "[$EnvName] Missing public guide $($expected.id)"
            }
            $expRel = @($expected.related_device_ids)
            $gotRel = @($got.related_device_ids)
            $diff = Compare-Object $expRel $gotRel
            if ($diff) {
                throw "[$EnvName] related_device_ids mismatch for $($expected.id): expected [$($expRel -join ',')] got [$($gotRel -join ',')]"
            }
        }
    } finally {
        Remove-Item $cookie -Force -ErrorAction SilentlyContinue
    }
}

$raw = [IO.File]::ReadAllText($PayloadPath, [Text.UTF8Encoding]::new($false))
$payload = $raw | ConvertFrom-Json

$targets = if ($Target -eq 'both') { @('staging', 'prod') } else { @($Target) }
foreach ($t in $targets) {
    Import-SmarthomeToTarget -EnvName $t -Payload $payload
}

Write-Host 'Import complete.'
