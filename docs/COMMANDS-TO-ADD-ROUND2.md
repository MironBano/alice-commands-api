# Команды для добавления — раунд 2

> **Дата проверки:** 2026-07-10 (создание) · **верификация фраз:** 2026-07-10 (audit)  
> **Эталон каталога:** `seed/catalog-audit-fixed.json` (**871** команда, schema v2, `content_version=50`)  
> **Предыдущий раунд:** [COMMANDS-TO-ADD.md](./COMMANDS-TO-ADD.md) (79 команд — уже на staging)  
> **Метод:** сверка **канонических фраз запуска** со страницами навыков на [dialogs.yandex.ru](https://dialogs.yandex.ru/store), [alice.yandex.ru/skills](https://alice.yandex.ru/skills) и [справкой Станции](https://alice.yandex.ru/support/ru/station/skills/).

## Сводка

| Метрика | Значение |
| --- | --- |
| Кандидатов из поиска | ~52 |
| Уже есть / частично покрыто | 16 |
| Исключены (дубль / не voice / низкая уверенность) | 12 |
| **К добавлению после audit (полный JSON ниже)** | **28** |
| Tier 2 (краткий список, без JSON) | 10 |

### Результат верификации (2026-07-10)

| Статус | Кол-во | Что сделано |
| --- | --- | --- |
| ✅ Подтверждено | 24 | Фразы сверены с карточкой навыка / справкой |
| 🔄 Заменено | 2→4 | Убраны нерабочие/дубли; добавлены голосовые детские сценарии Алиса Плюс |
| ⚠️ Ограничения | 2 | См. колонку «Заметки» в таблице ниже |

| ID | Верификация | Источник (канон) | Заметки |
| --- | --- | --- | --- |
| `general_multizadachnost` | ✅ | [non-obvious](https://alice.yandex.ru/support/ru/station/skills/non-obvious) | Фича, не навык; фразы из справки |
| `alice_plus_davai_sdelai_zaryadku` | ✅ | [alice-plus/kids](https://alice.yandex.ru/support/ru/station/skills/) | `requires_plus`; расписание — в приложении |
| `alice_plus_khochu_v_domik` | ✅ | alice-plus/kids | Квесты 3–5 лет |
| `alice_plus_khochu_vse_znat` | ✅ | alice-plus/kids | Квесты 5–8 лет |
| `alice_plus_mne_pora_spat` | ✅ | alice-plus/kids | «Спокойной ночи» |
| `station_sygraj_svoyu_melodiyu` | ⚠️ | [station/skills](https://alice.yandex.ru/support/ru/station/skills/) | Только **Станция Мини 1** |
| `timers_muzyka_na_budilnik` | ✅ | [musical-alarm](https://yandex.ru/alice/skills/musical-alarm) | `requires_plus`; фразы с офиц. страницы |
| `calls_vklyuchi_radionyanyu_v_detskoy` | ✅ | [radionyania](https://yandex.ru/alice/skills/radionyania) | Нужно имя комнаты в «Дом с Алисой» |
| `general_dobav_v_spisok_del` | ✅ | [non-obvious](https://alice.yandex.ru/support/ru/station/skills/non-obvious) + [to-do-list](https://yandex.ru/alice/skills/to-do-list) | Дополняет `general_spisok_del` |
| `kids_vo_chto_poigrat` | ✅ | dialogs games + обзоры | Discovery, не дубль «во что поиграем?» |
| `kids_nubik` | ✅ | [igra-nubik](https://dialogs.yandex.ru/store/skills/6069ecfd-igra-nubik) | ❌ «запусти **Игру** Нубик» не работает |
| `kids_nubik_2` | ✅ | dialogs games | «Сыграем в Нубик 2» |
| `kids_lavka_nubika` | ✅ | dialogs games | «Сыграем в Лавку Нубика» |
| `kids_smeshariki` | ✅ | [smeshariki](https://dialogs.yandex.ru/store/skills/03f16868-smeshariki) | |
| `kids_igra_vyshibaly` | ✅ | dialogs games | |
| `kids_samyi_umnyi` | ✅ | [slaboe-zveno](https://dialogs.yandex.ru/store/skills/7c5f2c0a-slaboe-zveno) | |
| `kids_kapitan_banalnost` | ✅ | [banal-nosti](https://dialogs.yandex.ru/store/skills/a77ad7a4-igra-v-banal-nosti) | |
| `kids_igra_pryatki` | ✅ | [pryatk](https://dialogs.yandex.ru/store/skills/f07baab9-pryatk) | Одна офиц. фраза запуска |
| `kids_s_pervoy_noty` | ⚠️ | dialogs games | Вторую phrase — эвристика, не в карточке |
| `kids_vopros_i_nakazanie` | ✅ | dialogs games | |
| `kids_put_geroev` | ✅ | [geroi-mecha](https://dialogs.yandex.ru/store/skills/91f1ddc1-geroi-mecha-i-magi) | ≠ `kids_alisa_kvest_put_geroia` |
| `kids_gonka_umov` | ✅ | dialogs games | |
| `kids_bolshoy_futbol` | ✅ | dialogs games | |
| `kids_shakhmaty_vslepu` | ✅ | [shahmaty-vslepu](https://dialogs.yandex.ru/store/skills/4edf5458-shahmaty-vslepu) | |
| `general_umnye_recepty` | ✅ | [umnye-recepty](https://dialogs.yandex.ru/store/skills/bc631b01-umnye-recepty) | |
| `kids_igra_kulinariya` | ✅ | [igra-kulinariya](https://dialogs.yandex.ru/store/skills/09848f16-igra-kulinariya) | |
| `general_tabata_trener` | ✅ | [tabata](https://dialogs.yandex.ru/store/skills/82b6097a-tabata-trenirovka) | Навык «Табата **тренер**» |
| `general_yoga_dlya_glaz` | ✅ | health_fitness | «Запусти навык Йога для глаз» |

**Убрано после audit (не добавлять):**

| ID (черновик) | Причина |
| --- | --- |
| `kids_potreniruy_rech` | Дубль: навык **«Легко сказать»** — уже `kids_legko_skazat`, `kids_zapusti_legko_skazat` |
| `alice_plus_nastroj_detskie_scenarii` | **Нет voice-команды**; расписание только в приложении «Дом с Алисой» |

### Уровни верификации

| Метка | Значение |
| --- | --- |
| `official-support` | Справка Яндекс Станции / Алисы |
| `official-browser` | Справка Яндекс Браузера (Android) |
| `catalog-skill` | Активный навык на dialogs.yandex.ru |
| `catalog-skill+` | Каталог + обзоры 2025–2026, формулировки совпадают |

### Уже есть — не добавлять

| Что искали | Чем покрыто |
| --- | --- |
| «Во что поиграем?» | `kids_davai_poigraem` |
| «Давай сыграем в слова» / «Верю — не верю» | `kids_davai_sygraem_v_slova`, `kids_veriu_ne_veriu` |
| «Сто к одному» | `kids_ugadai_otvet` (phrase + alias) |
| «Путь героя» (квест Алисы) | `kids_alisa_kvest_put_geroia` |
| «Быстрая тренировка» / «Умный счётчик калорий» | `general_bystraia_trenirovka`, `general_umnyi_schetchik_kalorii` |
| «Запомни / забудь меня» | `obscure_zapomni_menia` |
| «Поставь эту песню на будильник» | `timers_postav_etu_pesniu_na_budilnik_esli_igrae` |
| «Потренируй речь» / «Легко сказать» | `kids_legko_skazat`, `kids_zapusti_legko_skazat` |
| Передача сообщения между комнатами | `calls_pozhaluista_nagreite_vodu_v_chainike` (+ группа `calls_messages`) |
| «Курс доллара / евро / йены» | `general_kurs_*`, `quick_answers_*` |
| «Расскажи новости с сайта …» | phrase в `general_novosti_sporta` и др. |
| «Как дела?» | phrase в `obscure_*` / болталка |

### Исключены

| Команда | Причина |
| --- | --- |
| «Персонализированное общение» | Покрывается `obscure_zapomni_menia` + настройки аккаунта |
| «Найди игру про …» | Meta-поиск, нет стабильной phrase |
| «Какие есть новые викторины?» | Discovery UI, не продуктовая команда |
| «Мой сахар», «Доктор Слух» | Нужна предварительная настройка / мед. контекст |
| «Простой гипноз», «Без паники» | Узкая ниша, нет массового спроса в каталоге |
| «Забавные истории» | Дубль «Чепухи» / «Занимательных историй» |
| «Настрой детские сценарии» (voice) | Расписание только в приложении; голосом — «хочу в домик» и др. (см. JSON ниже) |
| «Игра сто к одному» (отдельный навык) | Уже alias у `kids_ugadai_otvet` |

### Группы для новых команд

Расширить существующие группы (новые `command_groups` не обязательны):

| Группа | Новые команды |
| --- | --- |
| `kids_catalog_skills` | игры из каталога (sort_order 220–350), incl. `kids_igra_kulinariya` |
| `general_health_skills` | табата тренер, йога для глаз |
| `general_cooking` | умные рецепты |
| `general_official_skills` | мультизадачность |
| `kids_education` | *(удалено)* «потренируй речь» — дубль «Легко сказать» |
| `timers_reminder` | добавь в список дел |
| `alice_plus_kids_routine` | «давай сделаем зарядку», «хочу в домик», «хочу все знать», «мне пора спать» |
| `calls_babymonitor` | радионяня в детской |
| `timers_alarm` | музыка на будильник |
| `station_personality` | сыграй свою мелодию (Станция Мини 1) |
| `kids_games` | «во что поиграть» (discovery) |

---

## Tier 2 — краткий список (без JSON, добавить позже)

| ID (черновик) | Навык | Источник |
| --- | --- | --- |
| `kids_barney` | Барни (экспедиция с Дроздовым) | dialogs.yandex.ru/store/games |
| `kids_milya_za_miley` | Миля за милей | dialogs.yandex.ru/store/games |
| `kids_sadik_plyushkina` | Садик Плюшкина | dialogs.yandex.ru/store/games |
| `kids_pochemu_krokodily_ne_lletayut` | Почему крокодилы не летают | dialogs.yandex.ru/store/games |
| `kids_zombi_dogonyayut` | Зомби догоняют | dialogs.yandex.ru/store/games |
| `kids_twister` | Игра в Твистер | t-j.ru комментарии / каталог |
| `general_zdorovaya_razminka` | Здоровая разминка | dialogs …/health_fitness |
| `general_fizruk_poschitay` | Физрук, посчитай! | dialogs …/health_fitness |
| `general_yoga_hram` | Храм Йоги | dialogs …/health_fitness |
| `quick_answers_konvertiruy_valyutu` | «Сколько в рублях будет 254 евро» | official-browser |

---

## Команды к добавлению (28)

Формат: полный объект `command` по schema v2.  
`updated_at` для всех новых записей: `2026-07-10T20:00:00Z`.  
У skill-команд в `source_url` — **прямая ссылка на карточку навыка** на dialogs.yandex.ru (где есть карточка); discovery-команды — на каталог games.

---

### 1. Официальная справка и Алиса Плюс (10)

#### `general_multizadachnost` — Мультизадачность

```json
{
  "id": "general_multizadachnost",
  "category_id": "general",
  "group_id": "general_official_skills",
  "sort_order": 15,
  "variant_label_ru": "Мультизадачность",
  "is_primary_in_group": false,
  "title_ru": "Мультизадачность",
  "phrases": [
    "Алиса, включи музыку и сделай громкость 20",
    "Алиса, включи свет и выключи музыку"
  ],
  "effect_description_ru": "Выполнит два действия в одной фразе (мультизадачность Алисы). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone", "tv"],
  "related_command_ids": ["music_vkliuchi_muzyku", "general_gromche"],
  "search_aliases": ["две команды", "сразу два", "мультизадачность"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["general", "official", "feature"]
}
```

#### `alice_plus_davai_sdelai_zaryadku` — Давай сделаем зарядку

```json
{
  "id": "alice_plus_davai_sdelai_zaryadku",
  "category_id": "alice_plus",
  "group_id": "alice_plus_kids_routine",
  "sort_order": 5,
  "variant_label_ru": "Детская зарядка",
  "is_primary_in_group": false,
  "title_ru": "Давай сделаем зарядку",
  "phrases": [
    "Алиса, давай сделаем зарядку",
    "Алиса, хочу сделать зарядку"
  ],
  "effect_description_ru": "Запустит детский сценарий «Зарядка» (Алиса Плюс). Расписание — в приложении «Дом с Алисой». Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "phone"],
  "related_command_ids": ["alice_plus_razbudit_rebenka_ego_liubimoi_melodiei_p"],
  "search_aliases": ["зарядка", "дети", "сценарий"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/kids",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["alice_plus", "kids", "official"]
}
```

#### `alice_plus_khochu_v_domik` — Хочу в домик

```json
{
  "id": "alice_plus_khochu_v_domik",
  "category_id": "alice_plus",
  "group_id": "alice_plus_kids_routine",
  "sort_order": 10,
  "variant_label_ru": "Хочу в домик",
  "is_primary_in_group": false,
  "title_ru": "Хочу в домик",
  "phrases": [
    "Алиса, хочу в домик",
    "Алиса, давай в домик"
  ],
  "effect_description_ru": "Запустит детский сценарий с квестами для 3–5 лет (Алиса Плюс). Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "phone"],
  "related_command_ids": ["alice_plus_davai_sdelai_zaryadku"],
  "search_aliases": ["домик", "квест", "малыши"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/kids",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["alice_plus", "kids", "official"]
}
```

#### `alice_plus_khochu_vse_znat` — Хочу все знать

```json
{
  "id": "alice_plus_khochu_vse_znat",
  "category_id": "alice_plus",
  "group_id": "alice_plus_kids_routine",
  "sort_order": 15,
  "variant_label_ru": "Хочу все знать",
  "is_primary_in_group": false,
  "title_ru": "Хочу все знать",
  "phrases": [
    "Алиса, хочу все знать",
    "Алиса, давай все узнаем"
  ],
  "effect_description_ru": "Запустит детский сценарий с квестами для 5–8 лет (Алиса Плюс). Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "phone"],
  "related_command_ids": ["alice_plus_khochu_v_domik"],
  "search_aliases": ["квест", "школьники", "познавательное"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/kids",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["alice_plus", "kids", "official"]
}
```

#### `alice_plus_mne_pora_spat` — Мне пора спать

```json
{
  "id": "alice_plus_mne_pora_spat",
  "category_id": "alice_plus",
  "group_id": "alice_plus_kids_routine",
  "sort_order": 20,
  "variant_label_ru": "Мне пора спать",
  "is_primary_in_group": false,
  "title_ru": "Мне пора спать",
  "phrases": [
    "Алиса, мне пора спать",
    "Алиса, пора спать"
  ],
  "effect_description_ru": "Запустит вечерний детский сценарий «Спокойной ночи» (Алиса Плюс). Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "phone"],
  "related_command_ids": ["alice_plus_khochu_vse_znat"],
  "search_aliases": ["сон", "ночь", "спокойной ночи"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/kids",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["alice_plus", "kids", "official"]
}
```

#### `station_sygraj_svoyu_melodiyu` — Сыграй свою мелодию

```json
{
  "id": "station_sygraj_svoyu_melodiyu",
  "category_id": "station_settings",
  "group_id": "station_personality",
  "sort_order": 50,
  "variant_label_ru": "Своя мелодия",
  "is_primary_in_group": false,
  "title_ru": "Сыграй свою мелодию",
  "phrases": [
    "Алиса, сыграй свою мелодию",
    "Алиса, сыграем свою мелодию"
  ],
  "effect_description_ru": "Запустит режим «сыграй свою мелодию» на поддерживаемой Станции (в справке — Станция Мини 1‑го поколения). Нужно: совместимая колонка Яндекса.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station"],
  "related_command_ids": [],
  "search_aliases": ["мелодия", "импровизация", "мини"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["station", "music", "official"]
}
```

#### `timers_muzyka_na_budilnik` — Музыка на будильник

```json
{
  "id": "timers_muzyka_na_budilnik",
  "category_id": "timers",
  "group_id": "timers_alarm",
  "sort_order": 180,
  "variant_label_ru": "Музыка на будильник",
  "is_primary_in_group": false,
  "title_ru": "Музыка на будильник",
  "phrases": [
    "Алиса, поставь на будильник Моцарта",
    "Алиса, установи на будильник мой плейлист"
  ],
  "effect_description_ru": "Назначит трек или плейлист из Яндекс Музыки на будильник (навык «Музыка на будильник»). Нужно: устройство с Алисой; подписка Яндекс Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "phone"],
  "related_command_ids": ["timers_postav_etu_pesniu_na_budilnik_esli_igrae"],
  "search_aliases": ["будильник", "мелодия", "музыка", "плейлист"],
  "source_url": "https://yandex.ru/alice/skills/musical-alarm",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["timers", "music", "official"]
}
```

#### `calls_vklyuchi_radionyanyu_v_detskoy` — Радионяня в детской

```json
{
  "id": "calls_vklyuchi_radionyanyu_v_detskoy",
  "category_id": "calls",
  "group_id": "calls_babymonitor",
  "sort_order": 20,
  "variant_label_ru": "Радионяня в детской",
  "is_primary_in_group": false,
  "title_ru": "Включи радионяню в детской",
  "phrases": [
    "Алиса, включи радионяню в детской",
    "Алиса, включи радионяню в детской комнате"
  ],
  "effect_description_ru": "Включит режим радионяни в указанной комнате (аудиомонитор для родителей). Нужно: две колонки или колонка + приложение «Дом с Алисой».",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["calls_vkliuchi_radioniania", "calls_vykliuchi_radioniania"],
  "search_aliases": ["радионяня", "детская", "монитор"],
  "source_url": "https://yandex.ru/alice/skills/radionyania",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["calls", "kids", "official"]
}
```

#### `general_dobav_v_spisok_del` — Добавь в список дел

```json
{
  "id": "general_dobav_v_spisok_del",
  "category_id": "timers",
  "group_id": "timers_reminder",
  "sort_order": 210,
  "variant_label_ru": "Добавь в список дел",
  "is_primary_in_group": false,
  "title_ru": "Добавь в список дел",
  "phrases": [
    "Алиса, добавь в список дел уборку",
    "Алиса, добавь в список дел на сегодня"
  ],
  "effect_description_ru": "Добавит пункт в голосовой список дел (навык to-do-list). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone", "tv"],
  "related_command_ids": ["general_spisok_del"],
  "search_aliases": ["todo", "задачи", "список дел"],
  "source_url": "https://yandex.ru/alice/skills/to-do-list",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["timers", "productivity", "official"]
}
```

#### `kids_vo_chto_poigrat` — Во что поиграть

```json
{
  "id": "kids_vo_chto_poigrat",
  "category_id": "kids",
  "group_id": "kids_games",
  "sort_order": 5,
  "variant_label_ru": "Во что поиграть",
  "is_primary_in_group": false,
  "title_ru": "Во что поиграть",
  "phrases": [
    "Алиса, во что поиграть?",
    "Алиса, во что поиграть"
  ],
  "effect_description_ru": "Предложит подборку игровых навыков из каталога. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_davai_poigraem"],
  "search_aliases": ["игры", "подборка", "discovery"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "discovery"]
}
```

---

### 2. Игры из каталога (14)

**Верификация:** `catalog-skill+`  
**Группа:** `kids_catalog_skills` (sort_order 220–350)

#### `kids_nubik` — Игра Нубик

```json
{
  "id": "kids_nubik",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 220,
  "variant_label_ru": "Нубик",
  "is_primary_in_group": false,
  "title_ru": "Игра Нубик",
  "phrases": [
    "Алиса, сыграем в игру Нубик",
    "Алиса, поиграем в игру Нубик",
    "Алиса, запусти навык Игра Нубик"
  ],
  "effect_description_ru": "Запустит приключение «Нубик»: крафт, выживание, мультиплеер и рейтинг. Не работает «запусти Игру Нубик» — только «игру Нубик» или «навык Игра Нубик». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["нубик", "minecraft", "крафт"],
  "source_url": "https://dialogs.yandex.ru/store/skills/6069ecfd-igra-nubik",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_nubik_2` — Нубик 2

```json
{
  "id": "kids_nubik_2",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 225,
  "variant_label_ru": "Нубик 2",
  "is_primary_in_group": false,
  "title_ru": "Нубик 2",
  "phrases": [
    "Алиса, сыграем в Нубик 2",
    "Алиса, запусти навык Нубик 2"
  ],
  "effect_description_ru": "Продолжение «Нубика»: сражение с Драконом края, крафт и сюжетная кампания. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_nubik"],
  "search_aliases": ["нубик 2", "дракон"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_lavka_nubika` — Лавка Нубика

```json
{
  "id": "kids_lavka_nubika",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 230,
  "variant_label_ru": "Лавка Нубика",
  "is_primary_in_group": false,
  "title_ru": "Лавка Нубика",
  "phrases": [
    "Алиса, сыграем в Лавку Нубика",
    "Алиса, запусти навык Лавка Нубика"
  ],
  "effect_description_ru": "Запустит экономическую игру: магазин, огород, счёт в уме и кристаллы. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_nubik"],
  "search_aliases": ["лавка", "магазин", "нубик"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_smeshariki` — Смешарики

```json
{
  "id": "kids_smeshariki",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 240,
  "variant_label_ru": "Смешарики",
  "is_primary_in_group": false,
  "title_ru": "Смешарики",
  "phrases": [
    "Алиса, запусти навык Смешарики",
    "Алиса, давай поиграем со Смешариками"
  ],
  "effect_description_ru": "Запустит квесты со Смешариками: загадки и приключения с персонажами сериала. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["смешарики", "крош", "нюша"],
  "source_url": "https://dialogs.yandex.ru/store/skills/03f16868-smeshariki",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_igra_vyshibaly` — Игра Вышибалы

```json
{
  "id": "kids_igra_vyshibaly",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 250,
  "variant_label_ru": "Вышибалы",
  "is_primary_in_group": false,
  "title_ru": "Игра Вышибалы",
  "phrases": [
    "Алиса, запусти навык Игра Вышибалы",
    "Алиса, сыграем в вышибалы"
  ],
  "effect_description_ru": "Онлайн-экшен: подбор игроков, викторина в бою, рейтинг. Сессия ~10 минут. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["вышибалы", "онлайн", "дуэль"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_samyi_umnyi` — Самый умный

```json
{
  "id": "kids_samyi_umnyi",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 260,
  "variant_label_ru": "Самый умный",
  "is_primary_in_group": false,
  "title_ru": "Самый умный",
  "phrases": [
    "Алиса, поиграем в Самого умного",
    "Алиса, давай сыграем в Самого умного",
    "Алиса, запусти навык Самый умный"
  ],
  "effect_description_ru": "Викторина в духе «Слабого звена»: раунды, выбывание слабых звеньев, уровни сложности. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["слабое звено", "викторина", "эрудиция"],
  "source_url": "https://dialogs.yandex.ru/store/skills/7c5f2c0a-slaboe-zveno",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_kapitan_banalnost` — Капитан Банальность

```json
{
  "id": "kids_kapitan_banalnost",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 270,
  "variant_label_ru": "Капитан Банальность",
  "is_primary_in_group": false,
  "title_ru": "Капитан Банальность",
  "phrases": [
    "Алиса, запусти навык Капитан Банальность",
    "Алиса, сыграем в Капитан Банальность",
    "Алиса, давай поиграем в Капитан Банальность"
  ],
  "effect_description_ru": "Угадайте 5 популярных ассоциаций к слову; есть режим для компании у колонки. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_ugadai_otvet"],
  "search_aliases": ["банальность", "ассоциации", "100 к 1"],
  "source_url": "https://dialogs.yandex.ru/store/skills/a77ad7a4-igra-v-banal-nosti",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_igra_pryatki` — Игра прятки

```json
{
  "id": "kids_igra_pryatki",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 280,
  "variant_label_ru": "Прятки",
  "is_primary_in_group": false,
  "title_ru": "Игра прятки",
  "phrases": [
    "Алиса, запусти навык Игра прятки"
  ],
  "effect_description_ru": "Голосовые прятки с Элис: загадки и угадывание места; есть музыкальный режим. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["прятки", "элис", "загадки"],
  "source_url": "https://dialogs.yandex.ru/store/skills/f07baab9-pryatk",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_s_pervoy_noty` — С Первой Ноты

```json
{
  "id": "kids_s_pervoy_noty",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 290,
  "variant_label_ru": "С Первой Ноты",
  "is_primary_in_group": false,
  "title_ru": "С Первой Ноты",
  "phrases": [
    "Алиса, запусти навык С Первой Ноты",
    "Алиса, угадай мелодию с первой ноты"
  ],
  "effect_description_ru": "Музыкальная викторина: угадайте трек по фрагменту (режим «По нотам»). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["music_raspoznai_pesniu", "kids_muzykalnyi_turnir"],
  "search_aliases": ["мелодия", "угадай песню", "ноты"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "music", "skill"]
}
```

#### `kids_vopros_i_nakazanie` — Вопрос и наказание

```json
{
  "id": "kids_vopros_i_nakazanie",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 300,
  "variant_label_ru": "Вопрос и наказание",
  "is_primary_in_group": false,
  "title_ru": "Вопрос и наказание",
  "phrases": [
    "Алиса, запусти навык Вопрос и наказание",
    "Алиса, сыграем в вопрос и наказание"
  ],
  "effect_description_ru": "Викторина для компании: за ошибки — смешные «наказания»; музыкальные вопросы. Нужно: устройство с Алисой; интернет; лучше 2+ игрока.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_pravda_ili_lozh"],
  "search_aliases": ["наказание", "викторина", "вечеринка"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_put_geroev` — Путь героев

```json
{
  "id": "kids_put_geroev",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 310,
  "variant_label_ru": "Путь героев",
  "is_primary_in_group": false,
  "title_ru": "Путь героев",
  "phrases": [
    "Алиса, запусти навык Путь героев"
  ],
  "effect_description_ru": "RPG-навык по мотивам видеоигры: миссии, турниры, прокачка героя. Отдельно от «Алиса-квест: Путь героя». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_alisa_kvest_put_geroia"],
  "search_aliases": ["rpg", "герой", "квест"],
  "source_url": "https://dialogs.yandex.ru/store/skills/91f1ddc1-geroi-mecha-i-magi",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_gonka_umov` — Гонка умов

```json
{
  "id": "kids_gonka_umov",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 320,
  "variant_label_ru": "Гонка умов",
  "is_primary_in_group": false,
  "title_ru": "Гонка умов",
  "phrases": [
    "Алиса, запусти навык Гонка умов",
    "Алиса, сыграем в гонку умов"
  ],
  "effect_description_ru": "Викторина-квест: пройти 80 км через лес, отвечая на вопросы; можно играть компанией. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["гонка", "квест", "лес"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_bolshoy_futbol` — Большой Футбол

```json
{
  "id": "kids_bolshoy_futbol",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 330,
  "variant_label_ru": "Большой Футбол",
  "is_primary_in_group": false,
  "title_ru": "Большой Футбол",
  "phrases": [
    "Алиса, запусти навык Большой Футбол",
    "Алиса, сыграем в большой футбол"
  ],
  "effect_description_ru": "Футбольный навык: играйте за команду против других игроков онлайн. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["футбол", "спорт", "онлайн"],
  "source_url": "https://dialogs.yandex.ru/store/games",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_shakhmaty_vslepu` — Шахматы вслепую

```json
{
  "id": "kids_shakhmaty_vslepu",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 340,
  "variant_label_ru": "Шахматы вслепую",
  "is_primary_in_group": false,
  "title_ru": "Шахматы вслепую",
  "phrases": [
    "Алиса, давай поиграем в Шахматы вслепую",
    "Алиса, запусти навык Шахматы вслепую",
    "Алиса, сыграем в Шахматы вслепую"
  ],
  "effect_description_ru": "Шахматы без доски: ходы вслух, уровни сложности и подсказки (отдельный навык от «Шахматы»). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_shakhmaty"],
  "search_aliases": ["шахматы вслепую", "слепые шахматы"],
  "source_url": "https://dialogs.yandex.ru/store/skills/4edf5458-shahmaty-vslepu",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

---

### 3. Еда и здоровье (4)

#### `general_umnye_recepty` — Умные рецепты

```json
{
  "id": "general_umnye_recepty",
  "category_id": "general",
  "group_id": "general_cooking",
  "sort_order": 120,
  "variant_label_ru": "Умные рецепты",
  "is_primary_in_group": false,
  "title_ru": "Умные рецепты",
  "phrases": [
    "Алиса, запусти навык Умные рецепты",
    "Алиса, попроси Умные рецепты добавить молоко",
    "Алиса, спроси у Умных рецептов как приготовить борщ"
  ],
  "effect_description_ru": "Поиск среди 13 000+ рецептов, пошаговое приготовление, список покупок и учёт продуктов дома. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_naidi_retsept_sharlotki"],
  "search_aliases": ["рецепты", "готовка", "борщ"],
  "source_url": "https://dialogs.yandex.ru/store/skills/bc631b01-umnye-recepty",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["general", "cooking", "skill"]
}
```

#### `kids_igra_kulinariya` — Игра Кулинария

```json
{
  "id": "kids_igra_kulinariya",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 350,
  "variant_label_ru": "Игра Кулинария",
  "is_primary_in_group": false,
  "title_ru": "Игра Кулинария",
  "phrases": [
    "Алиса, сыграем в Игру Кулинария",
    "Алиса, поиграем в Игру Кулинарию",
    "Алиса, запусти навык Игра Кулинария"
  ],
  "effect_description_ru": "Игра про повара: ресторан, кондитерская или фастфуд, рейтинг и кристаллы. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["кулинария", "повар", "ресторан"],
  "source_url": "https://dialogs.yandex.ru/store/skills/09848f16-igra-kulinariya",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `general_tabata_trener` — Табата тренер

```json
{
  "id": "general_tabata_trener",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 20,
  "variant_label_ru": "Табата тренер",
  "is_primary_in_group": false,
  "title_ru": "Табата тренер",
  "phrases": [
    "Алиса, запусти навык Табата тренер",
    "Алиса, попроси Табата тренера дать упражнения на руки, плечи и грудь"
  ],
  "effect_description_ru": "Интервальная табата-тренировка дома без снарядов. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_bystraia_trenirovka"],
  "search_aliases": ["табата", "фитнес", "интервал"],
  "source_url": "https://dialogs.yandex.ru/store/skills/82b6097a-tabata-trenirovka",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

#### `general_yoga_dlya_glaz` — Йога для глаз

```json
{
  "id": "general_yoga_dlya_glaz",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 30,
  "variant_label_ru": "Йога для глаз",
  "is_primary_in_group": false,
  "title_ru": "Йога для глаз",
  "phrases": [
    "Алиса, запусти навык Йога для глаз",
    "Алиса, гимнастика для глаз"
  ],
  "effect_description_ru": "Голосовой комплекс упражнений для глаз и зрения. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_pomeditiruem"],
  "search_aliases": ["йога", "глаза", "зрение"],
  "source_url": "https://dialogs.yandex.ru/store/categories/health_fitness",
  "updated_at": "2026-07-10T20:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

---

## Чеклист перед импортом

1. После добавления 28 команд: **843 + 28 = 871** commands.
2. `validateContent` + publish validation (`category_id` ↔ `group_id`, `preview_command_ids`).
3. Обновить `content_version` (+1) при publish.
