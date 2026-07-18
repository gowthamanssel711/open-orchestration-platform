# Gateway - Technical Reference

Detailed file-by-file, function-by-function documentation.

---

## `GatewayApplication.java`

**Package:** `com.oop.gateway`

Main Spring Boot application entry point for the reactive gateway.

| Method | Description |
|---|---|
| `main(String[] args)` | Bootstraps the Spring Cloud Gateway application (WebFlux-based, not servlet) |

---

## Config Package

### `SecurityConfig.java`

**Package:** `com.oop.gateway.config`

Configures security for the reactive gateway.

| Class/Method | Description |
|---|---|
| `SecurityConfig` | `@Configuration` class |
| `springSecurityFilterChain(ServerHttpSecurity)` | Configures OAuth2 resource server with JWT. Permits actuator/health endpoints. All other requests require authentication. Disables CSRF (stateless API gateway) |

**Note:** Gateway uses reactive Spring Security (`ServerHttpSecurity`), not servlet (`HttpSecurity`).

---

### `RateLimiterConfig.java`

**Package:** `com.oop.gateway.config`

Configures Redis-based rate limiting.

| Class/Method | Description |
|---|---|
| `RateLimiterConfig` | `@Configuration` class |
| `userKeyResolver()` | `@Bean` — returns a `KeyResolver` that extracts the rate-limit key from the request. Uses JWT subject claim or remote address as fallback |

**Rate limit key resolution:**
1. Try to extract user principal from JWT
2. Fall back to client IP address
3. This means rate limits are per-user (authenticated) or per-IP (unauthenticated)

---

### `OpenApiConfig.java`

**Package:** `com.oop.gateway.config`

Aggregated OpenAPI/Swagger configuration.

| Class/Method | Description |
|---|---|
| `OpenApiConfig` | Sets up aggregated API documentation from all downstream services |

---

## Filter Package

### `RequestLoggingFilter.java`

**Package:** `com.oop.gateway.filter`

Global filter that logs all incoming requests.

| Class/Method | Description |
|---|---|
| `RequestLoggingFilter` | Implements `GlobalFilter, Ordered` |
| `filter(ServerWebExchange, GatewayFilterChain)` | Logs: HTTP method, path, remote address, content-type. On response: logs status code and elapsed time |
| `getOrder()` | Returns high priority (low number) to ensure logging happens first |

**Log format:**
```
INFO  Gateway request: GET /api/v1/tasks from 127.0.0.1
INFO  Gateway response: 200 in 45ms
```

---

### `ApiKeyAuthFilter.java`

**Package:** `com.oop.gateway.filter`

Optional API key authentication for machine-to-machine communication.

| Class/Method | Description |
|---|---|
| `ApiKeyAuthFilter` | Implements `GlobalFilter` |
| `filter(ServerWebExchange, GatewayFilterChain)` | Checks for `X-API-Key` header. If present and valid, allows request through. If present and invalid, returns 401 |

**Not enabled by default** — activate via configuration when needed for external API consumers.

---

### `TenantEnrichmentFilter.java`

**Package:** `com.oop.gateway.filter`

Multi-tenant support filter.

| Class/Method | Description |
|---|---|
| `TenantEnrichmentFilter` | Implements `GlobalFilter` |
| `filter(ServerWebExchange, GatewayFilterChain)` | Extracts tenant identifier from JWT claims or request headers. Injects `X-Tenant-ID` header into downstream request for tenant isolation |

---

## Exception Package

### `GlobalErrorWebExceptionHandler.java`

**Package:** `com.oop.gateway.exception`

Custom error handler for the reactive gateway.

| Class/Method | Description |
|---|---|
| `GlobalErrorWebExceptionHandler` | Extends `AbstractErrorWebExceptionHandler` |
| `getRoutingFunction(ErrorAttributes)` | Returns router function that handles all errors |
| `renderErrorResponse(ServerRequest)` | Builds structured JSON error response with: status, error name, message, path, timestamp. Logs full exception with cause chain |
| `WebPropertiesConfig` | Inner `@Configuration` class providing `WebProperties` bean (required for error handler) |

**Response format:**
```json
{
  "status": 502,
  "error": "Bad Gateway",
  "message": "Connection refused: localhost:8082",
  "path": "/api/v1/tasks",
  "timestamp": "2026-07-17T10:00:00.000Z",
  "requestId": "abc-123"
}
```

**Cause chain logging:** Walks the exception cause chain and logs each level, making it easier to debug routing failures vs downstream service errors.
