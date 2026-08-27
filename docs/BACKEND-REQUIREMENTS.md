# BACKEND-REQUIREMENTS — alice-commands-api v1.0

**mob_id:** MOB-20260626-001  
**Дата:** 2026-06-26 · **Обновлено:** 2026-07-10 (smarthome, analytics, prod)  
**Статус:** ТЗ v1.0 — **реализовано**; **schema v2** + **category visuals** + **command of day** + **smarthome** + **analytics P0** — **реализовано**  
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
| Public API (manifest, bundle, affiliate, smarthome, analytics) | ✅ |
| Admin auth (один пользователь) | ✅ |
| CRUD каталога в админке | ✅ |
| Publish + rollback (5 версий) | ✅ |
| Preview draft bundle | ✅ |
| Import pilot JSON | ✅ |
| PostgreSQL + Flyway (V1–V10) | ✅ |
| Docker-compose local | ✅ |
| **Schema v2 command groups** | ✅ |
| **Delta sync** `GET /v1/content/delta` | ✅ |
| **Command of day** | ✅ |
| **Smart home devices** `GET /v1/smarthome/devices` | ✅ |
| **Analytics batch ingest** | ✅ |

### 1.3 Out of scope / отложено

- User accounts для app
- FCM push при publish (v1.0.1)
- Парсер Яндекса в runtime (только offline assist tool)
- Object storage S3 (v1.0.1; v1.0 — filesystem на VPS)
- Казахская локаль контента (v1.1)
- Auto-generate `command_groups` из парсера без human review

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
- Bundle содержит `command_groups[]` (schema v2; может быть пустым для backward compat)
- Команды могут иметь `group_id`, `sort_order`, `variant_label_ru`, `is_primary_in_group`, `search_aliases`

### B02b — Public content delta

| Поле | Значение |
| ---- | -------- |
| Endpoint | `GET /v1/content/delta?from={version}` |
| Auth | Нет |
| Priority | P1 |

**AC:**
- Возвращает incremental diff между published v{from} и current: `categories`, `command_groups`, `commands`, `scenario_templates`, `checklist_items`
- `updated` — полные объекты; `removed` — ids
- **409** `delta_unavailable` если bundle `from` не в retention (5 версий) → client fallback на full bundle
- См. [API.md](API.md), [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md)

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
| Categories | list, create, update, delete, reorder; **visual fields** (`icon_url`, `accent_color`, colors) |
| **Command groups** | list, create, update, delete, reorder; filter by category; visual override / inherit |
| **Icons** | `GET /icons/catalog`, `POST /icons/upload` (admin); public `GET /icons/v1/{slug}.svg` |
| Commands | list, create, update, delete, filter by category; **bulk assign group** |
| Scenario templates | list, create, update, delete |
| Checklist items | list, update order, link command |
| Affiliate blocks | list, create, update, delete |

**AC:**
- Изменения пишутся в **draft** (PostgreSQL), не в live bundle до Publish
- Валидация полей по [schema/content-bundle.schema.json](../schema/content-bundle.schema.json)
- Publish дополнительно: `CommandGroupValidationUseCase`, `CategoryVisualValidationUseCase` (URL allowlist, hex, SVG upload)
- `GET /admin/api/content/validation-warnings` — orphan commands, empty groups, visual warnings (не блокирует save)
- `source_url` обязателен на command/category
- Soft delete или hard delete с confirm в UI

### B06 — Publish pipeline

| Поле | Значение |
| ---- | -------- |
| Priority | P0 |

**AC:**
- Admin action **Publish**:
  1. Validate business rules (`CommandGroupValidationUseCase`)
  2. Validate full draft → JSON Schema
  3. Build bundle JSON (`schema_version: 2` from draft)
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
- Upload `seed/smart-home-groups-v2.json` или `seed/full-catalog.json`
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
| NFR-3 | Bundle + icons отдаются с nginx cache (DNS only, не CF proxy в РФ) |
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
| Staging | 13 категорий, ~300 команд; pilot **smart_home groups** | E2E admin + API + grouped UI |
| Prod | 300–500 команд + editorial groups по категориям | **Блокер grouped UI в app** |

Workflow: правки в admin (или import) → ревью владельцем → **Publish**.

---

## 6. Environments

| Env | URL | Назначение |
| --- | --- | ---------- |
| local | `http://localhost:8080` | docker-compose + Gradle |
| staging | `https://staging-api.alicecommands.ru` | Android debug/staging flavor |
| prod | `https://api.alicecommands.ru` | Release app |

См. [DEPLOYMENT.md](DEPLOYMENT.md).

---

## 7. Definition of Done — backend готов к Android

- [x] `GET /v1/content/manifest` — HTTPS staging
- [x] `GET /v1/content/bundle` — gzip, sha256 match
- [x] Bundle проходит JSON Schema
- [x] ≥13 категорий на staging (prod: 300–500)
- [x] Admin: login, edit command, publish, rollback
- [x] Import seed/smart-home.json работает
- [x] curl-чеклист из [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) пройден
- [x] AliceCommands `CONTENT_API_BASE_URL` указывает на staging
- [ ] Prod cutover завершён — см. [PROD-CUTOVER.md](PROD-CUTOVER.md)
- [ ] Android analytics flush → backend ingest в prod

---

## 8. Roadmap

| Версия | Фичи |
| ------ | ---- |
| v1.0 | B01–B10, admin UI, filesystem storage |
| **v1.0 + schema v2** | Command groups, delta sync, validation warnings |
| v1.0.1 | S3 storage, FCM hook on publish |
| v1.1 | `title_kk`, parser assist UI, bulk CSV import |

---

## 9. Трассировка → app FR

| Backend | App FR |
| ------- | ------ |
| B01, B02, **B02b** | F01, F22, NFR-9 (delta optional in app) |
| B03 | F25 |
| B06 | Content sync, F30 «Новое» |
| Admin CRUD | F01, F12, F17, F18 |

---

## 10. Открытые на ревью

**Статус:** решения зафиксированы в [REVIEW.md](REVIEW.md).

| # | Вопрос | Решение |
| - | ------ | ------- |
| 1 | Домен | `api.alicecommands.ru` (prod), `staging-api.alicecommands.ru` — см. [INFRASTRUCTURE.md](INFRASTRUCTURE.md) |
| 2 | GitHub repo | `alice-commands-api` |
| 3 | Admin UI | Ktor + static HTML + Alpine.js |
| 4 | Import pilot JSON | Да в v1.0 (B08) |
| 5 | Rollback | 5 версий bundle на диске |

---

*ТЗ v1.0 — 2026-06-26 · schema v2 — 2026-06-29 · category visuals — [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md)*
