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
| Обзор | `dashboard` | Live, draft, сервер, quick actions |
| Категории | `categories` | CRUD + reorder (↑/↓) + visual fields в модалке |
| **Оформление** | `category-visuals` | Таблица цветов и иконок всех категорий |
| **Библиотека иконок** | `icon-library` | Список SVG на сервере, загрузка |
| Команды | `commands` | CRUD, filter by category, **group fields**, bulk assign |
| **Группы команд** | `command-groups` | CRUD groups, reorder (↑/↓), visual override / inherit |
| Шаблоны | `scenarios` | Scenario templates CRUD |
| Чеклист | `checklist` | Checklist items reorder |
| Партнёрские блоки | `affiliate` | Affiliate blocks CRUD |
| **Отзывы** | `feedback` | Inbox отзывов из app |
| **Ошибки команд** | `command-reports` | Inbox reports по command_id |
| **Контент** | `content` | Мастер pipeline → editorial → diff → публикация |
| Импорт bundle | `import` | Upload content bundle JSON, diff vs live |
| Публикация | `publish` | Preview, Publish draft → live, History, Rollback |
| Справка API | `api` | In-app API reference |

Login screen до успешного `GET /admin/api/dashboard`.

### Три слоя данных (подписи в UI)

| Слой | Где в UI | Что значит |
| ---- | -------- | ---------- |
| Опубликовано (live) | Обзор, Контент | Bundle на диске — видит app |
| Черновик (draft) | Обзор, editorial «Черновик» | PostgreSQL draft |
| Pipeline / editorial | Контент, «Правка» | Тексты до rebuild draft |

---

## 3. Dashboard (Обзор)

| Block | Содержание |
| ----- | ---------- |
| Сервер | `/health` + `/ready`, DB/storage, ручная проверка |
| Опубликовано (live) | `content_version`, `published_at` |
| Черновик (draft) | counts + `hasUnpublishedChanges` + **command_groups count** |
| Обращения из app | `inbox.open_feedback`, `inbox.open_command_reports` |
| Actions | Preview draft, Перейти к публикации, Мастер обновления контента |

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
| **group_id** | select (optional) |
| **sort_order** | number (within group) |
| **variant_label_ru** | text (compact row label) |
| **is_primary_in_group** | checkbox |
| **search_aliases** | textarea (по строке) |
| requires_alice_word / requires_plus | checkbox |
| tags | comma (`needs_review` для pipeline) |
| source_url | url (required) |

Кнопка: **Сохранить draft** · Cancel

**Bulk assign:** в списке команд — выбор нескольких → назначить группу.

**Validation warnings:** `GET /admin/api/content/validation-warnings` — orphan commands, empty groups, icon_url без icon_key, low contrast (информативно, не блокирует save).

---

## 5a. Category visuals (Оформление + модалки)

Раздел **Оформление** (`category-visuals`) — таблица всех категорий:

| Колонка | Назначение |
| ------- | ---------- |
| Превью | Swatch + иконка (light) |
| icon_key | Offline fallback в app |
| icon_url | HTTPS URL SVG (из `ICON_PUBLIC_BASE_URL`) |
| Light / Dark | Color picker + hex `#RRGGBB` |
| Иконка | Выбор из каталога slug |
| Сохранить | `PUT /admin/api/categories/{id}` |

**Категории** / **Группы команд** — те же поля в модалке + upload SVG, пресеты цветов, preview light/dark.

**Библиотека иконок** — `GET /admin/api/icons/catalog` (`public_base_url`, список slug); upload через форму или API.

После правок visual-полей — **Публикация** (draft → live). См. [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md).

*Planned:* `device_types`, `related_command_ids` chips (см. BACKEND-REQUIREMENTS).

---

## 5b. Command groups (Группы команд)

| Field | Widget |
| ----- | ------ |
| id | text (slug, immutable after create) |
| category_id | select |
| title_ru | text |
| sort_order | number |
| preview_command_ids | comma-separated command ids |
| icon_key | text (optional) |
| icon_url | url (optional) |
| accent_color / accent_color_dark | color picker + hex |
| inherit visuals | checkbox (группа — сброс к категории) |

Reorder: **▲ / ▼** (не drag-and-drop). Фильтр по категории.

Publish блокируется при нарушении правил групп (см. [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md)).

---

## 6. Publish (Публикация)

- Текущая live-версия + **Опубликовать draft → live** (confirm)
- Preview draft (download)
- История last 5 + Rollback live (draft не меняется)

---

## 7. Import bundle

- **Не editorial JSON** — только content bundle (`categories`, `command_groups`, `commands`, …)
- Merge / Replace all (режим `sync` — только через `push-draft.ps1` на ПК)
- Diff **файла vs опубликованное** (`POST /import/preview`)
- Disabled при offline сервера

---

## 8. Контент (pipeline wizard)

Пошаговый мастер — см. [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md).

1. **Шаг 1** — ярлык «Alice 1 - Obnovit katalog» / `update-content.ps1`
2. **Шаг 2** — проверить статус draft (`GET /admin/api/content/pipeline`)
3. **Шаг 3 — Редактор текстов** — editorial-review, save/batch, export/import **editorial JSON**
4. **Legacy** — очередь по одной (collapsed `<details>`, аварийный путь)
5. **Шаг 4** — diff **draft vs опубликованное** (`GET /admin/api/content/draft-diff`)
6. **Шаг 5** — переход к разделу «Публикация»

Опционально: **Seed на сервере** — import merge/replace с VPS.

Deploy backend после изменений кода: `deploy-staging.ps1` или ярлык «6».

API: `POST /content/pipeline-sync`, `POST /content/rebuild-draft` (из скриптов; в UI нет отдельных кнопок).

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
- Drag-and-drop reorder groups (сейчас ▲/▼)

---

*См. [API.md](API.md) §2, [CONTENT-UPDATE.md](CONTENT-UPDATE.md), [INFRASTRUCTURE.md](INFRASTRUCTURE.md)*
