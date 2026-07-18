-- V2: Enhanced RBAC - tenant members, role assignments, expanded permissions

-- Tenant membership (which users belong to which tenant)
CREATE TABLE tenant_member (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL REFERENCES tenant(tenant_id),
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT 'user',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, user_id)
);

CREATE INDEX idx_tenant_member_user ON tenant_member(user_id);
CREATE INDEX idx_tenant_member_tenant ON tenant_member(tenant_id);

-- API key prefix for lookup (first 8 chars stored in plain)
ALTER TABLE api_key ADD COLUMN key_prefix VARCHAR(20);
CREATE INDEX idx_api_key_prefix ON api_key(key_prefix);

-- Expand permission model with resource_id for fine-grained control
ALTER TABLE permission ADD COLUMN resource_id VARCHAR(255) DEFAULT '*';

-- Seed additional permissions for the expanded role set
INSERT INTO permission (role, resource, action) VALUES
    -- platform-admin (inherits from admin wildcard)
    -- process-admin
    ('process-admin', 'process-definition', '*'),
    ('process-admin', 'process-instance', '*'),
    ('process-admin', 'task', '*'),
    ('process-admin', 'connector', '*'),
    ('process-admin', 'analytics', 'read'),
    -- analyst
    ('analyst', 'process-definition', 'read'),
    ('analyst', 'process-instance', 'read'),
    ('analyst', 'analytics', 'read'),
    ('analyst', 'analytics', 'export'),
    -- viewer
    ('viewer', 'process-definition', 'read'),
    ('viewer', 'process-instance', 'read'),
    ('viewer', 'connector', 'read'),
    ('viewer', 'analytics', 'read')
ON CONFLICT DO NOTHING;

-- Default tenant
INSERT INTO tenant (tenant_id, name) VALUES ('default', 'Default Tenant')
ON CONFLICT DO NOTHING;
