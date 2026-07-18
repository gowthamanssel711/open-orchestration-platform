-- Analytics Service: Aggregated metrics and SLA tracking

CREATE TABLE process_metrics (
    id BIGSERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL,
    metric_date DATE NOT NULL,
    instances_started INTEGER DEFAULT 0,
    instances_completed INTEGER DEFAULT 0,
    instances_cancelled INTEGER DEFAULT 0,
    avg_duration_ms BIGINT,
    p50_duration_ms BIGINT,
    p95_duration_ms BIGINT,
    p99_duration_ms BIGINT,
    UNIQUE(process_definition_key, metric_date)
);

CREATE INDEX idx_pm_definition_date ON process_metrics(process_definition_key, metric_date DESC);

CREATE TABLE activity_metrics (
    id BIGSERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL,
    activity_id VARCHAR(255) NOT NULL,
    activity_name VARCHAR(500),
    metric_date DATE NOT NULL,
    executions INTEGER DEFAULT 0,
    avg_duration_ms BIGINT,
    p95_duration_ms BIGINT,
    UNIQUE(process_definition_key, activity_id, metric_date)
);

CREATE INDEX idx_am_definition_activity ON activity_metrics(process_definition_key, activity_id);

CREATE TABLE sla_definition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    process_definition_key VARCHAR(255) NOT NULL,
    activity_id VARCHAR(255),
    target_duration_ms BIGINT NOT NULL,
    alert_threshold_percent INTEGER DEFAULT 80,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE sla_breach (
    id BIGSERIAL PRIMARY KEY,
    sla_definition_id BIGINT REFERENCES sla_definition(id),
    process_instance_id VARCHAR(255) NOT NULL,
    activity_id VARCHAR(255),
    actual_duration_ms BIGINT NOT NULL,
    target_duration_ms BIGINT NOT NULL,
    breached_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_sla_breach_definition ON sla_breach(sla_definition_id, breached_at DESC);
CREATE INDEX idx_sla_breach_unresolved ON sla_breach(resolved) WHERE resolved = FALSE;
