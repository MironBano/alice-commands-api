# Upload icon SVGs + catalog to staging VPS (no server rebuild).
#Requires -Version 5.1
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim().Trim('"').Trim("'")
        }
    }
}

$sshKey = if ($env:SSH_KEY_PATH) { $env:SSH_KEY_PATH } else { "$env:USERPROFILE\.ssh\id_ed25519_selectel" }
$sshHost = if ($env:SSH_HOST) { $env:SSH_HOST } else { "root@161.104.46.92" }
$iconDir = Join-Path $Root "content\icons\v1"
$catalog = Join-Path $Root "content\icon_catalog.json"
$visuals = Join-Path $Root "content\visuals_map.json"

if (-not (Test-Path $sshKey)) { throw "SSH key not found: $sshKey" }
if (-not (Test-Path $iconDir)) { throw "Missing $iconDir. Run: py tools/content/icon_sync.py" }

$count = (Get-ChildItem "$iconDir\*.svg").Count
Write-Host "Uploading $count SVGs to staging..."
ssh -i $sshKey $sshHost "mkdir -p /opt/alice-api/content/icons/v1 /opt/alice-api/storage/icons/v1"
scp -i $sshKey -r "$iconDir\*" "${sshHost}:/opt/alice-api/content/icons/v1/"
scp -i $sshKey -r "$iconDir\*" "${sshHost}:/opt/alice-api/storage/icons/v1/"
scp -i $sshKey $catalog "${sshHost}:/opt/alice-api/content/icon_catalog.json"
if (Test-Path $visuals) {
    scp -i $sshKey $visuals "${sshHost}:/opt/alice-api/content/visuals_map.json"
}

Write-Host "Done. Spot-check: curl -I https://staging-api.alicecommands.ru/icons/v1/quick_answers.svg"
