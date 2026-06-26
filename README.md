# alice-commands-api

Backend для [AliceCommands](https://github.com/) — справочник голосовых команд Яндекс Алисы.

**mob_id:** MOB-20260626-001 · **Статус:** ТЗ v1.0 (реализация — следующий этап)

## Назначение

- **Public API** — manifest + content bundle для Android app (offline sync)
- **Admin** — веб-редактор каталога (категории, команды, шаблоны, affiliate)
- **Publish pipeline** — PostgreSQL (draft) → immutable bundle.gz + manifest

## Документация

| Документ | Описание |
| -------- | -------- |
| [docs/BACKEND-REQUIREMENTS.md](docs/BACKEND-REQUIREMENTS.md) | **Главное ТЗ** |
| [docs/API.md](docs/API.md) | HTTP-контракт для Android |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Ktor, PostgreSQL, publish |
| [docs/ADMIN-UX.md](docs/ADMIN-UX.md) | Веб-админка |
| [docs/DATABASE.md](docs/DATABASE.md) | Схема БД |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | VPS, Cloudflare, HTTPS |
| [docs/RUNBOOK-PUBLISH.md](docs/RUNBOOK-PUBLISH.md) | Как выпустить контент |
| [docs/SECURITY.md](docs/SECURITY.md) | Auth, secrets |
| [schema/content-bundle.schema.json](schema/content-bundle.schema.json) | JSON Schema bundle |

## Связанный репозиторий

Android app: `AliceCommands` (отдельный repo).

## Локальный запуск (после реализации)

```bash
cp .env.example .env
docker compose up -d
# ./gradlew :server:run
```

## Стек (план)

Kotlin · Ktor 3 · PostgreSQL · Exposed · Flyway · kotlinx.serialization
