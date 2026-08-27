# AGENTS.md — alice-commands-api

## Проект

- **Название:** alice-commands-api
- **Тип:** Kotlin, Ktor 3, PostgreSQL, Gradle
- **Связанный app:** [AliceCommands](https://github.com/MironBano/AliceCommands) — **Full Clean** Android
- **idea_ref:** MOB-20260626-001
- **Prod:** **LIVE** с 2026-07-13 — `https://api.alicecommands.ru` (RuStore release)

## PRODUCTION LIVE — правила для агентов

**Канон:** `docs/PRODUCTION.md` · Cursor rule: `.cursor/rules/production-live.mdc`

| MUST | MUST NOT |
| ---- | -------- |
| Контент/smarthome: staging → `verify-staging` → `copy-staging-to-prod` → `verify-prod` | Publish / import replace на prod без staging QA |
| Код: `:server:test` + staging deploy перед prod | Breaking changes в public `/v1/*` без плана |
| После prod-изменений: `verify-prod.ps1` | Destructive ops на prod DB/storage |
| Минимальный diff; не трогать unrelated | Пропускать verifier на задачах «готово» |

## Сборка и тесты

```bash
docker compose up -d
./gradlew :server:test
./gradlew :server:run
```

Windows: `.\gradlew.bat :server:run`

Content validate: `.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"`  
Smarthome validate: `.\gradlew.bat :server:validateSmartHomeDevices`  
Push draft (канон): `.\scripts\push-draft.ps1` — `catalog-audit-fixed.json` → staging draft (replace; блокирует unpublished без `-Force`)  
Pull draft: `.\scripts\pull-draft.ps1` — staging draft → `catalog-audit-fixed.json` (после точечных правок в админке)  
Staging deploy: `.\scripts\deploy-staging.ps1` (см. `docs/INFRASTRUCTURE.md`)  
Prod deploy: `.\scripts\deploy-prod.ps1` · verify: `.\scripts\verify-prod.ps1`  
**Prod sync (контент):** `.\scripts\copy-staging-to-prod.ps1` — после QA на staging  
DNS (РФ, без VPN): `.\scripts\cloudflare-dns-direct.ps1` · CDN icons: `.\scripts\setup-cdn.ps1`

## Документация

| Путь | Назначение |
| ---- | ---------- |
| `docs/BACKEND-REQUIREMENTS.md` | **Главное ТЗ** |
| `docs/BACKEND-COMMAND-GROUPS.md` | **Schema v2 — command groups** |
| `docs/BACKEND-COMMAND-OF-DAY.md` | **Команда дня** — editorial bundle field |
| `docs/BACKEND-CATEGORY-VISUALS.md` | **Иконки + colors — CDN, admin, validation** |
| `docs/BACKEND-SMARTHOME-DEVICES.md` | **Устройства** — guides, picks, contextual targeting |
| `docs/ANALYTICS-BACKEND.md` | **Analytics** — batch ingest, admin dashboard |
| `docs/ANALYTICS-GLOSSARY.md` | **Analytics glossary** — метрики, FAQ, event_name |
| `docs/ARCHITECTURE.md` | **Light Clean** — §1.1 |
| `docs/API.md` | Контракт для Android (+ delta, smarthome, analytics) |
| `docs/CONTENT-UPDATE.md` | Runbook контента (канон catalog-audit-fixed) |
| `docs/CATALOG-FIXED-BUILD.md` | **Канонический каталог** — `catalog-audit-fixed.json`, deploy без orphan |
| `docs/CONTENT-PRODUCT-ROADMAP.md` | **Карта улучшений** — визуал, онбординг, сценарии, devices |
| `docs/PRODUCTION.md` | **LIVE prod** — staging-first, откат, снимок |
| `docs/INFRASTRUCTURE.md` | Selectel VPS, SSH, DNS, staging + prod deploy |
| `docs/PROD-CUTOVER.md` | Cutover checklist (завершён 2026-07-13) |
| `schema/content-bundle.schema.json` | JSON Schema **v2** (канон) |
| `schema/smarthome-devices.schema.json` | Smarthome snapshot schema |

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
6. Smarthome snapshot — отдельный файл manifest; auto-publish при admin save guides/picks.

## Контентный JSON — только вручную

**Запрещено насмерть:** Python/Node/PowerShell для массовой правки `seed/*.json`, `content/**`, editorial/inventory bundle.  
**Только:** StrReplace / Write в IDE. Проверка: `validateContent` с `-PcontentFile`.  
Правило: `.cursor/rules/no-content-scripts.mdc` (alwaysApply).

## Android repo

Не дублировать content logic в app. App sync → Room → Full Clean use cases.
