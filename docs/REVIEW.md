# REVIEW — закрытие открытых пунктов ТЗ backend

**Дата:** 2026-06-26 · **Обновлено:** 2026-06-29 (schema v2) · **Статус:** v1.0 + schema v2 зафиксировано

---

## Решения по открытым вопросам плана

| # | Вопрос | Решение |
| - | ------ | ------- |
| 1 | Имя репозитория | **`alice-commands-api`** — GitHub `MironBano/alice-commands-api` |
| 2 | Домен | **TBD перед prod**; рекомендация: `api.<domain>.ru` + `staging-api.<domain>.ru` — см. [DEPLOYMENT.md](DEPLOYMENT.md) |
| 3 | Admin UI стек | **Ktor + static HTML + Alpine.js** (не Compose Web) |
| 4 | Import pilot JSON | **Да в v1.0** — B08, экран Import в [ADMIN-UX.md](ADMIN-UX.md) |
| 5 | Rollback publish | **Да** — хранить **5** последних bundle ([BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) B06) |
| 6 | Bulk CSV import | **v1.0.1** (не v1.0) |
| 7 | Parser assist UI | **v1.1** |
| 8 | Command groups schema v2 | **Реализовано** — см. [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md) |
| 9 | Delta sync endpoint | **Реализовано** в v1.0+ (ранее планировался v1.0.1) |

---

## Schema v2 (2026-06-29)

| Тема | Решение |
| ---- | ------- |
| Pilot category | `smart_home` — `seed/smart-home-groups-v2.json` |
| Publish gate | `CommandGroupValidationUseCase` перед JSON Schema |
| Admin reorder | ▲/▼, не DnD |
| `full-catalog.json` | `schema_version: 2`; editorial groups — post-pilot |

---

## Согласовано с владельцем (сессия планирования)

| Тема | Значение |
| ---- | -------- |
| Язык | Kotlin / Ktor |
| Auth | Один admin + пароль в `.env` |
| Бюджет | до 1000 ₽/мес |
| Контент dev | Пилот УД (малые данные) |
| Контент store | 300–500 команд — блокер релиза app |
| Repo layout | Отдельно от AliceCommands |

---

## Чеклист «ТЗ backend готово» (план)

- [x] `BACKEND-REQUIREMENTS.md` — FR/NFR/DoD
- [x] `API.md` — совместим с app CONTENT-PIPELINE §4
- [x] `ADMIN-UX.md` — экраны + publish flow
- [x] `DEPLOYMENT.md` — VPS + Cloudflare + бюджет
- [x] `DATABASE.md` — ERD
- [x] `RUNBOOK-PUBLISH.md` — пошагово для владельца
- [x] `schema/content-bundle.schema.json`
- [x] Ссылка из AliceCommands `RESEARCH-INDEX.md`
- [x] `GAP-ANALYSIS.md`, `SECURITY.md`, `ARCHITECTURE.md`
- [ ] **Ревью walkthrough с владельцем** — async (этот документ + комментарии в issue)
- [x] **GitHub repo live** — https://github.com/MironBano/alice-commands-api

---

## Следующий этап (ops + app)

1. Staging: import `smart-home-groups-v2.json` (sync) → Publish → `verify-staging.ps1` (schema=2)
2. Android: grouped UI QA на staging ([RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) §10)
3. Editorial groups для остальных категорий в `full-catalog.json`
4. Prod publish — после app gate в RuStore

---

*Обновлять после ревью владельца.*
