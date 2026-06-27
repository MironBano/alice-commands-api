# Gap-анализ — backend vs app CONTENT-PIPELINE

**Дата:** 2026-06-26 · **Обновлено:** 2026-06-27 (реализация v1.0)

## Было в app ТЗ (CONTENT-PIPELINE)

- API контракт §4 без реализации
- «Backend опционально self-hosted»
- Publish: «редактор правит JSON в repo → CI»

## Стало (backend repo — реализовано)

| Тема | Решение | Статус |
| ---- | ------- | ------ |
| Источник истины runtime | **Published bundle** на API | ✅ Ktor + filesystem |
| Источник редактирования | **PostgreSQL draft** + admin UI | ✅ Exposed + Alpine.js |
| Publish | Кнопка в admin | ✅ PublishContentUseCase |
| Rollback | 5 последних bundle | ✅ RollbackPublishUseCase |
| Import / diff | Admin + scripts | ✅ ImportJsonUseCase, ContentDiffService |
| Content pipeline | Python + PowerShell | ✅ tools/content, scripts/ |
| CI validate | GitHub Actions | ✅ validate-content.yml |
| Fallback app | seed в APK + Room cache | без изменений (app) |

## Draft vs Published

```
Admin edits → PostgreSQL (categories, commands, …)
Publish     → content_v{N}.json.gz + current_manifest (immutable)
Android     → sync manifest → download bundle if newer
```

Rollback = `current_manifest.content_version` указывает на существующий bundle file.

## Affiliate

- Draft: `affiliate_blocks` table
- Publish: snapshot на диск → **отдельный** `GET /v1/affiliate/blocks`

## Anti-patterns

- Править live bundle файл вручную на сервере
- Дублировать категории в Android Kotlin
- Auto-publish парсера без human review

## Остаётся на v1.0.1+

- Delta sync endpoint
- S3 BundleStorage adapter
- Shared schema JAR между repos
