# Analytics glossary — alice-commands-api

Пояснения для вкладки **«Как читать»** в админке.  
Константы в приложении: `AliceCommands/.../AnalyticsEvents.kt`.

Связанные: [ANALYTICS-BACKEND.md](ANALYTICS-BACKEND.md), [ANALYTICS-COVERAGE-ITER2.md](ANALYTICS-COVERAGE-ITER2.md) (чек-лист emit app), [ADMIN-UX.md](ADMIN-UX.md).

---

## Карточки на обзоре

| Карточка | Поле API | Точная формула |
|----------|----------|----------------|
| Открывали приложение | `daily_active_installs` | `COUNT(DISTINCT install_id)` где `event_name = daily_active` за период |
| В среднем за день | `avg_dau` | среднее `daily[].dau` по всем календарным дням периода (включая нули) |
| События | `total_events` | `COUNT(*)` событий за период |
| Новые установки | `new_installs` | сумма `daily[].new_installs`: `install_id`, у которых **первый** день в analytics попадает в период |

Поле `unique_installs` / `raw_unique_installs` остаётся в API (distinct id с любым событием) — на обзоре не показывается.

**Период:** `from`/`to` — календарные дни Europe/Moscow (начало суток … конец суток). Кастомный диапазон в UI — ISO `YYYY-MM-DD`; после загрузки summary в шапке показываются `summary.from` / `summary.to` с сервера.

### График «По дням»

| Серия | Поле | Точная формула |
|-------|------|----------------|
| События | `daily[].events` | `COUNT(*)` за календарный день (Europe/Moscow) |
| Открывали приложение | `daily[].dau` | `COUNT(DISTINCT install_id)` с `daily_active` в этот день |
| Новые установки | `daily[].new_installs` | `install_id`, у которых **первый** день в analytics = этот день; сумма = `summary.new_installs` |

Дни — **Europe/Moscow**. Период просмотра ≤ `ANALYTICS_RAW_RETENTION_DAYS` (90).

---

## Почему в топе странные события

- **`pro_restore` (проверка покупок)** — приложение само проверяет покупки при каждом запуске. Это не «люди жмут Восстановить».
- **`content_sync` / `app_foreground`** — служебные действия при открытии. В топе это нормально.

---

## События, которые легко перепутать

Подписи в UI (`admin-web/js/admin.js`) — человеческим языком. Ниже только то, где название само себя не объясняет:

| Код | Как видно | Важно знать |
|-----|-----------|-------------|
| `daily_active` | Открыл приложение сегодня | Один раз в день на установку |
| `pro_restore` | Проверка покупок | Авто при запуске |
| `pro_activated` | Pro включился | После покупки или восстановления |
| `content_sync` | Скачал каталог | Часто при старте |
| `app_foreground` | Вернулся в приложение | Часто при каждом возврате |
| `time_in_app_tick` | Тик «время в приложении» | Служебный счётчик |
| `ui_click` | Нажал элемент | В «Что нажимали» можно разобрать кнопки |
| `search` | Поиск | Текст запроса **не** сохраняется — только `query_length`, `results_count`, опционально `device_type` / `category_id`. Zero-results = `results_count=0` (отдельного event нет). Канон: **`search`** (без alias `search_query`). |
| `search_result_click` | Клик результата | `command_id`, `position`; не путать с `command_view`. |
| `screen_view` / `route` | Экран | `route` — **конкретный** путь (`category/music`, `onboarding/welcome`), не шаблон NavHost. |
| `command_view` / `source` | Открыл команду | `source`: `catalog_cod` \| `quick` \| `search` \| `history` \| `related` \| `favorites` \| … |
| `command_tts` / `command_copy` | Озвучил / скопировал | Параметр `source` (как у `command_view`). Для **ранга популярных** TTS с `source` ∈ `try_now` / `quick` / `search` **исключаются** (anti-loop). См. [BACKEND-POPULAR-COMMANDS.md](BACKEND-POPULAR-COMMANDS.md). |
| `command_share` | Поделился | First-class; **не** дублировать через `ui_click command_share`. |
| `cod_impression` / `cod_open` | Команда дня | Impression карточки → открытие; TTS CoD — `command_tts` + `source=catalog_cod`. |
| `scenario_open` | Открыл сценарий | `template_id`; TTS шагов — `command_tts` + `scenario_id` (без фейкового `command_id`). |
| `category_click` | Клик категории с каталога | `category_id`, `featured=true\|false`. |
| `filter_change` | Чипы фильтра | `screen=category`, `group_id` и/или `device_type`. |
| `smarthome_tab_select` | Внутренний таб УД | `tab=commands\|templates\|devices`. |
| `favorite_remove` | Снял из избранного | Пара к `favorite_add`. |
| `favorite_list_create` / `favorite_list_delete` | CRUD списков | `list_id` без названия списка. |
| `widget_shown` / `widget_open` | Виджет | Open также через `deeplink_open` с `source=widget` (warm+cold). |
| `contextual_pick_click` | Клик по pick | **Канон воронки picks**; не `ui_click`. Deprecated: `affiliate_click`, `device_pick_click`. |
| `announcement_impression` | Увидел баннер (Ещё) | Редакционный баннер на «Ещё»; `announcement_id`, `revision`, `placement` (обычно `more`). **Не** РСЯ. CTA — `ui_click` + `element_id=more_announcement_cta` (см. воронку «Баннер (Ещё)»). |
| `popular_section_shown` | Блок «Популярное» | `screen`: `search` \| `quick` \| `catalog_try_now`. |
| `support_shop_open` | Магазин поддержки | Экран «Поддержать автора»; не `affiliate_click`. |
| `ads_rewarded_*` | Rewarded-реклама | `ads_rewarded_request` → `shown` → `earned` (воронка «Rewarded»); также `dismissed`, `failed`. `placement=support_author`. |
| `ads_feed_*` | Feed на «Поддержать автора» | `ads_feed_request` / `shown` / `clicked` / `failed`; Free only; `placement=support_author`. Shown = Impression. |
| `ads_banner_*` | Bottom-баннер РСЯ | `ads_banner_request` → `shown` → `clicked`; также `failed`. `placement=main_bottom`. **Shown = `onImpression`**, не `onAdLoaded`. Refresh (~35 с) — отдельные request/shown. **Не** путать с `announcement_impression` и `ads_feed_*`. |
| `app_error_non_fatal` | Ошибка (не краш) | `error_domain`, `error_type`; при превышении cap/domain — `error_type=cap_reached`. |
| `billing_error` | Ошибка RuStore Pay | Сеть/SDK при **покупке** или кнопке **«Восстановить»** на paywall. **Не** шлётся при тихой автопроверке покупок при старте (AliceCommands). Частые `UnknownHostException` / `RuStorePaymentNetworkException` ≠ «paywall сломан». |
| `bootstrap_error` | Ошибка при запуске | Инициализация app, не billing. |
| `review_error` / `update_error` | RuStore Review / Update | SDK in-app review и обновлений. |
| `ads_error` | Ошибка рекламы (общая) | Детали — `ads_*_failed`. |
| `pro_purchase` | Результат покупки Pro | `success`, `entry_point`; `error_type=cancelled` — отмена пользователем. |
| `push_*` | Push-уведомления | `push_permission_result`, `push_preference_change`, `push_received`, `push_open`, `push_dismiss`, `push_error`. |

Полный словарь подписей — вкладка **Аналитика → Как читать** (`admin-web/js/admin.js` → `ANALYTICS_EVENT_LABELS`).

### KPI: показы баннера РСЯ / DAU / день

| Метрика | Формула |
|---------|---------|
| Показы / пользователь / день | `COUNT(ads_banner_shown) / COUNT(DISTINCT install_id с daily_active)` за календарный день (Europe/Moscow) |
| Fill rate | `COUNT(ads_banner_shown) / COUNT(ads_banner_request)` |
| CTR | `COUNT(ads_banner_clicked) / COUNT(ads_banner_shown)` |

Фильтр `build_type=debug` в admin — как для остальных событий. Free-only: breakdown / user_properties `is_pro=false`.

### Намеренные дубли (канон)

Некоторые действия дают **два** события — это не баг:

| Действие | События | Зачем |
|----------|---------|-------|
| Переключение главной вкладки | `tab_select` + `ui_click` | `tab_select` — навигация (`tab`, `previous_tab`); `ui_click` — разбор кнопок (`element_id=tab_*`). |
| Отправка поиска | `search` + `ui_click` | `search` — метрики (`query_length`, `results_count`); `ui_click` — `element_id=search_submit`. |

Воронки и breakdown по кнопкам читают **`ui_click`**; тренды навигации/поиска — **`tab_select`** / **`search`**.

### User properties

| Ключ | Значения | Назначение |
|------|----------|------------|
| `persona`, `is_pro`, `app_language`, `theme_mode`, `content_version`, `install_id` | см. app | Сегменты в breakdown (`field_source=user_properties`). |
| `build_type` | `debug` \| `release` | Фильтр QA/debug-установок в admin (не в params). |

### content_sync phases

`content_sync` + `params.phase`:

| `phase` | Когда |
|---------|--------|
| *(основной каталог)* | bundle/manifest sync (как раньше) |
| `popular_commands` | Синк ranked popular с API |
| `announcements` | Синк баннеров «Ещё» |

Также: `trigger`, `success`, опционально `version` / `revision`.

### Deprecated event names

| Имя | Замена |
|-----|--------|
| `affiliate_click` | `contextual_pick_click` (picks) или `support_shop_open` (магазин поддержки) |
| `device_pick_click` | `contextual_pick_click` |

### Разбивка по сегментам

Breakdown по умолчанию читает **`params`**. Для Pro/persona/языка/темы — query **`field_source=user_properties`** и `param=is_pro` (или `persona`, `app_language`, `theme_mode`).

### Топ событий: «Действия» vs «Все»

По умолчанию на обзоре скрыты служебные: `pro_restore`, `content_sync`, `app_foreground`, `session_*`, `time_in_app_tick`, `*_impression` (в т.ч. `cod_impression`, `widget_shown`). Переключатель «Все события» показывает полный топ.

### Воронки (независимые шаги)

Каждый шаг воронки — `COUNT(DISTINCT install_id)`, **не** sequential cohort. Пресеты: CoD, Поиск, Сценарии, Виджет, First value, Pro, Подборки (`contextual_pick_*`), Rewarded (`ads_rewarded_*`), Баннер РСЯ (`ads_banner_*`), Баннер (Ещё) (`announcement_impression`). Для CTA редакционного баннера после `announcement_impression` смотрите breakdown `ui_click` → `element_id=more_announcement_cta` (воронка не фильтрует params).

Полный словарь `event_name` — вкладка **Как читать** в админке и `ANALYTICS_EVENT_LABELS` в `admin-web/js/admin.js`.
