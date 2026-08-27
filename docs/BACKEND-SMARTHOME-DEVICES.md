# Smart home devices — backend

**Schema:** [`schema/smarthome-devices.schema.json`](../schema/smarthome-devices.schema.json) · **Migrations:** V7, V8, V10  
**Public API:** `GET /v1/smarthome/devices` · **Static images:** `GET /devices/v1/{slug}.{webp|png|jpg}`

---

## 1. Назначение

Единый snapshot для вкладки «Устройства» в AliceCommands:

| Сущность | Назначение |
| -------- | ---------- |
| **guides** | Типы устройств — detail-screen с setup/capabilities (поверхности Алисы + IoT) |
| **picks** | Подборки товаров (Market / CPA) с contextual targeting для chips в каталоге |

**Канон контента:** [`seed/smarthome-devices-full.json`](../seed/smarthome-devices-full.json) — **35 guides** + **36 picks** (`pick_{guide_id}` + `pick_baby_monitor`). Picks несут `command_ids` для placement `command_detail` (140 команд, без дублей); все 102 `smart_home` команды покрыты.

**Правило зерна guides:** один guide на **функциональный** тип платформы Яндекса. Подтипы форм-фактора не размножаем (`light.lamp` / `light.strip` / `light.ceiling` → одна карточка `light`; все `smart_meter.*` → `smart_meter`). Датчики с разной функцией (`sensor_motion`, `sensor_water_leak`, …) — отдельные карточки. Enum `command_device_filter_id` / pick `device_types` остаётся только `station` \| `tv` \| `phone` (где говорят с Алисой).

### Inventory guides (35)

| id | title_ru | Yandex type / note | sort |
| -- | -------- | ------------------ | ---- |
| `station` | Колонка с Алисой | поверхность Алисы | 10 |
| `tv` | Умный телевизор с Алисой | поверхность Алисы | 20 |
| `phone` | Алиса на смартфоне | поверхность Алисы | 30 |
| `hub` | Хаб умного дома | инфраструктура (не `devices.types.*`) | 40 |
| `light` | Свет | `light` + все `light.*` | 50 |
| `socket` | Умная розетка | `socket` | 60 |
| `switch` | Выключатель и реле | `switch` + `switch.relay` | 70 |
| `sensor_motion` | Датчик движения | `sensor.motion` | 80 |
| `sensor_open` | Датчик открытия | `sensor.open` | 90 |
| `sensor_climate` | Датчик климата | `sensor.climate` | 100 |
| `sensor_water_leak` | Датчик протечки | `sensor.water_leak` | 110 |
| `sensor_smoke` | Датчик дыма | `sensor.smoke` | 120 |
| `sensor_gas` | Датчик газа | `sensor.gas` | 130 |
| `sensor_button` | Умная кнопка | `sensor.button` | 140 |
| `sensor_illumination` | Датчик освещённости | `sensor.illumination` | 150 |
| `sensor_vibration` | Датчик вибрации | `sensor.vibration` | 160 |
| `thermostat` | Термостат и обогрев | `thermostat` | 170 |
| `thermostat_ac` | Кондиционер | `thermostat.ac` | 180 |
| `humidifier` | Увлажнитель воздуха | `humidifier` | 190 |
| `purifier` | Очиститель воздуха | `purifier` | 200 |
| `ventilation` | Вентиляция и вентилятор | `ventilation` + `ventilation.fan` | 210 |
| `curtain` | Шторы и жалюзи | `openable.curtain` | 220 |
| `door_lock` | Умный замок | `openable.door_lock` | 230 |
| `valve` | Шаровой кран | `openable.valve` | 240 |
| `openable` | Двери, ворота и окна | `openable` | 250 |
| `camera` | Камера | `camera` | 260 |
| `vacuum_cleaner` | Робот-пылесос | `vacuum_cleaner` | 270 |
| `kettle` | Умный чайник | `cooking.kettle` | 280 |
| `coffee_maker` | Кофеварка | `cooking.coffee_maker` | 290 |
| `dishwasher` | Посудомоечная машина | `dishwasher` | 300 |
| `washing_machine` | Стиральная машина | `washing_machine` | 310 |
| `cooking` | Кухонная техника | `cooking` | 320 |
| `smart_meter` | Счётчик | все `smart_meter.*` | 330 |
| `pet_feeder` | Кормушка | `pet_feeder` | 340 |
| `pet_drinking_fountain` | Поилка | `pet_drinking_fountain` | 350 |

Не заводим отдельными guides: `other`, `iron`, дубли `media_device*` (ТВ уже `tv`), подтипы света и счётчиков.

**Successor** для deprecated `GET /v1/affiliate/blocks`. Affiliate CRUD в admin остаётся legacy.

---

## 2. Publish flow

```
Admin CRUD (guides / picks)
        │
        ▼
SmartHomeDevicesValidationUseCase (JSON Schema)
        │
        ▼
storage/manifest/smarthome_devices.json
        │
        ▼
GET /v1/smarthome/devices (public, Cache-Control max-age=300)
```

- Каждый **save** guide/pick в admin **автоматически** пересобирает snapshot (отдельно от content bundle publish).
- Content bundle publish **не** включает smarthome — отдельный файл manifest.
- **404** на public endpoint до первого save/publish guides или picks.

---

## 3. Public contract

### GET /v1/smarthome/devices

См. [API.md](API.md) § `/v1/smarthome/devices` — полный пример с contextual fields.

**Guides (обязательные поля):** `id`, `title_ru`, `summary_ru`, `capabilities_ru`, `setup_ru`, `action_url`, `sort_order`.

**Picks (обязательные поля):** `id`, `title_ru`, `action_url`, `sort_order`.

**Picks (contextual, V10):** `placements`, `tags`, `device_types`, `category_ids`, `command_group_ids`, `command_ids`, `scenario_template_ids`, `guide_ids`, `priority`, `cta_ru`, `starts_at`, `ends_at`, `max_impressions_per_session`.

**Placements enum:** `smart_home_devices`, `device_guide_detail`, `scenario_detail`, `command_detail`, `catalog_persona`, `search_empty`, `checklist_complete`.

**Targeting (как у `command_ids` на command detail):**

| Placement | Поле привязки | Правило app-resolver |
| --------- | ------------- | -------------------- |
| `command_detail` | `command_ids` | pick виден, если id команды ∈ `command_ids` |
| `device_guide_detail` | **`guides[].detail_referral_pick_ids`** | **канон:** упорядоченный список pick id для экрана guide detail |
| `device_guide_detail` (legacy) | `guide_ids` + `priority` | устарело; не гарантирует карточку «своего» типа |
| `smart_home_devices` | — | все picks с этим placement (вкладка «Устройства») |

**`detail_referral_pick_ids` (computed at publish):** для guide `G` — `[pick_G, pick_{related}…]` в порядке `related_device_ids`, только picks с placement `device_guide_detail`. Первый элемент — реферал **этого** типа устройства. См. `DeviceGuideReferralPicks.kt`.

**Сборка `guide_ids` в seed:** для `command_detail` и вкладки «Устройства»; для guide detail app читает **`detail_referral_pick_ids`**, не reverse-index по `guide_ids`.

**Compliance (optional):** `erid`, `advertiser_name`, `disclosure_ru` — не блокируют publish. См. [SECURITY.md](SECURITY.md) §8.

**URL policy:** `action_url` — только `https://` или `market://`; `image_url` — только `https://`.

### GET /devices/v1/{slug}.{webp|png|jpg}

Картинки guides/picks. Upload: `POST /admin/api/smarthome/upload-image` `{ "slug", "image_base64", "content_type"? }` — `image/webp`, `image/png`, `image/jpeg`. После upload пропишите возвращённый `image_url` в seed/guide и сделайте import.

| Env | Storage | Public URL |
| --- | ------- | ---------- |
| Local | `./storage/devices/v1/` | `{PUBLIC_BASE_URL}/devices/v1/{slug}.{ext}` |
| VPS | `/opt/alice-api/storage/devices/v1/` | `{PUBLIC_BASE_URL}/devices/v1/{slug}.{ext}` |

Ktor `staticFiles("/devices", …)` — fallback; nginx может проксировать на `:8080`/`:8081`.

---

## 4. Admin API

| Method | Path | Назначение |
| ------ | ---- | ---------- |
| GET | `/admin/api/smarthome/device-guides` | List guides |
| POST | `/admin/api/smarthome/device-guides` | Create |
| PUT | `/admin/api/smarthome/device-guides/{id}` | Update → auto-publish snapshot |
| DELETE | `/admin/api/smarthome/device-guides/{id}` | Delete → auto-publish |
| GET | `/admin/api/smarthome/device-picks` | List picks |
| POST | `/admin/api/smarthome/device-picks` | Create |
| PUT | `/admin/api/smarthome/device-picks/{id}` | Update |
| DELETE | `/admin/api/smarthome/device-picks/{id}` | Delete |
| POST | `/admin/api/smarthome/upload-image` | Upload image (webp/png/jpg) |

Admin UI: view **Устройства** — см. [ADMIN-UX.md](ADMIN-UX.md) §5e.

---

## 5. Database (V7–V10)

| Table | Migration | Назначение |
| ----- | --------- | ---------- |
| `device_guides` | V7 | Типы устройств |
| `device_picks` | V7 | Подборки; V10 — contextual columns |
| Seed picks from affiliate | V7 | Миграция products → picks |
| Product content seed | V8 | 8 picks + guide image_url |
| Contextual targeting | V10 | placements, tags, FK arrays, priority, scheduling |

См. [DATABASE.md](DATABASE.md).

---

## 6. Validation

```powershell
.\gradlew.bat :server:validateSmartHomeDevices
.\gradlew.bat :server:validateSmartHomeDevices -PcontentFile=seed/smarthome-devices-full.json
.\gradlew.bat :server:validateSmartHomeDevices -PcontentFile=seed/smarthome-devices-example.json
```

Publish gate: `SmartHomeDevicesValidationUseCase` + `JsonSmartHomeDevicesSchemaValidator`.

---

## 7. Ops scripts

| Script | Назначение |
| ------ | ---------- |
| `scripts/upload-device-images.ps1 -Target staging -ImagesDir <path>` | Upload PNG → `/devices/v1/{slug}.png` (map filename→guide id; затем обновить `image_url` в seed) |
| `scripts/import-smarthome-payload.ps1 -Target staging` | UTF-8 import guides/picks (default: `seed/smarthome-devices-full.json`; 2 pass для `related_device_ids`) |
| `scripts/republish-smarthome.ps1` | Re-trigger snapshot (не для восстановления контента) |
| `scripts/copy-staging-to-prod.ps1` | Копия bundle + smarthome staging → prod |

Пошагово: [ADMIN-CONTENT-GUIDE.md](ADMIN-CONTENT-GUIDE.md), [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) §14.

---

## 8. Config

| Env | Default | Назначение |
| --- | ------- | ---------- |
| `DEVICE_IMAGE_STORAGE_PATH` | `./storage/devices` | Каталог WebP (`v1/` subdir) |

Prod example: `deploy/.env.prod.example`.

---

## 9. Android integration

- Вкладка «Устройства» — guides + picks из snapshot.
- Contextual chips — resolver по `placements` + FK arrays + `priority`. UX: [APP-DEVICE-CHIPS-UX.md](APP-DEVICE-CHIPS-UX.md).

---

*См. [API.md](API.md), [CONTENT-PRODUCT-ROADMAP.md](CONTENT-PRODUCT-ROADMAP.md) § devices*
