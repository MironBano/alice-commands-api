# API — alice-commands-api

**Version:** v1 · **Base URL:** `{PUBLIC_BASE_URL}` (env: `PUBLIC_BASE_URL`)  
**Android:** `BuildConfig.CONTENT_API_BASE_URL` (staging / prod flavors)

---

## 1. Public endpoints

### GET /v1/content/manifest

**Response 200:**

```json
{
  "schema_version": 2,
  "content_version": 42,
  "published_at": "2026-06-26T12:00:00Z",
  "min_app_version": "1.0",
  "bundle_url": "https://api.example.ru/v1/content/bundle",
  "bundle_sha256": "a1b2c3...",
  "backup_url": "https://api.example.ru/v1/content/bundle-backup/content_v42.json.gz",
  "bundle_size_bytes": 185000
}
```

| Header | Значение |
| ------ | -------- |
| `ETag` | `"content-42"` |
| `Cache-Control` | `public, max-age=300` |

**304:** if `If-None-Match` matches.  
**404:** `{ "error": "not_found", "message": "No published content yet" }` — до первого Publish.

---

### GET /v1/content/bundle

Returns **gzip** body of full content bundle. Structure: [schema/content-bundle.schema.json](../schema/content-bundle.schema.json).

**Schema v2 additions:** `command_groups[]` and on each command: `group_id`, `sort_order`, `variant_label_ru`, `is_primary_in_group`, `search_aliases`. See [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md).

**Category / group visuals (optional, no schema_version bump):** on `categories[]` and `command_groups[]` — `icon_url`, `accent_color`, `accent_color_dark` (hex `#RRGGBB`); `icon_key` remains offline fallback. See [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md).

**Command of day (optional root field, no schema_version bump):** `command_of_day` — editorial policy + snapshot on publish date (`Europe/Moscow`). Modes: `manual` | `auto`. See [BACKEND-COMMAND-OF-DAY.md](BACKEND-COMMAND-OF-DAY.md).

Example category fragment (prod CDN; staging uses `staging-api.alicecommands.ru` host):

```json
{
  "id": "music",
  "title_ru": "Музыка",
  "sort_order": 3,
  "icon_key": "music_note",
  "icon_url": "https://cdn.alicecommands.ru/icons/v1/music_note.svg",
  "accent_color": "#7B4BB7",
  "accent_color_dark": "#C9A8F0",
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/"
}
```

| Header | Значение |
| ------ | -------- |
| `Content-Type` | `application/json` |
| `Content-Encoding` | `gzip` |
| `Cache-Control` | `public, max-age=86400, immutable` |
| `ETag` | `"content-42"` |

**304:** if `If-None-Match` matches.

**Client algorithm:**

1. GET manifest
2. If `content_version` > local → GET bundle
3. Verify sha256
4. Parse JSON → upsert Room
5. On network error → use Room / bundled seed

**Alternative (delta):** if app supports delta sync — `GET /v1/content/delta?from={localVersion}` before full bundle (see below).

---

### GET /v1/content/delta?from={version}

Incremental sync between published versions.

**Response 200:** JSON with `added` / `updated` (full objects) / `removed` (ids) per entity type: `categories`, `command_groups`, `commands`, `scenario_templates`, `checklist_items`. If `command_of_day` changed between versions — поле `command_of_day` с новым объектом (или `null`, если удалён из bundle).

| HTTP | Когда |
| ---- | ----- |
| 200 | Delta доступен |
| 409 `delta_unavailable` | Bundle `from` не в retention (5 версий) → use full bundle |
| 400 | Missing or invalid `from` |

---

### GET /v1/content/bundle-backup/{filename}

Direct download archived bundle by filename. Only `content_v{N}.json.gz` allowed (retention pool).

| Header | Значение |
| ------ | -------- |
| `Content-Encoding` | `gzip` |

`backup_url` в manifest указывает на текущую версию; endpoint также отдаёт предыдущие версии из retention.

---

### GET /icons/v1/{slug}.svg

Публичная отдача SVG категорий/групп. **Не** часть bundle — отдельный static endpoint.

| Среда | Base URL |
| ----- | -------- |
| Staging | `https://staging-api.alicecommands.ru/icons/v1/{slug}.svg` |
| Prod CDN | `https://cdn.alicecommands.ru/icons/v1/{slug}.svg` |

| Header | Значение |
| ------ | -------- |
| `Content-Type` | `image/svg+xml` |
| `Cache-Control` | `public, max-age=86400, immutable` |
| `Access-Control-Allow-Origin` | `*` |

**Реализация:** файлы на диске (`ICON_STORAGE_PATH/v1/`); nginx отдаёт `/icons/` напрямую на staging; отдельный vhost `cdn` на prod. Ktor `staticFiles("/icons", …)` — fallback. См. [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md) §4, [INFRASTRUCTURE.md](INFRASTRUCTURE.md) §2.

**404:** файл не найден. **NXDOMAIN на `cdn.*`:** нет DNS-записи — используйте staging URL или `setup-cdn.ps1`.

---

### GET /v1/smarthome/devices

Единый источник для вкладки «Устройства» в app: **guides** (типы устройств) + **picks** (подборки). Schema: [schema/smarthome-devices.schema.json](../schema/smarthome-devices.schema.json).

**Response 200:**

```json
{
  "schema_version": 1,
  "updated_at": "2026-07-06T12:00:00Z",
  "guides": [
    {
      "id": "station",
      "title_ru": "Колонка с Алисой",
      "summary_ru": "Голосовой помощник и центр умного дома",
      "capabilities_ru": "…полный текст для detail-screen…",
      "setup_ru": "…полный текст…",
      "setup_steps_ru": ["Шаг 1", "Шаг 2"],
      "related_devices_ru": "Смартфон для настройки",
      "related_device_ids": ["phone"],
      "command_device_filter_id": "station",
      "image_url": "https://staging-api.alicecommands.ru/devices/v1/station.webp",
      "action_url": "https://alice.yandex.ru/support/ru/station/",
      "sort_order": 10,
      "detail_referral_pick_ids": ["pick_station", "pick_hub"]
    }
  ],
  "picks": [
    {
      "id": "pick_smart_bulb",
      "title_ru": "Умная лампочка",
      "description_ru": "Для сценариев освещения",
      "price_hint_ru": "от 990 ₽",
      "image_url": "https://staging-api.alicecommands.ru/devices/v1/pick_smart_bulb.webp",
      "action_url": "https://market.yandex.ru/...",
      "sort_order": 10,
      "cta_ru": "Смотреть цену",
      "tags": ["smart_light"],
      "device_types": ["station"],
      "category_ids": ["smart_home"],
      "command_ids": ["sh_light_on"],
      "command_group_ids": ["sh_group_light"],
      "scenario_template_ids": ["S1"],
      "guide_ids": [],
      "placements": ["smart_home_devices", "command_detail", "scenario_detail"],
      "priority": 90
    }
  ]
}
```

| Поле guides (обяз.) | `id`, `title_ru`, `summary_ru`, `capabilities_ru`, `setup_ru`, `action_url`, `sort_order` |
| Guides (referral detail) | `detail_referral_pick_ids` — **computed at publish**: `pick_{guide.id}` + related picks с `device_guide_detail` |
| Поле picks (обяз.) | `id`, `title_ru`, `action_url`, `sort_order` |
| Picks (contextual, V10) | `placements`, `tags`, `device_types`, `category_ids`, `command_group_ids`, `command_ids`, `scenario_template_ids`, `guide_ids`, `priority`, `cta_ru`, `starts_at`, `ends_at`, `max_impressions_per_session` |
| URL policy | `action_url`: только `https://` и `market://` |
| Compliance | `erid`, `advertiser_name` — **опционально** (v1.0); если есть — app показывает строку маркировки |

| Header | Значение |
| ------ | -------- |
| `Cache-Control` | `public, max-age=300` |

**404** до первого publish guides/picks (admin CRUD auto-publishes snapshot).

**Картинки:** `GET /devices/v1/{slug}.webp` — WebP static (Ktor `staticFiles`). Upload: `POST /admin/api/smarthome/upload-image`.

Полный контракт: [BACKEND-SMARTHOME-DEVICES.md](BACKEND-SMARTHOME-DEVICES.md).

---

### GET /devices/v1/{slug}.webp

Публичная отдача WebP для guides/picks. Аналог `/icons/v1/` для device images.

| Среда | Base URL |
| ----- | -------- |
| Staging | `https://staging-api.alicecommands.ru/devices/v1/{slug}.webp` |
| Prod | `https://api.alicecommands.ru/devices/v1/{slug}.webp` |

| Header | Значение |
| ------ | -------- |
| `Content-Type` | `image/webp` |
| `Cache-Control` | `public, max-age=86400, immutable` |

**404:** файл не найден. Storage: `DEVICE_IMAGE_STORAGE_PATH/v1/`.

---

### GET /v1/affiliate/blocks

> **Deprecated** — используйте `/v1/smarthome/devices`. Ответ содержит заголовки `Deprecation: true`, `Sunset`, `Link: </v1/smarthome/devices>; rel="successor-version"`.

**Response 200:**

```json
{
  "schema_version": 1,
  "updated_at": "2026-06-26T12:00:00Z",
  "blocks": [
    {
      "id": "ud_starter",
      "context_category_id": "smart_home",
      "title_ru": "С чего начать умный дом",
      "erid": "XXXX",
      "advertiser_name": "ИП ...",
      "products": [
        {
          "title_ru": "Умная лампа",
          "market_url": "https://market.yandex.ru/...",
          "price_hint": "от 990 ₽"
        }
      ]
    }
  ]
}
```

**404** до первого publish с affiliate blocks.

---

### POST /v1/feedback

In-app общий отзыв. **Без auth.** Не принимает email/телефон (без ПДн).

**Request:**

```json
{
  "message": "Очень удобный каталог!",
  "rating": 5,
  "app_version": "1.0.0",
  "platform": "android",
  "locale": "ru-RU",
  "content_version": 42,
  "device_model": "Pixel 8"
}
```

| Field | Required | Notes |
| ----- | -------- | ----- |
| `message` | yes | 1–2000 chars |
| `rating` | no | 1–5 |
| `app_version`, `platform`, `locale`, `device_model` | no | max 128 chars |
| `content_version` | no | из manifest |

**Response 201:**

```json
{ "id": "uuid", "status": "open" }
```

**400** `validation_failed` · **429** `rate_limited` (см. `PUBLIC_SUBMISSION_RATE_LIMIT`).

---

### POST /v1/analytics/events/batch

Батч продуктовых событий из Android. **Без auth.** CamelCase DTO (как в app).

**Request:**

```json
{
  "events": [
    {
      "installId": "uuid",
      "sessionId": "uuid",
      "eventId": "uuid",
      "eventName": "screen_view",
      "occurredAt": 1710000000123,
      "appVersion": "1.2.0",
      "androidVersion": "14",
      "locale": "ru-RU",
      "userProperties": { "is_pro": "false" },
      "params": { "route": "home/catalog" }
    }
  ]
}
```

| Rule | Value |
| ---- | ----- |
| `events` | 1…50 |
| `eventId` | UUID, dedup globally |
| `eventName` | `[a-z0-9_]{1,64}` |
| `occurredAt` | Unix ms, не >5 мин в будущем, не старше 30 дней |
| `params` / `userProperties` | ≤32 keys, key ≤64, value ≤512 |
| Blocked param keys | **Exact** match (case-insensitive): `query`, `message`, `email`, `phone`, `text`, `search_query`. Разрешены метрики без текста: `query_length`, `message_length`, `results_count`. Substring-match не используется. |

**Response 202:**

```json
{ "accepted": 48, "duplicates": 2, "rejected": 0, "rejectedEventIds": [] }
```

При частичном reject список `rejectedEventIds` содержит UUID отклонённых событий (PII/schema). Клиент удаляет accepted/duplicates из outbox и poison-drop'ает rejected (без бесконечного retry).

**400** batch-level · **413** body > `ANALYTICS_MAX_BODY_BYTES` · **429** `rate_limited` (`ANALYTICS_RATE_LIMIT_PER_IP`, `ANALYTICS_EVENTS_PER_IP_PER_DAY`).

---

### POST /v1/commands/{command_id}/report

Сообщение об ошибке в конкретной команде. **Без auth.**

**Request:**

```json
{
  "issue_type": "wrong_effect",
  "message": "Описание не совпадает с реальностью",
  "content_version": 42,
  "category_id": "smart_home",
  "command_title": "Включить свет",
  "phrase_used": "Алиса, включи свет",
  "app_version": "1.0.0",
  "platform": "android",
  "locale": "ru-RU"
}
```

| `issue_type` | Смысл |
| ------------ | ----- |
| `wrong_effect` | Неверное описание эффекта |
| `outdated` | Команда устарела |
| `phrase_not_working` | Фраза не срабатывает |
| `requires_plus_wrong` | Неверно указан Plus |
| `wrong_device` | Неверное устройство |
| `other` | Другое |

**Response 201:**

```json
{ "id": "uuid", "status": "open", "command_exists_current": true }
```

**404** если `content_version` совпадает с текущим published и `command_id` не найден в live bundle.
Если версия клиента **старее** live — report принимается, `command_exists_current` может быть `false`.

Подробнее для Android: [APP-FEEDBACK-INTEGRATION.md](APP-FEEDBACK-INTEGRATION.md).

---

### GET /health

```json
{ "status": "ok" }
```

Always **200** (process alive).

### GET /ready

```json
{ "status": "ready", "database": "ok", "storage": "ok" }
```

**503** if database or bundle storage unavailable (`status: "not_ready"`).

---

## 2. Admin API (session cookie)

Cookie: `alice_admin_session` (HttpOnly, Secure on staging/prod, SameSite=Lax).

All require authenticated session unless noted.

| Method | Path | Description |
| ------ | ---- | ----------- |
| POST | `/admin/api/login` | `{ "username", "password" }` → `{ "ok": true }` |
| POST | `/admin/api/logout` | Invalidate session |
| GET | `/admin/api/dashboard` | Live manifest + draft stats + `hasUnpublishedChanges` + `inbox` counts |
| GET | `/admin/api/categories` | List draft categories |
| POST | `/admin/api/categories` | Create |
| PUT | `/admin/api/categories/reorder` | `{ "ordered_ids": [...] }` |
| PUT | `/admin/api/categories/{id}` | Update |
| DELETE | `/admin/api/categories/{id}` | Delete |
| GET | `/admin/api/command-groups?category_id=` | List command groups |
| POST | `/admin/api/command-groups` | Create group |
| PUT | `/admin/api/command-groups/reorder` | `{ "ordered_ids": [...] }` |
| PUT | `/admin/api/command-groups/{id}` | Update group |
| DELETE | `/admin/api/command-groups/{id}` | Delete group |
| PUT | `/admin/api/commands/bulk-assign-group` | `{ "command_ids": [...], "group_id": "..." }` |
| GET | `/admin/api/content/validation-warnings` | Orphan commands / empty groups / visual warnings |
| GET | `/admin/api/icons/catalog` | Icon catalog + accent presets + `public_base_url` |
| POST | `/admin/api/icons/upload` | `{ "slug", "svg" }` → `{ "slug", "icon_url", "icon_key" }` |
| GET | `/admin/api/commands?category_id=` | List (optional filter) |
| POST | `/admin/api/commands` | Create |
| PUT | `/admin/api/commands/{id}` | Update |
| DELETE | `/admin/api/commands/{id}` | Delete |
| GET | `/admin/api/scenario-templates` | List |
| POST | `/admin/api/scenario-templates` | Create |
| PUT | `/admin/api/scenario-templates/{id}` | Update |
| DELETE | `/admin/api/scenario-templates/{id}` | Delete |
| GET | `/admin/api/checklist-items` | List |
| PUT | `/admin/api/checklist-items` | Replace/reorder batch (array body) |
| GET | `/admin/api/command-of-day` | Settings + preview «сегодня» |
| PUT | `/admin/api/command-of-day` | `{ "mode", "command_id"?, "auto_category_id"?, "auto_seed"? }` → draft only |
| POST | `/admin/api/command-of-day/publish` | Publish только `command_of_day` в live bundle (без полного publish) |
| GET | `/admin/api/smarthome/device-guides` | List device guides |
| POST | `/admin/api/smarthome/device-guides` | Create guide → auto-publish smarthome snapshot |
| PUT | `/admin/api/smarthome/device-guides/{id}` | Update guide |
| DELETE | `/admin/api/smarthome/device-guides/{id}` | Delete guide |
| GET | `/admin/api/smarthome/device-picks` | List device picks |
| POST | `/admin/api/smarthome/device-picks` | Create pick |
| PUT | `/admin/api/smarthome/device-picks/{id}` | Update pick (contextual fields V10) |
| DELETE | `/admin/api/smarthome/device-picks/{id}` | Delete pick |
| POST | `/admin/api/smarthome/upload-image` | `{ "slug", "image_base64", "content_type"? }` → WebP URL |
| GET | `/admin/api/affiliate-blocks` | List (legacy) |
| POST | `/admin/api/affiliate-blocks` | Create |
| PUT | `/admin/api/affiliate-blocks/{id}` | Update |
| DELETE | `/admin/api/affiliate-blocks/{id}` | Delete |
| GET | `/admin/api/preview/bundle` | Draft JSON (no gzip) |
| POST | `/admin/api/publish` | `{ "min_app_version"?, "notes"? }` → publish result |
| POST | `/admin/api/publish/rollback` | `{ "content_version": 41 }` |
| GET | `/admin/api/publish/history` | Last 5 publishes |
| POST | `/admin/api/import/json?mode=merge\|replace` | Upload seed JSON (raw body). **replace** — канон (`push-draft.ps1`); merge — точечное обновление id |
| POST | `/admin/api/import/preview` | Diff incoming JSON vs **published** bundle |
| GET | `/admin/api/content/pipeline` | Live/draft stats, seed path, script hints (`pipeline` всегда `null`) |
| GET | `/admin/api/content/validation-warnings` | Warnings draft: `orphan_commands`, `empty_groups`, duplicate aliases |
| GET | `/admin/api/content/draft-diff` | Diff **текущего draft** vs опубликованный bundle |
| POST | `/admin/api/content/import-seed?mode=replace` | Import из `CONTENT_SEED_PATH` на VPS (replace) |
| GET | `/admin/api/docs` | JSON API reference для admin UI |
| GET | `/admin/api/feedback?status=open&search=` | Inbox: отзывы из app |
| POST | `/admin/api/feedback/{id}/resolve` | Закрыть отзыв |
| POST | `/admin/api/feedback/{id}/dismiss` | Отклонить отзыв |
| GET | `/admin/api/command-reports?status=open&command_id=&search=` | Inbox: ошибки в командах |
| POST | `/admin/api/command-reports/{id}/resolve` | Закрыть report |
| POST | `/admin/api/command-reports/{id}/dismiss` | Отклонить report |
| GET | `/admin/api/analytics/summary?from=YYYY-MM-DD&to=YYYY-MM-DD` | KPI: `daily_active_installs`, `avg_dau`, `total_events`, `unique_installs` / `raw_unique_installs`, `new_installs` (=Σ daily), top events, `daily[]` (Europe/Moscow; ≤ retention) |
| GET | `/admin/api/analytics/events?from=&to=&event_name=&install_id=&limit=100&offset=0` | Raw events explorer |
| GET | `/admin/api/analytics/funnel?from=&to=&steps=paywall_view,pro_purchase_start,pro_activated` | Funnel by distinct `install_id` |
| GET | `/admin/api/analytics/breakdown?from=&to=&event_name=ui_click&param=element_id&field_source=params` | Top values of `params[param]` or `user_properties[param]` when `field_source=user_properties` |

**Login rate limit:** `ADMIN_LOGIN_RATE_LIMIT` failures per IP per 15 min → **429** `rate_limited`. IP берётся из `X-Forwarded-For` / `X-Real-IP` (nginx).

**Icon catalog response** (`GET /admin/api/icons/catalog`):

```json
{
  "public_base_url": "https://staging-api.alicecommands.ru",
  "icons": [
    { "slug": "music_note", "label_ru": "Музыка", "url": "https://staging-api.alicecommands.ru/icons/v1/music_note.svg" }
  ],
  "accent_presets": [
    { "name": "violet", "light": "#7B4BB7", "dark": "#C9A8F0" }
  ]
}
```

URL в `icons[]` всегда строятся сервером из `ICON_PUBLIC_BASE_URL`, не из захардкоженного JSON.

**Publish validation:** `icon_url` host ∈ `ICON_URL_ALLOWED_HOSTS`; path `/icons/v1/{slug}.svg`; hex colors `#RRGGBB`.

**Public submission rate limit:** `PUBLIC_SUBMISSION_RATE_LIMIT` submissions per IP per 15 min → **429** на `/v1/feedback` и `/v1/commands/*/report`.

**Analytics rate limits:** `ANALYTICS_RATE_LIMIT_PER_IP` (default 120/15 min), `ANALYTICS_EVENTS_PER_IP_PER_DAY` (default 10000), `ANALYTICS_MAX_BODY_BYTES` (default 262144).

**Staging base URL:** `https://staging-api.alicecommands.ru`

---

## 2.1 Admin UI (browser)

| URL | Назначение |
| --- | ---------- |
| `/admin` | SPA login + dashboard |
| `/admin/js/admin.js` | Client (health polling, CRUD) |

In-app API docs: view **API** в sidebar или `GET /admin/api/docs`.

---

## 3. Error format

```json
{
  "error": "validation_failed",
  "message": "command.sh_light_on: source_url required",
  "details": []
}
```

| HTTP | Code |
| ---- | ---- |
| 400 | validation_failed |
| 401 | unauthorized |
| 404 | not_found |
| 409 | publish_conflict, delta_unavailable |
| 413 | payload_too_large (analytics batch body) |
| 429 | rate_limited |
| 500 | internal_error |

---

## 4. Android integration

| Build flavor | `CONTENT_API_BASE_URL` |
| ------------ | ---------------------- |
| debug / staging | `https://staging-api.alicecommands.ru` |
| release | `https://api.alicecommands.ru` (prod, DNS only — см. [INFRASTRUCTURE.md](INFRASTRUCTURE.md)) |

Manifest path: `{base}/v1/content/manifest`

**РФ:** API **не** проксировать через Cloudflare CDN (оранжевое облако) — ISP throttle. DNS only → VPS Selectel.

Certificate pinning: optional NFR (release).

---

*Совместимо с AliceCommands [CONTENT-PIPELINE.md](https://github.com/MironBano/AliceCommands/blob/main/docs/CONTENT-PIPELINE.md) §4*
