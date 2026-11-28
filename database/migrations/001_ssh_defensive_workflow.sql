BEGIN;
SET LOCAL search_path TO public, pg_catalog;

CREATE TABLE IF NOT EXISTS ssh_lab_sessions (
    id UUID PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id),
    assessment_id BIGINT NOT NULL REFERENCES assessments(id),
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    service_id BIGINT NOT NULL REFERENCES services(id),
    desired_running BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(12) NOT NULL CHECK (status IN ('STARTING','RUNNING','STOPPED','ERROR')),
    health JSONB,
    heartbeat_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ssh_lab_active
    ON ssh_lab_sessions ((true)) WHERE status <> 'STOPPED';

ALTER TABLE events ADD COLUMN IF NOT EXISTS ssh_lab_session_id UUID REFERENCES ssh_lab_sessions(id);
ALTER TABLE events ADD COLUMN IF NOT EXISTS collector_record_key VARCHAR(160);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ssh_collector_record
    ON events (ssh_lab_session_id, collector_record_key) WHERE ssh_lab_session_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ssh_failures
    ON events (exercise_id, service_id, ((data->>'sourceIp')), ((data->>'username')), event_timestamp, id)
    WHERE ssh_lab_session_id IS NOT NULL AND event_type = 'SSH_AUTH_FAILURE';

CREATE TABLE IF NOT EXISTS security_alerts (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id),
    service_id BIGINT NOT NULL REFERENCES services(id),
    lab_session_id UUID NOT NULL REFERENCES ssh_lab_sessions(id),
    source_ip VARCHAR(45) NOT NULL,
    account VARCHAR(100) NOT NULL,
    first_event_at TIMESTAMPTZ NOT NULL,
    last_event_at TIMESTAMPTZ NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','ACKNOWLEDGED','IN_PROGRESS','CLOSED')),
    assessment_id BIGINT REFERENCES assessments(id),
    report_id BIGINT REFERENCES incident_reports(id),
    CHECK (first_event_at <= last_event_at AND last_event_at <= detected_at)
);
CREATE INDEX IF NOT EXISTS idx_security_alerts_exercise_status ON security_alerts (exercise_id, status, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_security_alerts_detection_group
    ON security_alerts (lab_session_id, service_id, source_ip, account, first_event_at, last_event_at);
CREATE TABLE IF NOT EXISTS security_alert_events (
    alert_id BIGINT NOT NULL REFERENCES security_alerts(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    PRIMARY KEY (alert_id, event_id)
);
CREATE TABLE IF NOT EXISTS ssh_response_actions (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES security_alerts(id),
    actor_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(32) NOT NULL,
    detail TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ssh_response_alert ON ssh_response_actions (alert_id, id);

ALTER TABLE evidence ADD COLUMN IF NOT EXISTS ssh_assessment_id BIGINT REFERENCES assessments(id);

COMMIT;
