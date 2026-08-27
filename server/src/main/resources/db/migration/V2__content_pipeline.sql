-- Content pipeline: inventory (machine) + editorial (human) + work queue

CREATE TABLE inventory_items (
    command_id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL,
    phrases JSONB NOT NULL DEFAULT '[]',
    raw_result TEXT,
    source_url TEXT NOT NULL,
    section TEXT,
    requires_alice_word BOOLEAN NOT NULL DEFAULT TRUE,
    requires_plus BOOLEAN NOT NULL DEFAULT FALSE,
    device_types TEXT[] NOT NULL DEFAULT '{}',
    source_id TEXT,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deprecated BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE editorial_records (
    command_id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL,
    title_ru TEXT NOT NULL,
    effect_description_ru TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    approved_at TIMESTAMPTZ,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE content_queue (
    id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,
    command_id TEXT NOT NULL,
    phrase TEXT,
    category_id TEXT,
    title_ru TEXT,
    suggested_effect TEXT,
    raw_result TEXT,
    source_url TEXT,
    status TEXT NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_inventory_category ON inventory_items(category_id);
CREATE INDEX idx_editorial_status ON editorial_records(status);
CREATE INDEX idx_content_queue_status ON content_queue(status);
CREATE INDEX idx_content_queue_command ON content_queue(command_id);
