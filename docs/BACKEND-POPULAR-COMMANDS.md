# Backend Plan — Popular Commands (analytics + pins)

**mob_id:** MOB-20260626-001 · **Дата:** 2026-08-27 · **Статус:** implemented (Flyway V11 + use cases + admin UI; deploy staging/prod separately)

Цель: пул популярных команд — **источник истины на backend**. Ранг из `analytics_events` (TTS + views) + ручные pins в админке. Доставка отдельным endpoint (не content bundle). История прогонов с метриками для ручного просмотра.

App contract: [`APP-POPULAR-COMMANDS.md`](https://github.com/MironBano/AliceCommands/blob/main/docs/APP-POPULAR-COMMANDS.md) (локально: `../AliceCommands/docs/APP-POPULAR-COMMANDS.md`).

---

## 1. Why

| Сейчас | Проблема |
| ------ | -------- |
| Хардкод id в APK | Нет editorial / analytics управления без релиза |
| Breakdown analytics вручную | Нет готового ranked snapshot |
| Нет истории рангов | Нельзя сравнить «кто был популярен неделю назад» |

---

## 2. Ranking policy (канон)

**Окно:** 7 суток, календарь **Europe/Moscow**.

**Score** по `params.command_id`, **unique `install_id`**:

```
score = 3 × unique_tts + 1 × unique_view
```

- TTS: `event_name = command_tts`
- View: `event_name = command_view`

**Anti-loop:** не считать `command_tts` где `params.source` ∈ `try_now`, `quick`, `search`.

**Исключения:** пустой `command_id`; denylist (seed = contaminated: checklist/COD bias); id нет в live published catalog; из пары `sh_light_dim` / `sh_light_off` — одна с большим score (**только analytics/seed**; pins — editorial override, collapse не применяется).

**Сборка served pool (до 12 id):**

1. Admin pins (порядок редактора).
2. Добор analytics-рангом без дублей.
3. Если живых id &lt; 6 — pad seed-списком (текущий app whitelist, только id из каталога).

**Пересчёт:** in-process ticker каждые 6 ч + `POST …/recompute` + после save pins. Public GET читает snapshot.  
**Guard:** live catalog пуст или `served` пуст при уже непустом snapshot → **не** затирать snapshot (abort + log). Single-flight lock на recompute.

**История:** каждый пересчёт дописывает `popular_rank_runs` + `popular_rank_run_items` (топ analytics до 50 + все pins, флаг `in_served_pool`). Retention **180 дней**. Не отдаётся app.

---

## 3. Schema (Flyway V11)

### `popular_command_pins`

| Column | Type | Notes |
| ------ | ---- | ----- |
| `command_id` | TEXT PK | FK → commands |
| `sort_order` | INT | |
| `created_at` | TIMESTAMPTZ | |
| `created_by` | TEXT? | |

### `popular_command_denylist`

| Column | Type | Notes |
| ------ | ---- | ----- |
| `command_id` | TEXT PK | |
| `reason` | TEXT | |

Seed: `music_vkliuchi_muzyku`, `general_gromche`, `quick_commands_dalshe`, `timers_postav_taimer_na_5_minut`.

### `popular_commands_snapshot`

| Column | Type | Notes |
| ------ | ---- | ----- |
| `sort_order` | INT PK | |
| `command_id` | TEXT | |
| `source` | TEXT | `pinned` \| `analytics` \| `seed` |
| `unique_tts` | INT | |
| `unique_view` | INT | |
| `score` | INT | |
| `updated_at` | TIMESTAMPTZ | |

### `popular_rank_runs` / `popular_rank_run_items`

Шапка: `id`, `computed_at`, `window_from`, `window_to`, `window_days`, `trigger` (`ticker`\|`recompute`\|`pins`), `computed_by`.

Строки: `run_id`, `sort_order`, `command_id`, `source`, `unique_tts`, `unique_view`, `score`, `in_served_pool`.

Index: expression on `(params->>'command_id')` for command_tts / command_view.

---

## 4. Public API

```
GET /v1/popular-commands
Cache-Control: public, max-age=300
ETag: based on snapshot updated_at
```

```json
{
  "updated_at": "2026-08-27T09:00:00Z",
  "window_days": 7,
  "commands": [
    { "id": "music_muzyka", "source": "analytics", "score": 42 }
  ]
}
```

Auth: нет. Не в content bundle. `min_app_version` не поднимать.

---

## 5. Admin API

| Method | Path | Purpose |
| ------ | ---- | ------- |
| GET | `/admin/api/popular-commands` | Current snapshot + pins |
| PUT | `/admin/api/popular-commands/pins` | Ordered pin ids |
| POST | `/admin/api/popular-commands/recompute` | Force rank |
| GET | `/admin/api/popular-commands/history?limit=&offset=` | Run list |
| GET | `/admin/api/popular-commands/history/{runId}` | Run detail |

UI: current pool + pins + Save / Recompute; History list → detail table.

---

## 6. Seed pad ids

Same as app `OrganicPopularCommands.IDS`:

1. `music_luchshie_kompozitsii`
2. `music_muzyka`
3. `smart_home_vkliuchi_girliandu`
4. `sh_light_dim` / `sh_light_off`
5. `calls_pozvoni`
6. `music_muzyku_gromche`, `music_avtoradio`, `music_bitlz`

---

## 7. Architecture

Light Clean: `application/popular/` use cases; persistence in `infrastructure/persistence/`; routes in `PublicRoutes` / `AdminRoutes`.

Ticker: Application module coroutine every 6h → `RankPopularCommandsUseCase(trigger=ticker)` → prune history.

---

## 8. Verification

- [ ] Unit: weights, anti-loop sources, light-pair, pins-first, seed pad
- [ ] Integration: GET public, admin 401, recompute writes history
- [ ] Prune does not delete fresh runs
- [ ] Flyway V11 on staging

---

## 9. Related

- App: `AliceCommands/docs/APP-POPULAR-COMMANDS.md`
- Analytics: [`ANALYTICS-BACKEND.md`](ANALYTICS-BACKEND.md), [`ANALYTICS-GLOSSARY.md`](ANALYTICS-GLOSSARY.md)
- API: [`API.md`](API.md)
