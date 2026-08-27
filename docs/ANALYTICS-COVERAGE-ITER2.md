# Analytics coverage — iteration 2 (Android emit checklist)

**Статус:** admin labels + funnel presets + glossary готовы; **emit в AliceCommands реализован** (ит.2).  
**Канон констант:** `AliceCommands/.../AnalyticsEvents.kt` + `AnalyticsCommandSource.kt`.  
Админ-подписи: `admin-web/js/admin.js`.

## Контракт (не менять без sync glossary)

| `event_name` | Params | Где emit |
|--------------|--------|----------|
| `cod_impression` | `command_id` | Показ карточки CoD на каталоге (дедуп session/day) |
| `cod_open` | `command_id` | Тап по CoD до навигации |
| `scenario_open` | `template_id` | Вход на scenario detail |
| `category_click` | `category_id`, `featured=true\|false` | Тап категории с каталога |
| `command_view` | + `source` | При открытии команды: `catalog_cod`, `quick`, `search`, `history`, `related`, `favorites`, … |
| `search` | + optional `category_id` | Уже есть `query_length`/`results_count`; zero = `results_count=0` |
| `search_result_click` | `command_id`, `position`, optional `category_id` | Клик строки результата |
| `favorite_remove` | `command_id`, optional `list_id` | Снятие из избранного |
| `favorite_list_create` | `list_id` | Создание списка (без title) |
| `favorite_list_delete` | `list_id` | Удаление списка |
| `smarthome_tab_select` | `tab=commands\|templates\|devices` | Локальный таб УД |
| `command_share` | `command_id`, `source` | Share; **убрать** dual `ui_click` с `command_share` |
| `filter_change` | `screen=category`, `group_id` и/или `device_type` | Чипы в категории |
| `widget_shown` | optional `command_id` | Glance update |
| `widget_open` | optional `command_id` | Тап виджета |
| `deeplink_open` | `source=external\|widget` | `MainActivity.onCreate` **и** `onNewIntent` |
| `screen_view` | `route=onboarding/welcome` или `onboarding/disclaimer` | Онбординг вне MainShell |

## Sources (`AnalyticsCommandSource`)

Добавить/использовать: `RELATED`, `TRY_NOW`, `FIRST_VALUE_HERO`, `SCENARIO`, `CATALOG_COD`, … — для `command_view` / TTS / copy / share.

## DoD emit

- Unit-тесты use case / VM на CoD, search click, favorite_remove, smarthome tab, scenario_open, command_view.source, share без dual ui_click.
- Staging: POST новых имён → `rejected=0`.
- Ручной debug: CoD → category → search → scenario → favorites → УД tabs → widget cold+warm.
