#Requires -Version 5.1
# DNS (Cloudflare) + Let's Encrypt + nginx for cdn.alicecommands.ru icons.
# Requires CF_API_TOKEN in scripts\.env or project .env (see scripts\.env.example).

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "== 1/3 Cloudflare DNS (cdn + staging-api, DNS only) =="
& (Join-Path $PSScriptRoot "cloudflare-dns-direct.ps1")

Write-Host "== 2/3 Wait for DNS propagation =="
Start-Sleep -Seconds 45

Write-Host "== 3/3 Deploy nginx CDN + certbot on VPS =="
$sshKey = if ($env:SSH_KEY_PATH) { $env:SSH_KEY_PATH } else { "$env:USERPROFILE\.ssh\id_ed25519_selectel" }
$sshHost = if ($env:SSH_HOST) { $env:SSH_HOST } else { "root@161.104.46.92" }
$remoteDeploy = if ($env:DEPLOY_REMOTE_DEPLOY) { $env:DEPLOY_REMOTE_DEPLOY } else { "/opt/alice-api/deploy" }

scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn.conf") "${sshHost}:${remoteDeploy}/nginx-cdn.conf"
scp -i $sshKey (Join-Path $Root "deploy\nginx-cdn-bootstrap.conf") "${sshHost}:${remoteDeploy}/nginx-cdn-bootstrap.conf"

ssh -i $sshKey $sshHost @"
set -e
mkdir -p /opt/alice-api/storage/icons/v1
cp -f /opt/alice-api/content/icons/v1/*.svg /opt/alice-api/storage/icons/v1/ 2>/dev/null || true
if [ -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  cp ${remoteDeploy}/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn
else
  cp ${remoteDeploy}/nginx-cdn-bootstrap.conf /etc/nginx/sites-available/alice-cdn
fi
ln -sf /etc/nginx/sites-available/alice-cdn /etc/nginx/sites-enabled/alice-cdn
nginx -t && systemctl reload nginx
if [ ! -f /etc/letsencrypt/live/cdn.alicecommands.ru/fullchain.pem ]; then
  certbot certonly --webroot -w /var/www/html -d cdn.alicecommands.ru --non-interactive --agree-tos --register-unsafely-without-email
  cp ${remoteDeploy}/nginx-cdn.conf /etc/nginx/sites-available/alice-cdn
  nginx -t && systemctl reload nginx
fi
ENV_FILE=/opt/alice-api/.env
if grep -q '^ICON_PUBLIC_BASE_URL=' `$ENV_FILE; then
  sed -i 's|^ICON_PUBLIC_BASE_URL=.*|ICON_PUBLIC_BASE_URL=https://cdn.alicecommands.ru|' `$ENV_FILE
else
  printf 'ICON_PUBLIC_BASE_URL=https://cdn.alicecommands.ru\n' >> `$ENV_FILE
fi
systemctl restart alice-api
sleep 5
curl -sS -o /dev/null -w 'https_cdn:%{http_code}\n' https://cdn.alicecommands.ru/icons/v1/child.svg
"@

Write-Host "Done. Open: https://cdn.alicecommands.ru/icons/v1/child.svg"
