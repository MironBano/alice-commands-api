# API — alice-commands-api

**Version:** v1 · **Base URL:** `{PUBLIC_BASE_URL}`  
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
  "backup_url": "https://cdn.example.ru/content/content_v42.json.gz",
  "bundle_size_bytes": 185000
}
```

| Header | Значение |
| ------ | -------- |
| `ETag` | `"content-42"` |
| `Cache-Control` | `public, max-age=300` |

**304:** if `If-None-Match` matches.

---

### GET /v1/content/bundle

Returns **gzip** body of full content bundle. Structure: [schema/content-bundle.schema.json](../schema/content-bundle.schema.json).

| Header | Значение |
| ------ | -------- |
| `Content-Type` | `application/json` |
| `Content-Encoding` | `gzip` |
| `Cache-Control` | `public, max-age=86400, immutable` |
| `ETag` | `"content-42"` |

**Client algorithm:**

1. GET manifest
2. If `content_version` > local → GET bundle
3. Verify sha256
4. Parse JSON → upsert Room
5. On network error → use Room / bundled seed

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

---

### GET /health

```json
{ "status": "ok" }
```

### GET /ready

```json
{ "status": "ready", "database": "ok", "storage": "ok" }
```

---

## 2. Admin API (session cookie)

All require authenticated admin session unless noted.

| Method | Path | Description |
| ------ | ---- | ----------- |
| POST | `/admin/api/login` | `{ username, password }` |
| POST | `/admin/api/logout` | |
| GET | `/admin/api/categories` | List draft categories |
| POST | `/admin/api/categories` | Create |
| PUT | `/admin/api/categories/{id}` | Update |
| DELETE | `/admin/api/categories/{id}` | Delete |
| GET | `/admin/api/commands?category_id=` | List |
| POST | `/admin/api/commands` | Create |
| PUT | `/admin/api/commands/{id}` | Update |
| DELETE | `/admin/api/commands/{id}` | Delete |
| GET | `/admin/api/scenario-templates` | |
| PUT | `/admin/api/scenario-templates/{id}` | |
| GET | `/admin/api/checklist-items` | |
| PUT | `/admin/api/checklist-items` | Reorder batch |
| GET | `/admin/api/affiliate-blocks` | |
| PUT | `/admin/api/affiliate-blocks/{id}` | |
| GET | `/admin/api/preview/bundle` | Draft JSON (no gzip) |
| POST | `/admin/api/publish` | Publish live |
| POST | `/admin/api/publish/rollback` | `{ "content_version": 41 }` |
| GET | `/admin/api/publish/history` | Last 5 publishes |
| POST | `/admin/api/import/json` | Upload seed file |

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
| debug | `https://staging-api.<domain>` or local |
| release | `https://api.<domain>` |

Manifest path: `{base}/v1/content/manifest`  
Certificate pinning: optional NFR (release).

---

## 5. v1.0.1 (planned)

`GET /v1/content/delta?from={version}` — JSON patch list or partial bundle.

---

*Совместимо с AliceCommands [CONTENT-PIPELINE.md](https://github.com/MironBano/AliceCommands/blob/main/docs/CONTENT-PIPELINE.md) §4*
