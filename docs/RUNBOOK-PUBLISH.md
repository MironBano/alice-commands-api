# Runbook — публикация контента

**Для:** владелец продукта (без знания backend)  
**Цель:** выпустить новую версию каталога в app

> **PRODUCTION LIVE:** пользователи читают **prod**. Новый контент — сначала **staging**, затем `copy-staging-to-prod.ps1`. См. [PRODUCTION.md](PRODUCTION.md).

---

## 1. Когда публиковать

- Добавили/исправили команды в admin
- Weekly review контента (рекомендация: раз в неделю)
- Срочно: изменилась справка Яндекса (после `update-content.ps1` + review diff)

**Не публиковать** без ревью `source_url` и текстов фраз.

---

## 2. Пошагово (admin UI)

1. Откройте **https://staging-api.alicecommands.ru/admin** (prod: `https://api.alicecommands.ru/admin`)
2. Войдите (логин из `/opt/alice-api/.env`, staging: `miron`)
3. Dashboard → карточка **Сервер** должна быть OK; проверьте **hasUnpublishedChanges**
4. При смене иконок/цветов:
   - **Оформление** — таблица всех категорий (цвета, icon_url, превью) — быстрый путь
   - **Категории** / **Группы команд** — полная форма + загрузка SVG
   - **Библиотека иконок** — список slug на сервере
   - См. [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md)
5. **Preview** — скачайте/просмотрите JSON draft
6. Нажмите **Publish**
7. Подтвердите в модалке
8. Запишите новый `content_version`
9. На телефоне: pull-to-refresh в app или перезапуск → «Обновлено …»

### Иконки: где лежат и какие URL

| Что | Где |
| --- | --- |
| Файлы SVG | `/opt/alice-api/storage/icons/v1/{slug}.svg` на VPS |
| Отдача (staging) | `GET https://staging-api.alicecommands.ru/icons/v1/{slug}.svg` |
| Отдача (prod CDN) | `GET https://cdn.alicecommands.ru/icons/v1/{slug}.svg` (нужна DNS-запись `cdn`) |
| Pilot в репо | `content/icons/v1/` |
| Справочник slug | `content/icon_catalog.json` (URL строит сервер из `ICON_PUBLIC_BASE_URL`) |

**Staging bundle** сейчас использует `staging-api` в `icon_url` (app работает без отдельного CDN). После `setup-cdn.ps1` и republish с `USE_CDN_ICON_URLS=1` — `cdn.alicecommands.ru`.

Скрипт merge visuals в live bundle: `.\scripts\publish-staging-visuals.ps1` (см. `-FromVersion`, `USE_CDN_ICON_URLS`).

---

## 3. Проверка curl (staging)

```bash
curl -sS https://staging-api.alicecommands.ru/v1/content/manifest
curl -sS -D - -o /tmp/bundle.gz https://staging-api.alicecommands.ru/v1/content/bundle
# command_of_day (optional root field):
# gunzip -c /tmp/bundle.gz | jq '.command_of_day'
curl -sS -o /dev/null -w "%{http_code}\n" https://staging-api.alicecommands.ru/icons/v1/child.svg
```

PowerShell: `.\scripts\verify-staging.ps1` (manifest + sha256 + stats).

---

## 4. Import seed

| Файл | Когда |
| ---- | ----- |
| `seed/catalog-audit-fixed.json` | **Канон** — 885 команд, все группы; default в `push-draft.ps1` |
| `seed/smart-home-groups-v2.json` | Pilot groups (Умный дом), пустая БД |
| `seed/full-catalog.json` | Legacy pipeline после парсера support |

**Канон (рекомендуется):**

```powershell
.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"
.\scripts\push-draft.ps1
# Admin → Publish
```

1. Admin → **Import** (или скрипт выше)
2. **Diff vs опубликованная версия**
3. **Replace** для fixed catalog; **Sync** только для legacy `full-catalog.json` + `-RebuildDraft`
4. Publish

См. [CATALOG-FIXED-BUILD.md](CATALOG-FIXED-BUILD.md).

---

## 5. Rollback

Если после publish что-то не так:

1. Admin → **Publish history**
2. Выберите предыдущую версию (например v41)
3. **Rollback**
4. Проверьте manifest — `content_version` должен откатиться
5. App при следующем sync получит старый bundle

Хранится **5** последних bundle на сервере (`BUNDLE_RETENTION_COUNT`).

---

## 6. Affiliate (CPA)

1. Admin → **Affiliate**
2. Обновите ссылки (ERID опционально — см. SECURITY.md §8)
3. Сохранение блока сразу обновляет `GET /v1/affiliate/blocks`; общий **Publish** каталога не нужен
4. Проверьте `GET /v1/affiliate/blocks` и в app: Умный дом → маркировка «Реклама»

---

## 7. Content pipeline (staging)

Автоматизация **без** auto-publish:

```powershell
Copy-Item scripts\.env.example scripts\.env   # STAGING_API_URL, credentials
.\scripts\update-content.ps1                  # legacy: full-catalog + sync + rebuild-draft
# или для канона:
.\scripts\push-draft.ps1                      # catalog-audit-fixed + replace
# → verify manifest → admin → Publish при diff
```

Подробнее: [CONTENT-UPDATE.md](CONTENT-UPDATE.md).

---

## 8. Prod после LIVE (обязательный путь)

**Не публиковать напрямую на prod** без QA на staging.

1. Publish на **staging** (admin или `push-draft.ps1` + Publish)
2. `.\scripts\verify-staging.ps1`
3. Проверка app на staging flavor (по возможности)
4. `.\scripts\copy-staging-to-prod.ps1` — каталог + smarthome
5. `.\scripts\verify-prod.ps1`
6. Записать prod `content_version` из manifest

Чеклист качества (до copy):

- [ ] ≥ 300 команд, 13+ категорий (сейчас эталон: **885** / **13**)
- [ ] Каждая command имеет `source_url` https
- [ ] Preview / import diff прошёл вычитку
- [ ] Staging curl / `verify-staging.ps1` OK

Откат prod: Admin prod → **Publish history** → Rollback. Подробно: [PRODUCTION.md](PRODUCTION.md) §4.

---

## 9. Если что-то сломалось

| Симптом | Действие |
| ------- | -------- |
| App не обновляется | Проверить manifest version; сеть на телефоне |
| Иконки не грузятся в app | Проверить `icon_url` в bundle (host должен резолвиться); **канон:** `cdn.alicecommands.ru/icons/v1/` |
| `cdn.alicecommands.ru` NXDOMAIN | Добавить A `cdn` → VPS в Cloudflare (DNS only) → `setup-cdn.ps1` |
| Publish failed | Admin error message; не трогать live; fix draft |
| «Команды без группы» в админке | Stale editorial вне seed → очистить `seed/data/editorial.json`; `push-draft.ps1`; см. CATALOG-FIXED-BUILD.md |
| API down | SSH VPS → `systemctl status alice-api`; см. [DEPLOYMENT.md](DEPLOYMENT.md) |
| Нужен откат | Rollback в admin |
| Import diff пустой | Ещё не было publish — diff vs published недоступен |

---

## 10. Command groups (schema v2)

Pilot и rollout grouped UI — см. [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md).

**Staging pilot (smart_home):**

1. Deploy backend (`deploy-staging.ps1`) — Flyway `V4__command_groups`
2. Import `seed/smart-home-groups-v2.json` (admin → Import или `push-draft.ps1`)
3. Admin → **Группы команд** → review порядок / primary / aliases
4. Publish → `verify-staging.ps1` (schema=2, groups count)
5. Android staging QA: pull-to-refresh, grouped UI в «Умный дом»

**Production:** после QA на staging и группировки остальных категорий — тот же deploy на prod + publish. Rollback через Publish history если schema v2 ломает старую app (не должна при tolerant parsing).

**Android release gate:** grouped UI + Room v5; `min_app_version` не поднимать до подтверждения QA.

---

## 11. Command of day

Editorial «команда дня» — см. [BACKEND-COMMAND-OF-DAY.md](BACKEND-COMMAND-OF-DAY.md).

1. Deploy backend (Flyway `V6__command_of_day`)
2. Admin → **Команда дня** → manual pin или auto по категории → Save draft
3. Publish → в bundle появится `command_of_day` с `resolved_date` = today (Europe/Moscow)
4. Staging QA: app sync → карточка «Команда дня»; offline rollover для auto — по app-плану

**Staging (historical snapshot as_of 2026-07-02):**

| Параметр | Значение |
| -------- | -------- |
| `content_version` | 20 |
| `mode` | `auto` |
| `auto_category_id` | `obscure` («Неочевидные команды», 34 команды) |
| Snapshot сегодня | `obscure_disko_podsvetka` — «Диско подсветка» |

> Актуальную версию проверяйте: `GET /v1/content/manifest` на staging.

```powershell
.\scripts\verify-staging.ps1   # включает smoke command_of_day
```

---

## 12. CDN и icon_url (политика v44+)

| Среда | `icon_url` host в bundle |
| ----- | ------------------------ |
| **Prod / staging app** | `https://cdn.alicecommands.ru/icons/v1/{slug}.svg` |
| **До DNS CDN** | Временно `staging-api.../icons/v1/` (legacy) |

**Однократно на VPS:**

```powershell
.\scripts\setup-cdn.ps1   # DNS + cert + ICON_PUBLIC_BASE_URL
```

**Проверка после publish:**

```powershell
curl -sS -o NUL -w "%{http_code}" https://cdn.alicecommands.ru/icons/v1/music_note.svg
```

Bundle `seed/catalog-audit-fixed.json` — **885** команд (prod live `content_version=10`, staging — свой счётчик). CDN host в bundle. См. [CONTENT-PRODUCT-ROADMAP.md](CONTENT-PRODUCT-ROADMAP.md), [CATALOG-FIXED-BUILD.md](CATALOG-FIXED-BUILD.md).

---

## 13. Продуктовый контент (не команды)

Чеклист перед publish v44+:

- [ ] 8 `scenario_templates` + `deep_link_hint`
- [ ] 8 `checklist_items` (УД optional в конце)
- [ ] `command_of_day` manual/auto в админке
- [ ] Flyway `V8__product_content_seed` → admin **Устройства** → publish smarthome snapshot
- [ ] `validateContent` + `validateSmartHomeDevices`

Подробно: [CONTENT-PRODUCT-ROADMAP.md](CONTENT-PRODUCT-ROADMAP.md), [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md) § продуктовый контент.

---

## 14. Smart home / Устройства

Отдельный publish flow — **не** часть content bundle publish.

1. Deploy backend (Flyway `V7`–`V10`)
2. Admin → **Устройства** → guides + picks (или `import-smarthome-payload.ps1 -Target staging`)
3. Каждый save auto-публикует `storage/manifest/smarthome_devices.json`
4. Verify:

```powershell
curl -sS https://staging-api.alicecommands.ru/v1/smarthome/devices | jq '.guides | length, .picks | length'
curl -sS -o NUL -w "%{http_code}" https://staging-api.alicecommands.ru/devices/v1/station.webp
```

5. Prod: `copy-staging-to-prod.ps1` или import на prod admin

См. [BACKEND-SMARTHOME-DEVICES.md](BACKEND-SMARTHOME-DEVICES.md).

**Affiliate blocks** — legacy; новые подборки через **picks** в разделе Устройства.

---

## 15. Analytics

Deploy backend с Flyway `V9__analytics_events.sql` (входит в обычный deploy).

1. Env в `.env` / `.env.prod`: `ANALYTICS_RATE_LIMIT_PER_IP`, `ANALYTICS_EVENTS_PER_IP_PER_DAY`, `ANALYTICS_MAX_BODY_BYTES`, `ANALYTICS_RAW_RETENTION_DAYS`
2. Smoke ingest (local или staging):

```powershell
curl -X POST https://staging-api.alicecommands.ru/v1/analytics/events/batch `
  -H "Content-Type: application/json" `
  -d '{"events":[{"installId":"00000000-0000-4000-8000-000000000001","sessionId":"00000000-0000-4000-8000-000000000002","eventId":"00000000-0000-4000-8000-000000000003","eventName":"screen_view","occurredAt":1710000000123,"appVersion":"1.0.0","params":{"route":"test"}}]}'
# → 202 {"accepted":1,...}
```

3. Admin → **Аналитика** / **События** — KPI и raw events
4. Prod: тот же endpoint на `https://api.alicecommands.ru`

P1 backlog: retention cleanup job, funnel/breakdown endpoints — см. [ANALYTICS-BACKEND.md](ANALYTICS-BACKEND.md).

---

*Эскалация разработке: логи `/var/log/alice-api/app.log`, таблица `publish_history`*
