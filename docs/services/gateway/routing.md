# Routing

The gateway implements CQRS routing — splitting reads and writes to different backend services.

## Route Table

| Route ID | Path | Method | Target | Purpose |
|---|---|---|---|---|
| `runtime-deploy` | `/api/v1/deploy` | POST | Runtime Bundle (:8081) | Deploy BPMN + forms |
| `runtime-processes` | `/api/v1/processes/**` | ALL | Runtime Bundle (:8081) | Process definitions |
| `query-process-instances-read` | `/api/v1/process-instances/**` | GET | Query Service (:8082) | List/get instances |
| `runtime-process-instances-write` | `/api/v1/process-instances/**` | POST,PUT,DELETE | Runtime Bundle (:8081) | Start/cancel instances |
| `query-tasks-read` | `/api/v1/tasks/**` | GET | Query Service (:8082) | List/get tasks + forms |
| `tasks-write` | `/api/v1/tasks/**` | POST,PUT,PATCH,DELETE | Runtime Bundle (:8081) | Claim/complete tasks |
| `forms-read` | `/api/v1/forms/**` | GET | Runtime Bundle (:8081) | Get deployed forms |

## CQRS Routing Pattern

```
Client Request
    │
    ├── GET  ──────▶ Query Service (read-optimized)
    │
    └── POST/PUT ──▶ Runtime Bundle (write/execute)
```

This pattern ensures:
- Reads never hit the process engine (no load on execution)
- Writes go directly to the engine (no stale data)
- Services can scale independently

## Route Priority

Spring Cloud Gateway evaluates routes in order. More specific routes (with Method predicate) should come before catch-all routes.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `RUNTIME_BUNDLE_URL` | `http://localhost:8081` | Runtime bundle URL |
| `QUERY_SERVICE_URL` | `http://localhost:8082` | Query service URL |
| `AUDIT_SERVICE_URL` | `http://localhost:8083` | Audit service URL |
| `ANALYTICS_SERVICE_URL` | `http://localhost:8084` | Analytics service URL |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8085` | Notification service URL |
| `IDENTITY_SERVICE_URL` | `http://localhost:8087` | Identity service URL |

## Adding New Routes

To route a new endpoint:

```yaml
- id: my-new-route
  uri: ${MY_SERVICE_URL:http://localhost:9090}
  predicates:
    - Path=/api/v1/my-resource/**
    - Method=GET
```

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| 500 on GET | Route missing or points to wrong service | Check route order and target URL |
| 404 | No matching route | Add route for the path |
| 502 Bad Gateway | Target service is down | Start the target service |
| CORS error | Frontend origin not in allowedOriginPatterns | Add to globalcors config |
