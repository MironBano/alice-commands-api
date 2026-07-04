# Admin static UI

Single-page admin для alice-commands-api.

**Staging:** https://staging-api.alicecommands.ru/admin

## Stack

- HTML + [Alpine.js](https://alpinejs.dev/) 3 (CDN)
- Vanilla fetch + session cookie
- Health polling `/health` + `/ready` (5 min)

## Views (навигация)

| View | Раздел | Назначение |
| ---- | ------ | ---------- |
| `dashboard` | Обзор | Live / draft / сервер, быстрые действия |
| `categories` | Категории | CRUD + reorder |
| `category-visuals` | **Оформление** | Таблица цветов и icon_url всех категорий |
| `icon-library` | **Библиотека иконок** | Список SVG, upload, `public_base_url` |
| `command-groups` | **Группы команд** | CRUD groups, visual inherit/override, reorder ▲/▼ |
| `commands` | Команды | CRUD, group fields, bulk assign |
| `scenarios` | Шаблоны | Scenario templates CRUD |
| `checklist` | Чеклист | Checklist items |
| `affiliate` | Партнёрские блоки | Affiliate CRUD |
| `feedback` | Отзывы | Inbox feedback из app |
| `command-reports` | Ошибки команд | Inbox command reports |
| `content` | **Контент** | Мастер: pipeline → editorial → diff → публикация |
| `import` | Импорт bundle | Ручной import content bundle JSON в draft |
| `publish` | Публикация | Publish draft → live, история, rollback |
| `api` | Справка API | In-app API reference |

## Модель состояний

1. **Опубликовано (live)** — bundle/manifest на диске, видит app.
2. **Draft** — PostgreSQL, правки до publish.
3. **Pipeline / editorial** — inventory, editorial, queue; попадает в draft после save/import editorial и rebuild.

Два разных diff:

- **Import bundle:** файл vs опубликованное (live).
- **Контент, шаг 4:** draft vs опубликованное.

Editorial JSON (`records[].edit.*`) загружается только в **Контент → Редактор текстов**, не в «Импорт bundle».

## Features

- Status bar: server OK / degraded / offline
- CRUD с обработкой сетевых ошибок и loading states
- **Command groups:** CRUD, reorder, preview ids, validation warnings
- **Category visuals:** раздел «Оформление», color picker, icon catalog, SVG upload, light/dark preview
- Content wizard (5 шагов) + legacy queue в `<details>`
- Editorial export/import JSON
- После мутаций draft — `refreshAfterDraftMutation()` синхронизирует dashboard + pipeline

## Files

| File | Purpose |
| ---- | ------- |
| `index.html` | Layout, modals, wizard, diff UI, command-groups view |
| `js/admin.js` | API client, health, refresh logic |
| `css/admin.css` | Status bar, wizard, editorial table |

## Dev vs prod

| Env | Static source |
| --- | ------------- |
| `APP_ENV=local` | `admin-web/` directly (hot reload) |
| staging/prod | Gradle `copyAdminWeb` → JAR classpath `/admin` |

После изменений admin-web на staging: `.\scripts\deploy-staging.ps1`

## Docs

- [ADMIN-UX.md](../docs/ADMIN-UX.md) — UX spec
- [ADMIN-CONTENT-GUIDE.md](../docs/ADMIN-CONTENT-GUIDE.md) — пошаговый runbook
- [BACKEND-COMMAND-GROUPS.md](../docs/BACKEND-COMMAND-GROUPS.md) — schema v2 groups
- [BACKEND-CATEGORY-VISUALS.md](../docs/BACKEND-CATEGORY-VISUALS.md) — icons + colors
- [API.md](../docs/API.md) §2 — Admin API
