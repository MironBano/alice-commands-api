# AGENTS.md — alice-commands-api

## Проект

- **Название:** alice-commands-api
- **Тип:** Kotlin, Ktor 3, PostgreSQL, Gradle
- **Связанный app:** [AliceCommands](https://github.com/MironBano/AliceCommands) — **Full Clean** Android
- **idea_ref:** MOB-20260626-001

## Сборка и тесты

```bash
docker compose up -d
./gradlew :server:test
./gradlew :server:run
```

Windows: `.\gradlew.bat :server:run`

Content validate: `.\gradlew.bat :server:validateContent`  
Staging pipeline: `.\scripts\update-content.ps1` (см. `docs/CONTENT-UPDATE.md`)  
Staging deploy: `.\scripts\deploy-staging.ps1` (см. `docs/INFRASTRUCTURE.md`)  
DNS (РФ, без VPN): `.\scripts\cloudflare-dns-direct.ps1` · CDN icons: `.\scripts\setup-cdn.ps1`

## Документация

| Путь | Назначение |
| ---- | ---------- |
| `docs/BACKEND-REQUIREMENTS.md` | **Главное ТЗ** |
| `docs/BACKEND-COMMAND-GROUPS.md` | **Schema v2 — command groups** |
| `docs/BACKEND-COMMAND-OF-DAY.md` | **Команда дня** — editorial bundle field |
| `docs/BACKEND-CATEGORY-VISUALS.md` | **Иконки + colors — CDN, admin, validation** |
| `docs/ARCHITECTURE.md` | **Light Clean** — §1.1 |
| `docs/API.md` | Контракт для Android (+ delta, admin groups) |
| `docs/CONTENT-UPDATE.md` | Pipeline контента (scripts + Python) |
| `docs/INFRASTRUCTURE.md` | Selectel VPS, SSH, DNS, deploy |
| `schema/content-bundle.schema.json` | JSON Schema **v2** (канон) |

## Архитектура (обязательно)

**Стиль:** **Light Clean (hexagonal light)** — см. `docs/ARCHITECTURE.md` §1.1.

| MUST | MUST NOT |
| ---- | -------- |
| Publish / rollback / import → `application.publish.*UseCase` | Business logic в `routing { }` |
| Public `/v1/*` читает **published** bundle/manifest | Public routes → draft PostgreSQL |
| Ports: `BundleStorage`, `DraftRepository`, `SchemaValidator` | Inline validate→gzip→manifest в route |
| Exposed только в `infrastructure.persistence` | Ручное редактирование live bundle на диске |

**Admin CRUD:** routes → Exposed repository (без use case на каждый PUT) — допустимо.

## Продуктовые правила

1. Draft в PostgreSQL; runtime для app — immutable bundle + manifest.
2. Rollback — 5 последних bundle на диске.
3. `source_url` обязателен на command; publish только после human review.
4. Один admin user, пароль в `.env` (bcrypt hash).
5. Schema sync с app — см. `docs/SCHEMA-SYNC.md`.

## Android repo

Не дублировать content logic в app. App sync → Room → Full Clean use cases.
