# Документация — alice-commands-api

**Статус:** backend v1.0 на staging (Selectel VPS, admin UI, publish pipeline, content tools).

| Документ | Назначение |
| -------- | ---------- |
| [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) | **Главное ТЗ** v1.0 |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | **VPS, SSH, DNS (РФ), deploy, URLs** |
| [API.md](API.md) | HTTP-контракт для Android + Admin API |
| [ADMIN-UX.md](ADMIN-UX.md) | Веб-админка (health, content, API docs) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Light Clean, модули, publish flow |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Topology, nginx, CI, мониторинг |
| [CONTENT-UPDATE.md](CONTENT-UPDATE.md) | Python + scripts pipeline |
| [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) | Публикация контента (operator) |
| [DATABASE.md](DATABASE.md) | PostgreSQL schema (Flyway V1) |
| [SECURITY.md](SECURITY.md) | Auth, sessions, rate limit, secrets |
| [SCHEMA-SYNC.md](SCHEMA-SYNC.md) | Sync schema с Android |
| [GAP-ANALYSIS.md](GAP-ANALYSIS.md) | Delta vs app CONTENT-PIPELINE |
| [REVIEW.md](REVIEW.md) | Закрытые решения ТЗ |

**Schema:** [`../schema/content-bundle.schema.json`](../schema/content-bundle.schema.json)

**Staging:** https://staging-api.alicecommands.ru · Admin: `/admin`

**Scripts:**

| Script | Назначение |
| ------ | ---------- |
| `scripts/deploy-staging.ps1` | Deploy backend на VPS |
| `scripts/cloudflare-dns-direct.ps1` | DNS only (без CF proxy) |
| `scripts/update-content.ps1` | Content pipeline |
| `scripts/push-draft.ps1` | Import draft на staging |
| `scripts/verify-staging.ps1` | Manifest + sha256 check |

**Связанный app:** [AliceCommands](https://github.com/MironBano/AliceCommands) · **ИИ:** [AGENTS.md](../AGENTS.md)

**Код:** [server/README.md](../server/README.md) · [admin-web/README.md](../admin-web/README.md)
