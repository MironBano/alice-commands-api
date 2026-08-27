# Release audit v885 — идеал каталога

> **Канон:** [`seed/catalog-audit-fixed.json`](../seed/catalog-audit-fixed.json)  
> **Baseline:** `preview-bundle (10).json` (2026-07-11)  
> **Статус:** ✅ **885 / 885** — идеал достигнут (2026-07-11)  
> **`content_version`:** 52 (seed) → **54** (staging live)

## Итоговые метрики

| Метрика | До | После |
| --- | ---: | ---: |
| Команд | 885 | 885 |
| Пустых `search_aliases` | 337 | **0** |
| Команд с 1 alias | 183 | **0** |
| Infinitive titles | 5 | **0** |
| Generic multiroom effects | 3 | **0** |
| Dangling `related_command_ids` | 0 | **0** |
| Cross-cutting errors (groups/preview/checklist/cod) | 0 | **0** |

## Чеклист «идеал» (на команду) — все 885 ✅

- [x] `phrases[]` — говоримые, грамматика, без support-абзацев
- [x] `title_ru` — императив/карточка, не одно слово без контекста
- [x] `effect_description_ru` — 3-е лицо + «Нужно:», конкретно
- [x] `variant_label_ru` — различает варианты
- [x] `search_aliases[]` — минимум 2 синонима, без дублей title/phrases
- [x] `tags[]` — непустые, осмысленные
- [x] `related_command_ids` — валидные id
- [x] tech fields — корректны

## Прогресс по категориям

| category_id | команд | статус |
| --- | ---: | --- |
| music | ~197 | ✅ |
| smart_home + sh_* | ~104 | ✅ |
| tv_video | ~105 | ✅ |
| general | ~87 | ✅ |
| kids | ~87 | ✅ |
| timers | ~77 | ✅ |
| obscure | ~37 | ✅ |
| quick_answers | ~32 | ✅ |
| audiobooks | ~31 | ✅ |
| alice_plus | ~24 | ✅ |
| quick_commands | ~25 | ✅ |
| calls | ~22 | ✅ |
| station_settings | ~20 | ✅ |

## Ключевые правки (сводка)

| Область | Что сделано |
| --- | --- |
| **Aliases** | 337 пустых → 2–5 синонимов; 183 с 1 alias → 2+ |
| **Titles** | infinitive (5), one-word (music_laik, music_navyk, music_novosti, sh_*, tv_video, smart_home_svet) |
| **Effects** | generic multiroom (3); sh_socket «Подаст/Отключит» → «Включит/Выключит» |
| **ROUND3** | 14 команд уже с aliases/tags/effects по spec |
| **kids_nubik** | phrases/source_url проверены |
| **Cross-cutting** | 80 групп, checklist (8), command_of_day — 0 ошибок |

## Cross-cutting ✅

- [x] 80 групп — preview_command_ids валидны, 0 empty groups
- [x] 0 dangling related_command_ids
- [x] checklist_items (8) — id существуют
- [x] command_of_day — `music_vkliuchi_muzyku_50_kh` существует
- [x] validateContent OK
- [x] staging publish **v54** (2026-07-11) + `verify-staging.ps1` green

## Исключения (пустые aliases)

Нет — все 885 команд имеют ≥2 `search_aliases`.

## Definition of Done

- [x] **885 / 885** «идеал достигнут»
- [x] **0** открытых отклонений
- [x] `validateContent` OK
- [x] staging publish + `verify-staging.ps1` (v54, 885 commands)
