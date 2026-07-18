-- Audit Service: Immutable event log

CREATE TABLE audit_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(100),
    process_instance_id VARCHAR(255),
    process_definition_key VARCHAR(255),
    task_id VARCHAR(255),
    activity_id VARCHAR(255),
    user_id VARCHAR(255),
    payload JSONB,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_process_instance ON audit_event(process_instance_id, timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_event(event_type, timestamp DESC);
CREATE INDEX idx_audit_user ON audit_event(user_id, timestamp DESC);
CREATE INDEX idx_audit_timestamp ON audit_event(timestamp DESC);
CREATE INDEX idx_audit_payload ON audit_event USING gin(payload);
