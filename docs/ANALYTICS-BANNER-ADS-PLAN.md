# План: метрика показов баннерной рекламы (РСЯ)

**Цель:** считать **среднее число показов bottom-баннера на 1 пользователя в день** (и смежные KPI fill rate / CTR) из собственной analytics, без догадок по `session_start` / `screen_view`.

**Статус:** P0 реализовано (app emit + glossary/admin). P1/P2 — по плану ниже.  
**Связанные:** [ANALYTICS-GLOSSARY.md](ANALYTICS-GLOSSARY.md), [ANALYTICS-BACKEND.md](ANALYTICS-BACKEND.md), [ANALYTICS-COVERAGE-ITER2.md](ANALYTICS-COVERAGE-ITER2.md)  
**App-канон:** `AliceCommands/.../AnalyticsEvents.kt`, emit в `YandexAdsGateway` (banner callbacks).

---

## 1. Зачем

Сейчас по bottom-баннеру в product analytics есть только `ads_error` (ошибки).  
Rewarded и feed на «Поддержать автора» уже инструментированы (`ads_rewarded_*`, `ads_feed_*`). Bottom banner — нет.

Без `ads_banner_shown` нельзя ответить:

- сколько **реальных** показов баннера на free-пользователя в день;
- какой fill rate (`shown` / `request`);
- как refresh (≈35 с) влияет на объём impressions vs DAU.

`announcement_impression` — **другой** баннер (редакционный на «Ещё»), не РСЯ.

---

## 2. Целевая метрика (DoD продукта)

| Метрика | Формула | Комментарий |
|---------|---------|-------------|
| **Показы / пользователь / день** | `COUNT(ads_banner_shown) / COUNT(DISTINCT install_id с daily_active)` за день (или среднее по дням периода) | Базовый KPI |
| Показы / **free** DAU / день | то же, фильтр `user_properties.is_pro=false` (или только install_id без Pro в день) | Баннер не показывается Pro |
| Fill rate | `COUNT(ads_banner_shown) / COUNT(ads_banner_request)` | Качество загрузки |
| CTR | `COUNT(ads_banner_clicked) / COUNT(ads_banner_shown)` | Опционально P1 |
| Fail rate | `COUNT(ads_banner_failed) / COUNT(ads_banner_request)` | Диагностика |

**Не путать:** `onAdLoaded` ≠ показ. Канон «показа» — callback **`onImpression`** SDK (как у feed: `FeedAdEvent.Impression` → `ads_feed_shown`).

---

## 3. Контракт событий (app + docs sync)

Зеркало семейства `ads_feed_*` / `ads_rewarded_*`:

| `event_name` | Когда emit | Params |
|--------------|------------|--------|
| `ads_banner_request` | Каждый вызов `loadAd` / старт загрузки (в т.ч. refresh) | `placement` |
| `ads_banner_shown` | `BannerAdEventListener.onImpression` | `placement` |
| `ads_banner_clicked` | `onAdClicked` | `placement` |
| `ads_banner_failed` | `onAdFailedToLoad` / init fail; рядом с generic `ads_error` (см. §8 — оба допустимы) | `placement`, `error_type` |

**Константы (предложение):**

```text
ADS_BANNER_REQUEST = "ads_banner_request"
ADS_BANNER_SHOWN   = "ads_banner_shown"
ADS_BANNER_CLICKED = "ads_banner_clicked"
ADS_BANNER_FAILED  = "ads_banner_failed"
PLACEMENT_MAIN_BOTTOM = "main_bottom"   // новый placement рядом с support_author
```

Опционально (P2, не блокирует KPI): `duration_ms` на успешный load до impression; не логировать ad unit id в params (секрет / шум).

### Правила подсчёта

1. **Каждый impression считается** — включая refresh каждые ~35 с (`BannerAdDefaults.REFRESH_INTERVAL_MS`). Дедуп по сессии **не** делать: иначе KPI занизит монетизацию.
2. Emit **только** если баннер реально создаётся (`isAdsEnabled`, non-empty ad unit, `!isPro`, chrome виден). Не слать `request` при Pro / ads off.
3. `build_type=debug` можно фильтровать в admin (как сейчас); demo-banner в debug не смешивать с prod-отчётами без фильтра.
4. Не дублировать через `ui_click` / `screen_view`.

---

## 4. Где менять код (AliceCommands)

| Слой | Файл / место | Действие |
|------|--------------|----------|
| Константы | `domain/analytics/AnalyticsEvents.kt` | Добавить `ADS_BANNER_*`, `PLACEMENT_MAIN_BOTTOM` |
| Gateway | `data/gateway/YandexAdsGateway.kt` | В listener баннера: request / impression / click / failed → `analyticsGateway.logEvent` (или существующий track path, как у `reportAdError`) |
| Refresh | `requestLoad` / schedule refresh | Каждый `loadAd` → `ads_banner_request` |
| No-op | `NoOpAdsGateway` | Без событий (ok) |
| Тесты | unit на gateway / thin wrapper | Verify mapping callback → event name + placement; refresh шлёт повторный request |

UI (`BannerAd.kt`, `MainShell`) **не** обязан знать analytics — предпочтительно держать emit в gateway (единая точка с SDK callbacks).

---

## 5. Backend / admin (alice-commands-api)

Новых таблиц и эндпоинтов **не нужно** — raw `analytics_events` + summary/breakdown уже умеют считать по `event_name`.

| Артефакт | Изменение |
|----------|-----------|
| `docs/ANALYTICS-GLOSSARY.md` | Строка про `ads_banner_*` + формула KPI; отличить от `announcement_impression` |
| `docs/ANALYTICS-BACKEND.md` | Monetization: `ads_banner_*` рядом с `ads_rewarded_*` / упомянуть `ads_feed_*` |
| `admin-web/js/admin.js` | Labels + tip; пресет воронки «Баннер РСЯ»: `ads_banner_request,ads_banner_shown,ads_banner_clicked` |
| Карточка KPI на обзоре | **P2** (опционально): «Показы баннера / DAU» — иначе достаточно Events explorer + Excel/SQL по export |

### SQL-ориентир (ручная проверка после релиза)

```sql
-- Показы на DAU за календарный день (Europe/Moscow)
WITH days AS (
  SELECT date_trunc('day', occurred_at AT TIME ZONE 'Europe/Moscow')::date AS d,
         COUNT(*) FILTER (WHERE event_name = 'ads_banner_shown') AS shows,
         COUNT(DISTINCT install_id) FILTER (WHERE event_name = 'daily_active') AS dau
  FROM analytics_events
  WHERE occurred_at >= :from AND occurred_at < :to
  GROUP BY 1
)
SELECT d, shows, dau, ROUND(shows::numeric / NULLIF(dau, 0), 2) AS shows_per_dau
FROM days
ORDER BY d;
```

Для free-only: join/filter по последнему `user_properties.is_pro` или breakdown `field_source=user_properties` в admin.

---

## 6. Этапы внедрения

### P0 — измеримость (1 небольшой PR в app + sync docs/admin)

1. Константы `ads_banner_*` + `placement=main_bottom`.
2. Emit из `YandexAdsGateway` banner listener (request / shown / clicked / failed).
3. Unit-тесты на маппинг событий.
4. Glossary + admin labels + funnel preset.
5. Staging/release smoke: debug/staging build → Events explorer видит имена, `rejected=0`.

### P1 — отчётность

1. Зафиксировать в glossary формулу «показов / DAU / день».
2. (Опционально) короткий SQL/скрипт export в `tmp-analytics-export` runbook рядом с существующим analytics export.
3. Сверить порядок величины с кабинетом РСЯ (impressions) — sanity, не 1:1 по определению.

### P2 — удобство

1. Карточка/серия на admin overview.
2. Сегменты: `is_pro`, `app_version`, session length vs shows.
3. При необходимости — отдельный admin endpoint `ads_banner_kpi` (не обязателен, если хватает raw).

---

## 7. Acceptance checklist

- [x] В `AnalyticsEvents.kt` есть `ads_banner_*` и `PLACEMENT_MAIN_BOTTOM`.
- [x] `onImpression` → ровно `ads_banner_shown` (не `onAdLoaded`).
- [x] Refresh шлёт новый `request` + при успехе новый `shown`.
- [x] Pro / ads disabled → нулевой emit баннера.
- [x] Glossary отличает РСЯ-баннер от `announcement_impression` и от `ads_feed_*`.
- [x] Admin: русские labels + воронка.
- [ ] После 1–2 дней prod-данных: можно назвать число «показов / DAU / день» без оценочных допущений.

---

## 8. Вне скоупа

- Interstitial analytics (отдельное семейство, если появится).
- Изменение частоты refresh / UX баннера.
- Замена `ads_error` (оставить для generic errors; `ads_banner_failed` — product-funnel).
- Монетизационные прогнозы в canvas — после P0 на реальных `shown`.

---

## 9. Оценка объёма

| Работа | Где | Оценка |
|--------|-----|--------|
| Emit + константы + тесты | AliceCommands | S (полдня) |
| Glossary + admin labels/funnel | alice-commands-api | XS |
| KPI-карточка overview | alice-commands-api | S (P2) |

Блокеров на стороне API ingest нет: неизвестные `event_name` уже принимаются batch API.
