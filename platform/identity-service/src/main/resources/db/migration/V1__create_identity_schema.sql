-- Identity Service: RBAC and tenant management

CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE permission (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(100) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100),
    UNIQUE(role, resource, action, tenant_id)
);

CREATE INDEX idx_perm_role ON permission(role);
CREATE INDEX idx_perm_tenant ON permission(tenant_id);

CREATE TABLE api_key (
    id BIGSERIAL PRIMARY KEY,
    key_hash VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(100),
    roles TEXT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Seed default permissions
INSERT INTO permission (role, resource, action) VALUES
    ('admin', '*', '*'),
    ('operator', 'process-instance', 'read'),
    ('operator', 'process-instance', 'cancel'),
    ('operator', 'process-instance', 'retry'),
    ('operator', 'task', 'read'),
    ('operator', 'task', 'assign'),
    ('operator', 'incident', 'read'),
    ('operator', 'incident', 'resolve'),
    ('developer', 'process-definition', 'read'),
    ('developer', 'process-definition', 'deploy'),
    ('developer', 'process-definition', 'delete'),
    ('developer', 'modeler', '*'),
    ('user', 'task', 'read'),
    ('user', 'task', 'claim'),
    ('user', 'task', 'complete');
