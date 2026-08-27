#Requires -Version 5.1
# Re-publish smarthome snapshot from admin draft.
# WARNING: do not round-trip JSON via broken stdout encoding — use import-smarthome-payload.ps1 to restore content.
param(
    [ValidateSet('staging', 'prod')]
    [string]$Target = 'staging'
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
. (Join-Path $PSScriptRoot '_env.ps1')
. (Join-Path $PSScriptRoot '_staging-http.ps1')
. (Join-Path $PSScriptRoot '_prod-http.ps1')
Load-ScriptEnv

$cookie = Join-Path $env:TEMP "alice-republish-sh-$Target-cookies.txt"
Remove-Item $cookie -Force -ErrorAction SilentlyContinue

try {
    if ($Target -eq 'staging') {
        New-StagingSession -CookieJar $cookie
        $picks = Invoke-StagingJsonGet -Path '/admin/api/smarthome/device-picks' -CookieJar $cookie
        foreach ($pick in @($picks)) {
            $tmp = New-TemporaryFile
            try {
                $json = $pick | ConvertTo-Json -Depth 20 -Compress
                [IO.File]::WriteAllText($tmp.FullName, $json, [Text.UTF8Encoding]::new($false))
                Invoke-StagingJsonPut -Path "/admin/api/smarthome/device-picks/$($pick.id)" -CookieJar $cookie -BodyPath $tmp.FullName | Out-Null
            } finally {
                Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
            }
            Write-Host "Republished pick $($pick.id)"
        }
        $public = Invoke-StagingJsonGet -Path '/v1/smarthome/devices' -CookieJar $cookie
    } else {
        if (-not $env:PROD_API_URL) { $env:PROD_API_URL = 'https://api.alicecommands.ru' }
        New-ProdSession -CookieJar $cookie
        $picks = Invoke-ProdJsonGet -Path '/admin/api/smarthome/device-picks' -CookieJar $cookie
        foreach ($pick in @($picks)) {
            $tmp = New-TemporaryFile
            try {
                $json = $pick | ConvertTo-Json -Depth 20 -Compress
                [IO.File]::WriteAllText($tmp.FullName, $json, [Text.UTF8Encoding]::new($false))
                Invoke-ProdJsonPut -Path "/admin/api/smarthome/device-picks/$($pick.id)" -CookieJar $cookie -BodyPath $tmp.FullName | Out-Null
            } finally {
                Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
            }
            Write-Host "Republished pick $($pick.id)"
        }
        $public = Invoke-ProdJsonGet -Path '/v1/smarthome/devices' -CookieJar $cookie
    }

    $withPlacements = @($public.picks | Where-Object { $_.placements -and $_.placements.Count -gt 0 })
    Write-Host "Public picks with placements: $($withPlacements.Count) / $($public.picks.Count)"
    Write-Host "Guide title sample: $($public.guides[0].title_ru)"
} finally {
    Remove-Item $cookie -Force -ErrorAction SilentlyContinue
}

Write-Host "Done ($Target)."
