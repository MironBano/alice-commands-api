# Admin static UI

Single-page admin для alice-commands-api.

**Staging:** https://staging-api.alicecommands.ru/admin

## Stack

- HTML + [Alpine.js](https://alpinejs.dev/) 3 (CDN)
- Vanilla fetch + session cookie
- Health polling `/health` + `/ready` (5 min)

## Views

Dashboard · Categories · Commands · Scenarios · Checklist · Affiliate · Publish · Import · **Контент** · **API**

## Features (v1)

- Status bar: server OK / degraded / offline
- CRUD с обработкой сетевых ошибок и loading states
- Import diff vs published bundle
- Content pipeline panel + import seed
- In-app API reference (`GET /admin/api/docs`)

## Files

| File | Purpose |
| ---- | ------- |
| `index.html` | Layout, modals, diff UI |
| `js/admin.js` | API client, health, forms |
| `css/admin.css` | Status bar, API docs, diff badges |

## Dev vs prod

| Env | Static source |
| --- | ------------- |
| `APP_ENV=local` | `admin-web/` directly (hot reload) |
| staging/prod | Gradle `copyAdminWeb` → JAR classpath `/admin` |

## Docs

- [ADMIN-UX.md](../docs/ADMIN-UX.md) — UX spec
- [API.md](../docs/API.md) §2 — Admin API
- [INFRASTRUCTURE.md](../docs/INFRASTRUCTURE.md) — staging access
