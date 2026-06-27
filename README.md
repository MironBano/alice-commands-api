# alice-commands-api

Backend для [AliceCommands](https://github.com/MironBano/AliceCommands) — справочник голосовых команд Яндекс Алисы.

**GitHub:** https://github.com/MironBano/alice-commands-api

## Назначение

- **Public API** — manifest + content bundle для Android app (offline sync)
- **Admin** — веб-редактор каталога (категории, команды, шаблоны, affiliate)
- **Publish pipeline** — PostgreSQL (draft) → immutable `content_vN.json.gz` + manifest
- **Content tools** — Python-парсеры и PowerShell-скрипты для обновления каталога

## Быстрый старт (local)

```powershell
Copy-Item .env.example .env
# Заполните SESSION_SECRET (≥32 символов)
docker compose up -d
.\gradlew.bat :server:test
.\gradlew.bat :server:run
```

Admin UI: http://localhost:8080/admin

Первый publish: Admin → Import → `seed/import-smart-home.json` (Replace) → Publish.

Gradle скачивает JDK 21 автоматически (Foojay toolchain). При необходимости укажите `org.gradle.java.home` в `gradle-local.properties` (см. `gradle-local.properties.example`).

## Структура репозитория

```
alice-commands-api/
├── server/              # Ktor API (public + admin + publish use cases)
├── admin-web/           # Static admin UI (Alpine.js)
├── schema/              # JSON Schema — канон контракта с Android
├── seed/                # import-smart-home.json (pilot), full-catalog.json (pipeline)
├── tools/content/       # Python: fetch, parse, merge, build_bundle
├── scripts/             # PowerShell: update-content, push-draft, verify-staging, deploy-staging
├── deploy/              # systemd, nginx, remote-setup.sh, staging .env example
├── docs/                # ТЗ, API, архитектура, runbooks
└── .github/workflows/   # CI validate-content
```

## Команды разработчика

| Команда | Назначение |
| ------- | ---------- |
| `.\gradlew.bat :server:run` | Запуск API на `:8080` |
| `.\gradlew.bat :server:test` | Unit + integration (Testcontainers, нужен Docker) |
| `.\gradlew.bat :server:validateContent` | JSON Schema check для `seed/full-catalog.json` |
| `.\gradlew.bat :server:installDist` | Сборка дистрибутива для VPS (`server/build/install/server/`) |
| `.\scripts\update-content.ps1` | Полный pipeline контента → staging draft |
| `.\scripts\deploy-staging.ps1` | Деплой API на Selectel VPS |
| `.\scripts\cloudflare-dns-direct.ps1` | DNS only для API (без CF proxy, РФ) |

## Staging (prod-like)

| URL | |
| --- | --- |
| Admin | https://staging-api.alicecommands.ru/admin |
| API | https://staging-api.alicecommands.ru |

Инфраструктура, SSH, DNS: **[docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md)**.

## Документация

| Документ | Описание |
| -------- | -------- |
| [docs/BACKEND-REQUIREMENTS.md](docs/BACKEND-REQUIREMENTS.md) | **Главное ТЗ** |
| [docs/API.md](docs/API.md) | HTTP-контракт для Android |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Light Clean, Ktor, PostgreSQL |
| [docs/ADMIN-UX.md](docs/ADMIN-UX.md) | Веб-админка |
| [docs/DATABASE.md](docs/DATABASE.md) | Схема БД |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | VPS, nginx, systemd, staging |
| [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) | **Selectel VPS, SSH, DNS, deploy** |
| [docs/RUNBOOK-PUBLISH.md](docs/RUNBOOK-PUBLISH.md) | Как выпустить контент |
| [docs/CONTENT-UPDATE.md](docs/CONTENT-UPDATE.md) | Pipeline обновления каталога |
| [docs/SECURITY.md](docs/SECURITY.md) | Auth, secrets, rate limit |
| [docs/SCHEMA-SYNC.md](docs/SCHEMA-SYNC.md) | Sync schema с Android |
| [`server/README.md`](server/README.md) | Ktor module: run, test, deploy |
| [`admin-web/README.md`](admin-web/README.md) | Admin SPA assets |
| [`publish/README.md`](publish/README.md) | Publish use cases location |
| [docs/GAP-ANALYSIS.md](docs/GAP-ANALYSIS.md) | Delta vs app CONTENT-PIPELINE |
| [docs/REVIEW.md](docs/REVIEW.md) | Закрытые решения ТЗ |
| [schema/content-bundle.schema.json](schema/content-bundle.schema.json) | JSON Schema bundle |

Полный индекс: [docs/README.md](docs/README.md). Для ИИ-агентов: [AGENTS.md](AGENTS.md).

## Связанный репозиторий

Android app: [AliceCommands](https://github.com/MironBano/AliceCommands) (Full Clean, отдельный repo).

## Стек

Kotlin 2.1 · Ktor 3.1 · PostgreSQL 16 · Exposed · Flyway · kotlinx.serialization · Alpine.js (admin)
