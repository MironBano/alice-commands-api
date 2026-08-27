# ТЗ: Backend analytics batch — alice-commands-api

**Статус:** app ✅ готов · backend ✅ summary/events/funnel/breakdown + admin dashboard (staging deploy — см. RUNBOOK)  
**Клиент:** Android `AliceCommands` → `POST /v1/analytics/events/batch`  
**Репозиторий реализации:** этот репозиторий  
**Связанные документы (app repo):** `AliceCommands/docs/PRIVACY-AND-SUPPORT.md`, `domain/analytics/AnalyticsEvents.kt`, `data/remote/dto/AnalyticsBatchRequestDto.kt`

> Полный текст ТЗ синхронизирован с `AliceCommands/docs/ANALYTICS-BACKEND.md`. При расхождении — канон в app repo, затем перенос сюда.

---

## 1. Цель

Принимать батчи продуктовых событий с Android, хранить в PostgreSQL, показывать агрегаты и сырые события в admin UI. **Без PII:** нет текста поиска, email, телефона, ФИО.

AppMetrica остаётся отдельным каналом (Яндекс). Backend — второй источник истины для воронок, Pro, contextual picks, content_sync.

---

## 2. Текущее состояние

| Компонент | Статус |
|-----------|--------|
| Android outbox + flush | ✅ Room `analytics_outbox`, WorkManager, retry 10× |
| Android DTO | ✅ `AnalyticsBatchRequestDto` / `AnalyticsEventDto` |
| Endpoint на staging | ✅ после deploy |
| PostgreSQL таблицы | ✅ `V9__analytics_events.sql` |
| Admin UI | ✅ dashboard + events explorer |

---

## 3. Public API

### `POST /v1/analytics/events/batch`

**Auth:** нет (как `/v1/feedback`). Защита: rate limit по IP + dedup по `event_id` + валидация размера.

**Request** `Content-Type: application/json`:

```json
{
  "events": [
    {
      "installId": "uuid",
      "sessionId": "uuid",
      "eventId": "uuid",
      "eventName": "screen_view",
      "occurredAt": 1710000000123,
      "appVersion": "1.2.0",
      "androidVersion": "14",
      "locale": "ru-RU",
      "userProperties": {
        "persona": "smart_home",
        "is_pro": "false",
        "app_version": "1.2.0",
        "install_id": "uuid"
      },
      "params": {
        "route": "home/catalog"
      }
    }
  ]
}
```

**Response 202 Accepted:**

```json
{
  "accepted": 48,
  "duplicates": 2,
  "rejected": 0,
  "rejectedEventIds": []
}
```

При `rejected > 0` поле `rejectedEventIds` перечисляет UUID отклонённых событий (без значений params). Клиент удаляет accepted/duplicates и poison-drop'ает rejected.

| Код | Когда |
|-----|--------|
| 202 | Батч обработан (частичный reject допустим — см. ниже) |
| 400 | Невалидный JSON / пустой `events` / превышен лимит батча |
| 413 | Тело > `ANALYTICS_MAX_BODY_BYTES` (default 256 KB) |
| 429 | Rate limit по IP |
| 500 | Внутренняя ошибка |

**Правила валидации (P0):**

| Поле | Правило |
|------|---------|
| `events` | 1…50 элементов (совпадает с app `batchSize`) |
| `eventId` | UUID, уникален глобально → `INSERT … ON CONFLICT DO NOTHING` |
| `eventName` | `[a-z0-9_]{1,64}` |
| `installId`, `sessionId` | UUID |
| `occurredAt` | Unix ms, не в будущем >5 мин, не старше 30 дней |
| `appVersion` | ≤32 символов |
| `androidVersion` | ≤16 |
| `locale` | ≤16 |
| `userProperties`, `params` | JSON object, ≤32 ключей каждый, ключ ≤64, значение ≤512 |
| Запрещённые ключи в params | **Точное** совпадение ключа (case-insensitive): `query`, `message`, `email`, `phone`, `text`, `search_query`. Разрешены метрики без текста: `query_length`, `message_length`, `results_count`. Substring-match **не** используется (раньше резал `query_length`). |

**Rate limit (P0):** переиспользовать `PublicSubmissionRateLimiter` или отдельный счётчик:

- `ANALYTICS_RATE_LIMIT_PER_IP` = 120 запросов / 15 мин (env)
- `ANALYTICS_EVENTS_PER_IP_PER_DAY` = 10_000 (env, soft cap → 429)

**Idempotency:** повторная отправка того же `eventId` из outbox клиента — не ошибка, `duplicates++`.

---

## 4. Схема БД (Flyway)

### `V9__analytics_events.sql`

```sql
CREATE TABLE analytics_events (
    event_id         TEXT PRIMARY KEY,
    install_id       TEXT NOT NULL,
    session_id       TEXT NOT NULL,
    event_name       TEXT NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    received_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    app_version      TEXT,
    android_version  TEXT,
    locale           TEXT,
    user_properties  JSONB NOT NULL DEFAULT '{}',
    params           JSONB NOT NULL DEFAULT '{}',
    client_ip        TEXT
);

CREATE INDEX idx_analytics_events_occurred_at ON analytics_events (occurred_at DESC);
CREATE INDEX idx_analytics_events_event_name_occurred ON analytics_events (event_name, occurred_at DESC);
CREATE INDEX idx_analytics_events_install_id ON analytics_events (install_id);
CREATE INDEX idx_analytics_events_session_id ON analytics_events (session_id);
```

### `analytics_daily_rollup` (P1, опционально materialized)

Предагрегация для дашборда: `date`, `event_name`, `count`, `unique_installs`.

### Retention (P1)

- Env `ANALYTICS_RAW_RETENTION_DAYS` = 90
- Cron/job: удаление `occurred_at < now() - interval`

---

## 5. Архитектура сервера (Light Clean)

По образцу `SubmitFeedbackUseCase` / `FeedbackUseCases.kt`:

```
routes/PublicRoutes.kt          → POST /v1/analytics/events/batch
application/analytics/
  SubmitAnalyticsBatchUseCase.kt
  ListAnalyticsEventsUseCase.kt
  AnalyticsDashboardUseCase.kt
domain/
  AnalyticsEvent.kt, SubmitAnalyticsBatchRequest/Response
  ports: AnalyticsEventRepository, AnalyticsRateLimiter
infrastructure/persistence/
  AnalyticsEventsTable (Exposed)
  AnalyticsEventRepositoryImpl
routes/AdminRoutes.kt           → /admin/api/analytics/*
```

**SubmitAnalyticsBatchUseCase:**

1. Resolve `clientIp`
2. Rate limit check
3. Validate each event; split accepted / rejected
4. Bulk insert accepted (`ON CONFLICT (event_id) DO NOTHING`)
5. Return counts
6. **Лог ingest:** при `rejected > 0` — slf4j INFO с counts по причинам (`pii_key`, `bad_name`, …) **без** значений params/userProperties

---

## 6. Admin API (session auth, как feedback)

> **Реализовано:** summary (+ daily series) · events explorer · funnel · breakdown.  
> **Остаётся P1 ops:** retention purge job, daily rollup table.

| Method | Path | Статус | Назначение |
|--------|------|--------|------------|
| GET | `/admin/api/analytics/summary?from=&to=` | ✅ | KPI: `daily_active_installs`, `avg_dau`, events, `new_installs` (=Σ daily), `unique_installs` (API), top events, `daily[]` |
| GET | `/admin/api/analytics/events?from=&to=&event_name=&install_id=&limit=100&offset=0` | ✅ | Список сырых событий |
| GET | `/admin/api/analytics/funnel?steps=...&from=&to=` | ✅ | Воронка по distinct `install_id` |
| GET | `/admin/api/analytics/breakdown?event_name=...&param=...&from=&to=&field_source=params` | Top values параметра из `params` (default) или `user_properties` (`field_source=user_properties` для `is_pro`, `persona`, …) |

**Ограничение периода:** `from`/`to` — ISO dates, календарные сутки **Europe/Moscow**; inclusive days ≤ `ANALYTICS_RAW_RETENTION_DAYS` (default **90**), иначе 400.

**Summary response (пример):**

```json
{
  "from": "2026-07-01",
  "to": "2026-07-08",
  "daily_active_installs": 412,
  "total_events": 18340,
  "unique_installs": 890,
  "raw_unique_installs": 940,
  "new_installs": 95,
  "avg_dau": 51.5,
  "days_in_range": 8,
  "top_events": [
    { "event_name": "screen_view", "count": 5200 },
    { "event_name": "ui_click", "count": 3100 }
  ],
  "daily": [
    { "date": "2026-07-01", "events": 2100, "dau": 50, "unique_installs": 80, "new_installs": 12 },
    { "date": "2026-07-02", "events": 2300, "dau": 55, "unique_installs": 90, "new_installs": 8 }
  ]
}
```

**Funnel response (пример):**

```json
{
  "from": "2026-07-01",
  "to": "2026-07-08",
  "steps": [
    { "event_name": "paywall_view", "installs": 120, "conversion_from_previous": null, "conversion_from_first": null },
    { "event_name": "pro_purchase_start", "installs": 40, "conversion_from_previous": 33.3, "conversion_from_first": 33.3 },
    { "event_name": "pro_activated", "installs": 18, "conversion_from_previous": 45.0, "conversion_from_first": 15.0 }
  ]
}
```

Default `steps`: `paywall_view,pro_purchase_start,pro_activated`.  
Каждый шаг — **независимый** `COUNT(DISTINCT install_id)` (не sequential cohort); conversion может быть &gt;100% при пересечении аудиторий.

**Breakdown response (пример):**

```json
{
  "from": "2026-07-01",
  "to": "2026-07-08",
  "event_name": "ui_click",
  "param": "element_id",
  "field_source": "params",
  "items": [
    { "value": "home_command_card", "count": 420 },
    { "value": "paywall_cta", "count": 95 }
  ]
}
```

**Uniqueness:**

| Поле | Смысл |
|------|--------|
| `unique_installs` | Canonical: dominant `install_id` per `session_id` (max event count), затем `COUNT(DISTINCT)`. Схлопывает ghost id из client race на cold start при **общем** `session_id`. |
| `raw_unique_installs` | Сырой `COUNT(DISTINCT install_id)` без dedup — для диагностики. |
| `new_installs` | Сумма `daily[].new_installs` за период (удобный aggregate). |
| `daily_active_installs` | `COUNT(DISTINCT install_id)` где `event_name = daily_active` **за весь период** (UI: «Открывали приложение»). |
| `avg_dau` | Среднее `daily[].dau` по `days_in_range`. |
| `days_in_range` | Число inclusive календарных дней (= `daily.size`). |
| `daily[].unique_installs` | Raw `COUNT(DISTINCT install_id)` с **любым** событием в этот день. Не «новые». |
| `daily[].new_installs` | Число `install_id`, у которых **первый** календарный день (Europe/Moscow) в analytics = этот день. |

---

## 7. Admin UI (`admin-web/`)

Alpine.js, один пункт сайдбара **«Аналитика»**.

### Dashboard (`view=analytics`)

1. **Общий date bar:** пресеты 7 / 30 / 90 = последние N **inclusive** календарных дней + custom `from`/`to` (≤ retention)
2. **Сутки:** Europe/Moscow
3. **Вкладки:**
   - **Обзор** — Открывали приложение, В среднем за день, События, Новые установки; top events с фильтром «Действия / Все» и русскими подписями
   - **Тренд** — CSS bar chart по `daily[]` (нули без высоты; прореживание подписей при &gt;31 дне)
   - **Воронка** — пресеты (CoD, Поиск, Сценарии, Виджет, First value, Pro, picks, TTS) + editable `steps`; шаги **независимые**, не sequential cohort
   - **Разбивка** — top values `params[param]` или `user_properties[param]` (`field_source`); пресеты source/category/УД/picks/Pro
   - **События** — raw explorer
   - **Справка** — метрики, ingest, FAQ (`pro_restore` и др.), глоссарий

Операторский канон пояснений: [ANALYTICS-GLOSSARY.md](ANALYTICS-GLOSSARY.md).

### P2

- Экспорт CSV за период
- Alerts (опционально)
- Таблица `analytics_daily_rollup` + purge job

---

## 8. Каталог событий (контракт с app)

**Глоссарий с русскими описаниями и FAQ:** [ANALYTICS-GLOSSARY.md](ANALYTICS-GLOSSARY.md).

Источник констант в app: `AliceCommands/.../AnalyticsEvents.kt`. Основные группы:

| Группа | Примеры `event_name` |
|--------|----------------------|
| Navigation | `screen_view`, `tab_select`, `ui_click`, `smarthome_tab_select`, `filter_change`, `category_click` |
| Lifecycle | `session_start`, `session_end`, `daily_active`, `app_foreground` |
| Commands | `command_view` (+`source`), `command_tts`, `command_copy`, `command_share`, `favorite_add`, `favorite_remove` |
| Favorites lists | `favorite_list_create`, `favorite_list_delete` |
| Search | `search` (`query_length`, `results_count`, optional `category_id`), `search_result_click` |
| CoD / scenarios | `cod_impression`, `cod_open`, `scenario_open` |
| Widget / deeplink | `widget_shown`, `widget_open`, `deeplink_open` (`source=external\|widget`) |
| Monetization | `paywall_view`, `pro_gate_shown`, `pro_purchase_start`, `pro_activated`, `pro_restore` |
| Rating | `rating_prompt_shown`, `rating_star_selected` |
| Content | `content_sync` (+ `trigger`, `phase`, `success`) |
| Affiliate / picks | `contextual_pick_section_shown`, `contextual_pick_impression`, `contextual_pick_click` (не `affiliate_click`) |
| Errors | `app_error_non_fatal`, `billing_error`, `bootstrap_error`, `ads_error` |

**Итерация 2 (покрытие):** канон имён и params — [ANALYTICS-GLOSSARY.md](ANALYTICS-GLOSSARY.md); emit в app `AnalyticsEvents.kt`. Zero-results через `search.results_count=0`. Воронки в admin — независимые counts, не sequential cohort.

**User properties:** `persona`, `is_pro`, `app_language`, `theme_mode`, `content_version`, `install_id` — см. `AnalyticsUserProperties.kt`.

Admin UI не хардкодит полный enum в API — словарь подписей в `admin-web/js/admin.js` + glossary doc.

---

## 9. Безопасность и compliance

- Не логировать полное тело батча в prod logs
- IP хранить для abuse detection; не показывать в admin UI по умолчанию
- Policy уже описывает backend batch (`privacy_policy_*.txt`)
- **Не коммитить** prod secrets; опционально P1: `ANALYTICS_INGEST_TOKEN` header (потребует обновление app)

---

## 10. Тесты

| Тест | Тип |
|------|-----|
| Valid batch → 202, rows in DB | Integration (`ApiIntegrationTest`) |
| Duplicate `eventId` → `duplicates=1` | Integration |
| Invalid `eventName` → rejected, 202 с `rejected>0` | Unit |
| Rate limit → 429 | Integration |
| Admin summary auth 401 без cookie | Integration |
| Flyway migration up/down | CI |

---

## 11. Env / конфиг

```env
ANALYTICS_RATE_LIMIT_PER_IP=120
ANALYTICS_EVENTS_PER_IP_PER_DAY=10000
ANALYTICS_MAX_BODY_BYTES=262144
ANALYTICS_RAW_RETENTION_DAYS=90
```

---

## 12. Деплой

1. Flyway migrate на staging
2. Deploy server + admin-web
3. Обновить `docs/API.md` (раздел Public + Admin)
4. Smoke: `curl -X POST https://staging-api.alicecommands.ru/v1/analytics/events/batch -H 'Content-Type: application/json' -d '{"events":[...]}'`
5. Проверить admin dashboard
6. Prod после staging 24–48 ч

---

## 13. Definition of Done

- [x] `POST /v1/analytics/events/batch` на staging возвращает 202
- [ ] Android flush outbox успешен (нет роста `retryCount` в логах)
- [x] События видны в admin Explorer ≤1 мин после отправки
- [x] Dashboard показывает DAU, top events, тренд, воронку, breakdown
- [x] Rate limit и dedup работают
- [x] `docs/API.md` + `server/README.md` обновлены
- [x] Integration tests green
- [ ] Retention purge job

---

## 14. Оценка

| Фаза | Scope | Оценка |
|------|-------|--------|
| P0 | API + DB + ingest + admin list/summary | ✅ |
| P1 UI/API | Funnel, breakdown, daily series, date range ≤90 | ✅ |
| P1 ops | Retention purge job, daily rollup | остаётся |
| P2 | CSV export, ingest token | 1 день |

---

## 15. Ссылки на код клиента

```
AliceCommands/app/src/main/java/ru/appforsale/alicecommands/
  data/remote/dto/AnalyticsBatchRequestDto.kt
  data/remote/AnalyticsService.kt
  domain/usecase/FlushAnalyticsOutboxUseCase.kt
  domain/analytics/AnalyticsEvents.kt
```
