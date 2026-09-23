#!/bin/bash
# Send one test push via RuStore HTTP API (same payload shape as HttpRuStorePushSender).
# Usage on VPS:
#   source /opt/alice-api/.env.prod
#   ./send-test-push.sh <device_rustore_token>
set -euo pipefail
TOKEN="${1:-}"
if [ -z "$TOKEN" ]; then
  echo "Usage: $0 <device_rustore_token>" >&2
  exit 1
fi
PROJECT_ID="${RUSTORE_PUSH_PROJECT_ID:?RUSTORE_PUSH_PROJECT_ID missing}"
SERVICE_TOKEN="${RUSTORE_PUSH_SERVICE_TOKEN:?RUSTORE_PUSH_SERVICE_TOKEN missing}"
DEEPLINK="${2:-alicecommands://route/home/catalog?source=push}"
TITLE="${3:-Prod QA push}"
BODY="${4:-Тест доставки push}"
CHANNEL_ID="${5:-}"
CHANNEL_ID="${CHANNEL_ID:-push_content}"

payload=$(cat <<EOF
{
  "message": {
    "token": "$TOKEN",
    "notification": { "title": "$TITLE", "body": "$BODY" },
    "data": {
      "scenario": "qa",
      "push_scenario": "qa",
      "deeplink": "$DEEPLINK"
    },
    "android": {
      "notification": {
        "title": "$TITLE",
        "body": "$BODY",
        "channel_id": "$CHANNEL_ID",
        "click_action": "$DEEPLINK",
        "click_action_type": 1
      }
    }
  }
}
EOF
)

http_code=$(curl -sS -o /tmp/rustore-push-response.txt -w "%{http_code}" \
  -X POST "https://vkpns.rustore.ru/v1/projects/${PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${SERVICE_TOKEN}" \
  -H "Content-Type: application/json" \
  --data-binary "$payload")

echo "HTTP $http_code"
cat /tmp/rustore-push-response.txt
echo
if [ "$http_code" -lt 200 ] || [ "$http_code" -ge 300 ]; then
  exit 1
fi
