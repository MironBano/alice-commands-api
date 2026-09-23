-- Popular commands: pins, denylist, live snapshot, rank history

CREATE TABLE popular_command_pins (
    command_id   TEXT PRIMARY KEY,
    sort_order   INT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   TEXT
);

CREATE TABLE popular_command_denylist (
    command_id   TEXT PRIMARY KEY,
    reason       TEXT NOT NULL DEFAULT ''
);

INSERT INTO popular_command_denylist (command_id, reason) VALUES
    ('music_vkliuchi_muzyku', 'checklist/COD bias'),
    ('general_gromche', 'checklist/COD bias'),
    ('quick_commands_dalshe', 'checklist/COD bias'),
    ('timers_postav_taimer_na_5_minut', 'checklist/COD bias')
ON CONFLICT (command_id) DO NOTHING;

CREATE TABLE popular_commands_snapshot (
    sort_order   INT PRIMARY KEY,
    command_id   TEXT NOT NULL,
    source       TEXT NOT NULL,
    unique_tts   INT NOT NULL DEFAULT 0,
    unique_view  INT NOT NULL DEFAULT 0,
    score        INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE popular_rank_runs (
    id           BIGSERIAL PRIMARY KEY,
    computed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    window_from  DATE NOT NULL,
    window_to    DATE NOT NULL,
    window_days  INT NOT NULL,
    trigger      TEXT NOT NULL,
    computed_by  TEXT
);

CREATE INDEX idx_popular_rank_runs_computed_at ON popular_rank_runs (computed_at DESC);

CREATE TABLE popular_rank_run_items (
    run_id          BIGINT NOT NULL REFERENCES popular_rank_runs (id) ON DELETE CASCADE,
    sort_order      INT NOT NULL,
    command_id      TEXT NOT NULL,
    source          TEXT NOT NULL,
    unique_tts      INT NOT NULL DEFAULT 0,
    unique_view     INT NOT NULL DEFAULT 0,
    score           INT NOT NULL DEFAULT 0,
    in_served_pool  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (run_id, sort_order)
);

CREATE INDEX idx_popular_rank_run_items_run ON popular_rank_run_items (run_id);

CREATE INDEX idx_analytics_events_command_id_popular
    ON analytics_events ((params->>'command_id'))
    WHERE event_name IN ('command_tts', 'command_view');
