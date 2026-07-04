-- Command of day editorial settings (singleton row id=1)
CREATE TABLE command_of_day_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    mode VARCHAR(16) NOT NULL CHECK (mode IN ('manual', 'auto')),
    command_id TEXT NOT NULL REFERENCES commands(id) ON DELETE RESTRICT,
    auto_category_id TEXT REFERENCES categories(id) ON DELETE RESTRICT,
    auto_seed INTEGER NOT NULL DEFAULT 31 CHECK (auto_seed >= 1),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by TEXT
);

-- Row is created lazily by ExposedDraftRepository.ensureCommandOfDaySettingsInternal()
-- after catalog import (resolver on today Moscow).
