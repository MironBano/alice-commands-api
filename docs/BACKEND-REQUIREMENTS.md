# BACKEND-REQUIREMENTS — alice-commands-api v1.0

**mob_id:** MOB-20260626-001  
**Дата:** 2026-06-26  
**Статус:** ТЗ v1.0 — **реализовано** (Ktor server, admin UI, publish pipeline, content tools)  
**Связанный app:** AliceCommands (`ru.appforsale.alicecommands`)

---

## 1. Цели и границы

### 1.1 Цель

Backend — **источник истины** для структуры контента мобильного справочника команд Алисы:

- Public read-only API для Android (sync → Room → data-driven UI)
- Веб-админка для редактирования каталога без релиза APK
- Publish pipeline: draft в PostgreSQL → immutable bundle + manifest

### 1.2 In scope v1.0

| Область | v1.0 |
| ------- | ---- |
| Public API (manifest, bundle, affiliate) | ✅ |
| Admin auth (один пользователь) | ✅ |
| CRUD каталога в админке | ✅ |
| Publish + rollback (5 версий) | ✅ |
| Preview draft bundle | ✅ |
| Import pilot JSON | ✅ |
| PostgreSQL + Flyway | ✅ |
| Docker-compose local | ✅ |

### 1.3 Out of scope v1.0

- User accounts для app
- FCM push при publish (v1.0.1)
- Delta sync endpoint (v1.0.1)
- Парсер Яндекса в runtime (только offline assist tool)
- Object storage S3 (v1.0.1; v1.0 — filesystem на VPS)
- Казахская локаль контента (v1.1)

---

## 2. Архитектурные принципы

1. **DB = редакторский источник** (draft, mutable)
2. **Bundle = runtime источник для app** (immutable после publish)
3. **Publish** = атомарная операция: validate → bundle.gz → manifest → audit log
4. **Android не знает** списков категорий/команд в коде — только sync bundle ([NFR-9 app](https://github.com/MironBano/AliceCommands))
5. **Affiliate** — отдельная сущность; обновляется через admin без APK
6. **Light Clean:** publish/rollback/import — use cases; CRUD — thin routes → repo; см. [ARCHITECTURE.md](ARCHITECTURE.md) §1.1

См. [ARCHITECTURE.md](ARCHITECTURE.md), [GAP-ANALYSIS.md](GAP-ANALYSIS.md), [AGENTS.md](../AGENTS.md).

---

## 3. Functional Requirements

### B01 — Public manifest

| Поле | Значение |
| ---- | -------- |
| Endpoint | `GET /v1/content/manifest` |
| Auth | Нет |
| Priority | P0 |

**AC:**
- Возвращает JSON: `content_version`, `published_at`, `bundle_url`, `bundle_sha256`, `backup_url`, `min_app_version`, `schema_version`
- `Content-Type: application/json`
- Поддержка `ETag` / `If-None-Match` → 304
- p95 latency < 200 ms (без учёта CDN)

### B02 — Public bundle

| Поле | Значение |
| ---- | -------- |
| Endpoint | `GET /v1/content/bundle` |
| Auth | Нет |
| Priority | P0 |

**AC:**
- Отдаёт последний опубликованный bundle (gzip)
- `Content-Encoding: gzip`, `Content-Type: application/json`
- `Cache-Control: public, max-age=86400, immutable`
- SHA256 совпадает с manifest
- Размер gzip ≤ 2 MB

### B03 — Affiliate blocks

| Поле | Значение |
| ---- | -------- |
| Endpoint | `GET /v1/affiliate/blocks` |
| Auth | Нет |
| Priority | P0 |

**AC:**
- JSON массив блоков с `erid`, `advertiser_name`, `products[]`
- Только опубликованная версия affiliate
- Fallback app: bundled seed

### B04 — Admin authentication

| Поле | Значение |
| ---- | -------- |
| Priority | P0 |

**AC:**
- Один admin: `ADMIN_USERNAME` + `ADMIN_PASSWORD` из env
- Пароль хранится bcrypt hash в env или DB config
- Session cookie (HttpOnly, Secure in prod)
- Rate limit: 5 failed attempts / 15 min / IP
- Logout endpoint

### B05 — Admin CRUD content

| Сущность | Операции |
| -------- | -------- |
| Categories | list, create, update, delete, reorder |
| Commands | list, create, update, delete, filter by category |
| Scenario templates | list, create, update, delete |
| Checklist items | list, update order, link command |
| Affiliate blocks | list, create, update, delete |

**AC:**
- Изменения пишутся в **draft** (PostgreSQL), не в live bundle до Publish
- Валидация полей по [schema/content-bundle.schema.json](../schema/content-bundle.schema.json)
- `source_url` обязателен на command/category
- Soft delete или hard delete с confirm в UI

### B06 — Publish pipeline

| Поле | Значение |
| ---- | -------- |
| Priority | P0 |

**AC:**
- Admin action **Publish**:
  1. Validate full draft → JSON Schema
  2. Build bundle JSON
  3. gzip + sha256
  4. Write `content_v{N}.json.gz` to storage
  5. Update manifest atomically
  6. Increment `content_version`
  7. Write `publish_history` row
- Rollback: выбрать одну из **5** последних версий → manifest указывает на неё
- Publish fails → live bundle unchanged

### B07 — Preview draft

| Поле | Значение |
| ---- | -------- |
| Endpoint | `GET /admin/api/preview/bundle` |
| Auth | Admin session |
| Priority | P1 |

**AC:**
- Возвращает JSON bundle из draft без publish
- Не доступен публично

### B08 — Import JSON

| Поле | Значение |
| ---- | -------- |
| Priority | P1 |

**AC:**
- Upload `seed/import-smart-home.json` или pilot JSON
- Merge или replace draft (с confirm)
- Используется для dev seed и миграции из app repo

### B09 — Audit log

| Поле | Значение |
| ---- | -------- |
| Priority | P1 |

**AC:**
- Таблица `publish_history`: version, published_at, admin_user, sha256, notes
- Admin UI: список последних publish + rollback button

### B10 — Health

| Endpoint | Назначение |
| -------- | ---------- |
| `GET /health` | liveness |
| `GET /ready` | DB + storage writable |

---

## 4. Non-Functional Requirements

| ID | Требование |
| -- | ---------- |
| NFR-1 | Public API доступен 99.5% (niche app, solo) |
| NFR-2 | Manifest p95 < 200 ms |
| NFR-3 | Bundle отдаётся с CDN/nginx cache |
| NFR-4 | HTTPS only в staging/prod |
| NFR-5 | PostgreSQL backup weekly (manual/script) |
| NFR-6 | 152-ФЗ: только admin credentials; нет ПДн пользователей app |
| NFR-7 | 100k+ installs: без горизонтального масштабирования v1.0 |
| NFR-8 | Schema sync с Android repo ([schema/](../schema/)) |

---

## 5. Контент — этапы наполнения

| Этап | Объём | Критерий |
| ---- | ----- | -------- |
| Dev | Пилот УД: 8 команд, S1–S8, checklist | App sync staging |
| Staging | 13 категорий, ~50–80 команд | E2E admin + API |
| Prod | 300–500 команд | **Блокер релиза app в RuStore** |

Workflow: правки в admin (или import) → ревью владельцем → **Publish**.

---

## 6. Environments

| Env | URL (TBD) | Назначение |
| --- | --------- | ---------- |
| local | `http://localhost:8080` | docker-compose + Gradle |
| staging | `https://staging-api.alicecommands.ru` | Android debug/staging flavor |
| prod | `https://api.<domain>` | Release app |

См. [DEPLOYMENT.md](DEPLOYMENT.md).

---

## 7. Definition of Done — backend готов к Android

- [ ] `GET /v1/content/manifest` — HTTPS staging
- [ ] `GET /v1/content/bundle` — gzip, sha256 match
- [ ] Bundle проходит JSON Schema
- [ ] ≥13 категорий на staging (prod: 300–500)
- [ ] Admin: login, edit command, publish, rollback
- [ ] Import seed/smart-home.json работает
- [ ] curl-чеклист из [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) пройден
- [ ] AliceCommands `CONTENT_API_BASE_URL` указывает на staging

---

## 8. Roadmap

| Версия | Фичи |
| ------ | ---- |
| v1.0 | B01–B10, admin UI, filesystem storage |
| v1.0.1 | Delta endpoint, S3 storage, FCM hook on publish |
| v1.1 | `title_kk`, parser assist UI, bulk CSV import |

---

## 9. Трассировка → app FR

| Backend | App FR |
| ------- | ------ |
| B01, B02 | F01, F22, NFR-9 |
| B03 | F25 |
| B06 | Content sync, F30 «Новое» |
| Admin CRUD | F01, F12, F17, F18 |

---

## 10. Открытые на ревью

**Статус:** решения зафиксированы в [REVIEW.md](REVIEW.md).

| # | Вопрос | Решение |
| - | ------ | ------- |
| 1 | Домен | TBD перед prod — см. [DEPLOYMENT.md](DEPLOYMENT.md) |
| 2 | GitHub repo | `alice-commands-api` |
| 3 | Admin UI | Ktor + static HTML + Alpine.js |
| 4 | Import pilot JSON | Да в v1.0 (B08) |
| 5 | Rollback | 5 версий bundle на диске |

---

*ТЗ v1.0 — 2026-06-26*
