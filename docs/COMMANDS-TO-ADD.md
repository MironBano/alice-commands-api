# Команды для добавления в каталог

> **Дата проверки:** 2026-07-10  
> **Эталон текущего каталога:** `seed/catalog-audit-fixed.json` (**871** команда, schema v2, `content_version=50`)  
> **ROUND2:** см. [COMMANDS-TO-ADD-ROUND2.md](./COMMANDS-TO-ADD-ROUND2.md)
> **Метод:** сравнение `title_ru` / `phrases` / `search_aliases` + сверка с официальными страницами навыков и справкой Яндекса.

## Сводка проверки

| Метрика | Значение |
| --- | --- |
| Кандидатов из интернет-поиска | ~146 |
| Уже есть в bundle (полное/частичное покрытие) | 28 |
| Исключены (устарело / низкая уверенность / дубль) | 19 |
| **К добавлению (подтверждено)** | **79** |

### Уровни верификации

| Метка | Значение |
| --- | --- |
| `official-skill` | Страница навыка на alice.yandex.ru |
| `official-support` | Справка Яндекс Станции / Алисы |
| `official-browser` | Справка Яндекс Браузера (Android) |
| `catalog-skill` | Каталог dialogs.yandex.ru (активный навык) |
| `catalog-skill+` | Каталог + обзоры 2025–2026, формулировки совпадают |

### Уже есть в bundle — не добавлять

| Что искали | Чем покрыто в bundle |
| --- | --- |
| Расскажи шутку | `general_rasskazhi_anekdot` (alias `шутка`) |
| Во что поиграть? | `kids_davai_poigraem` (phrase `во что поиграем?`) |
| Тариф Комфорт (такси) | `general_komfort` |
| Анонс треков / DJ | `music_vkliuchi_rezhim_didzhei`, `music_didzhei` |
| Арендовать фильм | `tv_video_kupit` (alias `аренда`) |
| Запусти пылесос | `smart_home_vremia_uborki` (сценарий уборки) |
| Телепрограмма | `tv_video_skazhi_programmu_*` |
| Купи / арендуй фильм | `tv_video_kupit` |
| Перевод | `general_kak_budet_hello_po_angliiski` |
| Придумай сказку | `obscure_pomozhet_pridumat_skazku_ili_rasskazhet` |
| Подбрось монетку | `general_podbros_monetku` |
| Связь с поддержкой | `obscure_pomozhet_bystro_sviazatsia_so_sluzhboi_p` |
| Я тебя люблю | `calls_ia_tebia_liubliu` |
| Шёпотом | `obscure_shepotom` |
| Послание в будущее | `obscure_pomozhet_otpravit_vam_poslanie_v_budushc` |
| Что играет / что за песня (на колонке) | `music_chto_igraet`, `music_chto_seichas_igraet` |
| Список покупок | `general_dobav_v_spisok_pokupok_*` |
| Вызови такси | `general_vyzovi_taksi` |
| Заказ в Лавке | `obscure_pomozhet_bystro_oformit_zakaz_v_lavke` |
| Практика английского / таблица умножения | `alice_plus_davai_praktikovat_angliiskii`, `alice_plus_davai_uchit_tablitsu_umnozheniia` |
| Навык (общий) | `music_navyk` — слабое покрытие; добавляем отдельную команду «Запусти навык» |

### Исключены из списка добавления

| Команда | Причина |
| --- | --- |
| Окей, Google / Привет, Siri | Пасхалки, не продуктовые команды |
| Выключи компьютер / Перезагрузи / Спящий режим | Только Яндекс.Браузер Windows; в 2026 малоактуально |
| Paint / Skype | Зависит от установленных программ на ПК |
| Найди дешевле / Запиши в салон / Поговорить (режим) | Alice AI в **чате**, не голос на Станции |
| Давай придумаем / Шедеврум / Давай нарисуем | Чат Alice AI; skill URL yagpt 404; вынесены в отдельную секцию «только phone» |
| Ещё вариант / Другой вариант | UI поиска, не отдельный сценарий |
| Слушай, Яндекс | Активация, не команда каталога |

---

## Новые группы команд (command_groups)

Добавить в `command_groups` перед командами:

```json
{
  "id": "kids_catalog_skills",
  "category_id": "kids",
  "title_ru": "Навыки из каталога",
  "description_ru": "Игры и квесты сторонних разработчиков на dialogs.yandex.ru",
  "sort_order": 55,
  "icon_key": "games",
  "featured": true,
  "preview_command_ids": [
    "kids_igra_mafiia",
    "kids_ugadai_personazha",
    "kids_detskaia_viktorina"
  ]
}
```

```json
{
  "id": "general_official_skills",
  "category_id": "general",
  "title_ru": "Официальные навыки",
  "description_ru": "Навыки Яндекса: телефон, шоу, медитация, список дел",
  "sort_order": 55,
  "icon_key": "star",
  "featured": true,
  "preview_command_ids": [
    "general_naidi_moi_telefon",
    "general_utrennee_shou",
    "general_spisok_del"
  ]
}
```

```json
{
  "id": "general_taxi_manage",
  "category_id": "general",
  "title_ru": "Управление такси",
  "description_ru": "Статус заказа, тариф и оплата во время вызова",
  "sort_order": 65,
  "icon_key": "navigation",
  "featured": false,
  "preview_command_ids": [
    "general_gde_moe_taksi",
    "general_oplata_kartoi"
  ]
}
```

```json
{
  "id": "alice_plus_control",
  "category_id": "alice_plus",
  "title_ru": "Контроль и забота",
  "description_ru": "Маячок, блокировка и история использования (Алиса Плюс)",
  "sort_order": 40,
  "icon_key": "plus",
  "featured": false,
  "preview_command_ids": [
    "alice_plus_semeinyi_maiachok",
    "alice_plus_blokirovka_golosa"
  ]
}
```

```json
{
  "id": "general_phone_features",
  "category_id": "general",
  "title_ru": "Камера и сайты",
  "description_ru": "Распознавание фото, открытие сайтов и приложений (телефон)",
  "sort_order": 70,
  "icon_key": "search",
  "featured": false,
  "preview_command_ids": [
    "general_sdelaif_foto",
    "general_otkroi_sait"
  ]
}
```

```json
{
  "id": "general_health_skills",
  "category_id": "general",
  "title_ru": "Здоровье и спорт",
  "description_ru": "Тренировки, калории, медитация",
  "sort_order": 75,
  "icon_key": "fitness",
  "featured": false,
  "preview_command_ids": [
    "general_bystraia_trenirovka",
    "general_pomeditiruem"
  ]
}
```

```json
{
  "id": "obscure_easter",
  "category_id": "obscure",
  "title_ru": "Пасхалки и болталка",
  "description_ru": "Развлекательные фразы вне официальной справки",
  "sort_order": 80,
  "icon_key": "sparkles",
  "featured": false,
  "preview_command_ids": [
    "obscure_davai_poboltaem",
    "obscure_spoi_pesniu"
  ]
}
```

---

## Команды к добавлению

Формат: полный объект `command` по schema v2.  
`updated_at` для всех новых записей: `2026-07-10T12:00:00Z`.

---

### 1. Игры и навыки (каталог)

**Верификация:** `catalog-skill` / `catalog-skill+`  
**Источник:** [dialogs.yandex.ru/store](https://dialogs.yandex.ru/store), [ai-golos.ru топ игр](https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/)

#### `kids_igra_mafiia` — Игра Мафия

```json
{
  "id": "kids_igra_mafiia",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 10,
  "variant_label_ru": "Игра Мафия",
  "is_primary_in_group": true,
  "title_ru": "Игра Мафия",
  "phrases": [
    "Алиса, запусти навык Игра Мафия",
    "Алиса, сыграем в игру мафия",
    "Алиса, давай сыграем в Мафию"
  ],
  "effect_description_ru": "Запустит навык «Игра Мафия»: Алиса ведёт партию для компании от 4 человек (роли на смартфонах). Нужно: устройство с Алисой; интернет; компания игроков.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["мафия", "ведущий", "игра в мафию"],
  "source_url": "https://dialogs.yandex.ru/store/skills/064990fd-igra-mafiya",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_morskoi_boi` — Морской бой онлайн

```json
{
  "id": "kids_morskoi_boi",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 20,
  "variant_label_ru": "Морской бой онлайн",
  "is_primary_in_group": false,
  "title_ru": "Морской бой онлайн",
  "phrases": [
    "Алиса, запусти навык Морской бой онлайн",
    "Алиса, сыграем в морской бой"
  ],
  "effect_description_ru": "Запустит голосовой «Морской бой»: нужны лист и ручка для поля 10×10; координаты называете вслух. Можно играть вдвоём через две колонки. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["морской бой", "корабли"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_shakhmaty` — Шахматы

```json
{
  "id": "kids_shakhmaty",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 30,
  "variant_label_ru": "Шахматы",
  "is_primary_in_group": false,
  "title_ru": "Шахматы",
  "phrases": [
    "Алиса, запусти Шахматы",
    "Алиса, сыграем в шахматы"
  ],
  "effect_description_ru": "Запустит навык шахмат: ходы вслух в нотации, Алиса ведёт партию. Есть уровни сложности. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["шахматы", "шахматы вслепую"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_ugadai_personazha` — Угадай персонажа

```json
{
  "id": "kids_ugadai_personazha",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 40,
  "variant_label_ru": "Угадай персонажа",
  "is_primary_in_group": false,
  "title_ru": "Угадай персонажа",
  "phrases": [
    "Алиса, запусти навык Угадывание персонажа",
    "Алиса, давай поиграем в угадывание персонажа"
  ],
  "effect_description_ru": "Запустит игру в стиле Акинатора: загадайте персонажа, отвечайте «да», «нет», «не знаю» на вопросы Алисы. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_ugadai_aktera"],
  "search_aliases": ["акинатор", "угадай кто", "персонаж"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_detskaia_viktorina` — Детская викторина

```json
{
  "id": "kids_detskaia_viktorina",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 50,
  "variant_label_ru": "Детская викторина",
  "is_primary_in_group": false,
  "title_ru": "Детская викторина",
  "phrases": [
    "Алиса, запусти Детскую викторину",
    "Алиса, давай сыграем в викторину"
  ],
  "effect_description_ru": "Запустит детскую викторину с адаптивной сложностью; после ошибки объяснит правильный ответ. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["викторина", "вопросы"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_muzykalnyi_turnir` — Музыкальный турнир

```json
{
  "id": "kids_muzykalnyi_turnir",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 60,
  "variant_label_ru": "Музыкальный турнир",
  "is_primary_in_group": false,
  "title_ru": "Музыкальный турнир",
  "phrases": [
    "Алиса, запусти навык Музыкальный турнир",
    "Алиса, давай сыграем в музыкальный турнир"
  ],
  "effect_description_ru": "Запустит командную игру: Алиса проигрывает фрагменты песен, нужно угадать исполнителя или название. Нужно: устройство с Алисой; интернет; лучше звучит на колонке.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_ugadai_pesniu"],
  "search_aliases": ["музыка", "угадай песню", "турнир"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_chepuha` — Чепуха

```json
{
  "id": "kids_chepuha",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 70,
  "variant_label_ru": "Чепуха",
  "is_primary_in_group": false,
  "title_ru": "Чепуха",
  "phrases": [
    "Алиса, запусти Занимательные истории",
    "Алиса, давай сыграем в Чепуху"
  ],
  "effect_description_ru": "Запустит игру «Чепуха»: игроки по очереди называют слова, Алиса зачитывает абсурдную историю. Нужно: устройство с Алисой; интернет; лучше в компании.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["занимательные истории", "история", "компания"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_pravda_ili_lozh` — Правда или ложь

```json
{
  "id": "kids_pravda_ili_lozh",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 80,
  "variant_label_ru": "Правда или ложь",
  "is_primary_in_group": false,
  "title_ru": "Правда или ложь",
  "phrases": [
    "Алиса, запусти навык Правда или ложь",
    "Алиса, сыграем в правда или ложь"
  ],
  "effect_description_ru": "Запустит викторину: Алиса зачитывает утверждение, вы отвечаете «правда» или «ложь», затем объясняет ответ. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_veriu_ne_veriu"],
  "search_aliases": ["факты", "викторина"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_erudit_krossvord` — Эрудит-кроссворд

```json
{
  "id": "kids_erudit_krossvord",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 90,
  "variant_label_ru": "Эрудит-кроссворд",
  "is_primary_in_group": false,
  "title_ru": "Эрудит-кроссворд",
  "phrases": [
    "Алиса, запусти навык Эрудит-кроссворд"
  ],
  "effect_description_ru": "Запустит голосовой кроссворд: Алиса называет определение, вы отвечаете словом; при ошибке даёт подсказку. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["кроссворд", "эрудиция"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_ugadai_otvet` — Угадай ответ

```json
{
  "id": "kids_ugadai_otvet",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 100,
  "variant_label_ru": "Угадай ответ",
  "is_primary_in_group": false,
  "title_ru": "Угадай ответ",
  "phrases": [
    "Алиса, запусти навык Угадай ответ",
    "Алиса, сыграем в сто к одному"
  ],
  "effect_description_ru": "Запустит игру в стиле «100 к 1»: угадайте популярные ответы людей на вопрос. Нужно: устройство с Алисой; интернет; лучше в компании.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["100 к 1", "family feud", "популярные ответы"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_geo_viktorina` — Гео Викторина

```json
{
  "id": "kids_geo_viktorina",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 110,
  "variant_label_ru": "Гео Викторина",
  "is_primary_in_group": false,
  "title_ru": "Гео Викторина",
  "phrases": [
    "Алиса, запусти Гео Викторину"
  ],
  "effect_description_ru": "Запустит географическую викторину: столицы, флаги, население и др. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_poigraem_v_goroda", "kids_davai_sygraem_v_goroda"],
  "search_aliases": ["география", "страны", "столицы"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_alisa_kvest_put_geroia` — Алиса-квест: Путь героя

```json
{
  "id": "kids_alisa_kvest_put_geroia",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 120,
  "variant_label_ru": "Путь героя",
  "is_primary_in_group": false,
  "title_ru": "Алиса-квест: Путь героя",
  "phrases": [
    "Алиса, запусти Алиса-квест Путь героя",
    "Алиса, давай сыграем в квест"
  ],
  "effect_description_ru": "Запустит интерактивный аудиоквест с разветвлённым сюжетом (15–30 мин). Расширенные главы могут требовать Яндекс Плюс. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_kvest_detroit", "kids_davai_sygraem_v_kvest_pro_kosmos"],
  "search_aliases": ["квест", "история", "герой"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_pobeg_iz_komnaty` — Побег из комнаты

```json
{
  "id": "kids_pobeg_iz_komnaty",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 130,
  "variant_label_ru": "Побег из комнаты",
  "is_primary_in_group": false,
  "title_ru": "Побег из комнаты",
  "phrases": [
    "Алиса, включи Побег из комнаты",
    "Алиса, запусти навык Побег из комнаты"
  ],
  "effect_description_ru": "Запустит голосовой квест-головоломку: ищите подсказки и решайте загадки, чтобы выбраться. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["квест", "головоломка", "комната"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_golosovoi_detektiv` — Голосовой детектив

```json
{
  "id": "kids_golosovoi_detektiv",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 140,
  "variant_label_ru": "Голосовой детектив",
  "is_primary_in_group": false,
  "title_ru": "Голосовой детектив",
  "phrases": [
    "Алиса, запусти навык Голосовой детектив"
  ],
  "effect_description_ru": "Запустит детективное расследование: допросы, улики, выбор версии (20–60 мин). Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["детектив", "расследование"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_chto_gde_kogda` — Что? Где? Когда?

```json
{
  "id": "kids_chto_gde_kogda",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 150,
  "variant_label_ru": "Что? Где? Когда?",
  "is_primary_in_group": false,
  "title_ru": "Что? Где? Когда?",
  "phrases": [
    "Алиса, сыграем в Что-Где-Когда",
    "Алиса, сыграем в что где когда"
  ],
  "effect_description_ru": "Запустит эрудиционную игру: на каждый вопрос около минуты на обсуждение и ответ. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["чгк", "эрудиция"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_assotsiatsii_na_vremia` — Ассоциации на время

```json
{
  "id": "kids_assotsiatsii_na_vremia",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 160,
  "variant_label_ru": "Ассоциации на время",
  "is_primary_in_group": false,
  "title_ru": "Ассоциации на время",
  "phrases": [
    "Алиса, запусти Ассоциации на время"
  ],
  "effect_description_ru": "Запустит игру: за минуту угадать 7 слов по ассоциациям Алисы. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_davai_sygraem_v_slova"],
  "search_aliases": ["ассоциации", "слова"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_igra_v_snezhki` — Игра в снежки

```json
{
  "id": "kids_igra_v_snezhki",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 170,
  "variant_label_ru": "Игра в снежки",
  "is_primary_in_group": false,
  "title_ru": "Игра в снежки",
  "phrases": [
    "Алиса, запусти навык Игра в снежки",
    "Алиса, давай поиграем в игру снежки"
  ],
  "effect_description_ru": "Запустит аркадную игру со снежками и персонажами. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["снежки", "игра"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_kvest_korol_i_shut` — Квест «Король и Шут»

```json
{
  "id": "kids_kvest_korol_i_shut",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 180,
  "variant_label_ru": "Квест Король и Шут",
  "is_primary_in_group": false,
  "title_ru": "Квест «Король и Шут»",
  "phrases": [
    "Алиса, давай поиграем в квест Король и Шут"
  ],
  "effect_description_ru": "Запустит тематический аудиоквест по вселенной группы «Король и Шут». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["король и шут", "квест", "рок"],
  "source_url": "https://ai-golos.ru/yandex/top-10-igr-dlya-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_golosa_zhivotnykh` — Голоса животных

```json
{
  "id": "kids_golosa_zhivotnykh",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 190,
  "variant_label_ru": "Голоса животных",
  "is_primary_in_group": false,
  "title_ru": "Голоса животных",
  "phrases": [
    "Алиса, запусти навык Голоса животных",
    "Алиса, давай поиграем в голоса животных"
  ],
  "effect_description_ru": "Запустит игру на угадывание животных по звукам. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_ugadai_zhivotnoe", "kids_kak_govorit_korova"],
  "search_aliases": ["животные", "звуки"],
  "source_url": "https://trends.rbc.ru/trends/social/6863c3269a7947f7fa3f4d6f",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_viktorina_multfilmy` — Викторина про мультфильмы

```json
{
  "id": "kids_viktorina_multfilmy",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 200,
  "variant_label_ru": "Викторина про мультфильмы",
  "is_primary_in_group": false,
  "title_ru": "Викторина про мультфильмы",
  "phrases": [
    "Алиса, запусти викторину про мультфильмы"
  ],
  "effect_description_ru": "Запустит детскую викторину с вопросами о мультфильмах. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_detskaia_viktorina"],
  "search_aliases": ["мультики", "мультфильмы"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `kids_kholodnoe_serdtse` — Холодное Сердце

```json
{
  "id": "kids_kholodnoe_serdtse",
  "category_id": "kids",
  "group_id": "kids_catalog_skills",
  "sort_order": 210,
  "variant_label_ru": "Холодное Сердце",
  "is_primary_in_group": false,
  "title_ru": "Холодное Сердце",
  "phrases": [
    "Алиса, давай сыграем в Холодное Сердце"
  ],
  "effect_description_ru": "Запустит интерактивную игру по мотивам «Холодного сердца». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["холодное сердце", "эльза", "дисней"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "games", "skill"]
}
```

#### `general_zapusti_navyk` — Запусти навык

```json
{
  "id": "general_zapusti_navyk",
  "category_id": "general",
  "group_id": "general_official_skills",
  "sort_order": 5,
  "variant_label_ru": "Запусти навык",
  "is_primary_in_group": true,
  "title_ru": "Запусти навык",
  "phrases": [
    "Алиса, запусти навык",
    "Алиса, попроси",
    "Алиса, узнай у"
  ],
  "effect_description_ru": "Универсальный шаблон запуска навыка из каталога: «запусти навык [название]», «попроси [навык] [команда]». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["music_navyk", "general_chto_ty_umeesh"],
  "search_aliases": ["навык", "каталог", "dialogs"],
  "source_url": "https://yandex.ru/dev/dialogs/alice/doc/ru/activation",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill"]
}
```

---

### 2. Официальные навыки Яндекса

**Верификация:** `official-skill`

#### `general_naidi_moi_telefon` — Найди мой телефон

```json
{
  "id": "general_naidi_moi_telefon",
  "category_id": "general",
  "group_id": "general_official_skills",
  "sort_order": 10,
  "variant_label_ru": "Найди мой телефон",
  "is_primary_in_group": false,
  "title_ru": "Найди мой телефон",
  "phrases": [
    "Алиса, найди мой телефон",
    "Алиса, где мой телефон?"
  ],
  "effect_description_ru": "Позвонит на номер из Яндекс ID, чтобы найти телефон по звуку. Антиспам на телефоне может заблокировать звонок. Нужно: устройство с Алисой; привязанный номер в аккаунте.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone", "tv"],
  "related_command_ids": [],
  "search_aliases": ["телефон", "потерял", "найти"],
  "source_url": "https://alice.yandex.ru/skills/find-phone",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill", "phone"]
}
```

#### `general_utrennee_shou` — Утреннее шоу

```json
{
  "id": "general_utrennee_shou",
  "category_id": "general",
  "group_id": "general_official_skills",
  "sort_order": 20,
  "variant_label_ru": "Утреннее шоу",
  "is_primary_in_group": false,
  "title_ru": "Утреннее шоу",
  "phrases": [
    "Алиса, включи утреннее шоу",
    "Алиса, запусти утреннее шоу"
  ],
  "effect_description_ru": "Запустит утреннее шоу: погода, пробки, новости, подкасты и музыка по вашим настройкам в «Дом с Алисой». Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "tv"],
  "related_command_ids": ["general_vechernee_shou"],
  "search_aliases": ["утро", "шоу", "будильник"],
  "source_url": "https://alice.yandex.ru/skills/morning-show",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill"]
}
```

#### `general_vechernee_shou` — Вечернее шоу

```json
{
  "id": "general_vechernee_shou",
  "category_id": "general",
  "group_id": "general_official_skills",
  "sort_order": 30,
  "variant_label_ru": "Вечернее шоу",
  "is_primary_in_group": false,
  "title_ru": "Вечернее шоу",
  "phrases": [
    "Алиса, включи вечернее шоу"
  ],
  "effect_description_ru": "Запустит вечернее шоу: итоги дня, новости и расслабляющий контент перед сном. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "tv"],
  "related_command_ids": ["general_utrennee_shou"],
  "search_aliases": ["вечер", "шоу", "сон"],
  "source_url": "https://alice.yandex.ru/skills/morning-show",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill"]
}
```

#### `general_pomeditiruem` — Давай помедитируем

```json
{
  "id": "general_pomeditiruem",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 10,
  "variant_label_ru": "Давай помедитируем",
  "is_primary_in_group": true,
  "title_ru": "Давай помедитируем",
  "phrases": [
    "Алиса, давай помедитируем",
    "Алиса, включи музыку для медитации",
    "Алиса, включи мантры для сна"
  ],
  "effect_description_ru": "Запустит медитацию (~10 мин) с голосовыми подсказками; после можно включить спокойную музыку. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "tv"],
  "related_command_ids": ["obscure_vkliuchi_meditatsiiu"],
  "search_aliases": ["медитация", "сон", "расслабление"],
  "source_url": "https://alice.yandex.ru/skills/meditation",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill", "health"]
}
```

#### `general_kak_pishetsia` — Как пишется слово

```json
{
  "id": "general_kak_pishetsia",
  "category_id": "quick_answers",
  "group_id": "general_info",
  "sort_order": 200,
  "variant_label_ru": "Как пишется",
  "is_primary_in_group": false,
  "title_ru": "Как пишется слово",
  "phrases": [
    "Алиса, как пишется аномалия",
    "Алиса, как правильно пишется слово приближать"
  ],
  "effect_description_ru": "Прочитает слово по буквам для проверки правописания. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone", "tv"],
  "related_command_ids": [],
  "search_aliases": ["орфография", "правописание", "буквы"],
  "source_url": "https://alice.yandex.ru/skills/spelling",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "skill", "education"]
}
```

#### `general_spisok_del` — Список дел

```json
{
  "id": "general_spisok_del",
  "category_id": "timers",
  "group_id": "timers_reminder",
  "sort_order": 200,
  "variant_label_ru": "Список дел",
  "is_primary_in_group": false,
  "title_ru": "Список дел",
  "phrases": [
    "Алиса, список дел на завтра",
    "Алиса, какие у меня на сегодня задачи?",
    "Алиса, добавь заметку съездить погулять"
  ],
  "effect_description_ru": "Ведёт голосовой список дел: добавление, просмотр, удаление пунктов. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone", "tv"],
  "related_command_ids": ["timers_perechisli_napominaniia"],
  "search_aliases": ["задачи", "todo", "дела"],
  "source_url": "https://alice.yandex.ru/skills/to-do-list",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["timers", "skill", "productivity"]
}
```

#### `music_raspoznai_pesniu` — Распознай песню

```json
{
  "id": "music_raspoznai_pesniu",
  "category_id": "music",
  "group_id": "music_info",
  "sort_order": 70,
  "variant_label_ru": "Распознай песню",
  "is_primary_in_group": false,
  "title_ru": "Распознай песню",
  "phrases": [
    "Алиса, распознай песню",
    "Алиса, кто поёт",
    "Алиса, что за песня"
  ],
  "effect_description_ru": "Распознает мелодию через микрофон (аналог Shazam) и назовёт трек и исполнителя. Работает для звука рядом, не только с колонки. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [
    "music_chto_igraet",
    "obscure_podskazhet_nazvanie_neznakomoi_vam_pesni"
  ],
  "search_aliases": ["shazam", "узнай песню", "мелодия"],
  "source_url": "https://alice.yandex.ru/skills/recognize-song",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["music", "skill"]
}
```

#### `general_zakazhi_produkty_v_lavke` — Закажи продукты в Лавке

```json
{
  "id": "general_zakazhi_produkty_v_lavke",
  "category_id": "general",
  "group_id": "general_services",
  "sort_order": 200,
  "variant_label_ru": "Закажи продукты в Лавке",
  "is_primary_in_group": false,
  "title_ru": "Закажи продукты в Лавке",
  "phrases": [
    "Алиса, закажи продукты в Лавке"
  ],
  "effect_description_ru": "Поможет собрать и оформить заказ продуктов в Яндекс Лавке голосом. Нужно: устройство с Алисой; интернет; сервис Лавки в вашем городе.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [
    "obscure_pomozhet_bystro_oformit_zakaz_v_lavke"
  ],
  "search_aliases": ["лавка", "продукты", "доставка"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "lavka"]
}
```

#### `obscure_sochinim_skazku` — Давай сочиним сказку

```json
{
  "id": "obscure_sochinim_skazku",
  "category_id": "obscure",
  "group_id": "obscure_easter_eggs",
  "sort_order": 200,
  "variant_label_ru": "Сочиним сказку",
  "is_primary_in_group": false,
  "title_ru": "Давай сочиним сказку",
  "phrases": [
    "Алиса, давай сочиним сказку"
  ],
  "effect_description_ru": "Сочинит новую сказку по вашим подсказкам или расскажет из коллекции. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["obscure_pomozhet_pridumat_skazku_ili_rasskazhet", "kids_rasskazhi_skazku"],
  "search_aliases": ["сказка", "сочинить"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure", "kids"]
}
```

#### `obscure_pridumai_stikhotvorenie` — Придумай стихотворение

```json
{
  "id": "obscure_pridumai_stikhotvorenie",
  "category_id": "obscure",
  "group_id": "obscure_easter_eggs",
  "sort_order": 210,
  "variant_label_ru": "Придумай стихотворение",
  "is_primary_in_group": false,
  "title_ru": "Придумай стихотворение",
  "phrases": [
    "Алиса, придумай стихотворение про зиму"
  ],
  "effect_description_ru": "Прочитает стихотворение собственного сочинения на заданную тему. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_prochitai_stikh", "obscure_pridumai_rifmu"],
  "search_aliases": ["стих", "поэзия"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/non-obvious",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure"]
}
```

#### `kids_skazka_pro_zolushku` — Сказка про Золушку

```json
{
  "id": "kids_skazka_pro_zolushku",
  "category_id": "kids",
  "group_id": "kids_fairy_tales",
  "sort_order": 200,
  "variant_label_ru": "Сказка про Золушку",
  "is_primary_in_group": false,
  "title_ru": "Сказка про Золушку",
  "phrases": [
    "Алиса, включи сказку про Золушку",
    "Алиса, расскажи сказку про золушку"
  ],
  "effect_description_ru": "Включит или расскажет сказку про Золушку. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["kids_rasskazhi_skazku", "audiobooks_vkliuchi_skazku_nomer_1"],
  "search_aliases": ["золушка", "сказка"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "fairy_tales"]
}
```

---

### 3. Такси — управление заказом

**Верификация:** `official-skill` — [alice.yandex.ru/skills/taxi](https://alice.yandex.ru/skills/taxi)

#### `general_gde_moe_taksi` — Где моё такси?

```json
{
  "id": "general_gde_moe_taksi",
  "category_id": "general",
  "group_id": "general_taxi_manage",
  "sort_order": 10,
  "variant_label_ru": "Где моё такси?",
  "is_primary_in_group": true,
  "title_ru": "Где моё такси?",
  "phrases": [
    "Алиса, где моё такси?"
  ],
  "effect_description_ru": "Сообщит статус текущего заказа такси (где машина, когда приедет). Нужно: устройство с Алисой; активный заказ в Яндекс Go.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_vyzovi_taksi", "general_komfort"],
  "search_aliases": ["такси", "статус", "машина"],
  "source_url": "https://alice.yandex.ru/skills/taxi",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "taxi"]
}
```

#### `general_oplata_kartoi` — Оплата картой

```json
{
  "id": "general_oplata_kartoi",
  "category_id": "general",
  "group_id": "general_taxi_manage",
  "sort_order": 20,
  "variant_label_ru": "Оплата картой",
  "is_primary_in_group": false,
  "title_ru": "Оплата картой",
  "phrases": [
    "Алиса, оплата картой"
  ],
  "effect_description_ru": "Выберет оплату картой для текущего заказа такси. Нужно: устройство с Алисой; активный или новый заказ в Яндекс Go.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_vyzovi_taksi"],
  "search_aliases": ["такси", "карта", "оплата"],
  "source_url": "https://alice.yandex.ru/skills/taxi",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "taxi"]
}
```

#### `general_zakazhi_taksi_zavtra` — Закажи такси на завтра

```json
{
  "id": "general_zakazhi_taksi_zavtra",
  "category_id": "general",
  "group_id": "general_taxi_manage",
  "sort_order": 30,
  "variant_label_ru": "Такси на завтра",
  "is_primary_in_group": false,
  "title_ru": "Закажи такси на завтра",
  "phrases": [
    "Алиса, вызови такси до Белорусского вокзала завтра в 8 утра",
    "Алиса, закажи такси с Малой Полянки 5 до Манежной площади"
  ],
  "effect_description_ru": "Откроет Яндекс Go с подставленными адресом и временем; при необходимости уточнит адрес. Нужно: устройство с Алисой; приложение или веб Такси.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": ["general_vyzovi_taksi"],
  "search_aliases": ["такси", "завтра", "отложенный"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "taxi", "phone"]
}
```

---

### 4. Алиса Плюс

**Верификация:** `official-support` — [alice.yandex.ru/support/ru/assistant/alice-plus/](https://alice.yandex.ru/support/ru/assistant/alice-plus/)

#### `alice_plus_semeinyi_maiachok` — Семейный маячок

```json
{
  "id": "alice_plus_semeinyi_maiachok",
  "category_id": "alice_plus",
  "group_id": "alice_plus_control",
  "sort_order": 10,
  "variant_label_ru": "Семейный маячок",
  "is_primary_in_group": true,
  "title_ru": "Семейный маячок",
  "phrases": [
    "Алиса, семейный маячок",
    "Алиса, отправь сигнал близким"
  ],
  "effect_description_ru": "Отправит сигнал близким через функцию «Семейный маячок» (нужна настройка в приложении). Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station"],
  "related_command_ids": ["general_pomoshch_blizkikh"],
  "search_aliases": ["маячок", "сигнал", "близкие"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["alice_plus"]
}
```

#### `alice_plus_blokirovka_golosa` — Блокировка голосового управления

```json
{
  "id": "alice_plus_blokirovka_golosa",
  "category_id": "alice_plus",
  "group_id": "alice_plus_control",
  "sort_order": 20,
  "variant_label_ru": "Блокировка голоса",
  "is_primary_in_group": false,
  "title_ru": "Блокировка голосового управления",
  "phrases": [
    "Алиса, заблокируй голосовое управление",
    "Алиса, включи блокировку станции"
  ],
  "effect_description_ru": "Ограничит голосовое управление колонкой (родительский контроль). Нужно: умная колонка Яндекса кроме Станции Дуо Макс; опция Алиса Плюс; настройка в приложении.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station"],
  "related_command_ids": [],
  "search_aliases": ["блокировка", "дети", "родительский контроль"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["alice_plus"]
}
```

#### `alice_plus_istoriia_ispolzovaniia` — История использования

```json
{
  "id": "alice_plus_istoriia_ispolzovaniia",
  "category_id": "alice_plus",
  "group_id": "alice_plus_control",
  "sort_order": 30,
  "variant_label_ru": "История использования",
  "is_primary_in_group": false,
  "title_ru": "История использования колонки",
  "phrases": [
    "Алиса, история использования",
    "Алиса, что я слушал на колонке"
  ],
  "effect_description_ru": "Расскажет о недавней активности на колонке (контроль времени). Нужно: колонка кроме Дуо Макс или ТВ Станция; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "tv"],
  "related_command_ids": [],
  "search_aliases": ["история", "контроль времени", "родители"],
  "source_url": "https://alice.yandex.ru/support/ru/assistant/alice-plus/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["alice_plus"]
}
```

#### `kids_emotsionalnyi_sintez` — Эмоциональный синтез

```json
{
  "id": "kids_emotsionalnyi_sintez",
  "category_id": "kids",
  "group_id": "kids_activities",
  "sort_order": 200,
  "variant_label_ru": "Эмоциональный синтез",
  "is_primary_in_group": false,
  "title_ru": "Эмоциональный синтез",
  "phrases": [
    "Алиса, включи эмоциональный синтез"
  ],
  "effect_description_ru": "Включит более выразительную озвучку Алисы в детских сценариях и играх. Нужно: устройство с Алисой; опция Алиса Плюс.",
  "requires_alice_word": true,
  "requires_plus": true,
  "device_types": ["station", "tv"],
  "related_command_ids": [],
  "search_aliases": ["эмоции", "голос", "дети"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "alice_plus"]
}
```

---

### 5. Быстрые ответы, погода, навигация

**Верификация:** `official-browser`, `official-support`

#### `quick_answers_gde_ia` — Где я?

```json
{
  "id": "quick_answers_gde_ia",
  "category_id": "quick_answers",
  "group_id": "general_nav",
  "sort_order": 200,
  "variant_label_ru": "Где я?",
  "is_primary_in_group": false,
  "title_ru": "Где я?",
  "phrases": [
    "Алиса, где я?",
    "Алиса, где я нахожусь?"
  ],
  "effect_description_ru": "Определит ваше местоположение и назовёт адрес или район. Нужно: телефон с геолокацией и авторизацией в Яндексе.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": ["general_kak_dobratsia_do_kremlia"],
  "search_aliases": ["геолокация", "местоположение", "адрес"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "navigation", "phone"]
}
```

#### `general_zapomni_adres` — Запомни адрес

```json
{
  "id": "general_zapomni_adres",
  "category_id": "general",
  "group_id": "general_nav",
  "sort_order": 210,
  "variant_label_ru": "Запомни адрес",
  "is_primary_in_group": false,
  "title_ru": "Запомни адрес",
  "phrases": [
    "Алиса, запомни адрес",
    "Алиса, запомни, что дом на улице Тверской 10"
  ],
  "effect_description_ru": "Сохранит адрес «дом» или «работа» в Яндекс ID / Картах для такси и маршрутов. Нужно: устройство с Алисой; аккаунт Яндекса.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone", "station"],
  "related_command_ids": ["general_vyzovi_taksi"],
  "search_aliases": ["дом", "работа", "адрес"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "navigation"]
}
```

#### `quick_answers_teplo_li` — Тепло ли сейчас

```json
{
  "id": "quick_answers_teplo_li",
  "category_id": "quick_answers",
  "group_id": "qa_weather_city",
  "sort_order": 200,
  "variant_label_ru": "Тепло ли",
  "is_primary_in_group": false,
  "title_ru": "Тепло ли сейчас",
  "phrases": [
    "Алиса, тепло ли сейчас в Сочи?",
    "Алиса, холодно ли на улице?"
  ],
  "effect_description_ru": "Ответит, комфортная ли сейчас погода в указанном городе. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["quick_answers_kakaia_pogoda"],
  "search_aliases": ["погода", "температура", "комфорт"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "weather"]
}
```

#### `quick_answers_prognoz_dve_nedeli` — Прогноз на две недели

```json
{
  "id": "quick_answers_prognoz_dve_nedeli",
  "category_id": "quick_answers",
  "group_id": "qa_weather_city",
  "sort_order": 210,
  "variant_label_ru": "Прогноз на 2 недели",
  "is_primary_in_group": false,
  "title_ru": "Прогноз на две недели",
  "phrases": [
    "Алиса, дай прогноз погоды на две недели"
  ],
  "effect_description_ru": "Расскажет долгосрочный прогноз погоды. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["quick_answers_pogoda_na_nedeliu"],
  "search_aliases": ["погода", "14 дней", "прогноз"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "weather"]
}
```

#### `quick_answers_dorogi` — Как на дорогах

```json
{
  "id": "quick_answers_dorogi",
  "category_id": "quick_answers",
  "group_id": "general_nav",
  "sort_order": 220,
  "variant_label_ru": "Ситуация на дорогах",
  "is_primary_in_group": false,
  "title_ru": "Как там на дорогах",
  "phrases": [
    "Алиса, какие сейчас пробки?",
    "Алиса, как там на дорогах?",
    "Алиса, каков балл пробок в Дмитрове?"
  ],
  "effect_description_ru": "Сообщит уровень пробок в городе или по маршруту. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kak_dobratsia_do_kremlia"],
  "search_aliases": ["пробки", "балл", "дороги"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "navigation"]
}
```

#### `quick_answers_kuda_v_kino` — Куда сходить в кино

```json
{
  "id": "quick_answers_kuda_v_kino",
  "category_id": "quick_answers",
  "group_id": "general_nav",
  "sort_order": 230,
  "variant_label_ru": "Куда в кино",
  "is_primary_in_group": false,
  "title_ru": "Куда сходить в кино",
  "phrases": [
    "Алиса, куда сходить в кино?",
    "Алиса, найди кафе рядом"
  ],
  "effect_description_ru": "Найдёт кинотеатры или заведения поблизости по геолокации. Нужно: телефон с геолокацией; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": [],
  "search_aliases": ["кино", "кафе", "рядом"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "navigation", "phone"]
}
```

#### `quick_answers_den_nedeli` — Какой день недели

```json
{
  "id": "quick_answers_den_nedeli",
  "category_id": "quick_answers",
  "group_id": "general_calc",
  "sort_order": 200,
  "variant_label_ru": "День недели",
  "is_primary_in_group": false,
  "title_ru": "Какой день недели",
  "phrases": [
    "Алиса, какой день недели будет 6 октября?",
    "Алиса, какой день недели 31 декабря?"
  ],
  "effect_description_ru": "Назовёт день недели для указанной даты. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kakoe_segodnia_chislo"],
  "search_aliases": ["календарь", "дата"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "calendar"]
}
```

#### `quick_answers_raznitsa_vremeni` — Разница во времени

```json
{
      "id": "quick_answers_raznitsa_vremeni",
      "category_id": "general",
  "group_id": "general_calc",
  "sort_order": 210,
  "variant_label_ru": "Разница во времени",
  "is_primary_in_group": false,
  "title_ru": "Разница во времени",
  "phrases": [
    "Алиса, сколько времени в Нью-Йорке?",
    "Алиса, разница во времени с Берлином"
  ],
  "effect_description_ru": "Сообщит текущее время в городе или разницу с вашим часовым поясом. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kotoryi_chas", "quick_commands_kotoryi_chas"],
  "search_aliases": ["часовой пояс", "время"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers"]
}
```

#### `general_novosti_sporta` — Новости спорта

```json
{
  "id": "general_novosti_sporta",
  "category_id": "general",
  "group_id": "general_info",
  "sort_order": 210,
  "variant_label_ru": "Новости спорта",
  "is_primary_in_group": false,
  "title_ru": "Новости спорта",
  "phrases": [
    "Алиса, расскажи новости спорта",
    "Алиса, расскажи новости с сайта championat"
  ],
  "effect_description_ru": "Прочитает сводку новостей по теме или с указанного СМИ. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kakie_novosti", "quick_answers_rasskazhi_novosti"],
  "search_aliases": ["спорт", "новости"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "news"]
}
```

#### `general_faktorial` — Факториал

```json
{
  "id": "general_faktorial",
  "category_id": "quick_answers",
  "group_id": "general_calc",
  "sort_order": 220,
  "variant_label_ru": "Факториал",
  "is_primary_in_group": false,
  "title_ru": "Факториал",
  "phrases": [
    "Алиса, факториал 5"
  ],
  "effect_description_ru": "Посчитает факториал числа. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_skolko_budet_15_umnozhit_na_8"],
  "search_aliases": ["калькулятор", "математика"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "calc"]
}
```

#### `general_kurs_bitkoina` — Курс биткойна

```json
{
  "id": "general_kurs_bitkoina",
  "category_id": "quick_answers",
  "group_id": "general_calc",
  "sort_order": 230,
  "variant_label_ru": "Курс биткойна",
  "is_primary_in_group": false,
  "title_ru": "Курс биткойна",
  "phrases": [
    "Алиса, биткойн в долларах",
    "Алиса, курс биткойна"
  ],
  "effect_description_ru": "Сообщит текущий курс криптовалюты. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kurs_dollara"],
  "search_aliases": ["крипта", "биткоин", "курс"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "finance"]
}
```

#### `general_kurs_ieny` — Курс йены

```json
{
  "id": "general_kurs_ieny",
  "category_id": "quick_answers",
  "group_id": "general_calc",
  "sort_order": 240,
  "variant_label_ru": "Курс йены",
  "is_primary_in_group": false,
  "title_ru": "Курс йены",
  "phrases": [
    "Алиса, курс йены",
    "Алиса, сколько сегодня стоит иена?"
  ],
  "effect_description_ru": "Сообщит курс йены к рублю по ЦБ или рынку. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_kurs_dollara"],
  "search_aliases": ["йена", "валюта", "курс"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "finance"]
}
```

#### `quick_answers_udarenie` — Ударение в слове

```json
{
  "id": "quick_answers_udarenie",
  "category_id": "quick_answers",
  "group_id": "general_info",
  "sort_order": 220,
  "variant_label_ru": "Ударение",
  "is_primary_in_group": false,
  "title_ru": "Ударение в слове",
  "phrases": [
    "Алиса, ударение в слове свекла",
    "Алиса, ударение в слове торты"
  ],
  "effect_description_ru": "Покажет правильное ударение в слове. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["quick_answers_kto_takoi_pushkin"],
  "search_aliases": ["ударение", "словарь"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers"]
}
```

#### `quick_answers_elbrus` — Высота Эльбруса

```json
{
  "id": "quick_answers_elbrus",
  "category_id": "quick_answers",
  "group_id": "general_info",
  "sort_order": 230,
  "variant_label_ru": "Высота Эльбруса",
  "is_primary_in_group": false,
  "title_ru": "Высота Эльбруса",
  "phrases": [
    "Алиса, высота Эльбруса",
    "Алиса, расстояние до луны"
  ],
  "effect_description_ru": "Ответит справочным фактом о географии или расстоянии. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["quick_answers_stolitsa_iaponii"],
  "search_aliases": ["география", "факты", "эльбрус"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "facts"]
}
```

#### `quick_answers_lozhka` — Чайная и столовая ложка

```json
{
  "id": "quick_answers_lozhka",
  "category_id": "quick_answers",
  "group_id": "general_cooking",
  "sort_order": 200,
  "variant_label_ru": "Объём ложки",
  "is_primary_in_group": false,
  "title_ru": "Сколько миллилитров в ложке",
  "phrases": [
    "Алиса, сколько миллилитров в столовой ложке?",
    "Алиса, объём чайной ложки"
  ],
  "effect_description_ru": "Сообщит объём чайной или столовой ложки в мл. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_skolko_nado_muki"],
  "search_aliases": ["ложка", "мл", "кухня"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "cooking"]
}
```

#### `quick_answers_telefon_skoroi` — Телефон скорой

```json
{
  "id": "quick_answers_telefon_skoroi",
  "category_id": "quick_answers",
  "group_id": "general_info",
  "sort_order": 240,
  "variant_label_ru": "Телефон скорой",
  "is_primary_in_group": false,
  "title_ru": "Телефон скорой помощи",
  "phrases": [
    "Алиса, какой телефон скорой помощи?",
    "Алиса, телефон налоговой"
  ],
  "effect_description_ru": "Назовёт номер экстренной или справочной службы. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["calls_pozvoni"],
  "search_aliases": ["103", "112", "горячая линия"],
  "source_url": "https://voiceapp.ru/articles/yandex-alice-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["quick_answers", "emergency"]
}
```

---

### 6. Телефон: камера, сайты, таймеры

**Верификация:** `official-browser`

#### `general_sdelaif_foto` — Сделай фото

```json
{
  "id": "general_sdelaif_foto",
  "category_id": "general",
  "group_id": "general_phone_features",
  "sort_order": 10,
  "variant_label_ru": "Сделай фото",
  "is_primary_in_group": true,
  "title_ru": "Сделай фото",
  "phrases": [
    "Алиса, сделай фото",
    "Алиса, что изображено на картинке?"
  ],
  "effect_description_ru": "Откроет камеру, после снимка попытается распознать объекты на фото. Нужно: смартфон с приложением Яндекс или Браузер; доступ к камере.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": [],
  "search_aliases": ["камера", "фото", "распознать"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "phone", "camera"]
}
```

#### `general_otkroi_sait` — Открой сайт

```json
{
  "id": "general_otkroi_sait",
  "category_id": "general",
  "group_id": "general_phone_features",
  "sort_order": 20,
  "variant_label_ru": "Открой сайт",
  "is_primary_in_group": false,
  "title_ru": "Открой сайт",
  "phrases": [
    "Алиса, открой сайт Госуслуг",
    "Алиса, открой сайт Авито",
    "Алиса, открой 2ГИС"
  ],
  "effect_description_ru": "Откроет указанный сайт в браузере или приложении. Нужно: телефон с Яндекс Браузером или приложением Яндекс.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": [],
  "search_aliases": ["сайт", "браузер", "госуслуги"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "phone"]
}
```

#### `general_otkroi_instagram` — Открой Instagram

```json
{
  "id": "general_otkroi_instagram",
  "category_id": "general",
  "group_id": "general_phone_features",
  "sort_order": 30,
  "variant_label_ru": "Открой Instagram",
  "is_primary_in_group": false,
  "title_ru": "Открой Instagram",
  "phrases": [
    "Алиса, открой Instagram",
    "Алиса, запусти ВКонтакте"
  ],
  "effect_description_ru": "Запустит мобильное приложение по названию. Нужно: приложение установлено на телефоне.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": [],
  "search_aliases": ["приложение", "инстаграм"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "phone"]
}
```

#### `timers_zaseki_poltory_minuty` — Засеки полторы минуты

```json
{
  "id": "timers_zaseki_poltory_minuty",
  "category_id": "timers",
  "group_id": "timers_timer",
  "sort_order": 200,
  "variant_label_ru": "Полторы минуты",
  "is_primary_in_group": false,
  "title_ru": "Засеки полторы минуты",
  "phrases": [
    "Алиса, засеки полторы минуты",
    "Алиса, отсчитай 5 минут"
  ],
  "effect_description_ru": "Поставит таймер на указанное время, в том числе дробное (1 мин 30 сек). Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["timers_postav_taimer_na_5_minut"],
  "search_aliases": ["таймер", "отсчитай"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["timers"]
}
```

#### `timers_vykliuchi_budilniki_zavtra` — Выключи будильники на завтра

```json
{
  "id": "timers_vykliuchi_budilniki_zavtra",
  "category_id": "timers",
  "group_id": "timers_alarm",
  "sort_order": 200,
  "variant_label_ru": "Будильники на завтра",
  "is_primary_in_group": false,
  "title_ru": "Выключи все будильники на завтра",
  "phrases": [
    "Алиса, выключи все будильники на завтра"
  ],
  "effect_description_ru": "Отключит все будильники на указанный день. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["timers_udali_vse_budilniki"],
  "search_aliases": ["будильник", "отмена"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["timers", "alarm"]
}
```

#### `timers_postav_zadachu` — Поставь задачу

```json
{
  "id": "timers_postav_zadachu",
  "category_id": "timers",
  "group_id": "timers_reminder",
  "sort_order": 210,
  "variant_label_ru": "Поставь задачу",
  "is_primary_in_group": false,
  "title_ru": "Поставь задачу",
  "phrases": [
    "Алиса, поставь задачу позвонить маме сегодня в десять",
    "Алиса, поставь тудушку про утюг на завтра"
  ],
  "effect_description_ru": "Создаст задачу или напоминание с датой и временем. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [
    "timers_napomni_mne_pozvonit_mame_cherez_polchas",
    "general_spisok_del"
  ],
  "search_aliases": ["задача", "todo", "напоминание"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["timers", "productivity"]
}
```

#### `music_shum_moria` — Шум моря

```json
{
  "id": "music_shum_moria",
  "category_id": "music",
  "group_id": "music_genre_mood",
  "sort_order": 200,
  "variant_label_ru": "Шум моря",
  "is_primary_in_group": false,
  "title_ru": "Включи шум моря",
  "phrases": [
    "Алиса, включи шум моря",
    "Алиса, включи шум океана"
  ],
  "effect_description_ru": "Включит фоновые звуки моря для расслабления или сна. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["music_vkliuchi_shum_dozhdia", "obscure_zvuk_dozhdia"],
  "search_aliases": ["море", "океан", "эмибиент"],
  "source_url": "https://alice.yandex.ru/skills/ambient-sounds",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["music", "ambient"]
}
```

---

### 7. Здоровье, образование, умный дом

**Верификация:** `catalog-skill`, `official-support`

#### `general_bystraia_trenirovka` — Быстрая тренировка

```json
{
  "id": "general_bystraia_trenirovka",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 20,
  "variant_label_ru": "Быстрая тренировка",
  "is_primary_in_group": false,
  "title_ru": "Быстрая тренировка",
  "phrases": [
    "Алиса, запусти навык Быстрая тренировка"
  ],
  "effect_description_ru": "Запустит 7-минутную тренировку из 12 упражнений с музыкой и отсчётом повторений. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["спорт", "фитнес", "тренировка"],
  "source_url": "https://dialogs.yandex.ru/store/skills/cd571fe6-bystraya-trenirovk",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

#### `general_umnyi_schetchik_kalorii` — Умный счётчик калорий

```json
{
  "id": "general_umnyi_schetchik_kalorii",
  "category_id": "general",
  "group_id": "general_health_skills",
  "sort_order": 30,
  "variant_label_ru": "Счётчик калорий",
  "is_primary_in_group": false,
  "title_ru": "Умный счётчик калорий",
  "phrases": [
    "Алиса, запусти навык Умный счётчик калорий",
    "Алиса, попроси умный счётчик калорий записать"
  ],
  "effect_description_ru": "Запишет приём пищи и посчитает калории, БЖУ за день. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["калории", "диета", "еда"],
  "source_url": "https://dialogs.yandex.ru/store/skills/538d42cb-umnyj-schyotchik-kalo",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "health", "skill"]
}
```

#### `kids_umnyi_pereskaz` — Умный пересказ

```json
{
  "id": "kids_umnyi_pereskaz",
  "category_id": "kids",
  "group_id": "kids_education",
  "sort_order": 200,
  "variant_label_ru": "Умный пересказ",
  "is_primary_in_group": false,
  "title_ru": "Умный пересказ",
  "phrases": [
    "Алиса, запусти навык Умный пересказ",
    "Алиса, перескажи произведение из школьной программы"
  ],
  "effect_description_ru": "Кратко перескажет произведение из школьной программы голосом. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["школа", "литература", "пересказ"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "education", "skill"]
}
```

#### `kids_fizika_vokrug` — Физика вокруг

```json
{
  "id": "kids_fizika_vokrug",
  "category_id": "kids",
  "group_id": "kids_education",
  "sort_order": 210,
  "variant_label_ru": "Физика вокруг",
  "is_primary_in_group": false,
  "title_ru": "Физика вокруг",
  "phrases": [
    "Алиса, запусти навык Физика вокруг"
  ],
  "effect_description_ru": "Задаст задачу по физике с подсказками и объяснением решения. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["физика", "школа", "задачи"],
  "source_url": "https://ai-golos.ru/yandex/yandeks-dialogi-navyki-alisy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["kids", "education", "skill"]
}
```

#### `smart_home_zapusti_pylesos` — Запусти пылесос

```json
{
  "id": "smart_home_zapusti_pylesos",
  "category_id": "smart_home",
  "group_id": "smart_home_scenarios",
  "sort_order": 200,
  "variant_label_ru": "Запусти пылесос",
  "is_primary_in_group": false,
  "title_ru": "Запусти пылесос",
  "phrases": [
    "Алиса, запусти пылесос",
    "Алиса, запусти робот-пылесос"
  ],
  "effect_description_ru": "Запустит робот-пылесос в умном доме (если добавлен в «Дом с Алисой»). Нужно: робот-пылесос в аккаунте; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["smart_home_vremia_uborki"],
  "search_aliases": ["пылесос", "уборка", "робот"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["smart_home"]
}
```

#### `smart_home_umnyi_pult` — Умный пульт

```json
{
  "id": "smart_home_umnyi_pult",
  "category_id": "smart_home",
  "group_id": "smart_home_devices",
  "sort_order": 200,
  "variant_label_ru": "Умный пульт",
  "is_primary_in_group": false,
  "title_ru": "Умный пульт",
  "phrases": [
    "Алиса, включи кондиционер через пульт",
    "Алиса, настрой умный пульт"
  ],
  "effect_description_ru": "Управляет техникой через умный ИК-пульт Яндекса. Нужно: пульт в «Дом с Алисой»; обученные команды.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["smart_home_vkliuchi_konditsioner"],
  "search_aliases": ["пульт", "ик", "кондиционер"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["smart_home"]
}
```

#### `station_lava_lampa` — Лава-лампа

```json
{
  "id": "station_lava_lampa",
  "category_id": "station_settings",
  "group_id": "station_light_display",
  "sort_order": 200,
  "variant_label_ru": "Лава-лампа",
  "is_primary_in_group": false,
  "title_ru": "Лава-лампа",
  "phrases": [
    "Алиса, включи лава-лампу",
    "Алиса, режим лава-лампа"
  ],
  "effect_description_ru": "Включит режим подсветки «Лава-лампа» на Станции 2, Миди или Мини 3 Про. Нужно: совместимая колонка с подсветкой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station"],
  "related_command_ids": ["station_settings_rezhim_svecha", "station_settings_vkliuchi_nochnoi_rezhim"],
  "search_aliases": ["подсветка", "ночник", "атмосфера"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["station", "lighting"]
}
```

#### `obscure_zapomni_menia` — Запомни меня

```json
{
  "id": "obscure_zapomni_menia",
  "category_id": "obscure",
  "group_id": "obscure_device_modes",
  "sort_order": 200,
  "variant_label_ru": "Запомни меня",
  "is_primary_in_group": false,
  "title_ru": "Запомни меня",
  "phrases": [
    "Алиса, запомни меня",
    "Алиса, забудь меня"
  ],
  "effect_description_ru": "Сохранит или сбросит распознавание вашего голоса для персонализации. Нужно: устройство с Алисой; настройка в приложении.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["music_zabud_moi_golos", "music_nazyvai_menia_novoe_imia"],
  "search_aliases": ["голос", "персонализация"],
  "source_url": "https://alice.yandex.ru/support/ru/station/skills/audio-settings",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure"]
}
```

---

### 8. Развлечения и пасхалки

**Верификация:** `official-support`, сторонние справочники

#### `obscure_davai_poboltaem` — Давай поболтаем

```json
{
  "id": "obscure_davai_poboltaem",
  "category_id": "obscure",
  "group_id": "obscure_easter",
  "sort_order": 10,
  "variant_label_ru": "Давай поболтаем",
  "is_primary_in_group": true,
  "title_ru": "Давай поболтаем",
  "phrases": [
    "Алиса, давай поболтаем",
    "Алиса, хватит болтать"
  ],
  "effect_description_ru": "Включит или выключит режим свободного диалога с Алисой. Нужно: устройство с Алисой; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": [],
  "search_aliases": ["болталка", "разговор"],
  "source_url": "https://voiceapp.ru/articles/yandex-alisa-secret-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure", "fun"]
}
```

#### `obscure_spoi_pesniu` — Спой песню

```json
{
  "id": "obscure_spoi_pesniu",
  "category_id": "obscure",
  "group_id": "obscure_easter",
  "sort_order": 20,
  "variant_label_ru": "Спой песню",
  "is_primary_in_group": false,
  "title_ru": "Спой песню",
  "phrases": [
    "Алиса, спой песню"
  ],
  "effect_description_ru": "Алиса напоёт короткий фрагмент или шуточную «песню» голосом. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_rasskazhi_anekdot"],
  "search_aliases": ["песня", "музыка", "юмор"],
  "source_url": "https://voiceapp.ru/articles/yandex-alisa-secret-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure", "fun"]
}
```

#### `obscure_prochitai_tost` — Прочитай тост

```json
{
  "id": "obscure_prochitai_tost",
  "category_id": "obscure",
  "group_id": "obscure_easter",
  "sort_order": 30,
  "variant_label_ru": "Прочитай тост",
  "is_primary_in_group": false,
  "title_ru": "Прочитай тост",
  "phrases": [
    "Алиса, прочитай тост"
  ],
  "effect_description_ru": "Прочитает торжественный или шуточный тост. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["general_rasskazhi_anekdot"],
  "search_aliases": ["тост", "праздник"],
  "source_url": "https://alisayandeks.ru/komandy/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure", "fun"]
}
```

#### `obscure_odolzhi_deneg` — Одолжи денег

```json
{
  "id": "obscure_odolzhi_deneg",
  "category_id": "obscure",
  "group_id": "obscure_easter",
  "sort_order": 40,
  "variant_label_ru": "Одолжи денег",
  "is_primary_in_group": false,
  "title_ru": "Одолжи денег",
  "phrases": [
    "Алиса, одолжи денег",
    "Алиса, ты меня любишь?"
  ],
  "effect_description_ru": "Ответит шуточной репликой на личный вопрос. Нужно: устройство с Алисой.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["station", "phone"],
  "related_command_ids": ["calls_ia_tebia_liubliu"],
  "search_aliases": ["шутка", "любовь", "деньги"],
  "source_url": "https://voiceapp.ru/articles/yandex-alisa-secret-commands",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["obscure", "fun"]
}
```

#### `kids_parkovki_moskvy` — Парковки Москвы

```json
{
  "id": "kids_parkovki_moskvy",
  "category_id": "general",
  "group_id": "general_services",
  "sort_order": 220,
  "variant_label_ru": "Парковки Москвы",
  "is_primary_in_group": false,
  "title_ru": "Парковки Москвы",
  "phrases": [
    "Алиса, запусти навык Парковки Москвы"
  ],
  "effect_description_ru": "Поможет оплатить парковку в Москве голосом. Нужно: устройство с Алисой; интернет; привязанный автомобиль.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone", "station"],
  "related_command_ids": [],
  "search_aliases": ["парковка", "москва", "авто"],
  "source_url": "https://t-j.ru/list/alice/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill", "auto"]
}
```

#### `kids_moskovskii_avtobus` — Московский автобус

```json
{
  "id": "kids_moskovskii_avtobus",
  "category_id": "general",
  "group_id": "general_services",
  "sort_order": 230,
  "variant_label_ru": "Московский автобус",
  "is_primary_in_group": false,
  "title_ru": "Московский автобус",
  "phrases": [
    "Алиса, запусти навык Московский автобус"
  ],
  "effect_description_ru": "Сообщит, когда автобус приедет на остановку. Нужно: устройство с Алисой; интернет; Москва.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone", "station"],
  "related_command_ids": [],
  "search_aliases": ["транспорт", "автобус", "остановка"],
  "source_url": "https://t-j.ru/list/alice/",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "skill", "transport"]
}
```

---

### 9. Только телефон / Alice AI (опционально)

**Верификация:** `official-browser`, блог Alice 2026. На Станции **не работает** или ограничено.

#### `phone_davai_pridumaem` — Давай придумаем

```json
{
  "id": "phone_davai_pridumaem",
  "category_id": "general",
  "group_id": "general_phone_features",
  "sort_order": 100,
  "variant_label_ru": "Давай придумаем",
  "is_primary_in_group": false,
  "title_ru": "Давай придумаем",
  "phrases": [
    "Алиса, давай придумаем",
    "Алиса, придумай пост для соцсетей"
  ],
  "effect_description_ru": "Запустит режим генерации текста (YandexGPT / Alice AI): идеи, поздравления, посты. Нужно: приложение Яндекс или чат alice.yandex.ru; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": ["obscure_pomozhet_pridumat_skazku_ili_rasskazhet"],
  "search_aliases": ["gpt", "нейросеть", "текст"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "phone", "alice_ai"]
}
```

#### `phone_davai_narisuem` — Давай нарисуем

```json
{
  "id": "phone_davai_narisuem",
  "category_id": "general",
  "group_id": "general_phone_features",
  "sort_order": 110,
  "variant_label_ru": "Давай нарисуем",
  "is_primary_in_group": false,
  "title_ru": "Давай нарисуем",
  "phrases": [
    "Алиса, давай нарисуем",
    "Алиса, нарисуй рыжего кота в скафандре"
  ],
  "effect_description_ru": "Сгенерирует изображение по описанию (Шедеврум / Alice AI Art). Нужно: чат Alice AI или приложение Яндекс; интернет.",
  "requires_alice_word": true,
  "requires_plus": false,
  "device_types": ["phone"],
  "related_command_ids": [],
  "search_aliases": ["шедеврум", "картинка", "генерация"],
  "source_url": "https://yandex.ru/support/browser-mobile-android-phone/ru/useful-features/examples",
  "updated_at": "2026-07-10T12:00:00Z",
  "tags": ["general", "phone", "alice_ai"]
}
```

---

## Итоговый чеклист импорта

1. Добавить **7 групп** из раздела «Новые группы команд».
2. Добавить **79 команд** (JSON-блоки выше) в `commands[]`.
3. Прогнать `.\gradlew.bat :server:validateContent "-PcontentFile=..."` на обновлённом bundle.
4. Проверить `related_command_ids` — часть ссылается на существующие id из preview-bundle; при переименовании id сверить grep.
5. Команды с `device_types: ["phone"]` — пометить в админке/приложении как «только телефон».

### Примечания по актуальности

- Навыки из **dialogs.yandex.ru** периодически снимают с публикации; перед publish сверить карточку навыка.
- **Игра Мафия** и аналоги — сторонние навыки, активационные фразы могут отличаться; на карточке навыка всегда канон.
- **Поиск телефона** — звонок может блокироваться антиспамом (указано в официальном описании навыка).
- **Выключи компьютер** и аналоги **не включены** — устаревший сценарий ПК-браузера.

---

*Сгенерировано по результатам сверки с `preview-bundle (9).json` и официальными источниками Яндекса, 2026-07-10.*
