# Backend — In-app Announcements

**Дата:** 2026-08-28 · **Статус:** implemented

Редакторские баннеры для коммуникации с пользователями приложения. Публичный endpoint вне content bundle; админка CRUD + upload image.

App contract: [`APP-ANNOUNCEMENTS.md`](https://github.com/MironBano/AliceCommands/blob/main/docs/APP-ANNOUNCEMENTS.md) (локально: `../AliceCommands/docs/APP-ANNOUNCEMENTS.md`).

---

## Public API

`GET /v1/announcements` — без auth.

- Фильтр на сервере: `enabled=true`, окно `starts_at`/`ends_at` (UTC now).
- `Cache-Control: public, max-age=300`, ETag `"announcements-{updatedAtMillis}"`.
- Клиент дополнительно фильтрует по `min_app_version`/`max_app_version` и dismiss.

```json
{
  "updated_at": "2026-08-28T10:00:00Z",
  "items": [
    {
      "id": "ann_welcome",
      "revision": 1,
      "placement": "more",
      "title": "Заголовок",
      "body": "Текст",
      "image_url": "https://cdn.alicecommands.ru/announcements/v1/welcome.webp",
      "background_color": "#E8F5E9",
      "foreground_color": "#1B5E20",
      "cta_label": "Смотреть",
      "cta_action": "route",
      "cta_target": "home/smarthome",
      "dismissible": true,
      "priority": 10,
      "min_app_version": "1.0.0",
      "max_app_version": null,
      "starts_at": null,
      "ends_at": null
    }
  ]
}
```

`cta_action`: `url` | `route` | `deeplink` (вместе с `cta_target`).

---

## Admin API

Под `withAdminAuth`:

| Method | Path | Описание |
|--------|------|----------|
| GET | `/admin/api/announcements` | Все баннеры (включая disabled) |
| POST | `/admin/api/announcements` | Создать (`revision=1`) |
| GET | `/admin/api/announcements/{id}` | Один баннер |
| PUT | `/admin/api/announcements/{id}` | Обновить (auto `revision++`) |
| DELETE | `/admin/api/announcements/{id}` | Удалить |
| POST | `/admin/api/announcements/upload-image` | Upload `{ slug, image_base64, content_type? }` → CDN URL |

Publish каталога **не нужен**. Не bump глобальный `min_app_version`.

---

## DB

Flyway `V12__announcements.sql` — таблица `announcements`.

---

## Images

Storage: `ANNOUNCEMENT_IMAGE_STORAGE_PATH` (default `./storage/announcements`).

Public URL: `{ICON_PUBLIC_BASE_URL}/announcements/v1/{slug}.{webp|png|jpg}`.

Static mount: `/announcements` в Ktor.

---

## Verification

```bash
./gradlew test
# ApiIntegrationTest: announcements public and admin crud flow
```
