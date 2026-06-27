# Admin UX — alice-commands-api

**Stack:** Ktor `/admin` → SPA (`admin-web/`) + Alpine.js 3 + fetch  
**Auth:** cookie `alice_admin_session` (HttpOnly, Secure on staging/prod)  
**Staging:** https://staging-api.alicecommands.ru/admin

---

## 1. Global UI

### Status bar (всегда видна)

- Polling `/health` + `/ready` каждые **5 мин** (+ кнопка ↻)
- Состояния: **Сервер OK** / **не готов (DB/storage)** / **недоступен**
- На экране логина — подсказка при offline

### Ошибки и сохранение

- Сетевые ошибки: понятные сообщения (таймаут, VPN, connection reset)
- Формы CRUD: `Сохранение…`, блокировка при offline, `formError` в модалке
- Toast — успех; глобальный `error` — операции publish/import

---

## 2. Navigation (SPA views)

| View | ID | Назначение |
| ---- | -- | ---------- |
| Dashboard | `dashboard` | Live, draft, сервер, quick actions |
| Категории | `categories` | CRUD + reorder (↑/↓) |
| Команды | `commands` | CRUD, filter by category |
| Шаблоны | `scenarios` | Scenario templates CRUD |
| Чеклист | `checklist` | Checklist items reorder |
| Affiliate | `affiliate` | Affiliate blocks CRUD |
| Publish | `publish` | Preview, Publish, History, Rollback |
| Import | `import` | Upload JSON, diff, merge/replace |
| Контент | `content` | Pipeline scripts, import seed с сервера |
| API | `api` | In-app API reference (swagger-like) |

Login screen до успешного `GET /admin/api/dashboard`.

---

## 3. Dashboard

| Block | Содержание |
| ----- | ---------- |
| Сервер | `/health` + `/ready`, DB/storage, ручная проверка |
| Live | `content_version`, `published_at` |
| Draft | counts + `hasUnpublishedChanges` |
| Actions | Preview JSON, Publish, Обновление контента |

---

## 4. Login

- Поля: username, password
- Ошибки: неверный пароль / rate limit (429) / сеть (без VPN hint)
- Таймаут login: 60 с

---

## 5. Command edit

| Field | Widget |
| ----- | ------ |
| title_ru | text |
| phrases | textarea (по строке) |
| effect_description_ru | textarea |
| category_id | select |
| requires_alice_word / requires_plus | checkbox |
| tags | comma (`needs_review` для pipeline) |
| source_url | url (required) |

Кнопка: **Сохранить draft** · Cancel

*Planned:* `device_types`, `related_command_ids` chips (см. BACKEND-REQUIREMENTS).

---

## 6. Publish

- Текущая версия + **Опубликовать** (confirm)
- Preview JSON (download)
- История last 5 + Rollback

---

## 7. Import

- Merge / Replace all
- Diff vs **published** (`POST /import/preview`)
- Фильтры: added/changed/removed, `needs_review`
- Disabled при offline сервера

---

## 8. Контент (pipeline)

1. PowerShell команды с кнопкой **Копировать** (`update-content.ps1`, `push-draft.ps1`, `verify-staging.ps1`)
2. **Import seed → draft** — `POST /admin/api/content/import-seed?mode=merge|replace` (если `CONTENT_SEED_PATH` на VPS)
3. Flow: script → Import diff → Publish

---

## 9. API (in-app docs)

`GET /admin/api/docs` → секции Public / Auth / CRUD / Publish с method badges и `<details>`.

---

## 10. Assets

| Path | Назначение |
| ---- | ---------- |
| `admin-web/index.html` | Layout |
| `admin-web/js/admin.js` | API client, health, forms |
| `admin-web/css/admin.css` | Styles, status bar, API docs |

Local: static из `admin-web/` (`APP_ENV=local`). Staging/prod: в JAR через Gradle `copyAdminWeb`.

---

## 11. Future (v1.0.1+)

- TTS preview фраз
- KK fields
- Parser assist one-click import

---

*См. [API.md](API.md) §2, [CONTENT-UPDATE.md](CONTENT-UPDATE.md), [INFRASTRUCTURE.md](INFRASTRUCTURE.md)*
