# Content Update — runbook

**Для админа (пошагово):** [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md) — ярлыки на рабочий стол, мастер в admin UI.

**Цель:** обновить каталог команд на **staging** draft без auto-publish.

---

## Быстрый старт

**Админу:** папка **Alice Commands** на рабочем столе → **Alice 1 - Obnovit katalog** → **Alice 4 - Admin staging** → **Контент** → Publish. Подробно: [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md).

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

## Что делает pipeline (inventory / editorial / catalog)

Три слоя данных — **разные роли**, без перезаписи вычитанных описаний:

| Слой | Файл | Кто обновляет | Содержимое |
|------|------|---------------|------------|
| **Inventory** | `seed/data/inventory_snapshot.json` | парсер (машина) | фразы, `raw_result`, `source_url` |
| **Baseline** | `seed/data/inventory_baseline.json` | после Publish (`--finalize-baseline`) | снимок для diff |
| **Editorial** | `seed/data/editorial.json` | вы + approve в admin | `title`, `effect`, `status=approved` |
| **Queue** | `seed/data/queue.json` | diff inventory vs baseline | только NEW / GONE / needs_review |
| **Catalog** | `seed/full-catalog.json` | join approved editorial + inventory | `schema_version: 2`; editorial groups — post-pilot |

| Шаг | Команда | Результат |
|-----|---------|-----------|
| Fetch + parse | `tools/content/pipeline_run.py` | inventory snapshot + queue |
| Sync diff | внутри pipeline | очередь delta (не «500 changed») |
| Catalog | `catalog_build.py` | только `editorial.status=approved` |
| Validate | `gradlew :server:validateContent` | JSON Schema |
| Push | `scripts/push-draft.ps1 -Mode sync` | pipeline-sync + import SYNC |
| Verify | `scripts/verify-staging.ps1` | manifest + sha256 + **schema_version / groups count** |

**Publish не выполняется автоматически.** После Publish на staging:
`python tools/content/pipeline_run.py --finalize-baseline` — обновить baseline.

**Сеть РФ:** если DNS не резолвит `staging-api.alicecommands.ru`, `push-draft.ps1` и `verify-staging.ps1` автоматически используют `curl --resolve` на IP VPS (`STAGING_ORIGIN_IP` в `scripts/.env`, по умолчанию `161.104.46.92`).

Первичная инициализация editorial из command bank:
`python tools/content/pipeline_run.py --bootstrap --skip-fetch`

---

## Ручные команды

```powershell
# Полный pipeline (из cache)
python tools/content/pipeline_run.py --skip-fetch

# С fetch с support
python tools/content/pipeline_run.py --force-fetch

# Только validate
.\gradlew.bat :server:validateContent -PcontentFile=seed/full-catalog.json
.\gradlew.bat :server:validateContent -PcontentFile=seed/smart-home-groups-v2.json

# Pilot command groups (smart_home) — sync import
.\scripts\push-draft.ps1 -Mode sync -BundleFile seed/smart-home-groups-v2.json

# Push (sync — не затирает approved editorial)
.\scripts\push-draft.ps1 -Mode sync

# После Publish — зафиксировать baseline
python tools/content/pipeline_run.py --skip-fetch --finalize-baseline

# Push replace (осторожно на staging с данными)
.\scripts\push-draft.ps1 -Mode replace -BundleFile seed/full-catalog.json

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

Конфиг: `tools/content/sources.yaml` + **auto-discovery** из оглавления справки Яндекса.

| Тип | Откуда |
|-----|--------|
| **auto** | `discovery.overview_url` → `https://alice.yandex.ru/support/ru/station/skills/` — все ссылки «Возможности и команды» (~66 страниц) |
| manual | pilot `seed/smart-home-groups-v2.json` (groups), `seed/full-catalog.json` (full catalog) |
| curated | `tools/content/command_bank.py` |

Старые URL вида `/skills/timers/`, `/skills/music/` **больше не работают (404)**. Яндекс перешёл на slug-страницы: `/skills/timer`, `/skills/audio-settings`, `/station/call`, `/assistant/alice-plus/…`, `/smart-home/…`.

Проверка источников:

```powershell
py -3 tools/content/discover_sources.py
```

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
