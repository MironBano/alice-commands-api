# Ktor server

Модуль `:server` — HTTP API, publish pipeline, admin backend.

| Среда | URL |
| ----- | --- |
| Staging | https://staging-api.alicecommands.ru |
| Prod | https://api.alicecommands.ru |

## Запуск (local)

```powershell
docker compose up -d
Copy-Item ..\.env.example ..\.env
.\gradlew.bat :server:run
```

Admin: http://localhost:8080/admin

## Тесты

```powershell
.\gradlew.bat :server:test
```

Testcontainers (PostgreSQL) — нужен Docker; без Docker integration skipped.

## Validate content

```powershell
.\gradlew.bat :server:validateContent
.\gradlew.bat :server:validateContent -PcontentFile=../seed/smart-home-groups-v2.json
.\gradlew.bat :server:validateContent -PcontentFile=../seed/catalog-audit-fixed.json
.\gradlew.bat :server:validateSmartHomeDevices
.\gradlew.bat :server:validateSmartHomeDevices -PcontentFile=../seed/smarthome-devices-example.json
```

## Deploy

```powershell
.\scripts\deploy-staging.ps1   # staging :8080
.\scripts\deploy-prod.ps1      # prod :8081
```

Сборка: `.\gradlew.bat :server:installDist` → `server/build/install/server/`

## Public API

| Endpoint | Назначение |
| -------- | ---------- |
| `GET /v1/content/manifest` | Версия, sha256, min_app_version |
| `GET /v1/content/bundle` | gzip JSON bundle (schema v2) |
| `GET /v1/content/delta?from={version}` | Incremental diff vs current published |
| `GET /icons/v1/{slug}.svg` | Category/group SVG (public static) |
| `GET /devices/v1/{slug}.webp` | Smarthome device images |
| `GET /v1/smarthome/devices` | Guides + picks snapshot |
| `GET /v1/affiliate/blocks` | Affiliate blocks (**deprecated**) |
| `POST /v1/feedback` | In-app feedback (rate limited) |
| `POST /v1/analytics/events/batch` | Analytics batch ingest (rate limited, dedup by eventId) |
| `POST /v1/commands/{command_id}/report` | Command issue report (rate limited) |
| `GET /health`, `GET /ready` | Ops |

## Admin API (основное)

| Endpoint | Назначение |
| -------- | ---------- |
| `GET/POST/PUT/DELETE /admin/api/command-groups` | CRUD групп |
| `PUT /admin/api/command-groups/reorder` | Порядок групп |
| `POST /admin/api/commands/bulk-assign-group` | Массовое назначение group_id |
| `GET/PUT /admin/api/command-of-day` | Command of day settings |
| `POST /admin/api/command-of-day/publish` | Publish COD only |
| `GET/POST/PUT/DELETE /admin/api/smarthome/device-guides` | Device guides CRUD |
| `GET/POST/PUT/DELETE /admin/api/smarthome/device-picks` | Device picks CRUD |
| `POST /admin/api/smarthome/upload-image` | Upload WebP |
| `GET /admin/api/content/validation-warnings` | Orphan commands, empty groups, visual warnings |
| `GET /admin/api/icons/catalog` | Icon slugs + accent presets + `public_base_url` |
| `POST /admin/api/icons/upload` | Upload validated SVG |
| `GET /admin/api/content/pipeline` | Live/draft stats, seed, scripts |
| `GET /admin/api/content/draft-diff` | Diff draft vs published |
| `POST /admin/api/content/import-seed` | Import from `CONTENT_SEED_PATH` |
| `POST /admin/api/publish` | Publish draft → live |
| `GET /admin/api/docs` | API reference JSON |
| `GET /admin/api/analytics/summary` | Analytics dashboard KPI + daily series |
| `GET /admin/api/analytics/events` | Analytics raw events list |
| `GET /admin/api/analytics/funnel` | Funnel by distinct install_id |
| `GET /admin/api/analytics/breakdown` | Top values of event params |

Полный контракт: [API.md](../docs/API.md).

## Config

`.env` в корне repo — см. `.env.example`. На VPS: `/opt/alice-api/.env` (staging), `/opt/alice-api/.env.prod` (prod).

Optional: `CONTENT_SEED_PATH` — seed JSON на сервере для admin import-seed.

Analytics ingest (см. [ANALYTICS-BACKEND.md](../docs/ANALYTICS-BACKEND.md)):

| Env | Default | Назначение |
| --- | ------- | ---------- |
| `ANALYTICS_RATE_LIMIT_PER_IP` | 120 | Запросов batch / IP / 15 мин |
| `ANALYTICS_EVENTS_PER_IP_PER_DAY` | 10000 | Soft cap событий / IP / сутки |
| `ANALYTICS_MAX_BODY_BYTES` | 262144 | Макс. размер тела batch |
| `ANALYTICS_RAW_RETENTION_DAYS` | 90 | Retention raw events (P1 job) |

Icons: `ICON_STORAGE_PATH`, `ICON_PUBLIC_BASE_URL`, `ICON_URL_ALLOWED_HOSTS` — [BACKEND-CATEGORY-VISUALS.md](../docs/BACKEND-CATEGORY-VISUALS.md).

Device images: `DEVICE_IMAGE_STORAGE_PATH` — [BACKEND-SMARTHOME-DEVICES.md](../docs/BACKEND-SMARTHOME-DEVICES.md).

## Structure

[ARCHITECTURE.md](../docs/ARCHITECTURE.md) §1.1

| Package | Role |
| ------- | ---- |
| `routes/` | Ktor endpoints |
| `application/publish/` | Publish, rollback, import, validation use cases |
| `application/read/` | Manifest, bundle, diff, ContentDeltaService |
| `application/analytics/` | Analytics batch ingest + admin queries |
| `infrastructure/security/` | Session, rate limit, ClientIpResolver |

Migrations: Flyway **V1–V10**. См. [DATABASE.md](../docs/DATABASE.md).

## Docs

- [API.md](../docs/API.md)
- [BACKEND-COMMAND-GROUPS.md](../docs/BACKEND-COMMAND-GROUPS.md)
- [BACKEND-CATEGORY-VISUALS.md](../docs/BACKEND-CATEGORY-VISUALS.md)
- [BACKEND-SMARTHOME-DEVICES.md](../docs/BACKEND-SMARTHOME-DEVICES.md)
- [ANALYTICS-BACKEND.md](../docs/ANALYTICS-BACKEND.md)
- [INFRASTRUCTURE.md](../docs/INFRASTRUCTURE.md)
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md)
