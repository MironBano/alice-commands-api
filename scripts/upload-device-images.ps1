#Requires -Version 5.1
# Upload device PNGs to staging (or prod) via POST /admin/api/smarthome/upload-image.
param(
    [ValidateSet('staging', 'prod', 'both')]
    [string]$Target = 'staging',
    [string]$ImagesDir = '',
    [switch]$AllowMissing
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot '_env.ps1')
. (Join-Path $PSScriptRoot '_staging-http.ps1')
. (Join-Path $PSScriptRoot '_prod-http.ps1')
Load-ScriptEnv

if (-not $ImagesDir) {
    throw 'Pass -ImagesDir path to folder with PNG files'
}
if (-not (Test-Path -LiteralPath $ImagesDir)) {
    throw "Images dir not found: $ImagesDir"
}

# Filename (without .png) → guide slug (snake_case)
$Map = @{
    'light'                  = 'light'
    'socket'                 = 'socket'
    'switch'                 = 'switch'
    'sensor-motion'          = 'sensor_motion'
    'sensor-open'            = 'sensor_open'
    'sensor-climate'         = 'sensor_climate'
    'sensor-water-leak'      = 'sensor_water_leak'
    'sensor-smoke'           = 'sensor_smoke'
    'sensor-gas'             = 'sensor_gas'
    'sensor-button'          = 'sensor_button'
    'sensor-illumination'    = 'sensor_illumination'
    'sensor-vibration'       = 'sensor_vibration'
    'thermostat'             = 'thermostat'
    'thermostat-ac'          = 'thermostat_ac'
    'humidifier'             = 'humidifier'
    'purifier'               = 'purifier'
    'ventilation'            = 'ventilation'
    'openable-curtain'       = 'curtain'
    'openable-door-lock'     = 'door_lock'
    'valve'                  = 'valve'
    'openable'               = 'openable'
    'camera'                 = 'camera'
    'vacuum-cleaner'         = 'vacuum_cleaner'
    'cooking-kettle'         = 'kettle'
    'cooking-coffee-maker'   = 'coffee_maker'
    'dishwasher'             = 'dishwasher'
    'washing-machine'        = 'washing_machine'
    'cooking'                = 'cooking'
    'smart-meter'            = 'smart_meter'
    'pet-feeder'             = 'pet_feeder'
    'pet-drinking-fountain'  = 'pet_drinking_fountain'
    'station-no-bg'          = 'station'
    'phone-no-bg'            = 'phone'
    'hub-no-bg'              = 'hub'
    'baby_monitor'           = 'baby_monitor'
    'media-device-tv'        = 'tv'
}

function Upload-DeviceImagesToTarget {
    param(
        [ValidateSet('staging', 'prod')][string]$EnvName
    )
    $cookie = Join-Path $env:TEMP "alice-upload-devices-$EnvName-cookies.txt"
    Remove-Item $cookie -Force -ErrorAction SilentlyContinue
    try {
        if ($EnvName -eq 'staging') {
            New-StagingSession -CookieJar $cookie
            $post = { param($BodyPath) Invoke-StagingJsonPost -Path '/admin/api/smarthome/upload-image' -CookieJar $cookie -BodyPath $BodyPath }
            $base = $env:STAGING_API_URL.TrimEnd('/')
        } else {
            if (-not $env:PROD_API_URL) { $env:PROD_API_URL = 'https://api.alicecommands.ru' }
            New-ProdSession -CookieJar $cookie
            $post = { param($BodyPath) Invoke-ProdJsonPost -Path '/admin/api/smarthome/upload-image' -CookieJar $cookie -BodyPath $BodyPath }
            $base = $env:PROD_API_URL.TrimEnd('/')
        }

        $ok = 0
        $skip = 0
        foreach ($entry in $Map.GetEnumerator() | Sort-Object Name) {
            $fileName = "$($entry.Key).png"
            $path = Join-Path $ImagesDir $fileName
            if (-not (Test-Path -LiteralPath $path)) {
                Write-Warning "[$EnvName] missing file: $fileName"
                $skip++
                continue
            }
            $bytes = [IO.File]::ReadAllBytes($path)
            $b64 = [Convert]::ToBase64String($bytes)
            $body = @{
                slug         = $entry.Value
                content_type = 'image/png'
                image_base64 = $b64
            } | ConvertTo-Json -Compress
            $tmp = New-TemporaryFile
            try {
                [IO.File]::WriteAllText($tmp.FullName, $body, [Text.UTF8Encoding]::new($false))
                $res = & $post $tmp.FullName
                Write-Host "[$EnvName] $($entry.Value) -> $($res.image_url)"
                $ok++
            } finally {
                Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
            }
        }
        Write-Host "[$EnvName] uploaded=$ok skipped=$skip base=$base"
        if ($skip -gt 0 -and -not $AllowMissing) {
            throw "[$EnvName] $skip mapped PNG file(s) missing under $ImagesDir (use -AllowMissing for partial upload)"
        }
    } finally {
        Remove-Item $cookie -Force -ErrorAction SilentlyContinue
    }
}

$targets = if ($Target -eq 'both') { @('staging', 'prod') } else { @($Target) }
foreach ($t in $targets) {
    Upload-DeviceImagesToTarget -EnvName $t
}
Write-Host 'Upload complete.'
