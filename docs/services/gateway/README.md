# API Gateway

The API Gateway is the single entry point for all frontend requests. It handles routing, authentication, CORS, rate limiting, and request logging.

## Purpose

- Route requests to the correct backend service based on path and HTTP method
- Implement CQRS routing: GETs → Query Service, POSTs → Runtime Bundle
- Validate JWT tokens centrally
- Handle CORS for frontend development
- Rate limiting via Redis
- Request logging and tracing

## Technology

| Component | Technology |
|---|---|
| Framework | Spring Cloud Gateway (reactive) |
| Auth | OAuth2 Resource Server (JWT) |
| Rate Limiting | Redis + RequestRateLimiter |
| Observability | Spring Actuator + Prometheus |

## Port

`8080` (default)

## Package Structure

```
com.oop.gateway/
├── GatewayApplication.java
├── config/
│   ├── SecurityConfig.java          # JWT validation, permit paths
│   ├── RateLimiterConfig.java       # Redis rate limiter config
│   └── OpenApiConfig.java           # Aggregated API docs
├── filter/
│   ├── RequestLoggingFilter.java    # Logs all requests
│   ├── ApiKeyAuthFilter.java        # Optional API key auth
│   └── TenantEnrichmentFilter.java  # Multi-tenant header injection
└── exception/
    └── GlobalErrorWebExceptionHandler.java  # Detailed error responses
```

## Running

```bash
cd platform/gateway
mvn spring-boot:run
```

**Prerequisites:** Redis (for rate limiting), Keycloak (for JWT validation).

## Features

- [Routing](routing.md) — Route table and CQRS split logic
- [Security](security.md) — JWT validation and CORS
- [Filters](filters.md) — Rate limiting, logging, tenant enrichment
