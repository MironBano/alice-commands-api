ALTER TABLE push_tokens
    ADD COLUMN IF NOT EXISTS provider TEXT NOT NULL DEFAULT 'rustore';

CREATE INDEX IF NOT EXISTS idx_push_tokens_provider ON push_tokens (provider);
