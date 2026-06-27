#!/bin/bash
set -euo pipefail

DOMAIN="${1:-staging-api.alicecommands.ru}"
APP_DIR="/opt/alice-api"

echo "==> Packages"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq openjdk-21-jre-headless nginx postgresql certbot python3-certbot-nginx ufw

echo "==> Firewall"
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable || true

echo "==> PostgreSQL"
mkdir -p "${APP_DIR}"
if [[ -f "${APP_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${APP_DIR}/.env"
  set +a
fi
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='alice'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE USER alice WITH PASSWORD '${DATABASE_PASSWORD}';"
  sudo -u postgres psql -c "CREATE DATABASE alice_commands_staging OWNER alice;"
else
  echo "PostgreSQL user alice already exists"
fi

if [[ -f "${APP_DIR}/.db-credentials" ]]; then
  :
fi

echo "==> App directories"
mkdir -p "${APP_DIR}/storage/bundles" "${APP_DIR}/storage/manifest" /var/log/alice-api
chmod 755 "${APP_DIR}" /var/log/alice-api
chmod +x "${APP_DIR}/app/bin/server"
sed -i 's/\r$//' "${APP_DIR}/app/bin/server" 2>/dev/null || true

echo "==> systemd"
cp "${APP_DIR}/deploy/alice-api.service" /etc/systemd/system/alice-api.service
systemctl daemon-reload
systemctl enable alice-api

echo "==> nginx"
cp "${APP_DIR}/deploy/nginx-staging.conf" /etc/nginx/sites-available/alice-api
ln -sf /etc/nginx/sites-available/alice-api /etc/nginx/sites-enabled/alice-api
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo "==> TLS (Let's Encrypt)"
certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos --register-unsafely-without-email --redirect || \
  certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m admin@alicecommands.ru --redirect

echo "==> Start API"
systemctl restart alice-api
sleep 3
systemctl status alice-api --no-pager || true

echo "==> Done. Check: curl -sS https://${DOMAIN}/health"
