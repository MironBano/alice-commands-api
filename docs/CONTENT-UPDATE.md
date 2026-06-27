# Content Update — runbook

**Цель:** обновить каталог команд на **staging** draft без auto-publish.

---

## Быстрый старт

```powershell
# 1. Настройте credentials (один раз)
Copy-Item scripts\.env.example scripts\.env
# Отредактируйте: STAGING_API_URL, ADMIN_USERNAME, ADMIN_PASSWORD

# 2. Python deps (один раз)
pip install -r tools/content/requirements.txt

# 3. Полный pipeline
.\scripts\update-content.ps1

# 4. Admin UI
# https://staging-api.alicecommands.ru/admin → Import diff vs published → Publish
```

---

## Что делает pipeline

| Шаг | Команда | Результат |
|-----|---------|-----------|
| Fetch | `tools/content/fetch.py` | HTML в `tools/content/cache/` |
| Parse | parsers + `command_bank.py` | ParsedCommand[] |
| Merge | `merge.py` | dedupe по фразам |
| Build | `build_bundle.py` | `seed/full-catalog.json` |
| Validate | `gradlew :server:validateContent` | JSON Schema |
| Push draft | `scripts/push-draft.ps1` | POST import merge на staging |
| Verify | `scripts/verify-staging.ps1` | manifest + sha256 + stats |

**Publish не выполняется автоматически.**

---

## Ручные команды

```powershell
# Только сборка (без сети, из command bank + cache)
python tools/content/build_bundle.py --skip-fetch

# Принудительно обновить HTML с Yandex
python tools/content/build_bundle.py --force-fetch

# Только validate
.\gradlew.bat :server:validateContent -PcontentFile=seed/full-catalog.json

# Push без rebuild
.\scripts\push-draft.ps1 -Mode merge

# Push replace (осторожно на staging с данными)
.\scripts\push-draft.ps1 -Mode replace -BundleFile seed/import-smart-home.json

# Проверить опубликованный bundle на staging
.\scripts\verify-staging.ps1
```

---

## Review в admin

1. **Контент** (sidebar) — команды pipeline или **Import seed** с сервера
2. **Import** → `seed/full-catalog.json` (если через push-draft)
3. **Diff vs опубликованная версия** — Added / Changed / Removed
4. Фильтр **needs_review**
5. **Publish** на Dashboard / Publish view

---

## Admin: раздел «Контент»

| Действие | Описание |
| -------- | -------- |
| Copy scripts | `update-content.ps1`, `push-draft.ps1`, `verify-staging.ps1` |
| Import seed (merge/replace) | `POST /admin/api/content/import-seed` — нужен `CONTENT_SEED_PATH` на VPS |
| → Import / Publish | Review diff, затем Publish |

---

## Деплой backend

После изменений server/admin-web:

```powershell
.\scripts\deploy-staging.ps1
```

См. [INFRASTRUCTURE.md](INFRASTRUCTURE.md).

---

## Источники контента

Конфиг: `tools/content/sources.yaml`

| Приоритет | Источник |
|-----------|----------|
| primary | alice.yandex.ru/support (skills, music, timers, …) |
| backup | quick-commands, calls, kids, plus |
| baseline | `seed/import-smart-home.json` (УД pilot) |
| curated | `tools/content/command_bank.py` |

Парсер = **assist only**. Перед prod publish — вычитка `source_url` и diff.

---

## CI

GitHub Actions [validate-content.yml](../.github/workflows/validate-content.yml):

- **PR:** изменения `seed/`, `tools/content/`, `schema/`, `server/`
- **push main:** `seed/`, `tools/content/`, `schema/`
- Не пушит на staging; только schema gate

---

## Weekly workflow

1. `.\scripts\update-content.ps1 -ForceFetch` (раз в неделю или при изменении справки Яндекса)
2. Review diff в admin
3. Publish на staging → проверка Android staging flavor
4. Publish prod — только после чеклиста [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md)

---

*См. [API.md](API.md) `POST /admin/api/import/preview`, [ARCHITECTURE.md](ARCHITECTURE.md) §6*
