# Android — Feedback & Command Reports

**Scope:** контракт для app-разработки. Реализация UI в Android **не входит** в backend-задачу.

**Base URL:** `BuildConfig.CONTENT_API_BASE_URL` (тот же, что для `/v1/content/*`).

---

## 1. Общий отзыв

**Экран:** настройки / «Оставить отзыв».

**Endpoint:** `POST /v1/feedback`

**Минимальный payload:**

```json
{
  "message": "Текст отзыва пользователя"
}
```

**Рекомендуемый payload** (автоматически из app):

| Field | Источник |
| ----- | -------- |
| `app_version` | `BuildConfig.VERSION_NAME` |
| `platform` | `"android"` |
| `locale` | `Locale.getDefault().toLanguageTag()` |
| `content_version` | локальный manifest после sync |
| `device_model` | `Build.MODEL` (опционально) |
| `rating` | 1–5, если есть звёзды в UI |

**Не отправлять:** email, телефон, user id — backend не хранит ПДн.

**UX:**

- Успех: «Спасибо!»
- `429`: «Слишком много отправок, попробуйте позже»
- Offline: показать ошибку; retry-очередь — только если уже есть в app

---

## 2. Ошибка в команде

**Экран:** детали команды → «Сообщить об ошибке».

**Endpoint:** `POST /v1/commands/{command_id}/report`

`command_id` — поле `Command.id` из Room (primary key после sync bundle).

**Payload:**

```json
{
  "issue_type": "phrase_not_working",
  "message": "Не срабатывает на колонке",
  "content_version": 42,
  "category_id": "timers",
  "command_title": "Поставить таймер",
  "phrase_used": "Алиса, поставь таймер на 5 минут",
  "app_version": "1.0.0",
  "platform": "android",
  "locale": "ru-RU"
}
```

**issue_type** (обязательный select в UI):

| Value | Label (RU) |
| ----- | ---------- |
| `wrong_effect` | Неверное описание |
| `outdated` | Устарело |
| `phrase_not_working` | Фраза не работает |
| `requires_plus_wrong` | Неверно про Plus |
| `wrong_device` | Не работает на моём устройстве |
| `other` | Другое |

**Откуда брать данные:**

| Field | Источник |
| ----- | -------- |
| `command_id` | path param = `command.id` |
| `content_version` | manifest в Room / prefs |
| `category_id`, `command_title` | snapshot команды в UI |
| `phrase_used` | фраза, которую пользователь выбрал/ввёл (optional) |

**Ответы:**

- `201` — принято
- `404` — команда не найдена в **текущем** published bundle (показать «обновите каталог»)
- `400` — validation
- `429` — rate limit

Если у пользователя старый `content_version`, report всё равно может быть принят (`command_exists_current: false`).

---

## 3. Auth

Public endpoints **не требуют** session/cookie/API key.

---

## 4. Admin workflow (для контекста)

Обращения попадают в `/admin` → **Отзывы** / **Ошибки команд**. Редактор закрывает их через resolve/dismiss; правки контента — отдельно через draft/editorial.
