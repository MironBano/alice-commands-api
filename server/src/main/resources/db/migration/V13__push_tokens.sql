CREATE TABLE IF NOT EXISTS push_tokens (
    install_id TEXT PRIMARY KEY,
    rustore_token TEXT NOT NULL,
    timezone TEXT NOT NULL DEFAULT 'Europe/Moscow',
    persona TEXT,
    content_version INTEGER NOT NULL DEFAULT 0,
    master_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cod_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    cod_reminder_time TEXT NOT NULL DEFAULT '09:00',
    checklist_completed_count INTEGER NOT NULL DEFAULT 0,
    frequent_commands_json TEXT NOT NULL DEFAULT '[]',
    app_version TEXT,
    last_s1_at TIMESTAMPTZ,
    last_s3_at TIMESTAMPTZ,
    last_s6_at TIMESTAMPTZ,
    s4_sent BOOLEAN NOT NULL DEFAULT FALSE,
    s5_sent BOOLEAN NOT NULL DEFAULT FALSE,
    last_popular_hash TEXT,
    last_notified_content_version INTEGER NOT NULL DEFAULT 0,
    last_push_at TIMESTAMPTZ,
    pushes_this_week INTEGER NOT NULL DEFAULT 0,
    week_bucket TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_push_tokens_master ON push_tokens (master_enabled) WHERE master_enabled = TRUE;
CREATE INDEX IF NOT EXISTS idx_push_tokens_cod ON push_tokens (cod_enabled) WHERE cod_enabled = TRUE;
