#Requires -Version 5.1
# Deploy server build to Selectel VPS staging.
# Prerequisites: scripts/.env with SSH_* vars, SSH key at id_ed25519_selectel (see docs/INFRASTRUCTURE.md)
# Note: staging and prod share /opt/alice-api/app — jar upload stops BOTH units, then starts both.

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

function Start-AliceApiUnits {
    ssh -i $sshKey $sshHost "systemctl start alice-api; systemctl start alice-api-prod 2>/dev/null || true; systemctl is-active alice-api; systemctl is-active alice-api-prod 2>/dev/null || true"
}

Write-Host "== 1/5 Build =="
& .\gradlew.bat ":server:installDist" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path $Root "server\build\install\server"
if (-not (Test-Path $dist)) { Write-Error "Missing $dist" }

# Staging and prod share /opt/alice-api/app — stop both before replacing jars.
Write-Host "== 2/5 Stop staging+prod, upload jars, start both =="
ssh -i $sshKey $sshHost "systemctl stop alice-api; systemctl stop alice-api-prod 2>/dev/null || true"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
try {
    scp -i $sshKey -r "$dist\*" "${sshHost}:${remoteApp}/"
    if ($LASTEXITCODE -ne 0) { throw "scp app jars failed (exit $LASTEXITCODE)" }
} finally {
    Write-Host "Ensuring alice-api + alice-api-prod are started..."
    Start-AliceApiUnits | Write-Host
}

Write-Host "== 3/5 Upload content assets + nginx + admin-web =="
$stagingEnvLocal = Join-Path $Root "deploy\.env.staging"
if (-not (Test-Path $stagingEnvLocal)) {
    Write-Error "Missing $stagingEnvLocal — copy deploy\.env.staging.example and fill push vars."
}
ssh -i $sshKey $sshHost "mkdir -p /opt/alice-api/content/icons/v1 /opt/alice-api/seed /opt/alice-api/storage/icons /opt/alice-api/schema"
scp -i $sshKey -r (Join-Path $Root "content\icons\v1\*") "${sshHost}:/opt/alice-api/content/icons/v1/"
scp -i $sshKey (Join-Path $Root "content\icon_catalog.json") "${sshHost}:/opt/alice-api/content/icon_catalog.json"
scp -i $sshKey (Join-Path $Root "seed\catalog-audit-fixed.json") "${sshHost}:/opt/alice-api/seed/catalog-audit-fixed.json"
scp -i $sshKey (Join-Path $Root "schema\*.json") "${sshHost}:/opt/alice-api/schema/"
scp -i $sshKey (Join-Path $Root "deploy\nginx-staging.conf") "${sshHost}:${remoteDeploy}/nginx-staging.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn.conf") "${sshHost}:${remoteDeploy}/nginx-cdn.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn-bootstrap.conf") "${sshHost}:${remoteDeploy}/nginx-cdn-bootstrap.conf"
scp -i $sshKey (Join-Path $Root "deploy\certbot-reload-nginx.sh") "${sshHost}:${remoteDeploy}/certbot-reload-nginx.sh"
scp -i $sshKey (Join-Path $Root "deploy\smoke-after-deploy.sh") "${sshHost}:${remoteDeploy}/smoke-after-deploy.sh"
scp -i $sshKey (Join-Path $Root "deploy\.env.staging") "${sshHost}:${remoteDeploy}/.env.staging"
scp -i $sshKey -r (Join-Path $Root "admin-web\*") "${sshHost}:/opt/alice-api/admin-web/"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Windows checkouts may upload CRLF .sh; strip CR before bash. Also avoid PowerShell here-string mangling `set -e`.
Write-Host "== 4/5 CDN + icon env + restart with smoke =="
$step4 = @'
#!/bin/bash
set -e
ENV_FILE=/opt/alice-api/.env
touch "$ENV_FILE"
grep -q '^ICON_STORAGE_PATH=' "$ENV_FILE" || printf '\nICON_STORAGE_PATH=/opt/alice-api/storage/icons\n' >> "$ENV_FILE"
grep -q '^ICON_PUBLIC_BASE_URL=' "$ENV_FILE" || printf 'ICON_PUBLIC_BASE_URL=https://staging-api.alicecommands.ru\n' >> "$ENV_FILE"
grep -q '^ICON_URL_ALLOWED_HOSTS=' "$ENV_FILE" || printf 'ICON_URL_ALLOWED_HOSTS=staging-api.alicecommands.ru,cdn.alicecommands.ru,api.alicecommands.ru,localhost,127.0.0.1\n' >> "$ENV_FILE"
grep -q '^DEVICE_IMAGE_STORAGE_PATH=' "$ENV_FILE" || printf 'DEVICE_IMAGE_STORAGE_PATH=/opt/alice-api/storage/devices\n' >> "$ENV_FILE"
grep -q '^CONTENT_SEED_PATH=' "$ENV_FILE" || printf 'CONTENT_SEED_PATH=/opt/alice-api/seed/catalog-audit-fixed.json\n' >> "$ENV_FILE"
grep -q '^ANALYTICS_RATE_LIMIT_PER_IP=' "$ENV_FILE" || printf 'ANALYTICS_RATE_LIMIT_PER_IP=120\n' >> "$ENV_FILE"
grep -q '^ANALYTICS_EVENTS_PER_IP_PER_DAY=' "$ENV_FILE" || printf 'ANALYTICS_EVENTS_PER_IP_PER_DAY=10000\n' >> "$ENV_FILE"
grep -q '^ANALYTICS_MAX_BODY_BYTES=' "$ENV_FILE" || printf 'ANALYTICS_MAX_BODY_BYTES=262144\n' >> "$ENV_FILE"
grep -q '^ANALYTICS_RAW_RETENTION_DAYS=' "$ENV_FILE" || printf 'ANALYTICS_RAW_RETENTION_DAYS=90\n' >> "$ENV_FILE"
grep -q '^RUSTORE_PUSH_PROJECT_ID=' "$ENV_FILE" || printf 'RUSTORE_PUSH_PROJECT_ID=\n' >> "$ENV_FILE"
grep -q '^RUSTORE_PUSH_SERVICE_TOKEN=' "$ENV_FILE" || printf 'RUSTORE_PUSH_SERVICE_TOKEN=\n' >> "$ENV_FILE"
grep -q '^FCM_PROJECT_ID=' "$ENV_FILE" || printf 'FCM_PROJECT_ID=\n' >> "$ENV_FILE"
grep -q '^FCM_SERVICE_ACCOUNT_JSON=' "$ENV_FILE" || printf 'FCM_SERVICE_ACCOUNT_JSON=\n' >> "$ENV_FILE"
grep -q '^PUSH_CAMPAIGN_ENABLED=' "$ENV_FILE" || printf 'PUSH_CAMPAIGN_ENABLED=true\n' >> "$ENV_FILE"
grep -q '^PUSH_INSTALL_RATE_LIMIT=' "$ENV_FILE" || printf 'PUSH_INSTALL_RATE_LIMIT=30\n' >> "$ENV_FILE"
grep -q '^PUBLIC_SUBMISSION_RATE_LIMIT=' "$ENV_FILE" || printf 'PUBLIC_SUBMISSION_RATE_LIMIT=20\n' >> "$ENV_FILE"
set_kv() { key="$1"; val="$2"; [ -z "$val" ] && return 0; if grep -q "^${key}=" "$ENV_FILE"; then sed -i "s|^${key}=.*|${key}=${val}|" "$ENV_FILE"; else printf '%s=%s\n' "$key" "$val" >> "$ENV_FILE"; fi; }
if [ -f /opt/alice-api/deploy/.env.staging ]; then
  set_kv RUSTORE_PUSH_PROJECT_ID "$(grep -E '^RUSTORE_PUSH_PROJECT_ID=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv RUSTORE_PUSH_SERVICE_TOKEN "$(grep -E '^RUSTORE_PUSH_SERVICE_TOKEN=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv PUSH_CAMPAIGN_ENABLED "$(grep -E '^PUSH_CAMPAIGN_ENABLED=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv PUSH_INSTALL_RATE_LIMIT "$(grep -E '^PUSH_INSTALL_RATE_LIMIT=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
fi
# Manual QA: staging deploy also syncs push vars into prod .env.prod (shared jar on VPS).
PROD_ENV=/opt/alice-api/.env.prod
if [ -f "$PROD_ENV" ] && [ -f /opt/alice-api/deploy/.env.staging ]; then
  set_kv_prod() { key="$1"; val="$2"; [ -z "$val" ] && return 0; if grep -q "^${key}=" "$PROD_ENV"; then sed -i "s|^${key}=.*|${key}=${val}|" "$PROD_ENV"; else printf '%s=%s\n' "$key" "$val" >> "$PROD_ENV"; fi; }
  set_kv_prod RUSTORE_PUSH_PROJECT_ID "$(grep -E '^RUSTORE_PUSH_PROJECT_ID=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv_prod RUSTORE_PUSH_SERVICE_TOKEN "$(grep -E '^RUSTORE_PUSH_SERVICE_TOKEN=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv_prod PUSH_CAMPAIGN_ENABLED "$(grep -E '^PUSH_CAMPAIGN_ENABLED=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
  set_kv_prod PUSH_INSTALL_RATE_LIMIT "$(grep -E '^PUSH_INSTALL_RATE_LIMIT=' /opt/alice-api/deploy/.env.staging | head -n1 | cut -d= -f2-)"
fi
mkdir -p /opt/alice-api/storage/icons/v1 /opt/alice-api/storage/devices/v1
cp -f /opt/alice-api/content/icons/v1/*.svg /opt/alice-api/storage/icons/v1/ 2>/dev/null || true
cp /opt/alice-api/deploy/nginx-staging.conf /etc/nginx/sites-available/alice-api
if [ -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  cp /opt/alice-api/deploy/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn
else
  cp /opt/alice-api/deploy/nginx-cdn-bootstrap.conf /etc/nginx/sites-available/alice-cdn
fi
ln -sf /etc/nginx/sites-available/alice-cdn /etc/nginx/sites-enabled/alice-cdn
mkdir -p /etc/letsencrypt/renewal-hooks/deploy /var/www/html/.well-known/acme-challenge
sed -i 's/\r$//' /opt/alice-api/deploy/certbot-reload-nginx.sh
install -m 755 /opt/alice-api/deploy/certbot-reload-nginx.sh /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
if [ ! -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  nginx -t && systemctl reload nginx
  certbot certonly --webroot -w /var/www/html -d cdn.alicecommands.ru --non-interactive --agree-tos --register-unsafely-without-email && \
    cp /opt/alice-api/deploy/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn || true
fi
nginx -t
systemctl reload nginx
systemctl restart alice-api
systemctl restart alice-api-prod 2>/dev/null || true
sleep 10
systemctl is-active alice-api
systemctl is-active alice-api-prod 2>/dev/null || true
sed -i 's/\r$//' /opt/alice-api/deploy/smoke-after-deploy.sh
chmod +x /opt/alice-api/deploy/smoke-after-deploy.sh
bash /opt/alice-api/deploy/smoke-after-deploy.sh
'@
$step4Local = Join-Path $env:TEMP "alice-deploy-staging-step4.sh"
[System.IO.File]::WriteAllText($step4Local, ($step4 -replace "`r`n", "`n" -replace "`r", "`n"))
scp -i $sshKey $step4Local "${sshHost}:/tmp/alice-deploy-staging-step4.sh"
ssh -i $sshKey $sshHost "bash /tmp/alice-deploy-staging-step4.sh"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 5/5 Done =="
Write-Host "Done. Icons (staging): https://staging-api.alicecommands.ru/icons/v1/child.svg"
Write-Host "CDN (after setup-cdn.ps1): https://cdn.alicecommands.ru/icons/v1/child.svg"
Write-Host "API: https://staging-api.alicecommands.ru/health"
