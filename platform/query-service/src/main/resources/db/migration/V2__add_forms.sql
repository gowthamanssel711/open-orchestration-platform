-- Add form_key column to task table
ALTER TABLE task ADD COLUMN form_key VARCHAR(255);
CREATE INDEX idx_task_form_key ON task(form_key);

-- Form definitions table
CREATE TABLE form_definition (
    id BIGSERIAL PRIMARY KEY,
    form_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(500),
    schema_json TEXT NOT NULL,
    deployed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (form_key, version)
);

CREATE INDEX idx_form_key ON form_definition(form_key);
CREATE INDEX idx_form_key_version ON form_definition(form_key, version DESC);
