# Filters

Custom gateway filters for cross-cutting concerns.

## Request Logging Filter

**File:** `com.oop.gateway.filter.RequestLoggingFilter`

Logs every incoming request with method, path, and timing information. Useful for debugging routing issues.

```
INFO  Gateway: GET /api/v1/tasks - routed to query-tasks-read (23ms)
```

## Rate Limiting

Uses Spring Cloud Gateway's built-in `RequestRateLimiter` with Redis backend.

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 50
      redis-rate-limiter.burstCapacity: 100
```

| Parameter | Meaning |
|---|---|
| `replenishRate` | Requests per second allowed (sustained) |
| `burstCapacity` | Maximum burst size |

**Requires Redis** — if Redis is down, rate limiting is bypassed (fail-open).

**File:** `com.oop.gateway.config.RateLimiterConfig`

## API Key Auth Filter

**File:** `com.oop.gateway.filter.ApiKeyAuthFilter`

Optional filter for machine-to-machine authentication via API keys (as an alternative to JWT). Not enabled by default.

## Tenant Enrichment Filter

**File:** `com.oop.gateway.filter.TenantEnrichmentFilter`

For multi-tenant deployments. Extracts tenant information from the JWT or request header and injects it as a header for downstream services.

## Global Error Handler

**File:** `com.oop.gateway.exception.GlobalErrorWebExceptionHandler`

Catches all unhandled exceptions and returns structured JSON error responses:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Connection refused: localhost:8082",
  "path": "/api/v1/tasks",
  "timestamp": "2026-07-17T10:00:00.000Z"
}
```

Includes full cause chain in logs for debugging.
