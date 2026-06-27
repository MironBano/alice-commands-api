# API — alice-commands-api

**Version:** v1 · **Base URL:** `{PUBLIC_BASE_URL}` (env: `PUBLIC_BASE_URL`)  
**Android:** `BuildConfig.CONTENT_API_BASE_URL` (staging / prod flavors)

---

## 1. Public endpoints

### GET /v1/content/manifest

**Response 200:**

```json
{
  "schema_version": 1,
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

---

### GET /v1/content/bundle-backup/{filename}

Direct download archived bundle by filename. Only `content_v{N}.json.gz` allowed (retention pool).

| Header | Значение |
| ------ | -------- |
| `Content-Encoding` | `gzip` |

`backup_url` в manifest указывает на текущую версию; endpoint также отдаёт предыдущие версии из retention.

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
| GET | `/admin/api/dashboard` | Live manifest + draft stats + `hasUnpublishedChanges` |
| GET | `/admin/api/categories` | List draft categories |
| POST | `/admin/api/categories` | Create |
| PUT | `/admin/api/categories/reorder` | `{ "ordered_ids": [...] }` |
| PUT | `/admin/api/categories/{id}` | Update |
| DELETE | `/admin/api/categories/{id}` | Delete |
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
| GET | `/admin/api/affiliate-blocks` | List |
| POST | `/admin/api/affiliate-blocks` | Create |
| PUT | `/admin/api/affiliate-blocks/{id}` | Update |
| DELETE | `/admin/api/affiliate-blocks/{id}` | Delete |
| GET | `/admin/api/preview/bundle` | Draft JSON (no gzip) |
| POST | `/admin/api/publish` | `{ "min_app_version"?, "notes"? }` → publish result |
| POST | `/admin/api/publish/rollback` | `{ "content_version": 41 }` |
| GET | `/admin/api/publish/history` | Last 5 publishes |
| POST | `/admin/api/import/json?mode=merge\|replace` | Upload seed JSON (raw body) |
| POST | `/admin/api/import/preview` | Diff incoming JSON vs **published** bundle |
| GET | `/admin/api/content/pipeline` | Seed на сервере + подсказки scripts |
| POST | `/admin/api/content/import-seed?mode=merge\|replace` | Import из `CONTENT_SEED_PATH` на VPS |
| GET | `/admin/api/docs` | JSON API reference для admin UI |

**Login rate limit:** `ADMIN_LOGIN_RATE_LIMIT` failures per IP per 15 min → **429** `rate_limited`. IP берётся из `X-Forwarded-For` / `X-Real-IP` (nginx).

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

## 5. v1.0.1 (planned)

`GET /v1/content/delta?from={version}` — JSON patch list or partial bundle.

---

*Совместимо с AliceCommands [CONTENT-PIPELINE.md](https://github.com/MironBano/AliceCommands/blob/main/docs/CONTENT-PIPELINE.md) §4*
