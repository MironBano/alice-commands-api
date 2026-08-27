# Команды для добавления — раунд 3

> **Дата проверки:** 2026-07-11  
> **Эталон каталога:** `seed/catalog-audit-fixed.json` (**871** команда, schema v2, `content_version=50`)  
> **Предыдущие раунды:** [COMMANDS-TO-ADD.md](./COMMANDS-TO-ADD.md) (79) · [COMMANDS-TO-ADD-ROUND2.md](./COMMANDS-TO-ADD-ROUND2.md) (28)  
> **Метод:** сверка **канонических фраз запуска** со страницами навыков на [dialogs.yandex.ru](https://dialogs.yandex.ru/store), [справкой Станции](https://alice.yandex.ru/support/ru/station/skills/) и [примерами Браузера](https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples). Фразы из сторонних каталогов (aliceskill.ru, t-j.ru) **не** включались без подтверждения на dialogs.yandex.ru.

## Сводка

| Метрика | Значение |
| --- | --- |
| Кандидатов из ROUND2 Tier 2 + games + справка | ~35 |
| Уже есть / частично покрыто | 13 |
| Исключены (дубль / не найден / низкая уверенность) | 4 |
| **К добавлению (полный JSON ниже)** | **14** |
| Обогащение существующих (не новые id) | 2 команды |

### Результат верификации (2026-07-11)

| Статус | Кол-во | Что сделано |
| --- | --- | --- |
| ✅ Подтверждено | 12 | Фразы = карточка навыка или официальная справка |
| ⚠️ Ограничения | 2 | Одна офиц. фраза или мало отзывов — см. таблицу |

| ID | Верификация | Источник (канон) | Заметки |
| --- | --- | --- | --- |
| `kids_barney` | ✅ | [c8ce0130-zagadki-medvezhonk](https://dialogs.yandex.ru/store/skills/c8ce0130-zagadki-medvezhonk) | Официальный навык; 9k оценок |
| `kids_milya_za_miley` | ✅ | [3c067c6b-milya-za-mile](https://dialogs.yandex.ru/store/skills/3c067c6b-milya-za-mile) | 593 оценки |
| `kids_pochemu_krokodily_ne_letayut` | ✅ | [d5018de2-pochemu-krokodily-ne-letayu](https://dialogs.yandex.ru/store/skills/d5018de2-pochemu-krokodily-ne-letayu) | Премия Алисы; 978 оценок |
| `kids_zombi_dogonyayut` | ⚠️ | [games_trivia](https://dialogs.yandex.ru/store/categories/games_trivia_accessories) | **1** офиц. фраза; рейтинг 3,1; прямой slug карточки недоступен |
| `kids_ohota_na_vampusa` | ✅ | [96f0d0b7-ohota-na-vampusa](https://dialogs.yandex.ru/store/skills/96f0d0b7-ohota-na-vampusa) | 539 оценок, 4,5 |
| `kids_more_priklyucheniy` | ⚠️ | [722dda3c-morskie-priklyuchen](https://dialogs.yandex.ru/store/skills/722dda3c-morskie-priklyuchen) | **1** офиц. фраза; 281 оценка |
| `kids_smeshariki_v_mire_dinozavrov` | ✅ | [955a5db8-kvest-pro-dinozavrov](https://dialogs.yandex.ru/store/skills/955a5db8-kvest-pro-dinozavrov) | ≠ `kids_smeshariki`; 4k оценок |
| `kids_parlament_plyushkina` | ✅ | [bf660d17-parlament-plyushki](https://dialogs.yandex.ru/store/skills/bf660d17-parlament-plyushki) | **1** фраза; 20 оценок; не «Садик Плюшкина» |
| `general_zdorovaya_razminka` | ✅ | [33a057a6-zdorovaya-razmink](https://dialogs.yandex.ru/store/skills/33a057a6-zdorovaya-razmink) | **1** фраза; мало отзывов (28) |
| `general_fizruk_poschitay` | ✅ | [d44c9efd-fizruk-poschitaj](https://dialogs.yandex.ru/store/skills/d44c9efd-fizruk-poschitaj) | Фраза «До 30» — как на карточке |
| `timers_postav_vstrechu_s_direktorom` | ✅ | [non-obvious](https://alice.yandex.ru/support/ru/station/skills/non-obvious) | Диапазон времени; дополняет calendar-команды |
| `obscure_davai_zapishiem_kapsulu_vremeni` | ✅ | [non-obvious](https://alice.yandex.ru/support/ru/station/skills/non-obvious) | ≠ «отправь послание в будущее» |
| `obscure_shepot` | ✅ | [non-obvious](https://alice.yandex.ru/support/ru/station/skills/non-obvious) | Режим тихого ответа |
| `quick_answers_skolko_v_rublyakh_254_evro` | ✅ | [browser examples](https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples) | Конвертация суммы, не spot-курс |

### Уровни верификации

| Метка | Значение |
| --- | --- |
| `official-support` | Справка Яндекс Станции / Алисы |
| `official-browser` | Справка Яндекс Браузера (Android) |
| `catalog-skill` | Активный навык на dialogs.yandex.ru |
| `catalog-category` | Фраза подтверждена категорией games; карточка навыка недоступна по прямому URL |

### Уже есть — не добавлять

| Что искали | Чем покрыто |
| --- | --- |
| «Занимательные истории» / «Чепуха» | `kids_chepuha` |
| «Забавные истории» | Отдельный навык, но тот же жанр fill-in-the-blank; дубль UX с `kids_chepuha` |
| «Садик Плюшкина» (ROUND2 Tier 2) | **Ошибка названия** — в каталоге навык «Парламент Плюшкина» |
| «Храм Йоги» | Нет в [health_fitness](https://dialogs.yandex.ru/store/categories/health_fitness) на 2026-07-11 |
| «Игра в Твистер» / «Мой Твистер» | Нет карточки с канон. фразами на dialogs.yandex.ru |
| «Город Котопёсия» | Навык с таким названием не найден в каталоге |
| «Животные и их детёныши» | Карточка [244f069e](https://dialogs.yandex.ru/store/skills/244f069e-zhivotnye-i-ih-detyony) недоступна — не верифицировано |
| «На краю пустоши» | ~10 оценок; одна фраза; нишевый сюжет |
| «Угадай цену», «Счастливый фермер» | Нет подтверждённых канон. фраз |
| «Курс евро / доллара» (spot) | `general_kurs_*` |
| «Отправь послание в будущее» | `obscure_pomozhet_otpravit_vam_poslanie_v_budushc` |
| «Шёпотом» | `obscure_shepotom` — другая офиц. формулировка; ROUND3 добавляет «шепот» из non-obvious |
| «Добавь в список дел» | `general_dobav_v_spisok_del` (ROUND2) |
| «Сыграем в Нубик 2» | `kids_nubik_2` (фраза есть; см. обогащение `source_url`) |

### Исключены

| Команда | Причина |
| --- | --- |
| «Сыграем в Зомби догоняют» | Есть только на aliceskill.ru, **нет** на карточке dialogs |
| «Запустi Игру Нубик» | На карточке Нубика помечено как **не работает** |
| «Игра Зомби» (ae0c87c2) | Другой навык, не «Зомби догоняют» |
| Meta-discovery («найди игру про…») | Нестабильная phrase |

### Группы для новых команд

| Группа | Новые команды |
| --- | --- |
| `kids_catalog_skills` | Барни, Миля, крокодилы, зомби, Вампус, Море приключений, Смешарики-динозавры, Парламент Плюшкина |
| `general_health_skills` | Здоровая разминка, Физрук посчитай |
| `timers_reminder` | встреча с директором (диапазон) |
| `obscure_hidden` | капсула времени, шёпот |
| `general_info` | конвертация 254 € → ₽ |

---

## Обогащение существующих (не новые id)

Применить **StrReplace** в `seed/catalog-audit-fixed.json` при импорте партии:

### `kids_nubik` — доп. фразы с карточки

Карточка [6069ecfd-igra-nubik](https://dialogs.yandex.ru/store/skills/6069ecfd-igra-nubik) также содержит:

- «Сыграем в Нубик 1»
- «Давай поиграем в Игра Нубик»

### `kids_nubik_2` — исправить `source_url` и фразу

| Поле | Сейчас | Канон |
| --- | --- | --- |
| `source_url` | `…/store/games` | `https://dialogs.yandex.ru/store/skills/dc515d9b-nubik-2` |
| `phrases` | нет «Давай сыграем в Нубик два» | добавить с [карточки](https://dialogs.yandex.ru/store/skills/dc515d9b-nubik-2) |

---

## Команды к добавлению (14)

Формат: полный объект `command` по schema v2.  
`updated_at` для всех новых записей: `2026-07-11T00:00:00Z`.

---

### 1. Навыки — детские игры (8)

#### `kids_barney` — Барни

```json
{
  "id": "kids_barney",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 360,
  "variant_label_ru": "Барни",
  "is_primary_in_group": false,
  "title_ru": "Барни",
  "phrases": [
    "Алиса, запусти навык Барни",
    "Алиса, поиграем в Барни",
    "Алиса, сыграем в Барни"
  ],
  "effect_description_ru": "Экспедиция с Барни и Николаем Дроздовым: загадки и факты о животных. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_detskaia_viktorina"],
  "search_aliases": ["барни", "дроздов", "животные", "экспедиция"],
  "source_url": "https://dialogs.yandex.ru/store/skills/c8ce0130-zagadki-medvezhonk",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill", "official"]
}
```

#### `kids_milya_za_miley` — Миля за милей

```json
{
  "id": "kids_milya_za_miley",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 365,
  "variant_label_ru": "Миля за милей",
  "is_primary_in_group": false,
  "title_ru": "Миля за милей",
  "phrases": [
    "Алиса, запусти навык Миля за милей",
    "Алиса, давай поиграем в Миля за милей",
    "Алиса, сыграем в Миля за милей"
  ],
  "effect_description_ru": "Настольная гонка до 1000 миль: карты хода, саботаж соперника. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["миля", "гонка", "карточная игра"],
  "source_url": "https://dialogs.yandex.ru/store/skills/3c067c6b-milya-za-mile",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_pochemu_krokodily_ne_letayut` — Почему крокодилы не летают

```json
{
  "id": "kids_pochemu_krokodily_ne_letayut",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 370,
  "variant_label_ru": "Почему крокодилы не летают",
  "is_primary_in_group": false,
  "title_ru": "Почему крокодилы не летают",
  "phrases": [
    "Алиса, запусти навык Почему крокодилы не летают",
    "Алиса, сыграем в Почему крокодилы не летают",
    "Алиса, давай сыграем в Почему крокодилы не летают"
  ],
  "effect_description_ru": "50 способов «запустить» крокодильчика в полёт — отгадай 10 правильных. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["крокодил", "летать", "викторина"],
  "source_url": "https://dialogs.yandex.ru/store/skills/d5018de2-pochemu-krokodily-ne-letayu",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_zombi_dogonyayut` — Зомби догоняют

```json
{
  "id": "kids_zombi_dogonyayut",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 375,
  "variant_label_ru": "Зомби догоняют",
  "is_primary_in_group": false,
  "title_ru": "Зомби догоняют",
  "phrases": [
    "Алиса, запусти навык Зомби догоняют"
  ],
  "effect_description_ru": "Текстовое выживание: доберитесь до бункера за 100 км, выбирая одно из трёх действий на ход. В игре: ПОВТОРИ, ПОМОЩЬ, СНАЧАЛА. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["зомби", "выживание", "квест"],
  "source_url": "https://dialogs.yandex.ru/store/categories/games_trivia_accessories",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_ohota_na_vampusa` — Охота на Вампуса

```json
{
  "id": "kids_ohota_na_vampusa",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 380,
  "variant_label_ru": "Охота на Вампуса",
  "is_primary_in_group": false,
  "title_ru": "Охота на Вампуса",
  "phrases": [
    "Алиса, запусти навык Охота на Вампуса",
    "Алиса, сыграем в Охоту на Вампуса",
    "Алиса, давай поиграем в Охоту на Вампуса"
  ],
  "effect_description_ru": "Исследование пещеры на слух: охота на монстра, артефакты, лавка торговца. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["вампус", "пещера", "монстр", "охота"],
  "source_url": "https://dialogs.yandex.ru/store/skills/96f0d0b7-ohota-na-vampusa",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_more_priklyucheniy` — Море приключений

```json
{
  "id": "kids_more_priklyucheniy",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 385,
  "variant_label_ru": "Море приключений",
  "is_primary_in_group": false,
  "title_ru": "Море приключений",
  "phrases": [
    "Алиса, запусти навык Море приключений"
  ],
  "effect_description_ru": "Морской онлайн-квест: пираты, загадки Сфинкса, прокачка корсара и банды. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_kapitan_banalnost"],
  "search_aliases": ["море", "пираты", "корсар"],
  "source_url": "https://dialogs.yandex.ru/store/skills/722dda3c-morskie-priklyuchen",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_smeshariki_v_mire_dinozavrov` — Смешарики в мире динозавров

```json
{
  "id": "kids_smeshariki_v_mire_dinozavrov",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 390,
  "variant_label_ru": "Смешарики: динозавры",
  "is_primary_in_group": false,
  "title_ru": "Смешарики в мире динозавров",
  "phrases": [
    "Алиса, запусти навык Смешарики в мире динозавров",
    "Алиса, запусти навык Квест про динозавров"
  ],
  "effect_description_ru": "Квест: спасти Смешариков в мире динозавров. На части сцен нужно приложение Яндекса с визуалом. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_smeshariki"],
  "search_aliases": ["смешарики", "динозавры", "квест"],
  "source_url": "https://dialogs.yandex.ru/store/skills/955a5db8-kvest-pro-dinozavrov",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill", "official"]
}
```

#### `kids_parlament_plyushkina` — Парламент Плюшкина

```json
{
  "id": "kids_parlament_plyushkina",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 395,
  "variant_label_ru": "Парламент Плюшкина",
  "is_primary_in_group": false,
  "title_ru": "Парламент Плюшкина",
  "phrases": [
    "Алиса, запусти навык Парламент Плюшкина"
  ],
  "effect_description_ru": "Сатирическая мини-игра: подкуп депутатов иностранного парламента. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["плюшкин", "парламент", "сатира"],
  "source_url": "https://dialogs.yandex.ru/store/skills/bf660d17-parlament-plyushki",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

---

### 2. Здоровье (2)

#### `general_zdorovaya_razminka` — Здоровая разминка

```json
{
  "id": "general_zdorovaya_razminka",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 40,
  "variant_label_ru": "Здоровая разминка",
  "is_primary_in_group": false,
  "title_ru": "Здоровая разминка",
  "phrases": [
    "Алиса, запусти навык Здоровая разминка"
  ],
  "effect_description_ru": "Программа разминки на каждый день недели с таймером и музыкой. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_bystraia_trenirovka", "general_tabata_trener"],
  "search_aliases": ["разминка", "зарядка", "фитнес"],
  "source_url": "https://dialogs.yandex.ru/store/skills/33a057a6-zdorovaya-razmink",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

#### `general_fizruk_poschitay` — Физрук, посчитай!

```json
{
  "id": "general_fizruk_poschitay",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 45,
  "variant_label_ru": "Физрук, посчитай",
  "is_primary_in_group": false,
  "title_ru": "Физрук, посчитай!",
  "phrases": [
    "Алиса, запусти навык Физрук посчитай",
    "Алиса, попроси Физрука посчитать до 30"
  ],
  "effect_description_ru": "Зарядка с голосовым счётом: три комплекса, три табаты, пять скоростей. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_tabata_trener"],
  "search_aliases": ["физрук", "счёт", "зарядка"],
  "source_url": "https://dialogs.yandex.ru/store/skills/d44c9efd-fizruk-poschitaj",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

---

### 3. Официальная справка (4)

#### `timers_postav_vstrechu_s_direktorom` — Встреча с директором (диапазон)

```json
{
  "id": "timers_postav_vstrechu_s_direktorom",
  "category_id": "timers",
  "group_id": "timers_reminder",
  "sort_order": 125,
  "variant_label_ru": "Встреча с директором",
  "is_primary_in_group": false,
  "title_ru": "Поставь встречу с директором",
  "phrases": [
    "Алиса, поставь встречу с директором с 13:00 до 15:00",
    "поставь встречу с директором с 13:00 до 15:00"
  ],
  "effect_description_ru": "Добавит встречу в календарь с указанным временем начала и окончания. Нужно: устройство с Алисой; подключённый календарь.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [
    "timers_postav_vstrechu_s_roditeliami_v_10",
    "timers_ustanovi_v_piatnitsu_na_14_00_vstrechu_s"
  ],
  "search_aliases": ["календарь", "встреча", "директор"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["timers", "calendar", "official"]
}
```

#### `obscure_davai_zapishiem_kapsulu_vremeni` — Капсула времени

```json
{
  "id": "obscure_davai_zapishiem_kapsulu_vremeni",
  "category_id": "obscure",
  "group_id": "obscure_hidden",
  "sort_order": 85,
  "variant_label_ru": "Капсула времени",
  "is_primary_in_group": false,
  "title_ru": "Запишем капсулу времени",
  "phrases": [
    "Алиса, давай запишем капсулу времени",
    "давай запишем капсулу времени"
  ],
  "effect_description_ru": "Запишет голосовое послание себе в будущее (капсула времени). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["obscure_pomozhet_otpravit_vam_poslanie_v_budushc"],
  "search_aliases": ["капсула", "будущее", "послание"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["obscure", "official"]
}
```

#### `obscure_shepot` — Шёпот

```json
{
  "id": "obscure_shepot",
  "category_id": "obscure",
  "group_id": "obscure_hidden",
  "sort_order": 90,
  "variant_label_ru": "Шёпот",
  "is_primary_in_group": false,
  "title_ru": "Шёпот",
  "phrases": [
    "Алиса, шепот",
    "шепот"
  ],
  "effect_description_ru": "Переведёт Алису в режим шёпота: тихий ответ и лучшее распознавание тихой речи. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["obscure_shepotom"],
  "search_aliases": ["шёпот", "тихо", "ночь"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["obscure", "official"]
}
```

#### `quick_answers_skolko_v_rublyakh_254_evro` — Конвертация валюты

```json
{
  "id": "quick_answers_skolko_v_rublyakh_254_evro",
  "category_id": "quick_answers",
  "group_id": "qa_facts",
  "sort_order": 70,
  "variant_label_ru": "Конвертация валюты",
  "is_primary_in_group": false,
  "title_ru": "Сколько в рублях будет 254 евро",
  "phrases": [
    "Алиса, сколько в рублях будет 254 евро",
    "сколько в рублях будет 254 евро"
  ],
  "effect_description_ru": "Пересчитает сумму в евро (или другой валюте) в рубли по актуальному курсу. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kurs_euro", "general_kurs_dollara"],
  "search_aliases": ["конвертация", "евро", "рубли", "курс"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-11T00:00:00Z",
  "tags": ["quick_answers", "finance", "official-browser"]
}
```

---

## Импорт

1. Вручную добавить JSON-блоки в `seed/catalog-audit-fixed.json` (StrReplace / Write).
2. `.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"`
3. `.\scripts\push-draft.ps1` → Publish в admin при diff.

После импорта: **871 + 14 = 885** команд (+ обогащение `kids_nubik` / `kids_nubik_2`).
