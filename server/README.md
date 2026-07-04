# Ktor server

Модуль `:server` — HTTP API, publish pipeline, admin backend.

**Staging:** https://staging-api.alicecommands.ru

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
.\gradlew.bat :server:validateContent -PcontentFile=../seed/full-catalog.json
```

## Deploy staging

```powershell
.\scripts\deploy-staging.ps1
```

Сборка: `.\gradlew.bat :server:installDist` → `server/build/install/server/`

## Public API

| Endpoint | Назначение |
| -------- | ---------- |
| `GET /v1/content/manifest` | Версия, sha256, min_app_version |
| `GET /v1/content/bundle` | gzip JSON bundle (schema v2) |
| `GET /v1/content/delta?from={version}` | Incremental diff vs current published |
| `GET /icons/v1/{slug}.svg` | Category/group SVG (public static) |
| `GET /v1/affiliate/blocks` | Affiliate blocks snapshot |
| `POST /v1/feedback` | In-app feedback (rate limited) |
| `POST /v1/commands/report` | Command issue report (rate limited) |
| `GET /health`, `GET /ready` | Ops |

## Admin API (основное)

| Endpoint | Назначение |
| -------- | ---------- |
| `GET/POST/PUT/DELETE /admin/api/command-groups` | CRUD групп |
| `PUT /admin/api/command-groups/reorder` | Порядок групп |
| `POST /admin/api/commands/bulk-assign-group` | Массовое назначение group_id |
| `GET /admin/api/content/validation-warnings` | Orphan commands, empty groups, visual warnings |
| `GET /admin/api/icons/catalog` | Icon slugs + accent presets + `public_base_url` |
| `POST /admin/api/icons/upload` | Upload validated SVG |
| `GET /admin/api/content/pipeline` | Live/draft stats, seed, scripts |
| `GET /admin/api/content/draft-diff` | Diff draft vs published |
| `POST /admin/api/content/import-seed` | Import from `CONTENT_SEED_PATH` |
| `POST /admin/api/publish` | Publish draft → live |
| `GET /admin/api/docs` | API reference JSON |

Полный контракт: [API.md](../docs/API.md).

## Config

`.env` в корне repo — см. `.env.example`. На VPS: `/opt/alice-api/.env`.

Optional: `CONTENT_SEED_PATH` — seed JSON на сервере для admin import-seed.

## Structure

[ARCHITECTURE.md](../docs/ARCHITECTURE.md) §1.1

| Package | Role |
| ------- | ---- |
| `routes/` | Ktor endpoints |
| `application/publish/` | Publish, rollback, import, **CommandGroupValidationUseCase** |
| `application/read/` | Manifest, bundle, diff, **ContentDeltaService** |
| `infrastructure/security/` | Session, rate limit, ClientIpResolver |

Migrations: Flyway **V1–V5** (command_groups — V4, category visuals — V5). См. [DATABASE.md](../docs/DATABASE.md).

Env (icons): `ICON_STORAGE_PATH`, `ICON_PUBLIC_BASE_URL`, `ICON_URL_ALLOWED_HOSTS` — см. [BACKEND-CATEGORY-VISUALS.md](../docs/BACKEND-CATEGORY-VISUALS.md).

## Docs

- [API.md](../docs/API.md)
- [BACKEND-COMMAND-GROUPS.md](../docs/BACKEND-COMMAND-GROUPS.md)
- [BACKEND-CATEGORY-VISUALS.md](../docs/BACKEND-CATEGORY-VISUALS.md)
- [INFRASTRUCTURE.md](../docs/INFRASTRUCTURE.md)
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md)
