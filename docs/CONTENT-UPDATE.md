# Content Update — runbook

**Для админа:** [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md) · мастер в admin UI → **Контент** / **Команды**.

**Цель:** обновить каталог на **staging** без потери точечных правок.

---

## Простыми словами: три копии каталога

| Где | Что это | Когда обновляется |
|-----|---------|-------------------|
| **Draft** (черновик на сервере) | Рабочая копия в PostgreSQL | Правки в админке «Команды», или `push-draft` из файла |
| **Live** | То, что видит приложение | Кнопка **Публикация** |
| **Файл каталога** `seed/catalog-audit-fixed.json` | Копия в git-репозитории (бэкап/эталон) | `pull-draft.ps1` после правок в админке, или ручной StrReplace |

Раньше «seed» = этот файл. Не путать с live.

---

## Точечная правка 1–N команд (безопасный путь)

1. Admin → **Команды** → Изменить → вкладка **JSON (все поля)** или форма → **Сохранить в draft**
2. Admin → **Публикация** (если нужно в app)
3. На ПК: `.\scripts\pull-draft.ps1` — файл в репозитории ← актуальный draft  
   (ярлык **Alice 3 - Pull catalog**)
4. При желании: commit файла в git

Так правки не потеряются при следующем `push-draft`.

---

## Загрузка файла → draft (массово)

```powershell
.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"
.\scripts\push-draft.ps1
# Admin → Контент → diff → Публикация
.\scripts\verify-staging.ps1
```

Если на staging draft **отличается от live** (точечные правки в админке ещё не опубликованы), обычный `push-draft` **остановится**, чтобы не затереть draft файлом.

- Сохранить правки админки в файл: `.\scripts\pull-draft.ps1`, затем **Публикация** в админке.
- Сознательно залить файл поверх draft: `.\scripts\push-draft.ps1 -Force`.
- После Publish флаг unpublished снимается — обычный push снова без `-Force`.

---

## Бэкапы (ручной откат)

Старые наборы: **`seed/archive/`** — только вручную, скрипты не трогают.

---

## Ярлыки на рабочем столе

`scripts/desktop/Ustanovit-yarlyki.bat`

| Ярлык | Действие |
|-------|----------|
| Alice 1 - Push catalog | файл → staging draft |
| Alice 2 - Validate catalog | проверка schema |
| Alice 3 - Pull catalog | staging draft → файл |
| Alice 4 - Admin staging | открыть админку |

---

## Legacy pipeline (удалён)

Парсер support / `update-content.ps1` / editorial queue **не используются**.
