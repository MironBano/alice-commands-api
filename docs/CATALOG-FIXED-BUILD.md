# Сборка исправленного каталога (ручная)

**Источник:** `preview-bundle (8).json` (аудит в [`COMMAND-MANUAL-AUDIT.md`](./COMMAND-MANUAL-AUDIT.md))  
**Результат для загрузки:** [`seed/catalog-audit-fixed.json`](../seed/catalog-audit-fixed.json)  
**Метод:** только правки в JSON через редактор (**StrReplace**) — **без скриптов**.

**Эталон (только чтение):** [`seed/catalog-audit-fixed-REFERENCE-v43.json`](../seed/catalog-audit-fixed-REFERENCE-v43.json) — снимок v43 после скриптовых партий 6–10; **FIXED достиг byte-parity 2026-07-08** (сверять `git diff --no-index`, не копировать скриптом).

**Загрузка:** Admin → Import bundle (`POST /admin/api/import/json?mode=replace`) или `scripts/push-draft.ps1` после `validateContent`.

**Staging (2026-07-11):** `catalog-audit-fixed.json` — **885** команд, `content_version=52`, orphan=0. Release audit: [`RELEASE-AUDIT-v885.md`](./RELEASE-AUDIT-v885.md).

```powershell
.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"
.\scripts\push-draft.ps1
# Admin → Publish
```

**Legacy pipeline удалён** — import только **replace** (`push-draft.ps1`). Editorial/rebuild-draft больше не используются.

---

## REDO (2026-07-07) — после нарушения скриптами

| Шаг | Что | Статус |
| --- | --- | --- |
| 0 | Откат + **фаза 0** (9 P0 удалений) | ✅ v33 → … → v43 |
| 1 | Фазы 1–5 (ручные) | ✅ |
| 2 | Партии 6–10 (были скрипты — **только StrReplace**) | ✅ |
| 3 | Партии 11–12 (были частично ручные) | ✅ |
| 4 | **Партия 13** — parity с REFERENCE v43 (`sort_order`, phrases, effects, related) | ✅ **0 diff hunks** |
| 5 | `validateContent` + byte-identical сверка с REFERENCE | ✅ |

Правило проекта: [`.cursor/rules/no-content-scripts.mdc`](../.cursor/rules/no-content-scripts.mdc).

> Скриптовые `_patch_catalog_batch*.py` **удалены**. Повторять их логику — серией StrReplace по этому трекеру.

**Текущий статус (2026-07-11):** [`seed/catalog-audit-fixed.json`](../seed/catalog-audit-fixed.json) — **885** команд, `content_version=52`, `validateContent` OK, orphan=0. Release audit: [`RELEASE-AUDIT-v885.md`](./RELEASE-AUDIT-v885.md).

**Исторический эталон v43 (2026-07-08):** byte-identical с [`seed/catalog-audit-fixed-REFERENCE-v43.json`](../seed/catalog-audit-fixed-REFERENCE-v43.json) — **764** команд; дальнейшие правки (ROUND2 +28, product) только вручную в FIXED, не копировать REFERENCE скриптом.

---

## Метаданные bundle

| Поле | Было | Стало | Статус |
| --- | --- | --- | --- |
| `content_version` | 0 | **43** | ✅ |
| `published_at` | preview | **2026-07-07T10:30:00Z** | ✅ |
| Команд в `commands[]` | 798 | **764** | ✅ (−34: P0 удаления + переносы) |
| Parity с REFERENCE v43 | — | **byte-identical** | ✅ (2026-07-08) |

---

## Фазы (порядок как в аудите)

| Фаза | Категория | Команд | Статус | Примечание |
| ---: | --- | ---: | --- | --- |
| 0 | Глобальные удаления (P0) | −9 | ✅ | дубли, мусор pipeline |
| 1 | `alice_plus` | 21 | ✅ | −1 дубль; khvatit → general |
| 2 | `audiobooks` | 22 | ✅ | −15 сказок → kids; +nav/search |
| 3 | `calls` | 20 | ✅ | P0: rasskazhet, ia_tebia/miau → messages |
| 4 | `general` | 56 | ✅ | −22 general_timers; +kurs_euro; recipe/shopping |
| 5 | `music` | 179 | ✅ | P0+P1+P2; sort_order 0 коллизий |
| 6 | `tv_video` | 96 | ✅ | P0+P1; −1 alisa |
| 7 | `timers` | 71 | ✅ | +otmeni_taimery; timers_shutdown; −general дубли |
| 8 | `smart_home` | 100 | ✅ | slash-phrases, AC/socket, sportivnye novosti |
| 9 | `quick_answers` | 28 | ✅ | города в предложном; pushkin/stolitsa; nastroi_novosti |
| 10 | `quick_commands` | 24 | ✅ | sleduiushchii phrases; priglushi effect |
| 11 | `kids` | 87 | ✅ | +15 сказок из audiobooks; mudryi_uchitel, games aliases |
| 12 | `station_settings` | 25 | ✅ | podsvetka/ekran/mikrofon; voice-profile перенос |
| 13 | `obscure` | 35 | ✅ | hidden/easter phrases → говоримые команды |
| 14 | Группы / `preview_command_ids` / `related` | — | ✅ | related без dangling (0); snooze/calendar chains |
| 15 | `validateContent` + ручная сверка | — | ✅ | 764 commands, schema OK |
| 16 | **Parity REFERENCE v43** (партия 13) | — | ✅ | `git diff --no-index` → 0 hunks |

---

## Фаза 0 — удалить команду (P0)

| command_id | Причина | Статус |
| --- | --- | --- |
| `alice_plus_s_optsiei_alisa_plius_pomozhet_vyuchit_t` | дубль `alice_plus_davai_uchit_tablitsu_umnozheniia` | ✅ |
| `music_kakoe_radio_u_tebia_est_kakie_radiostant` | дубль `music_kakoe_radio_u_tebia_est` | ✅ |
| `timers_iandeks_s_alisoi` | мусор pipeline | ✅ |
| `quick_commands_iandeks_s_alisoi` | мусор pipeline | ✅ |
| `audiobooks_bitlz` | дубль `music_bitlz` | ✅ |
| `audiobooks_igrai_vezde` | дубль `music_igrai_vezde` | ✅ |
| `smart_home_dobroe_utro` | дубль `sh_scenario_morning` | ✅ |
| `quick_answers_aleksandr` | мусор translation | ✅ |
| `quick_answers_voskresene` | мусор translation | ✅ |

После каждого удаления: убрать id из `command_groups[].preview_command_ids` и `related_command_ids`.

---

## Журнал правок (по фазам)

### Фаза 0

- Удалено 9 команд; `content_version` → 33.
- Обновлены `preview_command_ids`: `alice_plus_education`, `qc_nav`.
- `quick_commands_nastroi_kolonku` → primary в `qc_nav`.

### Фаза 1 — alice_plus

- `alice_plus_khvatit`: `category_id` → `general`, `group_id` → `general_playback`, `requires_plus` → false, `device_types` + phone.
- `alice_plus_sokhrani`: `source_url` → `/alice-plus/chat`.
- `alice_plus_razbudit_rebenka_*`: title «Разбуди ребёнка», phrases → императив.
- `alice_plus_detskii_kontent_plius`: добавлен `variant_label_ru`.
- `alice_plus_est_li_u_menia_plius`: `requires_plus` → false.

### Фаза 2 — audiobooks

- `audiobooks_ne_vkliuchaet_*` → `music_pochemu_ne_vkliuchaetsia_muzyka` (music / `music_info`); phrases + title исправлены.
- `audiobooks_sleduiushchaia_glava` / `audiobooks_uskor_audioknigu`: разделены phrases; добавлены `audiobooks_predydushchaia_glava`, `audiobooks_zamedli_audioknigu`.
- `audiobooks_krossfeid` → `music_krossfeid` (`music_services`).
- Удалён `audiobooks_moia_volna` (дубль `music_vkliuchi_moiu_muzyku`).
- `audiobooks_vkliuchi_audioknigu_piknik_na_obochke`: «обochке» → «обочине».
- `audiobooks_vkliuchi_detektiv` + новый `audiobooks_vkliuchi_fantastiku`.
- `audiobooks_volshebnik_izumrudnogo_goroda`: `source_url` → `/skills/`.
- Группы: `ab_info`, `ab_nav`, `music_info` — обновлены `preview_command_ids`.

### Фаза 3 — calls

- `calls_rasskazhet_*`: title «Откуда сообщение»; phrases → императив.
- `calls_ia_tebia_liubliu`, `calls_miau`: `group_id` → `calls_messages`; effect без требования звонков.
- `calls_pozvoni_dedushka`: вторая phrase «позвони дедушка».
- `calls_vkliuchi_gromkuiu_sviaz`: aliases `intercom` → «громкая связь», «интерком».

### Фаза 4 — general (P0)

- Удалён дубль `general_eshche_raz_rasskazhet_*` (есть `general_povtori_ingredienty`).
- Рецепты: `davai_prigotovim`, `na_liubom_shage`, `naidi_retsept`, `vosproizvedet`, `predydushchii_shag` → cooking/phrases/effects.
- `general_ekonom` / `general_komfort` → `general_services`, effect = тарифы такси.
- `general_udali`, `general_otmeni_zakaz`, `general_otmeni_taimery`: effect в 3-м лице; timer source для otmeni_taimery.
- Сервисы: `mozhet_prislat_*`, `mozhet_samostoiatelno_*`, `opovestit_*`, `pomozhet_vam_*` — phrases + title + effect.
- Группы: `general_cooking`, `general_device`, `general_services` — preview обновлены.

### Фаза 5 — music (P0, частично)

- Удалены дубли: `music_vkliuchi_audioknigu_diuna` (есть `audiobooks_diuna`), `music_vkliuchi_bluetooth` (есть `general_vkliuchi_bluetooth`).
- Переносы: `music_novosti`/`music_pogoda` → `quick_answers`; `music_nazyvai_*`/`music_zabud_moi_golos` → `station_settings`; книги → `audiobooks`; `music_vkliuchi_skazki_v_spalne` → `kids`; `music_vykliuchi_bluetooth` → `general_device`.
- Effects/phrases: `khoroshee_nastroenie`, `kakoi_trek_igraet`, `vykliuchi_karaoke`, `znaesh_li_ty`, `gromkost_na_1_10`, genre_mood/radio, `postav_na_pauzu` (слэши), `elvisa_presli`, `khimeru`, `otkroi_pleilist`, favorites aliases.
- Группы: `qa_news`, `qa_weather_city`, `station_personality`, `ab_nav`, `music_podcasts`, `general_device` — preview обновлены.

**Партия 2 (playback/search P0):**
- `davai_poslushaem_*` в playback (~9): effect «радиостанцию» → подборка/жанр.
- Скорость AB: `music_kakaia_skorost_*`, `music_zamedli`, `music_uskor_v_poltora_raza` → `audiobooks`/`ab_nav`.
- Шаблоны: `vkliuchi_muzyku_50_kh`, `postav_muzyku_60/80_kh`, `vkliuchi_muzykalnye_novinki`, `vrubai_*`, `zapusti_disko_*`, `zapusti_nautilus_pompilius`.
- Удалены дубли: `music_vkliuchi_audioknigu_anna_karenina`, `music_zapusti_knigu_voina_i_mir`.
- Phrases: `vkliuchi` (speech-trainer), `sdelai_gromche` (HDMI), `postav_albom`, `romashki`, `metallica`, `vechno_molodoi`.
- Search effects: `belye_rozy`, `moe_serdtse`, `shtil`; `khimeru` title → «Химера».

**Партия 3 (закрытие music):**
- P1 phrases: genre_mood (русская, тренировка, джаз, вокал, радостное, поп, дождь, рок, хиты, джаз-трек), karaoke, belye_rozy, sleduiushchaia, evropa_plius.
- P0: `music_navyk` → `general`/`general_info`; `music_kakoe_radio_u_tebia_est` effect+phrases; `music_zapusti_pesniu_romashki` (сломанный JSON); metallica phrase; didzhei quote; search effects (химера, группа крови, танго, вечно молодой).
- P2: страны → `music_radio`; `music_a_eshche` → `music_info`; `music_chto_igraet` → `music_info` + related; `davai_poslushaem_radio` → `music_radio`.
- Effects: `vrubai_novuiu`, `zapusti_samoe_populiarnoe`, `vkliuchi_karaoke`.

### Фаза 6 — tv_video (P0, частично)

- Effects: «Назад в будущее» (kinopoisk + search), `peremotai_na_20`, телепрограмма (`kakie_filmy*`, `serialy_segodnia`), `posovetui_*`, `pokazhi_serial_triller`, `zapusti_multfilm_pro_kota`.
- Phrases: `smotret`→«смотри», `kupit`→«купи фильм»; разнесены zastavku, peremotai_10, spisok_dorozhek, enotom, khokkei, obyknovennoe_chudo, naidi_komediinyi_serial, zapusti_film_2012, posovetui_multfilm/serial.
- Таймеры → `timers`/`timers_timer`: `tv_video_vykliuchi_kolonku_cherez_2_chasa`, `vykliuchis_cherez_40_minut`, `vykliuchi_stantsiiu_v_polnoch`.
- Кавычки: Побег, Клиника, Король Лев, Форрест Гамп, Игра престолов, Наука 2.0, Планета HD.
- `vkliuchi_russkie_subtitry` effect+phrase.

**Партия 2 (P1 + закрытие):**
- Удалён `tv_video_alisa` (не команда каталога); `igrai` → primary.
- P1: gromche/kinopoisk, sleduiushchaia_seriia, kanal_5, HDMI phrases, novogodnii_kanal, skazhi_programmu дубли, komediiu, serial_druzia, dorozhki effects.
- related: nazad_v_budushchee ↔ kinopoisk; druzia ↔ klinika.

### Фаза 7 — timers ✅

- Удалён `timers_udali_budilnik_udali_vse_budilniki` (slash-дубль).
- Переносы: `chto_novogo`, `podpisatsia` → `station_settings`; `umnyi_dom` → `smart_home`; `nastroi_kolonku` → `station_settings`.
- Будильники → `timers_alarm`; календарь/напоминания → `timers_reminder`.
- Phrases разнесены: budi_7, otkliuchi, gromkost_5, skolko_do_budilnika, napomni_*, sobytie_kofe, vstrecha_roditeli, taimer_10, taimer_sna.
- Effects: ustanovi_v_piatnitsu (календарь), skolko_do_kontsa_taimera_sna; `otmeni_taimer` source → timer.
- related: snooze chain (`eshche_5` ↔ `dai_pospat` ↔ `razbudi_popozzhe`); budilniki query pair.

### Фаза 8 — smart_home ✅

- Slash-phrases: `vkliuchi_vykliuchi_rozetki_vezde`, `vkliuchi_vykliuchi_svet`, `vkliuchi_sleduiushchii_predydushchii_kan`.
- `vkliuchit_sportivnye_novosti` → говоримая phrase; `konditsioner`/`torsher` effects; `sh_socket_*` 3-е лицо.
- `sh_temp_query` / `kakaia_temperatura_v_detskoi` — влажность убрана из temperature ids.

### Фазы 9–10 — quick_answers / quick_commands ✅

- 9 городов: предложный падеж (Москве, Петербурге, …); `nastroi_novosti` effect; pushkin/stolitsa phrases.
- `quick_commands_sleduiushchii` — только playback phrases; `priglushi_svet` effect.

### Фаза 11 — kids ✅

- `davai_pogovorim_s_mudrym_uchitelem`: phrase[0], `kids_games`.
- `vkliuchi_nolika`, `kak_govorit_korova`, `ugadai_chislo`; `davai_trenirovat_zvuki` → `kids_activities`.
- `khochu_pomeniat` / `zanimatsia_s_neimoi` → `kids_activities`.

### Фазы 12–13 — station_settings / obscure ✅

- station: podsvetka/красная/свеча/экран/микроfon phrases; smeni_golos, govori_po_russki effects.
- obscure hidden: article phrases → команды (сказка, стих, два запроса, перевод, песня, лавка, поддержка, календарь, метроном); stroboskop effect.

### Фаза 15

- `validateContent` OK: 13 categories, 764 commands.
- Починен `music_kakoe_radio_u_tebia_est` (отсутствовали `source_url`, `updated_at`, `group_id`).

### Партия 4 — P1 дочистка (фазы 5–13)

- **smart_home:** 7× room light on/off phrases разнесены; охрана/замок; camera `sh_*` ↔ `smart_home_pokazhi` related.
- **music:** `postav_na_pauzu` (1 phrase + stop related); `vkliuchi` (1 phrase); `sdelai_gromche` / `prodolzhai_igrat` phrases.
- **general:** `pokazhi_spisok_pokupok` — только show phrase.
- **kids:** сказки Лазарев/казахский effects; `vybrat_personazha` → activities; 8 пар игр related (города, слова, загадки, legko_skazat, актёр, число, виселица, зоология); legko_skazat ↔ zapusti.
- **obscure:** 7 lazy ambient/light effects → глаголы 3-го лица.
- **station:** `music_nazyvai_*` / `music_zabud_*` related в personality.

### Партия 5 — P1/P2 дочистка

- **general:** `general_kurs_euro` (отделён от доллара); `tut_dalshe` ↔ `predydushchii_shag`; `udali_iz_spiska_pokupok_konfety` effect.
- **quick_commands:** QC без «Алиса» (dalshe, khvatit, povtori, alisa flag); nastroi_kolonku source; smart-home QC phrases; lazy effects (таймер, будильник, температура).
- **quick_answers:** `kakaia_pogoda_zavtra` ↔ `budet_li_dozhd`; `kakoi_veter` effect.
- **station_settings:** source_url → `ambient-light`, `speaker`, `voice-profile`, `clock-off`, `notifications`, `quick-commands` (21 команда).
- **sort_order:** kids_games (0 коллизий), alice_plus_content, music_radio.

### Партия 6 — глобальная дочистка

- **sort_order:** 0 коллизий во всех группах (67 → 0); перенумерация шаг 10.
- **general_timers:** удалены 22 дубля (`general_taimer_*`, `general_otmeni_*`); группа `general_timers` снята; checklist → `timers_postav_taimer_na_5_minut`.
- **timers:** +`timers_otmeni_taimery`; группа `timers_shutdown` (TV sleep timers).
- **phrases:** все команды ≤2 phrases; playback/QC/music/timers разнесены.
- **effects:** lazy «Ответит на вопрос» → конкретные глаголы (6 QA); AC effects по температуре/расписанию (7).
- **station_settings:** 0 generic `/skills/` (4 системные → speaker/notifications).
- **smart_home:** `vkliuchi_rozetku` ↔ `sh_socket_on`; AC related.
- **alice_plus_khvatit** → `general_device` (из playback).

### Партия 7 — kids/audiobooks + polish

- **15 сказок** `audiobooks_vkliuchi_skazku_nomer_*` → `kids` / `kids_fairy_tales`; source `/station/kids`.
- **Звонки:** дательный падеж (`позвони маме`); titles обновлены.
- **`general_kotoryi_chas`** → `general_info`; related ↔ `quick_commands_kotoryi_chas`.
- **Music lyrics:** `music_pokazhi_tekst_pesni` ↔ `music_pokazhi_slova_pesni`.
- **`kids_fiksiki`** → `kids_activities`; **kids_games** aliases (15).
- **Preview groups:** `ab_play`, `kids_fairy_tales`, `general_info`.
- **Second phrases:** 508 → 24 (QC без «Алиса» намеренно 1 phrase).
- `content_version` **37**.

### Партия 8 — финальная дочистка

- **general_social** preview: убран несуществующий `general_kak_dela`.
- **kids_games:** aliases для оставшихся 10 команд (0 пустых).
- **music radio:** `music_kakoe_radio_u_tebia_est` phrases dedup; related ↔ `music_kakie_radiostantsii`.
- **obscure_komfort** vs **general_komfort:** разные phrases (подсветка vs такси).
- **general_tishe** / **general_potishe:** effect + aliases + related.
- **timers_otmeni_taimer** ↔ **timers_otmeni_taimery**.
- **shopping:** `general_dobav_v_spisok_pokupok_iogurt_khleb_kefi` phrase[1] ≠ масло.
- **music seek:** `music_na_10_sekund_vpered` phrase[1] + effect; related ↔ `music_na_20_sekund_nazad`.
- `content_version` **39**; механический аудит: **0** issues.

### Партия 9 — P0 погода + recipe group

- **8 городов** `quick_answers_kakaia_pogoda_v_*`: phrase[1] без «завтра»; related ↔ `kakaia_pogoda_zavtra`.
- **`general_tut_dalshe`** → `general_cooking` (не playback).
- `content_version` **40**.

### Партия 10 — command_groups publish-ready

- **ab_play** preview: только члены группы (без detektiv/fantastiku из `ab_search`).
- **kids_education** preview: без `mudrym_uchitelem` (он в `kids_games`).
- Удалена пустая группа **ab_info** (0 команд).
- Group validation: **0** errors (как при publish).
- `content_version` **41**.

### Партия 11 — kids_games primary

- **kids_games:** один `is_primary_in_group` — `kids_davai_poigraem` (снят с mudrym_uchitelem и blogerov).
- Publish validation: **0** errors.
- `content_version` **42**.

### Партия 12 — ручная дочистка (после аудита скриптов)

- **general_cooking:** `general_tut_dalshe` sort_order 80→90 (коллизия с `predydushchii_shag`).
- Удалены `_patch_catalog_batch6.py` … `batch10.py` и `_check_groups.py`.
- `content_version` **43**.

### Партия 13 — parity с REFERENCE v43 (2026-07-07 … 2026-07-08)

Ручная сверка FIXED ↔ REFERENCE (`Grep` / `Read`, правки **StrReplace**). Крупные блоки:

- **`sort_order`:** `tv_playback` (61), `timers_timer` / `timers_alarm` / `timers_reminder`, `kids_education`, `kids_activities`, `kids_fairy_tales`, `obscure_hidden`, `qa_weather_city`, `qc_playback`, `general_info`, `general_device`, `alice_plus_education`, `general_services`, `qa_news` — drift **0**.
- **phrases:** `smart_home_light` (7 комнат), `timers_reminder` (3), `tv_search` (4), `tv_playback` (programmu), `qc_playback`, music/timers P0.
- **прочее:** `published_at`, checklist hint, `quick_commands_alisa` `requires_alice_word`, `tv_kinopoisk` variant_label.
- **финал (7 hunks):** tv effects (`predydushchii_kanal`, дорожки 18+/1-я, `zapusti_piatyi_kanal`, `pokazhi_novinki_filmov`); `scenario_morning` → `source_url` `/skills/news`; EOF newline.

**Проверка:** `git diff --no-index seed/catalog-audit-fixed.json seed/catalog-audit-fixed-REFERENCE-v43.json` → **IDENTICAL**; `validateContent` OK.

### Фаза 11 (ранее) — kids кавычки

- Кавычки: Угадай актера, Виселицу, Зоологию; music «Легко сказать».

---

*Обновляй статусы ✅ по завершении каждой фазы.*
