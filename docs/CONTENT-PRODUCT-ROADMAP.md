# Карта улучшений — продуктовый контент (не команды)

**Дата:** 2026-07-10 · **Связано с:** [`CATALOG-FIXED-BUILD.md`](CATALOG-FIXED-BUILD.md) (команды **871**, seed v50 ✅)  
**Источник правды bundle:** [`seed/catalog-audit-fixed.json`](../seed/catalog-audit-fixed.json) · **Эталон команд:** [`seed/catalog-audit-fixed-REFERENCE-v43.json`](../seed/catalog-audit-fixed-REFERENCE-v43.json)

Команды доведены до byte-parity. Этот документ — **трекер всего остального** в каталоге и смежных API: визуал, группы, онбординг, шаблоны, команда дня, устройства/подборки, реферальная монетизация.

**Правило правок bundle:** только ручной StrReplace/Write в JSON ([`no-content-scripts`](../.cursor/rules/no-content-scripts.mdc)). `seed/full-catalog.json` не трогать без отдельного решения.

---

## Baseline (v44, 2026-07-08)

| Сущность | Было v43 | Стало v44 |
| -------- | -------- | --------- |
| `categories` | 13, 3 пары дубль-цветов | Уникальные accent, CDN URL, featured 6 |
| `command_groups` | 70, 2 featured | 70, 3 featured (`qc_playback`, `smart_home_light`, `music_playback`) |
| `scenario_templates` | 2 | **8** + `deep_link_hint` |
| `checklist_items` | 5 | **8** |
| `command_of_day` | auto music, stale date | **manual** таймер, дата 2026-07-08 |
| `icon_url` | staging-api | **cdn.alicecommands.ru** (83) |
| `icon_catalog.json` | ~309 EN labels | **315 RU labels** |
| `device_picks` | миграция из affiliate | **+8** picks (V8 seed) |
| Рефералка | нет | [REFERRAL-EPIC.md](REFERRAL-EPIC.md) — отложено |

---

## Definition of Done (продуктовый контент)

1. **Визуал:** нет визуальных коллизий цветов между соседними категориями в сетке; все `icon_url` на production CDN; `validateContent` OK.
2. **Группы:** 70/70 с валидными preview; featured только там, где есть смысл в home/catalog UI.
3. **Онбординг:** checklist 7–8 шагов, покрывает station + phone + optional УД; hints без технического жаргона.
4. **Сценарии:** ≥6 шаблонов (утро/вечер/дети/УД/ТВ/сон), у каждого `source_url` + 2+ `example_phrases`.
5. **Команда дня:** `resolved_date` = дата publish; режим согласован с маркетингом (manual на релиз / auto по категориям).
6. **Устройства:** `GET /v1/smarthome/devices` — guides с `image_url`, picks с ценами и легальными ссылками; publish snapshot на staging/prod.
7. **Админка:** правки через UI или import bundle; после publish — `verify-staging.ps1` + визуальный QA в Android staging.

---

## Приоритеты

| Метка | Смысл |
| ----- | ----- |
| **P0** | Блокер релиза / видимый баг в app |
| **P1** | Сильно улучшает UX, делаем до prod |
| **P2** | Полировка, A/B, post-launch |
| **P3** | Новый продукт / исследование |

---

## 1. Цвета и иконки (categories + groups)

### 1.1 Коллизии палитры (P0 → P1)

Сейчас три пары категорий делят один `accent_color`:

| Цвет | Категории | Проблема | Предложение |
| ---- | --------- | -------- | ----------- |
| `#E8A317` amber | `general`, `quick_commands` | В сетке «две жёлтых» | `quick_commands` → `#F97316` (orange) или `#CA8A04` |
| `#7B4BB7` violet | `music`, `alice_plus` | Плюс неотличим от музыки | `alice_plus` → `#9333EA` или preset `indigo` `#6366F1` (если не занят `obscure`) |
| `#2563EB` blue | `audiobooks`, `tv_video` | Книги = ТВ | `audiobooks` → `#4F46E5` indigo или `#0D9488` teal |

Канон палитры: [`content/icon_catalog.json`](../content/icon_catalog.json) → `accent_presets` + [`content/visuals_map.json`](../content/visuals_map.json).

**Задачи**

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| V-01 | Развести 3 пары цветов в `categories[]` + `visuals_map.json` | P1 | ✅ |
| V-02 | Проверить контраст light/dark в Android (WCAG для chip на фоне) | P1 | ⬜ app QA |
| V-03 | Группы: accent только override (сейчас 0 групп с `accent_color` — наследование OK) | P2 | ✅ |
| V-04 | Для 2 featured-групп (`qc_playback`, `qc_media`) — оставить цвет категории `quick_commands` | P2 | ✅ qc_media unfeatured |

### 1.2 CDN vs staging-api (P0 infra + P1 content)

- Все **83** `icon_url` → `https://staging-api.alicecommands.ru/icons/v1/...`
- **0** ссылок на `cdn.alicecommands.ru` в bundle
- RUNBOOK: prod/staging app должны резолвить host из bundle

**Задачи**

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| V-10 | `scripts/setup-cdn.ps1` → DNS + cert + `ICON_PUBLIC_BASE_URL` | P0 | ⬜ ops |
| V-11 | Массовая замена `icon_url` на CDN в bundle (или publish с `USE_CDN_ICON_URLS=1`) | P1 | ✅ v44 |
| V-12 | `publish-staging-visuals.ps1` / publish flow: зафиксировать политику host в RUNBOOK | P1 | ✅ §12 |
| V-13 | Smoke: `curl` 200 на 13 category icons + 10 random group icons с CDN | P1 | ⬜ после V-10 |

### 1.3 Icon catalog (админка)

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| V-20 | `label_ru` для 315 slug — RU-подписи в админке | P2 | ✅ |
| V-21 | Сверить slug в bundle с файлами `content/icons/v1/*.svg` (315 файлов) | P1 | ✅ |
| V-22 | Документировать mapping «категория → рекомендуемый slug» в ADMIN-UX | P3 | ✅ §5c |

---

## 2. Категории — editorial

### 2.1 `featured` (P1)

**Сейчас featured:** `general`, `music`, `tv_video`, `timers`, `smart_home`, `quick_commands` (**6 из 13**).

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| C-01 | Согласовать лимит featured (рекомендация: **4–5** на home) | P1 | ✅ 6 (music, qa, timers, sh, kids, qc) |
| C-02 | Кандидаты снять featured: `general` (слишком широкая) или `tv_video` | P2 | ✅ |
| C-03 | Кандидаты добавить: `kids` (семейная аудитория), `quick_answers` (вход в каталог) | P2 | ✅ |

### 2.2 `device_types` (P1)

| category_id | device_types | Замечание |
| ----------- | ------------ | --------- |
| `smart_home` | station, phone | **Нет `tv`** — ТВ с Алисой тоже управляет УД |
| `kids`, `obscure`, `station_settings` | station only | OK для station-only команд |
| `tv_video` | station, tv | OK |

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| C-10 | `smart_home.device_types` + `tv` (если команды УД на ТВ есть в каталоге) | P1 | ✅ |
| C-11 | Пройти filter UX в app: station-only категории не показывать phone-only пользователям | P2 | ⬜ app |

### 2.3 `description_ru` (P2)

Короткие описания есть у всех 13 категорий. Улучшить копирайт под store/screenshots (1 строка + польза).

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| C-20 | Редактура `description_ru` (тон «для новичка») | P2 | ✅ v44 |
| C-21 | `source_url` у большинства → generic skills; точечно на topic pages | P3 | ⬜ |

---

## 3. Command groups (80)

**Хорошо уже сейчас:** 80/80 групп с `description_ru` и `preview_command_ids`, 0 dangling preview refs, **871/871** команд с `group_id`.

### 3.1 Featured groups (P2)

Featured: только `qc_playback`, `qc_media` (обе в `quick_commands`).

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| G-01 | Решить: нужны ли featured-группы вне quick_commands (напр. `smart_home_light`, `music_playback`) | P2 | ✅ |
| G-02 | Лимит featured groups ≤ 3 | P2 | ✅ |

### 3.2 Preview curation (P1)

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| G-10 | QA в app: preview = самые «говоримые» фразы, не дубли title | P1 | ⬜ app QA |
| G-11 | Группы с 3 preview → довести до 4 где есть сильные варианты | P2 | ✅ 4 группы |
| G-12 | `general` (11 групп): проверить, что overflow-группы не дублируют preview соседей | P2 | ✅ spot-check OK |

### 3.3 Распределение по категориям

| category_id | групп | Комментарий |
| ----------- | ----: | ----------- |
| general | 11 | Самая дробная; следить за UX «простыни» |
| quick_commands | 8 | OK; 2 featured-группы |
| music | 8 | OK |
| obscure | 5 | OK |
| tv_video | 5 | OK |
| calls | 5 | OK |
| timers | 5 | OK |
| smart_home | 4 | Pilot; расширять только editorial |
| quick_answers | 4 | OK |
| alice_plus | 4 | OK |
| kids | 4 | OK |
| station_settings | 4 | station-only |
| audiobooks | 3 | Мало групп — норм при 22 командах |

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| G-20 | `general`: при росте UI — merge мелких групп (shopping+cooking?) | P3 | ⬜ |

---

## 4. Сценарии (`scenario_templates`)

**Сейчас (2):**

| id | audience | Пробел |
| -- | -------- | ------ |
| `scenario_kids` | kids | OK базовый |
| `scenario_morning` | all | OK базовый |

**Нет ни у одного:** `deep_link_hint` (поле есть в schema).

### Целевой набор (P1)

| id (предлож.) | title_ru | audience | example_phrases (идея) |
| ------------- | -------- | -------- | ---------------------- |
| `scenario_evening` | Вечер | all | «Алиса, включи тихую музыку», «Алиса, выключи свет» |
| `scenario_sleep` | Перед сном | all | колыбельная, таймер, тихий режим |
| `scenario_smart_home` | Умный дом | all | свет, климат, «всё выключи» |
| `scenario_tv` | Кино вечер | all | кинопоиск, громкость, пауза |
| `scenario_guests` | Гости | all | музыка, свет, таймер на кухню |
| `scenario_away` | Уехали | all | режим отсутствия (если есть команды) |

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| S-01 | Добавить 4–6 сценариев с валидными `source_url` | P1 | ✅ 6 новых |
| S-02 | Заполнить `deep_link_hint` (category_id или screen id для app) | P1 | ✅ |
| S-03 | У каждого: `trigger_ru`, 3× `actions_ru`, 2+ `example_phrases` | P1 | ✅ |
| S-04 | Привязать example_phrases к реальным `command_id` (документировать в админке) | P2 | ✅ ADMIN-CONTENT-GUIDE |

---

## 5. Онбординг (`checklist_items`)

**Сейчас (5/10):**

| order | command_id | hint | Риск |
| ----: | ---------- | ---- | ---- |
| 1 | `timers_postav_taimer_na_5_minut` | таймер | OK |
| 2 | `music_vkliuchi_muzyku` | музыка | OK |
| 3 | `quick_answers_kakaia_pogoda` | погода | OK |
| 4 | `sh_light_on` | свет (если есть УД) | **P0 UX:** у многих нет ламп |
| 5 | `general_gromche` | громкость | OK |

### Целевой checklist (7–8)

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| O-01 | П.4: смягчить hint / alternate path («пропустить» в app) | P0 | ✅ hints v44 |
| O-02 | Добавить quick command: `quick_commands_dalshe` или media без «Алиса» | P1 | ✅ |
| O-03 | Добавить напоминание: `timers_*` reminder | P1 | ✅ |
| O-04 | Добавить «сказка» для семей: `kids_*` (optional step) | P2 | ✅ |
| O-05 | Порядок: простое → сложное (таймер → музыка → погода → громкость → УД optional) | P1 | ✅ |
| O-06 | Сверка FK: все `command_id` существуют после каждого publish | P0 | ✅ |

---

## 6. Команда дня (`command_of_day`)

**Сейчас:**

```json
{
  "mode": "auto",
  "command_id": "music_vkliuchi",
  "auto_category_id": "music",
  "auto_seed": 31,
  "resolved_date": "2026-07-06",
  "updated_at": "2026-07-04T15:55:43.178531Z"
}
```

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| D-01 | При publish обновить `resolved_date` на дату publish (автоматически в PublishUseCase — проверить) | P0 | ✅ backend |
| D-02 | Неделя релиза: `mode: manual` + сильная команда (УД/таймер/музыка) | P1 | ✅ timers |
| D-03 | Ротация auto по категориям: music → timers → smart_home → quick_answers (по неделям) | P2 | ⬜ editorial |
| D-04 | Admin → «Команда дня»: задокументировать в ADMIN-CONTENT-GUIDE | P2 | ✅ |

См. [`BACKEND-COMMAND-OF-DAY.md`](BACKEND-COMMAND-OF-DAY.md).

---

## 7. Устройства, подборки, монетизация

Контент **вне** content bundle — отдельный snapshot API.

| API | Таблицы | Admin |
| --- | ------- | ----- |
| `GET /v1/smarthome/devices` | `device_guides`, `device_picks` | Шаблоны / Устройства |
| `GET /v1/affiliate/blocks` | `affiliate_blocks` | **Deprecated** |

**V7 seed:** 3 guides (`station`, `tv`, `phone`), picks мигрированы из `affiliate_blocks.products`.

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| M-01 | Загрузить `image_url` (webp) для 3 guides через admin upload | P1 | ⬜ ops (URL в V8) |
| M-02 | Editorial: 5–10 `device_picks` (лампа, хаб, колонка) с `price_hint_ru` | P1 | ✅ V8 8 picks |
| M-03 | Юридическое: `erid` / маркировка — **опционально v1.0**, v1.1+ при legal | P2 legal | ⬜ deferred |
| M-04 | Publish smarthome snapshot на staging + verify в app вкладка «Устройства» | P1 | ⬜ после deploy |
| M-05 | Удалить/не наполнять новые `affiliate_blocks`; только picks | P1 | ✅ policy |
| M-06 | Deep link `action_url`: только `https://` и `market://` (уже в API) | P1 | ✅ validate |
| M-07 | Связать picks с `context_category_id` = smart_home в UI app | P2 | ⬜ app |

См. [`API.md`](API.md) § `/v1/smarthome/devices`, миграция [`V7__smarthome_devices.sql`](../server/src/main/resources/db/migration/V7__smarthome_devices.sql).

---

## 8. Рефералка и рост (вне текущего репо)

В **alice-commands-api** нет сущностей referral / invite / promo code.

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| R-01 | Продуктовое решение: нужна ли рефералка в v1.0 или post-launch | P3 | ✅ отложено |
| R-02 | Если да — отдельный epic: deep link, attribution, без ПДн в API | P3 | ✅ [REFERRAL-EPIC.md](REFERRAL-EPIC.md) |
| R-03 | Пока: `device_picks` + Market URL как мягкая монетизация | P1 | ✅ |

---

## 9. Метаданные bundle

| Поле | Сейчас | Задача | P |
| ---- | ------ | ------ | - |
| `min_app_version` | `1.0` | Поднять до `1.0.1`+ только после QA grouped UI + visuals в Android | P1 gate |
| `published_at` | `2026-07-10T22:16:00Z` | Обновлять при каждом product publish | P0 |
| `content_version` | **50** (seed) | Staging live **v51+** после publish; инкремент при каждом Publish | — |

| ID | Задача | P | Статус |
| -- | ------ | - | ------ |
| B-01 | Чеклист prod publish из RUNBOOK §8 + этот документ | P0 | ✅ RUNBOOK §13 |
| B-02 | После product-правок: `validateContent` + import replace **без** `rebuild-draft` | P0 | ✅ |

---

## 10. Workflow (как доводить)

```text
1. Правки draft:
   - bundle-поля → catalog-audit-fixed.json (StrReplace) ИЛИ Admin UI
   - guides/picks → Admin «Устройства»
   - command_of_day / checklist / scenarios → Admin соответствующие разделы

2. Проверка локально:
   .\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"

3. Staging:
   import replace (БЕЗ rebuild-draft для fixed catalog)
   → admin diff → Publish
   → .\scripts\verify-staging.ps1

4. CDN (параллельно или до prod):
   .\scripts\setup-cdn.ps1
   → обновить icon_url → повторный publish

5. Android staging:
   sync → home colors → groups → checklist → scenarios → devices tab
```

**Связанные документы:** [`BACKEND-CATEGORY-VISUALS.md`](BACKEND-CATEGORY-VISUALS.md), [`BACKEND-COMMAND-GROUPS.md`](BACKEND-COMMAND-GROUPS.md), [`RUNBOOK-PUBLISH.md`](RUNBOOK-PUBLISH.md), [`ADMIN-UX.md`](ADMIN-UX.md).

---

## 11. Сводный backlog (по волнам)

### Волна A — до первого product publish (P0–P1)

| ID | Кратко |
| -- | ------ |
| V-10, V-11 | CDN + icon_url |
| V-01 | Развести цвета |
| O-01, O-05, O-02 | Checklist UX |
| S-01, S-03 | +4 сценария минимум |
| D-01, D-02 | Команда дня актуальна |
| M-01, M-02, M-04 | Devices tab наполнен |
| B-01, B-02 | Publish discipline |

### Волна B — polish (P2)

| ID | Кратко |
| -- | ------ |
| C-01–03, C-20 | Featured + копирайт |
| V-20 | RU labels в icon catalog |
| G-10–12 | Preview QA |
| D-03 | Ротация команды дня |
| S-02, S-04 | deep_link_hint |

### Волна C — post-launch (P3)

| ID | Кратко |
| -- | ------ |
| R-01–02 | Рефералка (если нужна) |
| G-20 | Merge general groups |
| V-22, C-21 | Документация |

---

## 12. Журнал статуса

| Дата | Событие |
| ---- | ------- |
| 2026-07-08 | Создан документ; baseline v43; команды byte-identical |
| 2026-07-08 | **v44 product content** — bundle, V8 picks, icon_catalog RU, docs |

---

*Обновлять таблицы статусов (⬜ → 🔄 → ✅) по мере закрытия задач. Команды — только через [`CATALOG-FIXED-BUILD.md`](CATALOG-FIXED-BUILD.md).*
