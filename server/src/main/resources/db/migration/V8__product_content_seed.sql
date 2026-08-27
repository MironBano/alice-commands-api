-- Product content seed: device picks (smart home starter) + guide image_url placeholders.
-- Images: upload via admin POST /admin/api/smarthome/upload-image → /devices/v1/{slug}.webp

UPDATE device_guides SET
    image_url = 'https://cdn.alicecommands.ru/devices/v1/station.webp'
WHERE id = 'station' AND image_url IS NULL;

UPDATE device_guides SET
    image_url = 'https://cdn.alicecommands.ru/devices/v1/tv.webp'
WHERE id = 'tv' AND image_url IS NULL;

UPDATE device_guides SET
    image_url = 'https://cdn.alicecommands.ru/devices/v1/phone.webp'
WHERE id = 'phone' AND image_url IS NULL;

INSERT INTO device_picks (id, title_ru, description_ru, price_hint_ru, image_url, action_url, sort_order, updated_at)
VALUES
(
    'pick_smart_bulb',
    'Умная лампа',
    'Старт умного дома: голосом включить и выключить свет',
    'от 690 ₽',
    NULL,
    'https://market.yandex.ru/search?text=умная%20лампа%20яндекс',
    10,
    NOW()
),
(
    'pick_smart_socket',
    'Умная розетка',
    'Управление обычными приборами через Алису',
    'от 990 ₽',
    NULL,
    'https://market.yandex.ru/search?text=умная%20розетка',
    20,
    NOW()
),
(
    'pick_hub',
    'Хаб умного дома',
    'Связывает лампы и датчики в одну систему',
    'от 3 490 ₽',
    NULL,
    'https://market.yandex.ru/search?text=хаб%20умный%20дом%20яндекс',
    30,
    NOW()
),
(
    'pick_station_lite',
    'Колонка с Алисой',
    'Голосовой центр дома и пульт для умного дома',
    'от 4 990 ₽',
    NULL,
    'https://market.yandex.ru/search?text=колонка%20яндекс%20алиса',
    40,
    NOW()
),
(
    'pick_motion_sensor',
    'Датчик движения',
    'Автоматизация света при входе в комнату',
    'от 1 290 ₽',
    NULL,
    'https://market.yandex.ru/search?text=датчик%20движения%20умный%20дом',
    50,
    NOW()
),
(
    'pick_thermostat',
    'Терморегулятор',
    'Климат в доме голосом и по расписанию',
    'от 2 990 ₽',
    NULL,
    'https://market.yandex.ru/search?text=терморегулятор%20умный%20дом',
    60,
    NOW()
),
(
    'pick_curtain_motor',
    'Мотор для штор',
    'Открывать и закрывать шторы голосом',
    'от 4 500 ₽',
    NULL,
    'https://market.yandex.ru/search?text=умные%20шторы%20мотор',
    70,
    NOW()
),
(
    'pick_baby_monitor',
    'Радионяня',
    'Слушать ребёнка через колонку с Алисой',
    'от 2 490 ₽',
    NULL,
    'https://market.yandex.ru/search?text=радионяня%20яндекс',
    80,
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    title_ru = EXCLUDED.title_ru,
    description_ru = EXCLUDED.description_ru,
    price_hint_ru = EXCLUDED.price_hint_ru,
    action_url = EXCLUDED.action_url,
    sort_order = EXCLUDED.sort_order,
    updated_at = EXCLUDED.updated_at;
