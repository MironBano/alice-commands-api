# Документация — alice-commands-api

**Статус:** **PRODUCTION LIVE** (2026-07-13) — RuStore release → `https://api.alicecommands.ru`  
**Операции prod:** **[PRODUCTION.md](PRODUCTION.md)** (staging-first, verify, copy-staging-to-prod)

Backend v1.0 + **schema v2** + **category visuals** + **command of day** + **smarthome devices** + **analytics P0** · staging + prod на одном VPS.

| Документ | Назначение |
| -------- | ---------- |
| **[PRODUCTION.md](PRODUCTION.md)** | **LIVE** — правила изменений, откат, мониторинг |
| [PROD-CUTOVER.md](PROD-CUTOVER.md) | Cutover checklist (✅ завершён) |
| [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md) | **Command groups schema v2** — контракт, валидация, rollout |
| [BACKEND-COMMAND-OF-DAY.md](BACKEND-COMMAND-OF-DAY.md) | **Команда дня** — editorial bundle field, resolver, admin |
| [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md) | **Иконки + accent colors** — CDN, admin, validation |
| [BACKEND-SMARTHOME-DEVICES.md](BACKEND-SMARTHOME-DEVICES.md) | **Устройства** — guides, picks, contextual targeting, images |
| [ANALYTICS-BACKEND.md](ANALYTICS-BACKEND.md) | **Analytics ingest** — batch API, admin dashboard, rate limits |
| [ANALYTICS-GLOSSARY.md](ANALYTICS-GLOSSARY.md) | **Analytics glossary** — метрики, FAQ (`pro_restore`), event_name RU |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | **VPS, SSH, DNS (РФ), CDN, staging + prod deploy** |
| [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) | **Главное ТЗ** v1.0 (+ schema v2 extensions) |
| [API.md](API.md) | HTTP-контракт: manifest, bundle, smarthome, analytics, admin |
| [ADMIN-UX.md](ADMIN-UX.md) | Веб-админка (Оформление, группы, Устройства, Аналитика) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Light Clean, модули, publish flow, icon/device storage |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Topology, nginx (api + cdn), CI, мониторинг |
| [CONTENT-UPDATE.md](CONTENT-UPDATE.md) | Python + scripts pipeline |
| [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md) | **Пошаговая инструкция для админа** |
| [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) | Публикация контента + visuals + smarthome (operator) |
| [DATABASE.md](DATABASE.md) | PostgreSQL schema (Flyway **V1–V10**) |
| [SECURITY.md](SECURITY.md) | Auth, sessions, rate limit, analytics, icon URL allowlist |
| [SCHEMA-SYNC.md](SCHEMA-SYNC.md) | Sync schema v2 с Android |
| [GAP-ANALYSIS.md](GAP-ANALYSIS.md) | Backend vs app CONTENT-PIPELINE |
| [CONTENT-PRODUCT-ROADMAP.md](CONTENT-PRODUCT-ROADMAP.md) | **Карта улучшений** — визуал, онбординг, сценарии, devices |
| [APP-DEVICE-CHIPS-UX.md](APP-DEVICE-CHIPS-UX.md) | Contextual device picks в каталоге (app UX) |
| [REFERRAL-EPIC.md](REFERRAL-EPIC.md) | Рефералка — отложено post-launch |
| [APP-FEEDBACK-INTEGRATION.md](APP-FEEDBACK-INTEGRATION.md) | Public feedback + command reports |
| [CATALOG-FIXED-BUILD.md](CATALOG-FIXED-BUILD.md) | **Канон** — `catalog-audit-fixed.json` (885 cmd), deploy, orphan fix |
| [COMMANDS-TO-ADD-ROUND2.md](COMMANDS-TO-ADD-ROUND2.md) | ROUND2 — 28 добавленных команд (audit 2026-07-10) |
| [REVIEW.md](REVIEW.md) | Закрытые решения ТЗ |

**Schema:**

| Файл | Назначение |
| ---- | ---------- |
| [`schema/content-bundle.schema.json`](../schema/content-bundle.schema.json) | Bundle **v2** |
| [`schema/smarthome-devices.schema.json`](../schema/smarthome-devices.schema.json) | Smarthome snapshot **v1** |

**URLs:**

| Среда | API | Admin | Назначение |
| ----- | --- | ----- | ---------- |
| **Prod** | https://api.alicecommands.ru | `/admin` | **Пользователи (LIVE)** |
| Staging | https://staging-api.alicecommands.ru | `/admin` | QA, черновики |
| Icons CDN | https://cdn.alicecommands.ru/icons/v1/{slug}.svg | — | bundle `icon_url` |

**Актуальная `content_version`:** проверяйте live manifest (`GET /v1/content/manifest`), не захардкоженные числа в docs.

**Scripts:**

| Script | Назначение |
| ------ | ---------- |
| `scripts/deploy-staging.ps1` | Deploy backend + admin-web + icons + nginx (staging :8080) |
| `scripts/deploy-prod.ps1` | Deploy prod instance (:8081, storage-prod) |
| `scripts/verify-staging.ps1` | Manifest + sha256 + schema/groups smoke |
| `scripts/verify-prod.ps1` | Prod health + manifest smoke |
| `scripts/copy-staging-to-prod.ps1` | Bundle + smarthome snapshot staging → prod |
| `scripts/import-smarthome-payload.ps1` | UTF-8 import guides/picks |
| `scripts/cloudflare-dns-direct.ps1` | DNS only: `staging-api`, `api`, `cdn` (без CF proxy) |
| `scripts/setup-cdn.ps1` | DNS → certbot → nginx cdn vhost |
| `scripts/publish-staging-visuals.ps1` | Merge visuals в live bundle + publish |
| `scripts/push-draft.ps1` | Import draft на staging (default: `catalog-audit-fixed.json`, replace) |
| `scripts/update-content.ps1` | Legacy content pipeline → `full-catalog.json` |
| `scripts/sync-icons-staging.ps1` | Sync pilot SVG на VPS |

**Связанный app:** [AliceCommands](https://github.com/MironBano/AliceCommands) · **ИИ:** [AGENTS.md](../AGENTS.md)

**Код:** [server/README.md](../server/README.md) · [admin-web/README.md](../admin-web/README.md)
