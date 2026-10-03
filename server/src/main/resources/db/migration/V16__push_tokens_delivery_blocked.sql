-- Quarantine RuStore tokens that return 404 (entity not found) so campaign ticks stop retrying.
ALTER TABLE push_tokens
    ADD COLUMN IF NOT EXISTS delivery_blocked_reason TEXT;
