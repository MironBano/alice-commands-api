CREATE TABLE analytics_events (
    event_id         TEXT PRIMARY KEY,
    install_id       TEXT NOT NULL,
    session_id       TEXT NOT NULL,
    event_name       TEXT NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    received_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    app_version      TEXT,
    android_version  TEXT,
    locale           TEXT,
    user_properties  JSONB NOT NULL DEFAULT '{}',
    params           JSONB NOT NULL DEFAULT '{}',
    client_ip        TEXT
);

CREATE INDEX idx_analytics_events_occurred_at ON analytics_events (occurred_at DESC);
CREATE INDEX idx_analytics_events_event_name_occurred ON analytics_events (event_name, occurred_at DESC);
CREATE INDEX idx_analytics_events_install_id ON analytics_events (install_id);
CREATE INDEX idx_analytics_events_session_id ON analytics_events (session_id);
CREATE INDEX idx_analytics_events_client_ip_received ON analytics_events (client_ip, received_at DESC);

CREATE TABLE analytics_request_attempts (
    ip_address   TEXT NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_analytics_request_attempts_ip_time ON analytics_request_attempts (ip_address, attempted_at DESC);
