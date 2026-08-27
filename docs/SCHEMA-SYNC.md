# Schema sync — backend ↔ Android app

**Канон:** `schema/content-bundle.schema.json` в **этом репозитории**.

---

## Правило

1. Изменения schema — сначала здесь, bump `schema_version` (**v2** = command groups)
2. Копировать в app: `AliceCommands/schema/content-bundle.schema.json` (или submodule позже)
3. CI обоих репо: validate `seed/` и bundle против schema

---

## Schema v2 (2026-06-29)

| Добавлено | Описание |
| --------- | -------- |
| `command_groups[]` | id, category_id, title_ru, sort_order, preview_command_ids, icon_key, icon_url, accent_color, accent_color_dark |
| `categories[]` visuals | optional `icon_url`, `accent_color`, `accent_color_dark` (see [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md)) |
| `commands[].group_id` | FK на group (optional) |
| `commands[].sort_order` | порядок внутри группы |
| `commands[].variant_label_ru` | короткая подпись в compact UI |
| `commands[].is_primary_in_group` | preview / default variant |
| `commands[].search_aliases` | доп. строки для поиска |

Backend: Flyway `V4__command_groups.sql`, admin CRUD, publish validation.  
Pilot: `seed/smart-home-groups-v2.json`. См. [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md).

`min_app_version` в manifest остаётся **1.0** до релиза grouped UI в app.

---

## Процедура bump schema

1. Обновить `content-bundle.schema.json`
2. Обновить [DATABASE.md](DATABASE.md), [API.md](API.md), [BACKEND-COMMAND-GROUPS.md](BACKEND-COMMAND-GROUPS.md)
3. Обновить app [CONTENT-SCHEMA.md](https://github.com/MironBano/AliceCommands/blob/main/docs/CONTENT-SCHEMA.md)
4. Указать `min_app_version` в manifest если breaking change для клиентов

---

## Shared models (реализация)

Дублирование kotlinx.serialization data classes в обоих репо (v1.0).  
Validate: `./gradlew :server:validateContent` + CI [validate-content.yml](../.github/workflows/validate-content.yml).

v1.0.1: вынести в published JAR `alice-commands-schema` (optional).

---

*См. [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) NFR-8*
