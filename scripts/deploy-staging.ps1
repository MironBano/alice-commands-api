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

Write-Host "== 2/5 Upload app =="
scp -i $sshKey -r "$dist\*" "${sshHost}:${remoteApp}/"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 3/5 Upload content assets (icons, catalog, seed) =="
ssh -i $sshKey $sshHost "mkdir -p /opt/alice-api/content/icons/v1 /opt/alice-api/seed /opt/alice-api/storage/icons"
scp -i $sshKey -r (Join-Path $Root "content\icons\v1\*") "${sshHost}:/opt/alice-api/content/icons/v1/"
scp -i $sshKey (Join-Path $Root "content\icon_catalog.json") "${sshHost}:/opt/alice-api/content/icon_catalog.json"
scp -i $sshKey (Join-Path $Root "seed\full-catalog.json") "${sshHost}:/opt/alice-api/seed/full-catalog.json"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 4/6 Upload nginx config =="
scp -i $sshKey (Join-Path $Root "deploy\nginx-staging.conf") "${sshHost}:${remoteDeploy}/nginx-staging.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn.conf") "${sshHost}:${remoteDeploy}/nginx-cdn.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn-bootstrap.conf") "${sshHost}:${remoteDeploy}/nginx-cdn-bootstrap.conf"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 5/6 Upload admin-web =="
scp -i $sshKey -r (Join-Path $Root "admin-web\*") "${sshHost}:/opt/alice-api/admin-web/"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 6/6 Restart services + CDN + icon env =="
ssh -i $sshKey $sshHost @"
set -e
ENV_FILE=/opt/alice-api/.env
touch `$ENV_FILE
grep -q '^ICON_STORAGE_PATH=' `$ENV_FILE || printf '\nICON_STORAGE_PATH=/opt/alice-api/storage/icons\n' >> `$ENV_FILE
grep -q '^ICON_PUBLIC_BASE_URL=' `$ENV_FILE || printf 'ICON_PUBLIC_BASE_URL=https://staging-api.alicecommands.ru\n' >> `$ENV_FILE
grep -q '^ICON_URL_ALLOWED_HOSTS=' `$ENV_FILE || printf 'ICON_URL_ALLOWED_HOSTS=staging-api.alicecommands.ru,cdn.alicecommands.ru,api.alicecommands.ru,localhost,127.0.0.1\n' >> `$ENV_FILE
grep -q '^CONTENT_SEED_PATH=' `$ENV_FILE || printf 'CONTENT_SEED_PATH=/opt/alice-api/seed/full-catalog.json\n' >> `$ENV_FILE
mkdir -p /opt/alice-api/storage/icons/v1
cp -f /opt/alice-api/content/icons/v1/*.svg /opt/alice-api/storage/icons/v1/ 2>/dev/null || true
cp ${remoteDeploy}/nginx-staging.conf /etc/nginx/sites-available/alice-api
if [ -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  cp ${remoteDeploy}/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn
else
  cp ${remoteDeploy}/nginx-cdn-bootstrap.conf /etc/nginx/sites-available/alice-cdn
fi
ln -sf /etc/nginx/sites-available/alice-cdn /etc/nginx/sites-enabled/alice-cdn
if [ ! -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  nginx -t && systemctl reload nginx
  certbot certonly --webroot -w /var/www/html -d cdn.alicecommands.ru --non-interactive --agree-tos --register-unsafely-without-email && \
    cp ${remoteDeploy}/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn || true
fi
nginx -t
systemctl reload nginx
systemctl restart alice-api
sleep 10
systemctl is-active alice-api
curl -sS http://127.0.0.1:8080/health
curl -sS -o /dev/null -w 'api_icon:%{http_code}\n' http://127.0.0.1:8080/icons/v1/child.svg
curl -sS -o /dev/null -w 'cdn_icon:%{http_code}\n' https://cdn.alicecommands.ru/icons/v1/child.svg || true
"@
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Done. Icons (staging): https://staging-api.alicecommands.ru/icons/v1/child.svg"
Write-Host "CDN (after setup-cdn.ps1): https://cdn.alicecommands.ru/icons/v1/child.svg"
Write-Host "API: https://staging-api.alicecommands.ru/health"
