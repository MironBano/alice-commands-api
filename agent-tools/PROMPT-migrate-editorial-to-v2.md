# Задача: вручную перевести editorial-export в content bundle schema v2

## Контекст

Проект: **alice-commands-api** / Android-приложение AliceCommands.

Я перешёл на **schema v2** (command groups). Нужно обновить большой JSON с командами так, чтобы он **полностью** соответствовал новой схеме content bundle — не editorial export v1.

**Исходный файл:**

`C:\Users\rybak\Downloads\editorial-export-all (2).json`

Сейчас это **editorial export v1** (~800 записей, часто одна длинная строка JSON):

- корень: `schema_version: 1`, `exported_at`, `filter`, `instructions`, `records[]`
- каждая запись: `command_id`, `category_id`, `phrases`, `source_url`, `published`, `draft`, `edit { title_ru, effect_description_ru, status }`

**Целевой формат:** content bundle **schema_version: 2** — канон проекта:

- `schema/content-bundle.schema.json`
- `docs/BACKEND-COMMAND-GROUPS.md`
- пример pilot: `seed/smart-home-groups-v2.json`

**Важно:** это **bundle** для импорта в Admin → «Импорт bundle» или `push-draft.ps1`, затем публикация draft → live. Это **не** editorial JSON для «Редактор текстов» — у editorial другой формат (`records[].edit`).

---

## Жёсткие ограничения

1. **ЗАПРЕЩЕНО:** Python/Node/PowerShell-скрипты, batch-автоматизация, массовые regex по всему файлу, генераторы «одним кликом».
2. Работай **вручную**: читаешь запись → принимаешь решение → правишь JSON (как код через ApplyPatch/редактор).
3. Допустимо: поиск по файлу (rg/grep), чтение кусками, правка пакетами.
4. **Не меняй `command.id`** (бывший `command_id`) — стабильность для favorites/history/app.
5. Не придумывай требования (подписки, устройства), если они не очевидны из команды; неочевидное — только с подтверждением из `source_url` или официальной справки Яндекса.
6. Не дублируй команды; похожие по смыслу — в одну `command_group`, id не удаляй без причины в отчёте.

---

## Целевая структура JSON (schema v2)

    {
      "schema_version": 2,
      "content_version": 0,
      "published_at": "2026-06-30T12:00:00Z",
      "min_app_version": "1.0",
      "categories": [],
      "command_groups": [],
      "commands": [],
      "scenario_templates": [],
      "checklist_items": []
    }

### categories[]

Для каждой уникальной `category_id` из исходника:

- обязательно: `id`, `title_ru`, `sort_order`, `source_url`
- опционально: `description_ru`, `icon_key`, `featured`, `device_types`

### command_groups[] (новое в v2)

Смысловые группы внутри категории (Свет, Музыка, Будильник, Алиса Плюс и т.д.):

- `id` — стабильный slug `[a-z0-9_]+`, например `smart_home_light`
- `category_id`
- `title_ru` — заголовок группы в UI
- `description_ru` — коротко, по делу
- `sort_order` — порядок группы в категории (10, 20, 30…)
- опционально: `icon_key`, `featured`, `preview_command_ids` (3–5 id команд из этой группы)

### commands[]

Для **каждой** записи из `records[]`:

- `id` = `command_id` (не менять)
- `category_id` — из записи
- `title_ru` = `edit.title_ru`; если пусто/мусор → `draft` → `published` → осмысленный ручной title
- `effect_description_ru` = очищенный `edit.effect_description_ru` (см. правила качества)
- `phrases` — из `phrases` / `phrase_example` (минимум 1)
- `requires_alice_word` — true, если фраза начинается с «Алиса»; иначе по смыслу
- `requires_plus` — true только для alice_plus или если явно из справки
- `device_types` — `["station","phone"]` по умолчанию; `tv` только если команда про ТВ
- `source_url` — обязателен, https, из записи
- `updated_at` — ISO datetime (можно единый timestamp миграции)
- `tags` — минимум `[category_id]`
- `related_command_ids` — `[]` (группа заменяет старый граф)

**Новые поля v2 (заполнить вручную):**

- `group_id` — FK на `command_groups.id` (nullable только если команда реально одиночная вне групп)
- `sort_order` — порядок внутри группы (10, 20, 30…)
- `variant_label_ru` — короткая подпись варианта (часто укороченный `title_ru`)
- `is_primary_in_group` — ровно **одна** `true` на группу (главный вариант для preview)
- `search_aliases` — 0–5 синонимов для поиска, без дублей с title/phrases
- `published_at` — можно опустить для draft-каталога

---

## Маппинг editorial → bundle v2

| Источник | Куда в v2 |
|----------|-----------|
| `records[].command_id` | `commands[].id` |
| `records[].category_id` | `commands[].category_id` + `categories[].id` |
| `records[].edit.title_ru` | `commands[].title_ru` + частично `variant_label_ru` |
| `records[].edit.effect_description_ru` | `commands[].effect_description_ru` |
| `records[].phrases` | `commands[].phrases` |
| `records[].source_url` | `commands[].source_url` |
| `edit.status` | **не** переносить (bundle не editorial) |
| `published` / `draft` / `raw_result` | только справка при вычитке, мусор не копировать в effect |

---

## Правила качества title / effect

- Title: короткий, ≤42 символов, без обрезанных кавычек «…
- Effect: понятно пользователю, без мусора из support/raw_result
- В конце effect — блок **«Нужно:»** с устройствами/подключением, только подтверждённые требования
- Убрать: длинные списки пунктов меню справки, generic «Установит будильник по запросу» без контекста
- Если title — вопрос («какая скорость обдува»), effect описывает **ответ/статус**, а не несвязанное действие

---

## Правила группировки (ручные)

1. Группируй **по смыслу для пользователя**, не только по префиксу id.
2. В одной группе — варианты одной темы: «включи свет / выключи свет / яркость света».
3. Одиночная команда — отдельная группа 1:1 (предпочтительно) или `group_id: null` с пояснением в отчёте.
4. `preview_command_ids`: primary + 2–4 характерных варианта из той же группы.
5. Валидация (BACKEND-COMMAND-GROUPS §5):
   - `group.category_id` существует
   - `command.group_id` принадлежит той же `category_id`
   - в группе ≥1 команда
   - не более одной `is_primary_in_group: true`
   - `preview_command_ids` только из этой группы

---

## Порядок работы

Файл большой — работай **пакетами по категориям**:

1. Инвентаризация: список `category_id` + количество команд.
2. Для каждой категории: список `command_groups` → разнести команды → заполнить v2-поля → проверить primary/sort_order/preview.
3. Промежуточные JSON по категориям → в конце склей в один файл.
4. Финальная самопроверка по DoD.

---

## Выходные файлы

1. `C:\Users\rybak\Downloads\full-catalog-v2-manual.json` — итоговый bundle schema v2
2. `C:\Users\rybak\Downloads\full-catalog-v2-manual-report.md` — отчёт:
   - сколько категорий / групп / команд
   - спорные группировки и где не уверен
   - команды без `group_id` и почему
   - что исправлено в title/effect
   - что не удалось проверить по `source_url`

---

## DoD (готово, когда всё выполнено)

- [ ] `schema_version: 2`
- [ ] Все обязательные поля command заполнены
- [ ] `command_groups` покрывают все сгруппированные команды
- [ ] Нет orphan `group_id`
- [ ] Не более одного primary на группу
- [ ] `source_url` https у каждой команды
- [ ] Нет мусорных effect из raw_result
- [ ] UTF-8, валидный JSON
- [ ] Если есть репо: `.\gradlew.bat :server:validateContent -PcontentFile=...`

---

## Вложения к чату (обязательно)

1. `C:\Users\rybak\Downloads\editorial-export-all (2).json` — исходник
2. `alice-commands-api/schema/content-bundle.schema.json` — схема
3. `alice-commands-api/docs/BACKEND-COMMAND-GROUPS.md` — семантика групп
4. `alice-commands-api/seed/smart-home-groups-v2.json` — образец группировки

---

## Старт

Начни с инвентаризации категорий в исходном файле и предложи план `command_groups` для **первой** категории. Затем переходи к ручной миграции без скриптов.
