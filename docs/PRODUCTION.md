# Production — live (пользовательский релиз)

**Статус:** **LIVE** — приложение AliceCommands в RuStore читает **prod API**  
**as_of:** 2026-07-13  
**idea_ref:** MOB-20260626-001

> **С этого момента prod обслуживает реальных пользователей.** Любое изменение backend, контента или схемы — только через staging → проверка → prod. Прямые эксперименты на prod **запрещены**.

---

## 1. Текущее состояние prod (снимок)

Проверка live: `.\scripts\verify-prod.ps1` · manifest: `GET https://api.alicecommands.ru/v1/content/manifest`

| Компонент | Значение (2026-07-13) |
| --------- | --------------------- |
| Public API | https://api.alicecommands.ru |
| Health | `GET /health` → `{"status":"ok"}` |
| Catalog `schema_version` | **2** |
| Catalog `content_version` | **10** (счётчик prod; staging — свой) |
| Команды / категории / группы | **885** / **13** / **80** |
| Smarthome | **35** guides, **36** picks |
| Рефералы picks | `market.yandex.ru/cc/*` (без `price_hint_ru`) |
| Android release default | `https://api.alicecommands.ru` |
| Staging (pre-prod QA) | https://staging-api.alicecommands.ru |
| CDN icons | https://cdn.alicecommands.ru/icons/v1/ |
| systemd | `alice-api-prod` (:8081), БД `alice_commands` |

**Синхронизация контента со staging:** на момент первого релиза prod и staging отдают **одинаковую полезную нагрузку** (команды, picks, guides, cc-ссылки). Различаются `content_version`, `published_at`, `bundle_sha256` (метаданные publish).

---

## 2. Золотое правило изменений

```
staging → verify → copy-staging-to-prod → verify-prod
```

| Тип изменения | Обязательный путь |
| ------------- | ----------------- |
| **Контент каталога** | Правки на staging (admin или `push-draft.ps1`) → Publish → `verify-staging.ps1` → `copy-staging-to-prod.ps1` → `verify-prod.ps1` |
| **Smarthome (guides/picks)** | `import-smarthome-payload.ps1 -Target staging` → проверка `/v1/smarthome/devices` → `copy-staging-to-prod.ps1` (или import prod после полного QA) |
| **Backend / admin-web** | `deploy-staging.ps1` → smoke на staging → `deploy-prod.ps1` → `verify-prod.ps1` |
| **Только код без контента** | Staging deploy + `:server:test` → prod deploy |

**Не делать на prod первым:** Import replace, Publish, массовые правки picks, эксперименты с schema.

---

## 3. Команды оператора (routine)

```powershell
# После QA на staging — выровнять prod
.\scripts\verify-staging.ps1
.\scripts\copy-staging-to-prod.ps1
.\scripts\verify-prod.ps1

# Деплой backend (после staging)
.\scripts\deploy-staging.ps1
.\scripts\deploy-prod.ps1
.\scripts\verify-prod.ps1
```

`copy-staging-to-prod.ps1` по умолчанию идёт через VPS (`copy-staging-to-prod-remote.sh`): bundle + smarthome из канона `seed/smarthome-devices-full.json`, two-pass guides, SQL-clear legacy picks.

---

## 4. Откат

| Симптом | Действие |
| ------- | -------- |
| Плохой publish каталога | Admin **prod** → Publish history → **Rollback** (5 последних bundle) |
| Плохой smarthome | Re-run `copy-staging-to-prod.ps1` с known-good staging; или rollback picks через admin |
| Сломан backend | `deploy-prod.ps1` с предыдущим коммитом; `journalctl -u alice-api-prod` |
| Критический инцидент | App override на staging URL (только debug/emergency); **не** трогать staging DB |

---

## 5. Совместимость с app (нельзя ломать)

- **Public `/v1/*`** — только published bundle/manifest/smarthome snapshot; без breaking changes в JSON без `min_app_version`.
- **Schema v2** — `command_groups`, `command_of_day`, category visuals: app tolerant parsing; не удалять обязательные поля без согласования с Android.
- **Delta sync** — `GET /v1/content/delta` должен оставаться консистентным после publish.
- **Smarthome** — `guides[].detail_referral_pick_ids` computed at publish; picks без `price_hint_ru` (поле deprecated в UI).
- **Analytics** — `POST /v1/analytics/events/batch` → 202; не ужесточать rate limit без замера.

Перед изменением контракта: `docs/API.md`, `docs/SCHEMA-SYNC.md`, PR в Android repo.

---

## 6. Чеклист перед любым prod-изменением

- [ ] Изменение протестировано на **staging**
- [ ] `.\gradlew.bat :server:test` (для кода)
- [ ] `validateContent` / `validateSmartHomeDevices` (для seed JSON)
- [ ] `verify-staging.ps1` green
- [ ] `copy-staging-to-prod.ps1` или осознанный prod-only deploy с rollback-планом
- [ ] `verify-prod.ps1` green
- [ ] Сравнить manifest: команды/picks не пропали (counts ≥ эталона)

---

## 7. Мониторинг (минимум)

```powershell
.\scripts\verify-prod.ps1
curl.exe -sS https://api.alicecommands.ru/health
curl.exe -sS https://api.alicecommands.ru/v1/content/manifest
curl.exe -sS https://api.alicecommands.ru/v1/smarthome/devices | findstr /C:"pick_station"
```

На VPS: `journalctl -u alice-api-prod -f`, `systemctl status alice-api-prod nginx`.

---

## 8. Связанные документы

| Документ | Назначение |
| -------- | ---------- |
| [PROD-CUTOVER.md](PROD-CUTOVER.md) | Исторический cutover checklist (завершён) |
| [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) | Публикация контента, rollback |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | VPS, DNS, порты, пути |
| [DEPLOYMENT.md](DEPLOYMENT.md) | nginx, systemd, CI |
| [API.md](API.md) | Контракт для Android |
| [CATALOG-FIXED-BUILD.md](CATALOG-FIXED-BUILD.md) | Канон каталога |
| [RELEASE-AUDIT-v885.md](RELEASE-AUDIT-v885.md) | Аудит 885 команд |

---

*Первый пользовательский релиз: 2026-07-13. Prod content_version=10, staging live — отдельный счётчик.*
