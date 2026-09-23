-- App install anchor for S4/S5 retention windows (not push opt-in date).
ALTER TABLE push_tokens
    ADD COLUMN IF NOT EXISTS app_installed_at TIMESTAMPTZ;

UPDATE push_tokens
SET app_installed_at = COALESCE(app_installed_at, created_at)
WHERE app_installed_at IS NULL;
