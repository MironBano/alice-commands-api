-- In-app announcement banners (admin-managed, side-channel endpoint)

CREATE TABLE announcements (
    id                  TEXT PRIMARY KEY,
    revision            INT NOT NULL DEFAULT 1,
    placement           TEXT NOT NULL DEFAULT 'more',
    title               TEXT NOT NULL,
    body                TEXT,
    image_url           TEXT,
    background_color    TEXT NOT NULL DEFAULT '#E3F2FD',
    foreground_color    TEXT,
    cta_label           TEXT,
    cta_action          TEXT,
    cta_target          TEXT,
    dismissible         BOOLEAN NOT NULL DEFAULT TRUE,
    priority            INT NOT NULL DEFAULT 0,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    min_app_version     TEXT,
    max_app_version     TEXT,
    starts_at           TIMESTAMPTZ,
    ends_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_announcements_enabled_priority ON announcements (enabled, priority DESC, updated_at DESC);
