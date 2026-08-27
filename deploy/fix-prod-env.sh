#!/usr/bin/env bash
set -euo pipefail
# One-time fix: sync secrets from staging .env into .env.prod (safe line-by-line merge)
> /opt/alice-api/.env.prod.new
while IFS= read -r line || [ -n "$line" ]; do
  [[ "$line" =~ ^[[:space:]]*# ]] && { echo "$line" >> /opt/alice-api/.env.prod.new; continue; }
  [[ -z "${line// }" ]] && { echo "$line" >> /opt/alice-api/.env.prod.new; continue; }
  key="${line%%=*}"
  key="${key// /}"
  case "$key" in
    DATABASE_USER|DATABASE_PASSWORD|ADMIN_USERNAME|ADMIN_PASSWORD|SESSION_SECRET)
      if [ -f /opt/alice-api/.env ]; then
        override=$(grep -E "^${key}=" /opt/alice-api/.env | head -n1 || true)
        if [ -n "$override" ]; then
          echo "$override" >> /opt/alice-api/.env.prod.new
          continue
        fi
      fi
      ;;
  esac
  echo "$line" >> /opt/alice-api/.env.prod.new
done < /opt/alice-api/.env.prod
mv /opt/alice-api/.env.prod.new /opt/alice-api/.env.prod
systemctl restart alice-api-prod
sleep 8
systemctl is-active alice-api-prod
curl -sS http://127.0.0.1:8081/health
