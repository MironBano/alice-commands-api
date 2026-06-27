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
.\gradlew.bat :server:validateContent -PcontentFile=../seed/import-smart-home.json
```

## Deploy staging

```powershell
.\scripts\deploy-staging.ps1
```

Сборка: `.\gradlew.bat :server:installDist` → `server/build/install/server/`

## Admin API (новое в v1)

| Endpoint | Назначение |
| -------- | ---------- |
| `GET /admin/api/content/pipeline` | Seed status + script hints |
| `POST /admin/api/content/import-seed` | Import from `CONTENT_SEED_PATH` |
| `GET /admin/api/docs` | API reference JSON |

Public: `GET /health`, `GET /ready`

## Config

`.env` в корне repo — см. `.env.example`. На VPS: `/opt/alice-api/.env`.

Optional: `CONTENT_SEED_PATH` — seed JSON на сервере для admin import-seed.

## Structure

[ARCHITECTURE.md](../docs/ARCHITECTURE.md) §1.1

| Package | Role |
| ------- | ---- |
| `routes/` | Ktor endpoints |
| `application/publish/` | Publish, rollback, import |
| `application/read/` | Manifest, bundle, diff |
| `infrastructure/security/` | Session, rate limit, ClientIpResolver |

## Docs

- [API.md](../docs/API.md)
- [INFRASTRUCTURE.md](../docs/INFRASTRUCTURE.md)
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md)
