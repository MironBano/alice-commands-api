-- Smart home devices: guides (device types) + picks (product recommendations)

CREATE TABLE device_guides (
    id TEXT PRIMARY KEY,
    title_ru TEXT NOT NULL,
    summary_ru TEXT NOT NULL,
    capabilities_ru TEXT NOT NULL,
    setup_ru TEXT NOT NULL,
    setup_steps_ru TEXT[] NOT NULL DEFAULT '{}',
    related_devices_ru TEXT,
    related_device_ids TEXT[] NOT NULL DEFAULT '{}',
    command_device_filter_id TEXT,
    image_url TEXT,
    action_url TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE device_picks (
    id TEXT PRIMARY KEY,
    title_ru TEXT NOT NULL,
    description_ru TEXT,
    price_hint_ru TEXT,
    image_url TEXT,
    action_url TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Migrate affiliate block products → picks (one pick per product)
INSERT INTO device_picks (id, title_ru, description_ru, price_hint_ru, image_url, action_url, sort_order, updated_at)
SELECT
    'pick_' || regexp_replace(ab.id, '[^a-zA-Z0-9_]', '_', 'g') || '_' || p.ordinality,
    COALESCE(NULLIF(trim(p.value->>'title_ru'), ''), ab.title_ru),
    ab.title_ru,
    NULLIF(trim(p.value->>'price_hint'), ''),
    NULL,
    COALESCE(NULLIF(trim(p.value->>'market_url'), ''), 'https://market.yandex.ru/'),
    (row_number() OVER (ORDER BY ab.updated_at, ab.id, p.ordinality)) * 10,
    ab.updated_at
FROM affiliate_blocks ab
CROSS JOIN LATERAL jsonb_array_elements(ab.products) WITH ORDINALITY AS p(value, ordinality)
WHERE jsonb_array_length(ab.products) > 0
ON CONFLICT (id) DO NOTHING;

-- Pilot guides: station, tv, phone
INSERT INTO device_guides (
    id, title_ru, summary_ru, capabilities_ru, setup_ru, setup_steps_ru,
    related_devices_ru, related_device_ids, command_device_filter_id,
    image_url, action_url, sort_order
) VALUES
(
    'station',
    'Колонка с Алисой',
    'Голосовой помощник и центр умного дома в одной колонке',
    'Алиса отвечает на вопросы, включает музыку и подкасты, управляет умным домом голосом. На колонках с экраном показывает погоду, таймеры и видео. Поддерживает мультирум и сценарии «Умного дома».',
    'Подключите колонку к розетке и Wi‑Fi, откройте приложение «Дом с Алисой» и следуйте мастеру настройки. Войдите в аккаунт Яндекса — после этого Алиса готова к работе.',
    ARRAY[
        'Включите колонку и дождитесь приветствия',
        'Подключите к Wi‑Fi через приложение «Дом с Алисой»',
        'Войдите в аккаунт Яндекса',
        'Скажите: «Алиса, найди устройства» для привязки умного дома'
    ],
    'Смартфон с приложением «Дом с Алисой» для первичной настройки',
    ARRAY['phone'],
    'station',
    NULL,
    'https://alice.yandex.ru/support/ru/station/',
    10
),
(
    'tv',
    'Умный телевизор с Алисой',
    'Голос с пульта или колонки, умный дом на большом экране',
    'Управляйте контентом и умным домом с экрана ТВ: голосовой поиск, запуск приложений, сценарии освещения и климата. Алиса на ТВ работает вместе с колонкой или через пульт с микрофоном.',
    'Подключите телевизор к Wi‑Fi, войдите в аккаунт Яндекса в настройках ТВ или через приложение. Убедитесь, что микрофон на пульте включён или рядом есть колонка с Алисой.',
    ARRAY[
        'Включите ТВ и подключите к Wi‑Fi',
        'Войдите в аккаунт Яндекса на телевизоре',
        'Проверьте микрофон на пульте или привяжите колонку',
        'Скажите: «Алиса, найди устройства» для умного дома'
    ],
    'Колонка с Алисой или пульт с микрофоном',
    ARRAY['station'],
    'tv',
    NULL,
    'https://alice.yandex.ru/support/ru/tv/',
    20
),
(
    'phone',
    'Алиса на смартфоне',
    'Голосовой помощник в кармане и пульт для умного дома',
    'Запускайте Алису кнопкой или фразой, управляйте устройствами умного дома, ставьте напоминания и таймеры. Удобно как мобильный пульт, когда вы не дома.',
    'Установите приложение «Яндекс» или «Дом с Алисой», войдите в аккаунт Яндекса и разрешите доступ к микрофону. В настройках умного дома привяжите устройства к тому же аккаунту.',
    ARRAY[
        'Установите приложение «Яндекс» или «Дом с Алисой»',
        'Войдите в аккаунт Яндекса',
        'Разрешите доступ к микрофону',
        'Откройте раздел «Умный дом» и привяжите устройства'
    ],
    'Колонка или ТВ с тем же аккаунтом Яндекса',
    ARRAY['station', 'tv'],
    'phone',
    NULL,
    'https://alice.yandex.ru/support/ru/alice-on-phone/',
    30
)
ON CONFLICT (id) DO NOTHING;
