# 07 — Security Model

## Authentication Architecture

### Identity Provider: Keycloak

```
┌──────────────┐      ┌───────────────┐      ┌─────────────────┐
│   Browser    │─────▶│   Keycloak    │◀─────│ External IdP    │
│   (OIDC)    │◀─────│   (OIDC/OAuth)│      │ (LDAP/SAML/SSO) │
└──────┬───────┘      └───────────────┘      └─────────────────┘
       │ JWT
       ▼
┌──────────────┐      ┌───────────────┐
│ API Gateway  │─────▶│ Backend       │
│ (validates)  │      │ Services      │
└──────────────┘      └───────────────┘
```

### Authentication Methods

| Method | Use Case | Implementation |
|--------|----------|----------------|
| **OIDC Login** | Browser users (UI apps) | Keycloak redirect flow |
| **JWT Bearer** | API clients (after login) | `Authorization: Bearer <token>` |
| **API Keys** | Service-to-service, CI/CD | `X-API-Key: oop_live_sk_...` |
| **Client Credentials** | Machine-to-machine (OAuth2) | Client ID + Secret |
| **mTLS** | Inter-service (internal) | Mutual TLS certificates |

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "email": "john@company.com",
  "preferred_username": "john.doe",
  "realm_access": {
    "roles": ["platform-admin", "process-developer"]
  },
  "resource_access": {
    "operate-ui": { "roles": ["viewer", "operator"] },
    "tasklist-ui": { "roles": ["task-user"] }
  },
  "tenant_id": "tenant-abc",
  "groups": ["/managers", "/engineering"],
  "iat": 1721042400,
  "exp": 1721046000
}
```

---

## Authorization Model: RBAC

### Permission Structure

```
Tenant
  └── Role
        └── Permission
              ├── Resource Type (process, instance, task, connector, admin)
              ├── Resource ID (* for all, or specific key)
              └── Actions (create, read, update, delete, execute)
```

### Default Roles

| Role | Scope | Permissions |
|------|-------|-------------|
| **platform-admin** | All | Full access to everything |
| **process-admin** | Processes | Deploy, delete, manage all process definitions |
| **operator** | Instances | View all instances, resolve incidents, cancel/retry |
| **process-developer** | Own processes | Deploy, test, view own process instances |
| **task-user** | Assigned tasks | View/complete tasks assigned to user or groups |
| **analyst** | Analytics | View analytics, create dashboards, export reports |
| **viewer** | Read-only | View processes, instances (no actions) |

### Permission Matrix

```
                     process  instance  task    connector  analytics  admin
                     C R U D  C R U D   R U D   C R U D    R         C R U D
─────────────────────────────────────────────────────────────────────────────
platform-admin       ✓ ✓ ✓ ✓  ✓ ✓ ✓ ✓  ✓ ✓ ✓  ✓ ✓ ✓ ✓   ✓         ✓ ✓ ✓ ✓
process-admin        ✓ ✓ ✓ ✓  ✓ ✓ ✓ ✓  ✓ ✓ ✓  ✓ ✓ ✓ ✓   ✓         · · · ·
operator             · ✓ · ·  · ✓ ✓ ✓  ✓ ✓ ·  · ✓ · ·   ✓         · · · ·
process-developer    ✓ ✓ ✓ ✓  ✓ ✓ · ·  ✓ · ·  ✓ ✓ ✓ ✓   ✓         · · · ·
task-user            · · · ·  · · · ·  ✓ ✓ ·  · · · ·   ·         · · · ·
analyst              · ✓ · ·  · ✓ · ·  · · ·  · ✓ · ·   ✓         · · · ·
viewer               · ✓ · ·  · ✓ · ·  · · ·  · ✓ · ·   ✓         · · · ·
```

### Resource-Level Permissions

Beyond role-based, fine-grained permissions on specific resources:

```json
{
  "role": "process-developer",
  "permissions": [
    {
      "resourceType": "process-definition",
      "resourceId": "order-process",
      "actions": ["deploy", "start-instance", "view"]
    },
    {
      "resourceType": "process-definition",
      "resourceId": "payment-*",
      "actions": ["view"]
    }
  ]
}
```

---

## Multi-Tenancy Security

### Tenant Isolation Levels

```
Level 1: Row-Level Security (shared DB)
┌─────────────────────────────────────────────┐
│ PostgreSQL                                   │
│                                             │
│ SELECT * FROM process_instances              │
│ WHERE tenant_id = current_setting('app.tid')│
│                                             │
│ CREATE POLICY tenant_isolation ON instances  │
│   USING (tenant_id = current_setting(       │
│     'app.tenant_id'))                       │
└─────────────────────────────────────────────┘

Level 2: Schema-per-Tenant (shared DB)
┌─────────────────────────────────────────────┐
│ PostgreSQL                                   │
│ ├── schema: tenant_abc                       │
│ │   └── process_instances, tasks, ...        │
│ ├── schema: tenant_xyz                       │
│ │   └── process_instances, tasks, ...        │
│ └── schema: shared                           │
│     └── tenant_config, billing, ...          │
└─────────────────────────────────────────────┘

Level 3: Database-per-Tenant
┌────────────────┐  ┌────────────────┐
│ DB: tenant_abc │  │ DB: tenant_xyz │
│ Full isolation │  │ Full isolation │
└────────────────┘  └────────────────┘
```

### Tenant Context Propagation

```
Request → Gateway extracts tenant_id from JWT
       → Sets X-Tenant-Id header
       → All downstream services use tenant context
       → Database queries scoped to tenant
       → Kafka messages tagged with tenant_id
```

---

## API Key Security

### Key Format

```
oop_live_sk_a3b8c9d2e1f4...   # Production secret key
oop_test_sk_x7y8z9a1b2c3...   # Test/staging secret key
oop_live_pk_m4n5o6p7q8r9...   # Production public key (limited)
```

### Key Properties

| Property | Description |
|----------|-------------|
| Prefix | `oop_live_sk_` (environment + type) |
| Length | 48 characters (256-bit entropy) |
| Storage | SHA-256 hashed (only prefix stored in plain) |
| Rotation | 90-day recommended, configurable |
| Scopes | Assignable permissions (same RBAC model) |
| Rate limit | Per-key rate limiting |
| IP whitelist | Optional IP restriction |
| Expiry | Optional expiration date |

---

## Network Security

### TLS Configuration

| Communication | TLS Version | Certificate |
|---------------|-------------|-------------|
| Client → Gateway | TLS 1.3 | Public CA (Let's Encrypt) |
| Gateway → Services | TLS 1.3 | Internal CA (cert-manager) |
| Service → Service | mTLS | Service mesh certificates |
| Service → Kafka | TLS 1.3 | Internal CA |
| Service → PostgreSQL | TLS 1.3 | Internal CA |

### Network Policies (Kubernetes)

```yaml
# Only gateway accepts external traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-external-to-services
spec:
  podSelector:
    matchLabels:
      tier: backend
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: gateway
```

### Allowed Communication Matrix

```
External → Gateway         ✓ (only entry point)
External → Any Service     ✗ (blocked)
Gateway → All Services     ✓
Service → Kafka            ✓
Service → PostgreSQL       ✓
Service → Service          ✓ (via service mesh)
Service → External         ✓ (via connector service only)
```

---

## Data Security

### Encryption

| Data State | Method |
|-----------|--------|
| In transit | TLS 1.3 (all connections) |
| At rest (DB) | PostgreSQL TDE or volume encryption |
| At rest (Kafka) | Kafka TLS + volume encryption |
| At rest (backups) | AES-256 encryption before upload |
| Process variables | Application-level encryption (optional) |
| Secrets/credentials | HashiCorp Vault / K8s Secrets (encrypted etcd) |

### Sensitive Data Handling

```java
// Variables marked as sensitive are encrypted before storage
@ProcessVariable(sensitive = true)
private String creditCardNumber;

// Masked in logs and API responses
// Stored encrypted in database
// Not included in Kafka events (or encrypted)
```

### PII Compliance

| Requirement | Implementation |
|-------------|----------------|
| Right to erasure (GDPR) | Process instance data deletion API |
| Data minimization | Configurable variable retention policies |
| Access logging | Full audit trail of who accessed what |
| Data residency | Tenant-specific database location |
| Encryption at rest | Volume-level + field-level encryption |

---

## Security Headers

```http
# All responses include:
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 0
Content-Security-Policy: default-src 'self'; script-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

---

## Vulnerability Management

### Dependency Scanning

- **Renovate Bot**: Automated dependency update PRs
- **Trivy**: Container image vulnerability scanning in CI
- **OWASP Dependency-Check**: Maven/npm dependency audit
- **Snyk**: Real-time vulnerability monitoring (optional)

### Security Testing

| Test Type | Tool | Frequency |
|-----------|------|-----------|
| SAST (static analysis) | SonarQube / Semgrep | Every PR |
| DAST (dynamic testing) | OWASP ZAP | Weekly |
| Container scanning | Trivy | Every build |
| Dependency audit | `mvn verify` + `pnpm audit` | Every build |
| Penetration testing | External firm | Quarterly |
| Secret scanning | GitLeaks / TruffleHog | Every commit |

---

## Incident Response

### Security Event Monitoring

```yaml
# Alert on suspicious activity:
alerts:
  - name: multiple-failed-logins
    condition: failed_login_count > 10 in 5m
    severity: warning
    
  - name: privilege-escalation-attempt
    condition: forbidden_count > 5 in 1m from same_ip
    severity: critical
    
  - name: unusual-api-key-usage
    condition: api_key_requests > 10x baseline
    severity: warning
    
  - name: admin-action-from-new-ip
    condition: admin_action AND ip NOT IN known_ips
    severity: high
```
