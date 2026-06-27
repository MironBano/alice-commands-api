# Schema sync — backend ↔ Android app

**Канон:** `schema/content-bundle.schema.json` в **этом репозитории**.

---

## Правило

1. Изменения schema — сначала здесь, bump `schema_version`
2. Копировать в app: `AliceCommands/schema/content-bundle.schema.json` (или submodule позже)
3. CI обоих репо: validate `seed/` и bundle против schema

---

## Процедура bump schema

1. Обновить `content-bundle.schema.json`
2. Обновить [DATABASE.md](DATABASE.md), [API.md](API.md)
3. Обновить app [CONTENT-SCHEMA.md](https://github.com/MironBano/AliceCommands/blob/main/docs/CONTENT-SCHEMA.md)
4. Указать `min_app_version` в manifest если breaking change

---

## Shared models (реализация)

Дублирование kotlinx.serialization data classes в обоих репо (v1.0).  
Validate: `./gradlew :server:validateContent` + CI [validate-content.yml](../.github/workflows/validate-content.yml).

v1.0.1: вынести в published JAR `alice-commands-schema` (optional).

---

*См. [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) NFR-8*
