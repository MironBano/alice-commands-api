#Requires -Version 5.1
# Перевод staging-api на прямой A → Selectel VPS (без Cloudflare Proxy).
# Нужно для доступа из России без VPN (провайдеры throttle Cloudflare CDN).
#
# 1. Cloudflare → My Profile → API Tokens → Create Token
#    Template: "Edit zone DNS" для зоны alicecommands.ru
# 2. Добавьте в scripts\.env:
#    CF_API_TOKEN=...
#    CF_ZONE_NAME=alicecommands.ru
# 3. .\scripts\cloudflare-dns-direct.ps1

$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot ".env"
$rootEnvFile = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"
foreach ($file in @($envFile, $rootEnvFile)) {
    if (Test-Path $file) {
        Get-Content $file | ForEach-Object {
            if ($_ -match '^\s*([^#=]+)=(.*)$') {
                $key = $matches[1].Trim()
                if (-not (Get-Item -Path "env:$key" -ErrorAction SilentlyContinue)) {
                    Set-Item -Path "env:$key" -Value $matches[2].Trim().Trim('"').Trim("'")
                }
            }
        }
    }
}

$token = $env:CF_API_TOKEN
$zoneName = if ($env:CF_ZONE_NAME) { $env:CF_ZONE_NAME } else { "alicecommands.ru" }
$originIp = if ($env:STAGING_ORIGIN_IP) { $env:STAGING_ORIGIN_IP } else { "161.104.46.92" }
$recordNames = if ($env:CF_DNS_RECORDS) {
    $env:CF_DNS_RECORDS.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
} elseif ($env:CF_STAGING_RECORD) {
    @($env:CF_STAGING_RECORD.Trim())
} else {
    @("staging-api", "cdn")
}

if (-not $token) {
    Write-Error "CF_API_TOKEN missing in scripts\.env"
}

$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

Write-Host "== Zone $zoneName =="
$zoneResp = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones?name=$zoneName" -Headers $headers
if (-not $zoneResp.success -or $zoneResp.result.Count -eq 0) {
    Write-Error "Zone not found or token has no access"
}
$zoneId = $zoneResp.result[0].id
Write-Host "Zone ID: $zoneId"

foreach ($recordName in $recordNames) {
    Write-Host "== DNS record $recordName =="
    $dnsResp = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records?type=A&name=$recordName.$zoneName" -Headers $headers
    $fqdn = "$recordName.$zoneName"

    if ($dnsResp.result.Count -eq 0) {
        Write-Host "Creating A record $fqdn -> $originIp (DNS only, proxied=false)"
        $body = @{
            type = "A"
            name = $recordName
            content = $originIp
            proxied = $false
            ttl = 300
        } | ConvertTo-Json
        $create = Invoke-RestMethod -Method Post -Uri "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records" -Headers $headers -Body $body
        if (-not $create.success) { Write-Error ($create.errors | ConvertTo-Json) }
    } else {
        $rec = $dnsResp.result[0]
        Write-Host "Updating $($rec.id): $fqdn -> $originIp proxied=false (was: $($rec.content), proxied=$($rec.proxied))"
        $body = @{
            type = "A"
            name = $recordName
            content = $originIp
            proxied = $false
            ttl = 300
        } | ConvertTo-Json
        $update = Invoke-RestMethod -Method Put -Uri "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$($rec.id)" -Headers $headers -Body $body
        if (-not $update.success) { Write-Error ($update.errors | ConvertTo-Json) }
    }
}

Write-Host "== Verify (may take 1-5 min to propagate) =="
foreach ($recordName in $recordNames) {
    $fqdn = "$recordName.$zoneName"
    Write-Host "nslookup $fqdn 8.8.8.8  -> should show $originIp (NOT 104.x / 172.x Cloudflare)"
    Write-Host "curl https://$fqdn/health"
}
