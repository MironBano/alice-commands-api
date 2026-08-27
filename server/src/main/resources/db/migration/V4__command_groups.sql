-- Command groups schema v2

CREATE TABLE command_groups (
    id TEXT PRIMARY KEY,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    title_ru TEXT NOT NULL,
    description_ru TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    icon_key TEXT,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    preview_command_ids TEXT[] NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE commands ADD COLUMN group_id TEXT REFERENCES command_groups(id) ON DELETE SET NULL;
ALTER TABLE commands ADD COLUMN sort_order INT;
ALTER TABLE commands ADD COLUMN variant_label_ru TEXT;
ALTER TABLE commands ADD COLUMN is_primary_in_group BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE commands ADD COLUMN search_aliases TEXT[] NOT NULL DEFAULT '{}';

CREATE INDEX idx_command_groups_category_sort ON command_groups(category_id, sort_order);
CREATE INDEX idx_commands_group_sort ON commands(group_id, sort_order);
