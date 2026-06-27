#Requires -Version 5.1
# Deploy server build to Selectel VPS staging.
# Prerequisites: scripts/.env with SSH_* vars, SSH key at id_ed25519_selectel (see docs/INFRASTRUCTURE.md)

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
$remoteApp = if ($env:DEPLOY_REMOTE_APP) { $env:DEPLOY_REMOTE_APP } else { "/opt/alice-api/app" }
$remoteDeploy = if ($env:DEPLOY_REMOTE_DEPLOY) { $env:DEPLOY_REMOTE_DEPLOY } else { "/opt/alice-api/deploy" }

if (-not (Test-Path $sshKey)) {
    Write-Error "SSH key not found: $sshKey. See docs/INFRASTRUCTURE.md"
}

Write-Host "== 1/4 Build =="
& .\gradlew.bat ":server:installDist" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path $Root "server\build\install\server"
if (-not (Test-Path $dist)) { Write-Error "Missing $dist" }

Write-Host "== 2/4 Upload app =="
scp -i $sshKey -r "$dist\*" "${sshHost}:${remoteApp}/"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 3/4 Upload nginx config =="
scp -i $sshKey (Join-Path $Root "deploy\nginx-staging.conf") "${sshHost}:${remoteDeploy}/nginx-staging.conf"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 4/4 Restart services =="
ssh -i $sshKey $sshHost @"
set -e
cp ${remoteDeploy}/nginx-staging.conf /etc/nginx/sites-available/alice-api
nginx -t
systemctl reload nginx
systemctl restart alice-api
sleep 2
systemctl is-active alice-api
curl -sS http://127.0.0.1:8080/health
"@

Write-Host "Done. Check: https://staging-api.alicecommands.ru/health"
