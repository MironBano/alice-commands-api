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

### GET /v1/affiliate/blocks

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
| GET | `/admin/api/affiliate-blocks` | List |
| POST | `/admin/api/affiliate-blocks` | Create |
| PUT | `/admin/api/affiliate-blocks/{id}` | Update |
| DELETE | `/admin/api/affiliate-blocks/{id}` | Delete |
| GET | `/admin/api/preview/bundle` | Draft JSON (no gzip) |
| POST | `/admin/api/publish` | `{ "min_app_version"?, "notes"? }` → publish result |
| POST | `/admin/api/publish/rollback` | `{ "content_version": 41 }` |
| GET | `/admin/api/publish/history` | Last 5 publishes |
| POST | `/admin/api/import/json?mode=sync\|merge\|replace` | Upload seed JSON (raw body). **sync** (default в `push-draft.ps1`) — catalog + merge approved editorial |
| POST | `/admin/api/import/preview` | Diff incoming JSON vs **published** bundle |
| GET | `/admin/api/content/pipeline` | Live/draft stats, inventory/queue counts, seed, script paths |
| POST | `/admin/api/content/pipeline-sync` | Sync inventory + editorial + queue с локального `seed/data/*` |
| GET | `/admin/api/content/queue?status=open` | Очередь editorial (NEW / GONE / needs_review) |
| GET | `/admin/api/content/editorial-review?filter=review\|changed\|pending\|queue\|added\|removed\|all&search=` | Редактор: все изменения (published vs draft vs edit) |
| GET | `/admin/api/content/editorial-export?filter=&search=` | Скачать JSON для правки в ИИ (attachment) |
| POST | `/admin/api/content/editorial-import` | Загрузить JSON после ИИ (raw body, тот же формат что export) |
| POST | `/admin/api/content/editorial/batch` | Сохранить правки из UI: `{ "records": [{ command_id, title_ru, effect_description_ru, status }] }` |
| POST | `/admin/api/content/queue/{id}/approve` | Approve queue item → editorial approved |
| POST | `/admin/api/content/queue/{id}/dismiss` | Dismiss queue item |
| POST | `/admin/api/content/rebuild-draft` | Пересобрать draft из pipeline DB (после import sync) |
| GET | `/admin/api/content/draft-diff` | Diff **текущего draft** vs опубликованный bundle |
| POST | `/admin/api/content/import-seed?mode=sync\|merge\|replace` | Import из `CONTENT_SEED_PATH` на VPS |
| GET | `/admin/api/docs` | JSON API reference для admin UI |
| GET | `/admin/api/feedback?status=open&search=` | Inbox: отзывы из app |
| POST | `/admin/api/feedback/{id}/resolve` | Закрыть отзыв |
| POST | `/admin/api/feedback/{id}/dismiss` | Отклонить отзыв |
| GET | `/admin/api/command-reports?status=open&command_id=&search=` | Inbox: ошибки в командах |
| POST | `/admin/api/command-reports/{id}/resolve` | Закрыть report |
| POST | `/admin/api/command-reports/{id}/dismiss` | Отклонить report |

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
| 409 | publish_conflict |
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
