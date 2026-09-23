# Referral CDN photos — productPhoto → our host

**Дата:** 2026-09-16 · **Канон:** AliceCommands `SPEC-REFERRAL-PRODUCT-LINKS.md`

App Coil загружает только хосты из `CDN_IMAGE_HOST_ALLOWLIST` / `CdnImageUrlPolicy`  
(`cdn.alicecommands.ru`, `staging-cdn…`, `api…`, `staging-api…`).  
**Не** добавлять `avatars.mds.yandex.net` в allowlist.

## Flow

1. Affiliate API `link/create` / `article/create` → `productPhoto` (часто mds.yandex.net).
2. Скачать байты на сервере (cron или admin).
3. `POST /admin/api/smarthome/upload-image`  
   body: `{ "slug": "pick_<id>", "image_base64": "…", "content_type": "image/webp" }`  
   (также png/jpeg).
4. В ответ — public `image_url` вида `{PUBLIC_BASE_URL}/devices/v1/{slug}.webp`.
5. Прописать `image_url` в device pick и publish snapshot.

Ops scripts: `scripts/upload-device-images.ps1`, admin UI upload.

## Fallback

Пока фото не залито — допустим наш SVG/иконка типа **только если** title/цена уже от оффера SKU.  
Не оставлять abstract title «Умная лампа» на чужой product URL.
