#!/usr/bin/env bash
set -euo pipefail
set -a
source /opt/alice-api/.env
set +a
COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT

curl -sS -c "$COOKIE_JAR" -X POST "http://127.0.0.1:8080/admin/api/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}"

echo ""
curl -sS -b "$COOKIE_JAR" -X PUT "http://127.0.0.1:8080/admin/api/smarthome/device-guides/station" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "station",
    "title_ru": "Колонка с Алисой",
    "summary_ru": "Голосовой помощник и центр умного дома в одной колонке",
    "capabilities_ru": "Алиса отвечает на вопросы, включает музыку и подкасты, управляет умным домом голосом.",
    "setup_ru": "Подключите колонку к розетке и Wi‑Fi, откройте приложение «Дом с Алисой».",
    "setup_steps_ru": ["Включите колонку", "Подключите к Wi‑Fi", "Войдите в аккаунт Яндекса"],
    "related_devices_ru": "Смартфон с приложением «Дом с Алисой»",
    "related_device_ids": ["phone"],
    "command_device_filter_id": "station",
    "action_url": "https://alice.yandex.ru/support/ru/station/",
    "sort_order": 10
  }'
echo ""
