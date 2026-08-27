-- Contextual DevicePick targeting (placements, tags, FK) for AliceCommands app resolver.

ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS erid TEXT;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS advertiser_name TEXT;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS disclosure_ru TEXT;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS cta_ru TEXT;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS device_types TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS category_ids TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS command_group_ids TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS command_ids TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS scenario_template_ids TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS guide_ids TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS placements TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS starts_at TIMESTAMPTZ;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS ends_at TIMESTAMPTZ;
ALTER TABLE device_picks ADD COLUMN IF NOT EXISTS max_impressions_per_session INTEGER;

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['starter_kit', 'speaker', 'station']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    guide_ids = ARRAY['station']::TEXT[],
    placements = ARRAY['smart_home_devices', 'device_guide_detail', 'catalog_persona', 'checklist_complete', 'search_empty']::TEXT[],
    priority = 100,
    updated_at = NOW()
WHERE id = 'pick_station_lite';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['smart_light']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    scenario_template_ids = ARRAY['S1', 'S4']::TEXT[],
    command_ids = ARRAY['sh_light_on']::TEXT[],
    command_group_ids = ARRAY['sh_group_light']::TEXT[],
    placements = ARRAY['smart_home_devices', 'scenario_detail', 'command_detail', 'search_empty']::TEXT[],
    priority = 90,
    updated_at = NOW()
WHERE id = 'pick_smart_bulb';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['smart_home']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    scenario_template_ids = ARRAY['S2']::TEXT[],
    command_ids = ARRAY['sh_socket_on']::TEXT[],
    command_group_ids = ARRAY['sh_group_socket']::TEXT[],
    placements = ARRAY['smart_home_devices', 'scenario_detail', 'command_detail', 'search_empty']::TEXT[],
    priority = 85,
    updated_at = NOW()
WHERE id = 'pick_smart_socket';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['starter_kit', 'smart_home']::TEXT[],
    device_types = ARRAY['station', 'tv', 'phone']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    placements = ARRAY['smart_home_devices', 'device_guide_detail', 'catalog_persona', 'search_empty']::TEXT[],
    priority = 80,
    updated_at = NOW()
WHERE id = 'pick_hub';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['security']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    scenario_template_ids = ARRAY['S5']::TEXT[],
    placements = ARRAY['smart_home_devices', 'scenario_detail', 'search_empty']::TEXT[],
    priority = 70,
    updated_at = NOW()
WHERE id = 'pick_motion_sensor';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['smart_home']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    placements = ARRAY['smart_home_devices', 'search_empty']::TEXT[],
    priority = 60,
    updated_at = NOW()
WHERE id = 'pick_thermostat';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['smart_home']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    scenario_template_ids = ARRAY['S6']::TEXT[],
    placements = ARRAY['smart_home_devices', 'scenario_detail', 'search_empty']::TEXT[],
    priority = 55,
    updated_at = NOW()
WHERE id = 'pick_curtain_motor';

UPDATE device_picks SET
    cta_ru = 'Смотреть цену',
    tags = ARRAY['starter_kit']::TEXT[],
    device_types = ARRAY['station']::TEXT[],
    category_ids = ARRAY['smart_home']::TEXT[],
    scenario_template_ids = ARRAY['S7']::TEXT[],
    placements = ARRAY['smart_home_devices', 'scenario_detail', 'checklist_complete']::TEXT[],
    priority = 50,
    updated_at = NOW()
WHERE id = 'pick_baby_monitor';
