# Security

The Runtime Bundle uses Keycloak JWT tokens for authentication and integrates with Activiti's identity system.

## Authentication Flow

```
Frontend (Keycloak JS)
    │ (gets JWT token)
    ▼
API Gateway
    │ (validates JWT, forwards)
    ▼
Runtime Bundle SecurityConfig
    │ (validates JWT, extracts roles)
    ▼
Activiti Identity (UserGroupManager)
    │ (maps JWT claims to Activiti user/groups)
    ▼
Process Engine (applies security policies)
```

## JWT Validation

Configured as an OAuth2 Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/oop
          jwk-set-uri: http://localhost:8180/realms/oop/protocol/openid-connect/certs
```

## Role Mapping

`SecurityConfig` uses a `KeycloakRealmRoleConverter` that:
1. Reads `realm_access.roles` from the JWT
2. Maps each role to a Spring `ROLE_` authority
3. **Always injects** `ROLE_ACTIVITI_USER` for all authenticated users

This is critical — Activiti Cloud requires `ROLE_ACTIVITI_USER` to access `ProcessRuntime`.

```java
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    // Extracts realm_access.roles from JWT
    // Always adds ROLE_ACTIVITI_USER
}
```

## Activiti Identity Integration

`ActivitiIdentityConfig` provides a `UserGroupManager` bean that:
1. Reads `preferred_username` from the JWT as the Activiti user
2. Always includes `ACTIVITI_USER` group
3. Adds `ACTIVITI_ADMIN` group for users with admin roles

This is required for Activiti Cloud's internal security checks (process visibility, task assignment, etc.).

## Service-to-Service Auth

Activiti Cloud internally calls Keycloak to validate identities. It uses OAuth2 client credentials:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: activiti-keycloak
            client-secret: <secret>
            authorization-grant-type: client_credentials
            scope: openid
        provider:
          keycloak:
            token-uri: http://localhost:8180/realms/oop/protocol/openid-connect/token
```

The registration must be named `keycloak` — Activiti resolves it from `${activiti.cloud.services.oauth2.iam-name:keycloak}`.

## Keycloak Setup Required

1. **Client `activiti-keycloak`** — with service accounts enabled
2. **Service account roles** — `realm-management` client roles: `view-users`, `query-users`, `query-groups`, `view-realm`
3. **Client `tasklist-ui`** — public client for frontend authentication

## Endpoints Security

All endpoints require a valid JWT token. No endpoints are publicly accessible.

## Implementation

**Files:**
- `com.oop.runtime.config.SecurityConfig` — Spring Security configuration
- `com.oop.runtime.config.ActivitiIdentityConfig` — Activiti identity bridge
