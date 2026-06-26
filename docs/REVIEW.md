# REVIEW — закрытие открытых пунктов ТЗ backend

**Дата:** 2026-06-26 · **Статус:** зафиксировано для v1.0

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
- [ ] **GitHub repo live** — требует `gh auth login` + push

---

## Следующий этап (реализация)

1. BL-015 — Ktor public API + publish (MVP)
2. BL-016 — Admin UI + полный каталог
3. Android BL-004 — sync на staging URL

---

*Обновлять после ревью владельца.*
