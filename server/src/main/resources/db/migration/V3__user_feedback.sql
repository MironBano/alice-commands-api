-- User feedback and command issue reports from Android app

CREATE TABLE user_feedback (
    id TEXT PRIMARY KEY,
    message TEXT NOT NULL,
    rating INTEGER,
    app_version TEXT,
    platform TEXT,
    locale TEXT,
    content_version INTEGER,
    device_model TEXT,
    client_ip TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE command_reports (
    id TEXT PRIMARY KEY,
    command_id TEXT NOT NULL,
    issue_type TEXT NOT NULL,
    message TEXT,
    content_version INTEGER,
    category_id TEXT,
    command_title TEXT,
    phrase_used TEXT,
    app_version TEXT,
    platform TEXT,
    locale TEXT,
    command_exists_current BOOLEAN NOT NULL DEFAULT FALSE,
    client_ip TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE public_submission_attempts (
    ip_address TEXT NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_feedback_status ON user_feedback(status);
CREATE INDEX idx_user_feedback_created ON user_feedback(created_at DESC);
CREATE INDEX idx_command_reports_status ON command_reports(status);
CREATE INDEX idx_command_reports_command ON command_reports(command_id);
CREATE INDEX idx_command_reports_created ON command_reports(created_at DESC);
CREATE INDEX idx_public_submission_ip_time ON public_submission_attempts(ip_address, attempted_at);
