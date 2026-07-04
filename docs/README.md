# Документация — alice-commands-api

**Статус:** backend v1.0 + **schema v2 (command groups)** + **category visuals** + **command of day** на staging (v20).

| Документ | Назначение |
| -------- | ---------- |
| [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) | **Главное ТЗ** v1.0 (+ schema v2 extensions) |
| [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md) | **Command groups schema v2** — контракт, валидация, rollout |
| [BACKEND-COMMAND-OF-DAY.md](BACKEND-COMMAND-OF-DAY.md) | **Команда дня** — editorial bundle field, resolver, admin |
| [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md) | **Иконки + accent colors** — CDN, admin, validation |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | **VPS, SSH, DNS (РФ), CDN, deploy, ICON_* env** |
| [API.md](API.md) | HTTP-контракт: manifest, bundle, **/icons/v1/**, admin icons API |
| [ADMIN-UX.md](ADMIN-UX.md) | Веб-админка (**Оформление**, группы, content wizard) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Light Clean, модули, publish flow, icon storage |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Topology, nginx (api + cdn), CI, мониторинг |
| [CONTENT-UPDATE.md](CONTENT-UPDATE.md) | Python + scripts pipeline |
| [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md) | **Пошаговая инструкция для админа** |
| [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) | Публикация контента + visuals (operator) |
| [DATABASE.md](DATABASE.md) | PostgreSQL schema (Flyway **V1–V6**) |
| [SECURITY.md](SECURITY.md) | Auth, sessions, rate limit, icon URL allowlist |
| [SCHEMA-SYNC.md](SCHEMA-SYNC.md) | Sync schema v2 с Android |
| [GAP-ANALYSIS.md](GAP-ANALYSIS.md) | Backend vs app CONTENT-PIPELINE |
| [APP-FEEDBACK-INTEGRATION.md](APP-FEEDBACK-INTEGRATION.md) | Public feedback + command reports |
| [REVIEW.md](REVIEW.md) | Закрытые решения ТЗ |

**Schema:** [`../schema/content-bundle.schema.json`](../schema/content-bundle.schema.json) — **v2** + optional visual fields on categories/groups

**Staging:** https://staging-api.alicecommands.ru · Admin: `/admin` · Icons: `/icons/v1/{slug}.svg`

**Scripts:**

| Script | Назначение |
| ------ | ---------- |
| `scripts/deploy-staging.ps1` | Deploy backend + admin-web + icons + nginx |
| `scripts/cloudflare-dns-direct.ps1` | DNS only: `staging-api` + `cdn` (без CF proxy) |
| `scripts/setup-cdn.ps1` | DNS → certbot → nginx cdn vhost |
| `scripts/publish-staging-visuals.ps1` | Merge visuals в live bundle + publish |
| `scripts/update-content.ps1` | Content pipeline |
| `scripts/push-draft.ps1` | Import draft на staging |
| `scripts/verify-staging.ps1` | Manifest + sha256 + schema/groups count |

**Связанный app:** [AliceCommands](https://github.com/MironBano/AliceCommands) · **ИИ:** [AGENTS.md](../AGENTS.md)

**Код:** [server/README.md](../server/README.md) · [admin-web/README.md](../admin-web/README.md)
