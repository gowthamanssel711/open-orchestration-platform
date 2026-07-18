-- Query Service: Denormalized read models for fast lookups

CREATE TABLE process_instance (
    id BIGSERIAL PRIMARY KEY,
    process_instance_id VARCHAR(255) UNIQUE NOT NULL,
    process_definition_key VARCHAR(255),
    process_definition_id VARCHAR(255),
    business_key VARCHAR(255),
    initiator VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_pi_status ON process_instance(status);
CREATE INDEX idx_pi_definition_key ON process_instance(process_definition_key);
CREATE INDEX idx_pi_initiator ON process_instance(initiator);
CREATE INDEX idx_pi_started_at ON process_instance(started_at DESC);

CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(500),
    description TEXT,
    process_instance_id VARCHAR(255),
    process_definition_key VARCHAR(255),
    assignee VARCHAR(255),
    candidate_groups VARCHAR(1000),
    status VARCHAR(50) NOT NULL,
    priority INTEGER DEFAULT 0,
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_task_assignee ON task(assignee);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_assignee_status ON task(assignee, status);
CREATE INDEX idx_task_process_instance ON task(process_instance_id);
CREATE INDEX idx_task_created_at ON task(created_at DESC);
