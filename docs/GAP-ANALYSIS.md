# Gap-анализ — backend vs app CONTENT-PIPELINE

**Дата:** 2026-06-26

## Было в app ТЗ (CONTENT-PIPELINE)

- API контракт §4 без реализации
- «Backend опционально self-hosted»
- Publish: «редактор правит JSON в repo → CI»

## Стало (backend repo)

| Тема | Решение |
| ---- | ------- |
| Источник истины runtime | **Published bundle** на API |
| Источник редактирования | **PostgreSQL draft** + admin UI |
| Publish | Кнопка в admin, не только git CI |
| CI | Опционально: validate on PR; publish — через admin или CI trigger |
| Fallback app | seed в APK + Room cache (без изменений) |

## Draft vs Published

```
Admin edits → PostgreSQL (draft_* tables)
Publish     → bundle_v{N}.json.gz + manifest.json (immutable)
Android     → sync manifest → download bundle if newer
```

Rollback = manifest.content_version указывает на старый bundle file (хранится 5 шт.).

## Affiliate

- Draft: `affiliate_blocks` table
- Publish: включается в bundle **или** отдельный `affiliate/blocks.json` (v1.0: **отдельный endpoint**, синхронизируется при publish)

## Anti-patterns

- Править live bundle файл вручную на сервере
- Дублировать категории в Android Kotlin
- Auto-publish парсера без human review
