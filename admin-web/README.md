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
| `dashboard` | Обзор | Live / draft / сервер |
| `categories` | Категории | CRUD + reorder |
| `category-visuals` | Оформление | Цвета и icon_url |
| `command-groups` | Группы команд | CRUD groups, reorder |
| `commands` | Команды | CRUD, форма + **JSON (все поля)**, поиск, pull-draft hint |
| `content` | **Контент** | validate → push-draft → diff → публикация |
| `import` | Импорт bundle | replace-only import JSON |
| `publish` | Публикация | Publish draft → live, rollback |
| `api` | Справка API | In-app reference |

Полный список — см. sidebar в `index.html`.

## Модель состояний

1. **Опубликовано (live)** — bundle/manifest на диске, видит app.
2. **Draft** — PostgreSQL, правки до publish.

Канон seed: `seed/catalog-audit-fixed.json` → `push-draft.ps1` (replace).

## Files

| File | Purpose |
| ---- | ------- |
| `index.html` | Layout, wizard, diff UI |
| `js/admin.js` | API client, health, refresh |
| `css/admin.css` | Status bar, wizard |

## Docs

- [ADMIN-CONTENT-GUIDE.md](../docs/ADMIN-CONTENT-GUIDE.md)
- [CONTENT-UPDATE.md](../docs/CONTENT-UPDATE.md)
- [API.md](../docs/API.md)
