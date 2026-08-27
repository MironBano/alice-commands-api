# Gap-анализ — backend vs app CONTENT-PIPELINE

**Дата:** 2026-06-26 · **Обновлено:** 2026-06-29 (schema v2 command groups + delta)

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
| Import / diff | Admin + scripts | ✅ ImportJsonUseCase, ContentDiffService (+ `command_groups` section) |
| **Command groups (schema v2)** | Editorial groups in bundle | ✅ CRUD admin, validation, pilot seed |
| **Delta sync** | `GET /v1/content/delta` | ✅ ContentDeltaService |
| Content pipeline | Python + PowerShell | ✅ tools/content, scripts/ |
| CI validate | GitHub Actions | ✅ validate-content.yml |
| Fallback app | seed в APK + Room cache | без изменений (app) |

## Draft vs Published

```
Admin edits → PostgreSQL (categories, command_groups, commands, …)
Publish     → content_v{N}.json.gz (schema_version 2) + current_manifest (immutable)
Android     → sync manifest → full bundle or delta if from in retention
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

- S3 BundleStorage adapter
- Shared schema JAR между repos
- FCM hook on publish
- Editorial `command_groups` для всех категорий (post-pilot rollout)
