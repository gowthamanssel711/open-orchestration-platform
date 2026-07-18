# Keycloak

OAuth2 / OpenID Connect provider for all platform authentication.

## Access

| | Value |
|---|---|
| Admin Console | http://localhost:8180 |
| Admin User | admin |
| Admin Password | admin |
| Realm | `oop` |
| Token Endpoint | `http://localhost:8180/realms/oop/protocol/openid-connect/token` |

## Realm: `oop`

Auto-imported from `deploy/keycloak/realm-export.json` on first startup.

## Clients

| Client ID | Type | Purpose |
|---|---|---|
| `tasklist-ui` | Public | Frontend SPA authentication |
| `activiti-keycloak` | Confidential (service account) | Runtime bundle service-to-service auth |

### tasklist-ui

- **Access Type:** Public (no secret)
- **Redirect URIs:** `http://localhost:*`
- **Web Origins:** `http://localhost:*`
- **Standard Flow:** Enabled (authorization code)
- **Direct Access Grants:** Enabled (for testing with curl)

### activiti-keycloak

- **Access Type:** Confidential
- **Service Accounts Enabled:** Yes
- **Service Account Roles:** `realm-management` client roles:
  - `view-users`
  - `query-users`
  - `query-groups`
  - `view-realm`

These roles allow the runtime bundle to look up user/group information from Keycloak.

## Users

Create users in Keycloak admin console or via realm-export:

| Username | Password | Roles |
|---|---|---|
| admin | admin | ACTIVITI_USER, ACTIVITI_ADMIN |

## Getting a Token (for testing)

```bash
# Password grant (direct access)
TOKEN=$(curl -s -X POST http://localhost:8180/realms/oop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=tasklist-ui" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r '.access_token')

echo $TOKEN
```

## JWT Claims Used by Backend

| Claim | Used By | Purpose |
|---|---|---|
| `preferred_username` | SecurityConfig, TaskController | Identify the user |
| `realm_access.roles` | SecurityConfig | Map to Spring authorities |
| `email` | UI display | Show user email |

## Realm Export

The `deploy/keycloak/realm-export.json` file is imported on Keycloak startup. To update:

1. Make changes in Keycloak admin console
2. Export: Realm Settings → Action → Partial export → Include clients, roles
3. Replace `deploy/keycloak/realm-export.json`

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| `invalid_client_credentials` | Wrong client secret | Check secret in Keycloak vs application.yml |
| `client_not_found` | Client doesn't exist | Check realm-export was imported |
| Token expired | Default 5min lifetime | Re-request token |
| `issuer-uri` mismatch | Docker vs localhost URL | Ensure consistent URL in all configs |
