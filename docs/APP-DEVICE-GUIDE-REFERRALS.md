# Android — рефералы на экране типа устройства (guide detail)

**Статус:** ТЗ для app · **Backend:** `guides[].detail_referral_pick_ids` (computed at publish)  
**idea_ref:** MOB-20260626-001

## Проблема

На guide detail блок «Что купить» показывал 2 **чужих** pick (top по `priority` из `guide_ids`), без карточки **этого** типа (например стиральная машина → датчик протечки + кран, без `pick_washing_machine`).

## Решение (канон)

Читать **`guide.detail_referral_pick_ids`** — упорядоченный список id picks:

1. **`pick_{guide.id}`** — реферал этого типа (если pick есть в snapshot)
2. **`pick_{related}`** для каждого `guide.related_device_ids`, у pick есть placement `device_guide_detail`

Пример `washing_machine`:

```json
"detail_referral_pick_ids": [
  "pick_washing_machine",
  "pick_sensor_water_leak",
  "pick_sensor_vibration",
  "pick_valve"
]
```

UI: показать **первые N** (рекомендуем **2**): своё устройство + первый related.

## Resolver (псевдокод)

```kotlin
fun picksForGuideDetail(guide: DeviceGuide, picksById: Map<String, DevicePick>, limit: Int = 2): List<DevicePick> =
    guide.detail_referral_pick_ids
        .mapNotNull { picksById[it] }
        .take(limit)
```

**Не использовать** для guide detail: сортировку всех picks по `priority` / фильтр только по `guide_ids`.

## Fallback (старый bundle)

Если `detail_referral_pick_ids` пуст:

1. `picksById["pick_${guide.id}"]` первым
2. затем `pick_{id}` для `related_device_ids` с `device_guide_detail`

## DoD

- [ ] Стиральная машина: карточка «Стиральная машина» + один related
- [ ] Датчик протечки: датчик + хаб/кран (не колонка/свет)
- [ ] Unit-тест resolver на `detail_referral_pick_ids`

## Связанные документы

- [BACKEND-SMARTHOME-DEVICES.md](BACKEND-SMARTHOME-DEVICES.md) § targeting
- [API.md](API.md) § `/v1/smarthome/devices`
