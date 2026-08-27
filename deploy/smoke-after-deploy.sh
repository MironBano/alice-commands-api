#!/bin/bash
# Post-deploy smoke on VPS after staging deploy (shared jar restart).
# Mutates only staging (:8080). Prod (:8081) — health check only.
set -euo pipefail
curl -sS -f http://127.0.0.1:8080/health >/dev/null
curl -sS -f http://127.0.0.1:8081/health >/dev/null
TS=$(($(date +%s) * 1000))
EVENT_ID="$(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')"
cat >/tmp/alice-analytics-smoke.json <<EOF
{"events":[{"installId":"11111111-1111-4111-8111-111111111111","sessionId":"22222222-2222-4222-8222-222222222222","eventId":"${EVENT_ID}","eventName":"screen_view","occurredAt":${TS}}]}
EOF
acode=$(curl -sS -o /tmp/alice-analytics-smoke.out -w '%{http_code}' \
  -X POST http://127.0.0.1:8080/v1/analytics/events/batch \
  -H 'Content-Type: application/json' \
  --data-binary @/tmp/alice-analytics-smoke.json)
echo "analytics:${acode}"
test "${acode}" = "202"
fcode=$(curl -sS -o /tmp/alice-staging-feedback.json -w '%{http_code}' \
  -X POST http://127.0.0.1:8080/v1/feedback \
  -H 'Content-Type: application/json' \
  -d '{"message":"staging deploy smoke","app_version":"deploy","platform":"script"}')
echo "feedback:${fcode}"
test "${fcode}" = "201"
curl -sS -o /dev/null -w 'api_icon:%{http_code}\n' http://127.0.0.1:8080/icons/v1/child.svg
curl -sS -o /dev/null -w 'cdn_icon:%{http_code}\n' https://cdn.alicecommands.ru/icons/v1/child.svg || true
echo smoke_ok
