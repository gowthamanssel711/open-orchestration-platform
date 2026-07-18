# Security

The gateway validates JWT tokens and handles CORS configuration.

## JWT Validation

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/oop
          jwk-set-uri: http://localhost:8180/realms/oop/protocol/openid-connect/certs
```

The gateway validates tokens at the edge — downstream services also validate (defense in depth), but the gateway catches invalid tokens early.

## CORS Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOriginPatterns:
              - "http://localhost:*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

This allows all `localhost` origins during development. For production, restrict to specific domains.

## Default Filters

Applied to all routes:

```yaml
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
  - AddResponseHeader=X-Request-Id, ${random.uuid}
```

- **DedupeResponseHeader** — prevents duplicate CORS headers when downstream also sets them
- **AddResponseHeader** — adds a unique request ID for tracing

## Actuator Endpoints

Management endpoints are exposed for health checks and monitoring:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,gateway
  endpoint:
    gateway:
      enabled: true
```

The `gateway` actuator endpoint (`/actuator/gateway/routes`) shows all configured routes — useful for debugging.

## Implementation

**File:** `com.oop.gateway.config.SecurityConfig`
