#!/usr/bin/env bash
# Bootstrap production env + database (idempotent). Called from deploy-prod.ps1
set -euo pipefail

REMOTE_DEPLOY="${1:-/opt/alice-api/deploy}"

if [ ! -f /opt/alice-api/.env.prod ]; then
  > /opt/alice-api/.env.prod
  while IFS= read -r line || [ -n "$line" ]; do
  [[ "$line" =~ ^[[:space:]]*# ]] && { echo "$line" >> /opt/alice-api/.env.prod; continue; }
  [[ -z "${line// }" ]] && { echo "$line" >> /opt/alice-api/.env.prod; continue; }
    key="${line%%=*}"
    key="${key// /}"
    case "$key" in
      DATABASE_USER|DATABASE_PASSWORD|ADMIN_USERNAME|ADMIN_PASSWORD|SESSION_SECRET)
        if [ -f /opt/alice-api/.env ]; then
          override=$(grep -E "^${key}=" /opt/alice-api/.env | head -n1 || true)
          if [ -n "$override" ]; then
            echo "$override" >> /opt/alice-api/.env.prod
            continue
          fi
        fi
        ;;
    esac
    echo "$line" >> /opt/alice-api/.env.prod
  done < "${REMOTE_DEPLOY}/.env.prod.example"
  echo "Created /opt/alice-api/.env.prod"
fi

DB_USER=$(grep '^DATABASE_USER=' /opt/alice-api/.env.prod | cut -d= -f2-)
if ! sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='alice_commands'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE DATABASE alice_commands OWNER ${DB_USER};"
  echo "Created database alice_commands"
fi

mkdir -p /opt/alice-api/storage-prod/bundles /opt/alice-api/storage-prod/manifest /var/log/alice-api-prod
command -v jq >/dev/null 2>&1 || apt-get install -y jq >/dev/null 2>&1 || true
