# alice-commands-api

Backend для [AliceCommands](https://github.com/MironBano/AliceCommands) — справочник голосовых команд Яндекс Алисы.

**GitHub:** https://github.com/MironBano/alice-commands-api (создайте repo и push — см. ниже)

## Назначение

- **Public API** — manifest + content bundle для Android app (offline sync)
- **Admin** — веб-редактор каталога (категории, команды, шаблоны, affiliate)
- **Publish pipeline** — PostgreSQL (draft) → immutable bundle.gz + manifest

## Документация

| Документ | Описание |
| -------- | -------- |
| [docs/BACKEND-REQUIREMENTS.md](docs/BACKEND-REQUIREMENTS.md) | **Главное ТЗ** |
| [docs/API.md](docs/API.md) | HTTP-контракт для Android |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | **Light Clean**, Ktor, PostgreSQL |
| [docs/ADMIN-UX.md](docs/ADMIN-UX.md) | Веб-админка |
| [docs/DATABASE.md](docs/DATABASE.md) | Схема БД |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | VPS, Cloudflare, HTTPS |
| [docs/RUNBOOK-PUBLISH.md](docs/RUNBOOK-PUBLISH.md) | Как выпустить контент |
| [docs/SECURITY.md](docs/SECURITY.md) | Auth, secrets |
| [docs/GAP-ANALYSIS.md](docs/GAP-ANALYSIS.md) | Delta vs app CONTENT-PIPELINE |
| [docs/REVIEW.md](docs/REVIEW.md) | Закрытые решения ТЗ |
| [docs/SCHEMA-SYNC.md](docs/SCHEMA-SYNC.md) | Sync schema с Android |
| [schema/content-bundle.schema.json](schema/content-bundle.schema.json) | JSON Schema bundle |

## Связанный репозиторий

Android app: `AliceCommands` (отдельный repo).

## Публикация на GitHub

```powershell
cd C:\Users\rybak\AndroidStudioProjects\alice-commands-api
gh auth login
gh repo create MironBano/alice-commands-api --public --source=. --remote=origin --push
```

Или вручную: создайте пустой repo `alice-commands-api` на GitHub → `git push -u origin main`.

---

```bash
cp .env.example .env
docker compose up -d
# ./gradlew :server:run
```

## Стек (план)

Kotlin · Ktor 3 · PostgreSQL · Exposed · Flyway · kotlinx.serialization
