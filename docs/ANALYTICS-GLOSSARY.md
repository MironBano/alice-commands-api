# Analytics glossary — alice-commands-api

Пояснения для вкладки **«Как читать»** в админке.  
Константы в приложении: `AliceCommands/.../AnalyticsEvents.kt`.

Связанные: [ANALYTICS-BACKEND.md](ANALYTICS-BACKEND.md), [ADMIN-UX.md](ADMIN-UX.md).

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
| `search` | Поиск | Текст запроса **не** сохраняется — только `query_length`, `results_count`, опционально `device_type`. Канон: **`search`** (alias `search_query` с app ≥ следующего релиза после ит.1 **не** шлётся). |
| `screen_view` / `route` | Экран | `route` — **конкретный** путь (`category/music`, `command/abc`), не шаблон NavHost (`category/{categoryId}`). |
| `command_tts` / `command_copy` | Озвучил / скопировал | Параметр `source`: откуда действие (`command_detail`, `quick`, `catalog_cod`, `search`, …). |
| `contextual_pick_click` | Клик по pick | Воронка рефералки; не путать с устаревшим `affiliate_click`. |
| `app_error_non_fatal` | Ошибка (не краш) | После ит.1 app — через backend outbox, не только AppMetrica. |

### Разбивка по сегментам

Breakdown по умолчанию читает **`params`**. Для Pro/persona/языка/темы — query **`field_source=user_properties`** и `param=is_pro` (или `persona`, `app_language`, `theme_mode`).

### Топ событий: «Действия» vs «Все»

По умолчанию на обзоре скрыты служебные: `pro_restore`, `content_sync`, `app_foreground`, `session_*`, `time_in_app_tick`, `*_impression`. Переключатель «Все события» показывает полный топ.

Остальные имена в UI читаются напрямую («Открыл команду», «Скопировал команду»…) — полный словарь в `admin-web/js/admin.js` (`ANALYTICS_EVENT_LABELS`).
