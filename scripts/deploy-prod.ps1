#Requires -Version 5.1
# Deploy production API on same VPS as staging (shared app jars, port 8081, separate storage-prod, nginx api.alicecommands.ru).
# Prerequisites: scripts/.env with SSH_*, CF_API_TOKEN (for first-time DNS), ADMIN_* for prod login.
# Does NOT modify /opt/alice-api/.env (staging).

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
$skipDns = $env:SKIP_PROD_DNS -eq '1'

if (-not (Test-Path $sshKey)) {
    Write-Error "SSH key not found: $sshKey. See docs/INFRASTRUCTURE.md"
}

function Start-AliceApiUnits {
    ssh -i $sshKey $sshHost "systemctl start alice-api; systemctl start alice-api-prod; systemctl is-active alice-api; systemctl is-active alice-api-prod"
}

if (-not $skipDns) {
    $prevRecords = $env:CF_DNS_RECORDS
    $env:CF_DNS_RECORDS = if ($env:CF_PROD_DNS_RECORDS) { $env:CF_PROD_DNS_RECORDS } else { "api,cdn,staging-api" }
    Write-Host "== 0/8 Cloudflare DNS (api + cdn, DNS only) =="
    try {
        & (Join-Path $PSScriptRoot "cloudflare-dns-direct.ps1")
    } catch {
        Write-Warning "DNS setup failed (set SKIP_PROD_DNS=1 to skip): $_"
    } finally {
        if ($prevRecords) { $env:CF_DNS_RECORDS = $prevRecords } else { Remove-Item Env:CF_DNS_RECORDS -ErrorAction SilentlyContinue }
    }
    Start-Sleep -Seconds 30
}

Write-Host "== 1/8 Build =="
& .\gradlew.bat ":server:installDist" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path $Root "server\build\install\server"
if (-not (Test-Path $dist)) { Write-Error "Missing $dist" }

# Staging and prod share /opt/alice-api/app — never overwrite jars under a live JVM
# (causes NoClassDefFoundError on first use of classes loaded after zip replace).
Write-Host "== 2/8 Stop staging+prod, upload jars, start both =="
ssh -i $sshKey $sshHost "systemctl stop alice-api-prod alice-api"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
try {
    scp -i $sshKey -r "$dist\*" "${sshHost}:${remoteApp}/"
    if ($LASTEXITCODE -ne 0) { throw "scp app jars failed (exit $LASTEXITCODE)" }
} finally {
    Write-Host "Ensuring alice-api + alice-api-prod are started..."
    Start-AliceApiUnits | Write-Host
}

Write-Host "== 3/8 Upload deploy configs + seed =="
ssh -i $sshKey $sshHost "mkdir -p /opt/alice-api/deploy /opt/alice-api/seed /opt/alice-api/storage-prod/bundles /opt/alice-api/storage-prod/manifest /var/log/alice-api-prod"
scp -i $sshKey (Join-Path $Root "deploy\nginx-prod.conf") "${sshHost}:${remoteDeploy}/nginx-prod.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-prod-bootstrap.conf") "${sshHost}:${remoteDeploy}/nginx-prod-bootstrap.conf"
scp -i $sshKey (Join-Path $Root "deploy\alice-api-prod.service") "${sshHost}:${remoteDeploy}/alice-api-prod.service"
scp -i $sshKey (Join-Path $Root "deploy\.env.prod.example") "${sshHost}:${remoteDeploy}/.env.prod.example"
scp -i $sshKey (Join-Path $Root "deploy\bootstrap-prod.sh") "${sshHost}:${remoteDeploy}/bootstrap-prod.sh"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn.conf") "${sshHost}:${remoteDeploy}/nginx-cdn.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn-bootstrap.conf") "${sshHost}:${remoteDeploy}/nginx-cdn-bootstrap.conf"
scp -i $sshKey (Join-Path $Root "seed\catalog-audit-fixed.json") "${sshHost}:/opt/alice-api/seed/catalog-audit-fixed.json"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 4/8 Upload admin-web =="
scp -i $sshKey -r (Join-Path $Root "admin-web\*") "${sshHost}:/opt/alice-api/admin-web/"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 5/8 Bootstrap prod DB + .env.prod =="
ssh -i $sshKey $sshHost "chmod +x ${remoteDeploy}/bootstrap-prod.sh && bash ${remoteDeploy}/bootstrap-prod.sh ${remoteDeploy}"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 6/8 nginx + systemd unit refresh =="
$remoteNginx = @'
set -e
cp REMOTE_DEPLOY/alice-api-prod.service /etc/systemd/system/alice-api-prod.service
systemctl daemon-reload
systemctl enable alice-api-prod
if [ -f /etc/letsencrypt/live/api.alicecommands.ru/fullchain.pem ]; then
  cp REMOTE_DEPLOY/nginx-prod.conf /etc/nginx/sites-available/alice-api-prod
else
  cp REMOTE_DEPLOY/nginx-prod-bootstrap.conf /etc/nginx/sites-available/alice-api-prod
fi
ln -sf /etc/nginx/sites-available/alice-api-prod /etc/nginx/sites-enabled/alice-api-prod
if [ -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  cp REMOTE_DEPLOY/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn
else
  cp REMOTE_DEPLOY/nginx-cdn-bootstrap.conf /etc/nginx/sites-available/alice-cdn
fi
ln -sf /etc/nginx/sites-available/alice-cdn /etc/nginx/sites-enabled/alice-cdn
nginx -t && systemctl reload nginx
if [ ! -f /etc/letsencrypt/live/api.alicecommands.ru/fullchain.pem ]; then
  certbot certonly --webroot -w /var/www/html -d api.alicecommands.ru --non-interactive --agree-tos --register-unsafely-without-email && \
    cp REMOTE_DEPLOY/nginx-prod.conf /etc/nginx/sites-available/alice-api-prod && nginx -t && systemctl reload nginx || true
fi
if [ ! -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  certbot certonly --webroot -w /var/www/html -d cdn.alicecommands.ru --non-interactive --agree-tos --register-unsafely-without-email && \
    cp REMOTE_DEPLOY/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn && nginx -t && systemctl reload nginx || true
fi
systemctl restart alice-api-prod
systemctl restart alice-api
sleep 12
systemctl is-active alice-api-prod
systemctl is-active alice-api
curl -sS -f http://127.0.0.1:8081/health >/dev/null
curl -sS -f http://127.0.0.1:8080/health >/dev/null
code=$(curl -sS -o /tmp/alice-prod-feedback.json -w '%{http_code}' -X POST http://127.0.0.1:8081/v1/feedback -H 'Content-Type: application/json' -d '{"message":"prod deploy smoke","app_version":"deploy","platform":"script"}')
echo "feedback:$code"
test "$code" = "201"
grep -q '"id"' /tmp/alice-prod-feedback.json
'@ -replace 'REMOTE_DEPLOY', $remoteDeploy
ssh -i $sshKey $sshHost $remoteNginx
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 7/8 External smoke =="
$prodUrl = if ($env:PROD_API_URL) { $env:PROD_API_URL } else { "https://api.alicecommands.ru" }
curl.exe -sS --fail --max-time 30 "$prodUrl/health" 2>&1 | Write-Host
Write-Host "Done. Prod API: $prodUrl/health"
Write-Host "Admin: $prodUrl/admin"
Write-Host "Next: .\scripts\copy-staging-to-prod.ps1  then  .\scripts\verify-prod.ps1"
