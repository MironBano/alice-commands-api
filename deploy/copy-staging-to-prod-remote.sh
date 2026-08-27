#!/usr/bin/env bash
# Run on VPS: copy published staging catalog + smarthome to prod (localhost :8081).
set -euo pipefail

STAGING_URL="${STAGING_URL:-https://staging-api.alicecommands.ru}"
PROD_URL="${PROD_URL:-http://127.0.0.1:8081}"
ADMIN_USER=$(grep '^ADMIN_USERNAME=' /opt/alice-api/.env.prod | cut -d= -f2-)
ADMIN_PASS=$(grep '^ADMIN_PASSWORD=' /opt/alice-api/.env.prod | cut -d= -f2-)
TMP=/tmp/alice-copy-prod
rm -rf "$TMP"
mkdir -p "$TMP"

echo "== download staging bundle =="
curl -sS "${STAGING_URL}/v1/content/bundle" -o "$TMP/bundle.gz"
gunzip -c "$TMP/bundle.gz" > "$TMP/bundle.json"

echo "== prod admin login =="
curl -sS -c "$TMP/cj" -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
  "${PROD_URL}/admin/api/login" > /dev/null

echo "== import + publish =="
curl -sS -b "$TMP/cj" -H 'Content-Type: application/json' \
  --data-binary @"$TMP/bundle.json" \
  "${PROD_URL}/admin/api/import/json?mode=replace"
echo ""
curl -sS -b "$TMP/cj" -H 'Content-Type: application/json' \
  -d '{"min_app_version":"1.0","notes":"copied from staging"}' \
  "${PROD_URL}/admin/api/publish"
echo ""

echo "== smarthome devices =="
if ! command -v jq >/dev/null 2>&1; then
  echo "jq not installed — smarthome copy skipped (install jq or use admin UI)"
  exit 1
fi

if [ -f /tmp/alice-smarthome-full.json ]; then
  cp /tmp/alice-smarthome-full.json "$TMP/devices.json"
  echo "using payload /tmp/alice-smarthome-full.json"
else
  curl -sS "${STAGING_URL}/v1/smarthome/devices" -o "$TMP/devices.json"
  jq 'del(.guides[].detail_referral_pick_ids)' "$TMP/devices.json" > "$TMP/devices-stripped.json"
  mv "$TMP/devices-stripped.json" "$TMP/devices.json"
fi

echo "== clear prod smarthome picks (SQL — admin DELETE republish fails on broken FK) =="
DB_NAME=alice_commands
sudo -u postgres psql -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "DELETE FROM device_picks;"
pick_left=$(sudo -u postgres psql -d "$DB_NAME" -tAc "SELECT COUNT(*) FROM device_picks;")
echo "device_picks remaining: ${pick_left}"
if [ "${pick_left}" != "0" ]; then
  echo "failed to clear device_picks" >&2
  exit 1
fi

upsert_guide() {
  local guide="$1"
  local gid
  gid=$(echo "$guide" | jq -r '.id')
  local code
  code=$(curl -sS -o "$TMP/resp.json" -w '%{http_code}' -b "$TMP/cj" -H 'Content-Type: application/json' -X PUT \
    -d "$guide" "${PROD_URL}/admin/api/smarthome/device-guides/${gid}" || echo 000)
  if [ "$code" = "404" ]; then
    curl -sS -b "$TMP/cj" -H 'Content-Type: application/json' \
      -d "$guide" "${PROD_URL}/admin/api/smarthome/device-guides" > /dev/null
  elif [ "$code" -lt 200 ] || [ "$code" -ge 300 ]; then
    echo "guide ${gid} failed HTTP ${code}: $(cat "$TMP/resp.json" 2>/dev/null | head -c 400)" >&2
    exit 1
  fi
}

upsert_pick() {
  local pick="$1"
  local pid
  pid=$(echo "$pick" | jq -r '.id')
  local code
  code=$(curl -sS -o "$TMP/resp.json" -w '%{http_code}' -b "$TMP/cj" -H 'Content-Type: application/json' -X PUT \
    -d "$pick" "${PROD_URL}/admin/api/smarthome/device-picks/${pid}" || echo 000)
  if [ "$code" = "404" ]; then
    curl -sS -b "$TMP/cj" -H 'Content-Type: application/json' \
      -d "$pick" "${PROD_URL}/admin/api/smarthome/device-picks" > /dev/null
  elif [ "$code" -lt 200 ] || [ "$code" -ge 300 ]; then
    echo "pick ${pid} failed HTTP ${code}: $(cat "$TMP/resp.json" 2>/dev/null | head -c 400)" >&2
    exit 1
  fi
}

# Pass 1: guides without related_device_ids (FK may not exist yet on prod).
jq -c '.guides[] | .related_device_ids = []' "$TMP/devices.json" | while read -r guide; do
  upsert_guide "$guide"
done

# Pass 2: full guides.
jq -c '.guides[]' "$TMP/devices.json" | while read -r guide; do
  upsert_guide "$guide"
done

# Picks from staging snapshot.
jq -c '.picks[]' "$TMP/devices.json" | while read -r pick; do
  upsert_pick "$pick"
done

# Remove legacy picks on prod that are not in staging snapshot.
jq -r '.picks[].id' "$TMP/devices.json" | sort -u > "$TMP/staging_pick_ids.txt"
curl -sS -b "$TMP/cj" "${PROD_URL}/admin/api/smarthome/device-picks" -o "$TMP/prod_picks.json"
jq -r '.[].id' "$TMP/prod_picks.json" | while read -r pid; do
  if ! grep -qx "$pid" "$TMP/staging_pick_ids.txt"; then
    echo "delete orphan pick ${pid}"
    curl -sS -b "$TMP/cj" -X DELETE "${PROD_URL}/admin/api/smarthome/device-picks/${pid}" > /dev/null
  fi
done

guide_count=$(curl -sS "${PROD_URL}/v1/smarthome/devices" | jq '.guides | length')
pick_count=$(curl -sS "${PROD_URL}/v1/smarthome/devices" | jq '.picks | length')
staging_guide_count=$(jq '.guides | length' "$TMP/devices.json")
staging_pick_count=$(jq '.picks | length' "$TMP/devices.json")
echo "smarthome prod: ${guide_count} guides, ${pick_count} picks (staging: ${staging_guide_count}/${staging_pick_count})"
if [ "$guide_count" != "$staging_guide_count" ] || [ "$pick_count" != "$staging_pick_count" ]; then
  echo "smarthome count mismatch" >&2
  exit 1
fi

echo "== manifest =="
curl -sS "${PROD_URL}/v1/content/manifest"
echo ""
