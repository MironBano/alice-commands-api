-- Initial schema for alice-commands-api draft + publish state

CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    title_ru TEXT NOT NULL,
    title_kk TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    icon_key TEXT,
    description_ru TEXT,
    source_url TEXT NOT NULL,
    device_types TEXT[] NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE commands (
    id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    title_ru TEXT NOT NULL,
    phrases JSONB NOT NULL DEFAULT '[]',
    effect_description_ru TEXT NOT NULL DEFAULT '',
    requires_alice_word BOOLEAN NOT NULL DEFAULT TRUE,
    requires_plus BOOLEAN NOT NULL DEFAULT FALSE,
    device_types TEXT[] NOT NULL DEFAULT '{}',
    related_command_ids TEXT[] NOT NULL DEFAULT '{}',
    source_url TEXT NOT NULL,
    published_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tags TEXT[] NOT NULL DEFAULT '{}'
);

CREATE TABLE scenario_templates (
    id TEXT PRIMARY KEY,
    title_ru TEXT NOT NULL,
    trigger_ru TEXT,
    actions_ru JSONB NOT NULL DEFAULT '[]',
    example_phrases JSONB NOT NULL DEFAULT '[]',
    audience TEXT,
    deep_link_hint TEXT,
    source_url TEXT NOT NULL
);

CREATE TABLE checklist_items (
    id TEXT PRIMARY KEY,
    item_order INT NOT NULL,
    command_id TEXT NOT NULL REFERENCES commands(id) ON DELETE CASCADE,
    hint_ru TEXT
);

CREATE TABLE affiliate_blocks (
    id TEXT PRIMARY KEY,
    context_category_id TEXT,
    title_ru TEXT NOT NULL,
    erid TEXT,
    advertiser_name TEXT,
    products JSONB NOT NULL DEFAULT '[]',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE current_manifest (
    content_version INT PRIMARY KEY,
    bundle_path TEXT NOT NULL,
    bundle_sha256 TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    min_app_version TEXT NOT NULL DEFAULT '1.0',
    schema_version INT NOT NULL DEFAULT 1,
    bundle_size_bytes BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE publish_history (
    id BIGSERIAL PRIMARY KEY,
    content_version INT NOT NULL,
    bundle_sha256 TEXT NOT NULL,
    admin_username TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT
);

CREATE TABLE admin_sessions (
    id TEXT PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE login_attempts (
    ip_address TEXT NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_commands_category ON commands(category_id);
CREATE INDEX idx_commands_tags ON commands USING GIN(tags);
CREATE INDEX idx_categories_sort ON categories(sort_order);
CREATE INDEX idx_login_attempts_ip ON login_attempts(ip_address, attempted_at);
CREATE INDEX idx_checklist_order ON checklist_items(item_order);
