# Runbook — публикация контента

**Для:** владелец продукта (без знания backend)  
**Цель:** выпустить новую версию каталога в app

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

## 4. Import seed (первый раз / dev)

| Файл | Когда |
| ---- | ----- |
| `seed/smart-home-groups-v2.json` | Первый pilot (Умный дом + groups), пустая БД |
| `seed/full-catalog.json` | После content pipeline, полный каталог |

1. Admin → **Import**
2. Upload JSON
3. Просмотрите **Diff vs опубликованная версия**
4. Для регулярного pipeline — **Sync** (default в `push-draft.ps1`); **Replace all** только на пустой staging
5. Publish

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
2. Обновите ссылки / ERID
3. Сохранение блока сразу обновляет `GET /v1/affiliate/blocks`; общий **Publish** каталога не нужен
4. Проверьте `GET /v1/affiliate/blocks` и в app: Умный дом → маркировка «Реклама»

---

## 7. Content pipeline (staging)

Автоматизация **без** auto-publish:

```powershell
Copy-Item scripts\.env.example scripts\.env   # STAGING_API_URL, credentials
.\scripts\update-content.ps1
# → pipeline → validate → push draft sync + pipeline-sync → verify manifest
# Далее: admin → очередь editorial → diff → Publish
# После Publish: python tools/content/pipeline_run.py --skip-fetch --finalize-baseline
```

Подробнее: [CONTENT-UPDATE.md](CONTENT-UPDATE.md).

---

## 8. Чеклист перед prod publish (store release)

- [ ] ≥ 300 команд, 13+ категорий
- [ ] Каждая command имеет `source_url` https
- [ ] Preview / import diff прошёл вычитку
- [ ] Staging curl / `verify-staging.ps1` OK
- [ ] Android staging flavor sync OK
- [ ] Publish на **prod** (не staging)
- [ ] App release build указывает prod URL

---

## 9. Если что-то сломалось

| Симптом | Действие |
| ------- | -------- |
| App не обновляется | Проверить manifest version; сеть на телефоне |
| Иконки не грузятся в app | Проверить `icon_url` в bundle (host должен резолвиться); staging: `staging-api.../icons/v1/` |
| `cdn.alicecommands.ru` NXDOMAIN | Добавить A `cdn` → VPS в Cloudflare (DNS only) → `setup-cdn.ps1` |
| Publish failed | Admin error message; не трогать live; fix draft |
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

**Staging (проверено 2026-07-02):**

| Параметр | Значение |
| -------- | -------- |
| `content_version` | 20 |
| `mode` | `auto` |
| `auto_category_id` | `obscure` («Неочевидные команды», 34 команды) |
| Snapshot сегодня | `obscure_disko_podsvetka` — «Диско подсветка» |

```powershell
.\scripts\verify-staging.ps1   # включает smoke command_of_day
```

---

*Эскалация разработке: логи `/var/log/alice-api/app.log`, таблица `publish_history`*
