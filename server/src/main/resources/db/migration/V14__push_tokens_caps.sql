-- Caps + install age for retention campaigns (S4/S5, ≤1/day, ≤3/week).
ALTER TABLE push_tokens
    ADD COLUMN IF NOT EXISTS last_push_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pushes_this_week INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS week_bucket TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE push_tokens
SET created_at = COALESCE(created_at, updated_at, NOW())
WHERE created_at IS NULL;
