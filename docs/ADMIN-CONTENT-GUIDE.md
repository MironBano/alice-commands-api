# Инструкция админа: обновление каталога команд

Пошаговое руководство для pipeline **inventory → editorial → catalog**.  
Технические детали — [CONTENT-UPDATE.md](CONTENT-UPDATE.md).

---

## Что происходит (коротко)

1. **На ПК** — парсер обновляет **inventory** (фразы с support), diff формирует **очередь** (только новое/пропавшее).
2. **Catalog** (`full-catalog.json`) = только **approved editorial** + фразы из inventory. Парсер **не перезаписывает** ваши описания.
3. **Push** — `pipeline-sync` + import **SYNC** на staging draft.
4. **В админке** — **Контент → Редактор текстов** (save/import editorial), diff draft vs опубликованное, **Публикация**.

```text
[Ярлык 1] → inventory + queue + catalog → push SYNC
         → [Админка: Контент → редактор → diff → Публикация]
         → [--finalize-baseline после Publish]
```

---

## Однократная настройка

### 1. Credentials (`scripts/.env`)

| Переменная | Пример |
|------------|--------|
| `STAGING_API_URL` | `https://staging-api.alicecommands.ru` |
| `ADMIN_USERNAME` | логин |
| `ADMIN_PASSWORD` | пароль |

### 2. Python

```powershell
pip install -r tools/content/requirements.txt
```

### 3. Ярлыки на рабочем столе

Один раз: `scripts\desktop\Ustanovit-yarlyki.bat` → папка **Alice Commands** (6 ярлыков внутри).

**Обновить каталог** ярлыки не создаёт и не дублирует.

### 4. Первый запуск pipeline (editorial из command bank)

```powershell
python tools/content/pipeline_run.py --bootstrap --skip-fetch
```

Ярлык **1** делает bootstrap автоматически, если `seed/data/editorial.json` ещё нет.

### 5. Deploy backend на staging (после обновления кода admin/server)

Нужен актуальный server и admin UI (редактор текстов, editorial import):

```powershell
.\scripts\deploy-staging.ps1
```

Или ярлык **«6. Deploy staging (backend)»** на рабочем столе.

Без deploy в админке может не быть редактора текстов и API `pipeline-sync`.

---

## Ярлыки

| Ярлык | Действие |
|-------|----------|
| **1. Обновить каталог** | `pipeline_run` → validate → push **sync** → verify |
| **2. Собрать локально** | То же без push |
| **3. Force fetch** | Обновить HTML с support, затем push |
| **4. Открыть админку** | Браузер → `/admin` → **Контент** |
| **5. Проверить staging** | Опубликованный manifest (live) |
| **6. Deploy staging** | Сборка + upload + restart API на VPS |

Папка **Alice Commands** на рабочем столе — только после `Ustanovit-yarlyki.bat` (один раз).

---

## Регулярный workflow

### Шаг 1 — Ярлык «1. Обновить каталог»

Внутри скрипта:

| # | Действие |
|---|----------|
| 1 | Parse → `seed/data/inventory_snapshot.json` |
| 2 | Diff vs baseline → `seed/data/queue.json` |
| 3 | Сборка `seed/full-catalog.json` (только approved) |
| 4 | `validateContent` |
| 5 | `pipeline-sync` + import **SYNC** |
| 6 | Verify manifest |

Успех: `Draft import OK (sync)` и verify без ошибок.

### Шаг 2 — Админка → **Контент**

Блок **Pipeline**: inventory / approved / **queue**.

### Шаг 3 — **Редактор текстов** (в админке)

Раздел **Контент → шаг 3**:

1. Фильтры: **К вычитке**, Изменено, Ожидает, Очередь и т.д.
2. Колонки: **Опубликовано** (live), **Черновик** (draft), **Правка** (editorial).
3. Правка title/effect → **Сохранить правки** (rebuild draft).
4. **Скачать JSON** → правка в ИИ → **Загрузить JSON** (editorial, не bundle).
5. Формат: `edit.title_ru`, `edit.effect_description_ru`, `edit.status=approved`.

Legacy-очередь (approve по одной) — в collapsed-блоке ниже, только для аварийных случаев.

### Шаг 4 — Diff draft vs опубликованное

Фильтры Added / Changed / Removed. Крупные Removed — не публиковать без проверки.

### Шаг 5 — Публикация

Раздел **Публикация** → **Опубликовать draft → live**.

```powershell
python tools/content/pipeline_run.py --skip-fetch --finalize-baseline
```

Зафиксирует baseline inventory для следующего diff.

---

### Pilot: группы команд (schema v2)

Для категории **Умный дом** (`smart_home`) — editorial seed с группами:

```powershell
.\gradlew.bat :server:validateContent -PcontentFile=seed/smart-home-groups-v2.json
.\scripts\push-draft.ps1 -Mode sync -BundleFile seed/smart-home-groups-v2.json
```

В админке: **Группы команд** → проверить порядок и preview ids → **Публикация**.  
Checklist QA: [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) §10.

`full-catalog.json` уже `schema_version: 2`; editorial groups по остальным категориям — post-pilot.

---

## Разделы админки

| Раздел | Зачем |
|--------|-------|
| **Контент** | Мастер: pipeline, editorial, diff, переход к публикации |
| **Оформление** | Цвета и иконки всех категорий (таблица, быстрое сохранение) |
| **Библиотека иконок** | Список SVG на сервере, upload |
| **Группы команд** | CRUD групп, reorder, visual override / inherit |
| **Команды** | Точечные правки draft (+ group_id, aliases) |
| **Публикация** | Publish draft → live, rollback |
| **Импорт bundle** | Ручная загрузка content bundle (не editorial JSON) |

### Оформление категорий (иконки + цвета)

Перед publish pilot visuals:

1. **Оформление** — задайте `accent_color` / `accent_color_dark`, выберите иконку из каталога или вставьте `icon_url`.
2. Убедитесь, что `icon_key` заполнен (offline fallback в app).
3. **Публикация** → Publish.
4. Проверка: `curl https://staging-api.alicecommands.ru/icons/v1/child.svg` → 200.

На staging URL иконок — `staging-api.alicecommands.ru`, не `cdn` (CDN — prod после DNS). Подробнее: [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md), [INFRASTRUCTURE.md](INFRASTRUCTURE.md) §2.

---

## Частые проблемы

| Симптom | Решение |
|---------|---------|
| Загрузил editorial JSON в «Импорт bundle» | Используйте **Контент → Редактор текстов → Загрузить JSON** |
| Нет редактора текстов / старый UI | `deploy-staging.ps1` или ярлык **6** |
| `pipeline-sync failed` | Deploy не сделан или старый API |
| Push OK, catalog ~300, не 800 | Норма: в app только **approved**, не весь parse |
| Очередь снова открылась | После Publish — `--finalize-baseline` |
| Approve в admin | Сразу в draft; локальный `editorial.json` обновится при следующем push с ПК |
| `cdn.alicecommands.ru` не открывается | NXDOMAIN — нет DNS; на staging используйте `staging-api.../icons/v1/` |
| Превью иконки в админке пустое | Проверьте `icon_url` host; после deploy каталог строит URL из `ICON_PUBLIC_BASE_URL` |

---

## Ссылки

- [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md)
- [CONTENT-UPDATE.md](CONTENT-UPDATE.md)
- [ADMIN-UX.md](ADMIN-UX.md)
- [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md)

*Обновлено: 2026-07-01 — schema v2 groups + category visuals*
