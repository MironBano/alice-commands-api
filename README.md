# alice-commands-api

Backend для [AliceCommands](https://github.com/MironBano/AliceCommands) — справочник голосовых команд Яндекс Алисы.

**GitHub:** https://github.com/MironBano/alice-commands-api

> **PRODUCTION LIVE (2026-07-13):** приложение в RuStore читает **https://api.alicecommands.ru**. Контент и деплой — только **staging → verify → prod**. Канон: **[docs/PRODUCTION.md](docs/PRODUCTION.md)**.

## Назначение

- **Public API** — manifest + content bundle + **delta sync** + **smarthome devices** + **analytics** для Android app
- **Admin** — веб-редактор каталога (категории, оформление, группы команд, команды, устройства, аналитика)
- **Publish pipeline** — PostgreSQL (draft) → immutable `content_vN.json.gz` + manifest + smarthome snapshot

## Быстрый старт (local)

```powershell
Copy-Item .env.example .env
# Заполните SESSION_SECRET (≥32 символов)
docker compose up -d
.\gradlew.bat :server:test
.\gradlew.bat :server:run
```

Admin UI: http://localhost:8080/admin

Первый publish: Admin → Import → `seed/smart-home-groups-v2.json` (Sync) → review **Группы команд** → Publish. См. [docs/RUNBOOK-PUBLISH.md](docs/RUNBOOK-PUBLISH.md) §10.

Gradle скачивает JDK 21 автоматически (Foojay toolchain). При необходимости укажите `org.gradle.java.home` в `gradle-local.properties` (см. `gradle-local.properties.example`).

## Структура репозитория

```
alice-commands-api/
├── server/              # Ktor API (public + admin + publish use cases)
├── admin-web/           # Static admin UI (Alpine.js)
├── schema/              # JSON Schema — content bundle v2 + smarthome devices
├── content/             # icon_catalog.json, pilot SVG (icons/v1/)
├── seed/                # catalog-audit-fixed.json (канон), archive/ (бэкапы), smart-home-groups-v2.json (pilot)
├── scripts/             # PowerShell: push-draft, deploy-staging/prod, verify
├── deploy/              # systemd, nginx, prod/staging env examples
├── docs/                # ТЗ, API, архитектура, runbooks
└── .github/workflows/   # CI validate-content
```

## Команды разработчика

| Команда | Назначение |
| ------- | ---------- |
| `.\gradlew.bat :server:run` | Запуск API на `:8080` |
| `.\gradlew.bat :server:test` | Unit + integration (Testcontainers, нужен Docker) |
| `.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"` | JSON Schema check канона (**885** команд) |
| `.\gradlew.bat :server:validateSmartHomeDevices` | Smarthome schema check |
| `.\scripts\push-draft.ps1` | **Файл** → staging draft (replace; без `-Force` не затирает unpublished) |
| `.\scripts\pull-draft.ps1` | Staging draft → **файл** `catalog-audit-fixed.json` |
| `.\scripts\deploy-staging.ps1` | Деплой staging на Selectel VPS (:8080) |
| `.\scripts\deploy-prod.ps1` | Деплой prod instance (:8081) |
| `.\scripts\verify-staging.ps1` / `verify-prod.ps1` | Smoke checks |
| `.\scripts\cloudflare-dns-direct.ps1` | DNS only для API и CDN (без CF proxy, РФ) |
| `.\scripts\setup-cdn.ps1` | Поднять `cdn.alicecommands.ru` |
| `.\scripts\import-smarthome-payload.ps1` | Import guides/picks UTF-8 |

## Staging и Prod

| Среда | URL | Назначение |
| ----- | --- | ---------- |
| **Prod API** | https://api.alicecommands.ru | **Пользователи (LIVE)** |
| Prod admin | https://api.alicecommands.ru/admin | Publish после staging QA |
| Staging API | https://staging-api.alicecommands.ru | Pre-prod QA, черновики |
| Staging admin | https://staging-api.alicecommands.ru/admin | Основная работа с контентом |
| CDN icons | https://cdn.alicecommands.ru/icons/v1/ | Иконки в bundle |

**Синхронизация prod:** `.\scripts\copy-staging-to-prod.ps1` · проверка: `.\scripts\verify-prod.ps1`

Инфраструктура: **[docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md)** · **LIVE ops:** **[docs/PRODUCTION.md](docs/PRODUCTION.md)**

## Документация

| Документ | Описание |
| -------- | -------- |
| [docs/README.md](docs/README.md) | **Полный индекс документации** |
| [docs/BACKEND-REQUIREMENTS.md](docs/BACKEND-REQUIREMENTS.md) | **Главное ТЗ** |
| [docs/BACKEND-SMARTHOME-DEVICES.md](docs/BACKEND-SMARTHOME-DEVICES.md) | Устройства (guides + picks) |
| [docs/ANALYTICS-BACKEND.md](docs/ANALYTICS-BACKEND.md) | Analytics ingest + admin |
| [docs/API.md](docs/API.md) | HTTP-контракт для Android |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Light Clean, Ktor, PostgreSQL |
| [docs/DATABASE.md](docs/DATABASE.md) | Схема БД (Flyway V1–V10) |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | VPS, nginx, systemd |
| [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) | Selectel VPS, SSH, DNS |
| [docs/PRODUCTION.md](docs/PRODUCTION.md) | **LIVE prod** — правила изменений, откат, снимок |
| [docs/PROD-CUTOVER.md](docs/PROD-CUTOVER.md) | Cutover checklist (завершён) |
| [docs/CATALOG-FIXED-BUILD.md](docs/CATALOG-FIXED-BUILD.md) | **Канонический каталог** (885 cmd, deploy, orphan) |
| [docs/CONTENT-UPDATE.md](docs/CONTENT-UPDATE.md) | Fixed catalog + legacy pipeline |
| [docs/RUNBOOK-PUBLISH.md](docs/RUNBOOK-PUBLISH.md) | Как выпустить контент |
| [server/README.md](server/README.md) | Ktor module: run, test, deploy |
| [AGENTS.md](AGENTS.md) | Инструкции для ИИ-агентов |

## Связанный репозиторий

Android app: [AliceCommands](https://github.com/MironBano/AliceCommands) (Full Clean, отдельный repo).

## Стек

Kotlin 2.1 · Ktor 3.1 · PostgreSQL 16 · Exposed · Flyway V1–V10 · kotlinx.serialization · Alpine.js (admin)
