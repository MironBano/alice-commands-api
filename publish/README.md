# Publish logic

Publish pipeline реализован в **`server/src/main/kotlin/.../application/publish/`**:

- `PublishContentUseCase` — draft → validate → gzip → sha256 → manifest
- `RollbackPublishUseCase` — переключение на предыдущий bundle
- `ImportJsonUseCase` — merge/replace draft из JSON

Storage: `infrastructure/storage/FilesystemBundleStorage.kt`

Отдельный Gradle-модуль `publish/` не используется в v1.0. Выделение в shared library — опционально v1.0.1.

См. [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) §5.
