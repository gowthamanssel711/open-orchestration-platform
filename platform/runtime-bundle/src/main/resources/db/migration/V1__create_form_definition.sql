-- Form definitions deployed via API
CREATE TABLE IF NOT EXISTS form_definition (
    id BIGSERIAL PRIMARY KEY,
    form_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(500),
    schema_json TEXT NOT NULL,
    deployed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deployed_by VARCHAR(255),
    UNIQUE (form_key, version)
);

CREATE INDEX IF NOT EXISTS idx_form_key ON form_definition(form_key);
CREATE INDEX IF NOT EXISTS idx_form_key_version ON form_definition(form_key, version DESC);
