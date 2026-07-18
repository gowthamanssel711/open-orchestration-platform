# 05 — API Design

## API Principles

1. **RESTful**: Resource-oriented URLs, standard HTTP methods
2. **Versioned**: `/api/v1/` prefix, breaking changes = new version
3. **Consistent**: Same patterns across all endpoints
4. **Paginated**: Cursor-based pagination for large collections
5. **Filterable**: Query parameters for filtering, sorting
6. **Documented**: OpenAPI 3.1 specification (auto-generated)

---

## Base URL

```
Production:  https://api.orchestration.company.com/api/v1
Development: http://localhost:8080/api/v1
```

---

## Authentication

All requests require a Bearer token (JWT from Keycloak):

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

Or an API key for service-to-service:

```http
X-API-Key: oop_live_sk_abc123...
```

---

## Common Response Format

### Success

```json
{
  "data": { ... },
  "meta": {
    "requestId": "req-uuid",
    "timestamp": "2026-07-15T10:30:00Z"
  }
}
```

### Collection (Paginated)

```json
{
  "data": [ ... ],
  "pagination": {
    "totalCount": 1234,
    "pageSize": 25,
    "cursor": "eyJpZCI6MTAwfQ==",
    "hasMore": true
  },
  "meta": {
    "requestId": "req-uuid",
    "timestamp": "2026-07-15T10:30:00Z"
  }
}
```

### Error

```json
{
  "error": {
    "code": "PROCESS_NOT_FOUND",
    "message": "Process definition 'order-process' not found",
    "details": {
      "processDefinitionKey": "order-process"
    }
  },
  "meta": {
    "requestId": "req-uuid",
    "timestamp": "2026-07-15T10:30:00Z"
  }
}
```

---

## Endpoint Reference

### Process Definitions

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/processes` | Deploy a process definition (BPMN file) |
| `GET` | `/processes` | List all process definitions |
| `GET` | `/processes/{key}` | Get process definition by key |
| `GET` | `/processes/{key}/versions` | List all versions |
| `GET` | `/processes/{key}/xml` | Get BPMN XML |
| `DELETE` | `/processes/{key}` | Delete process definition |

#### Deploy Process

```http
POST /api/v1/processes
Content-Type: multipart/form-data

file: @order-process.bpmn
```

Response:
```json
{
  "data": {
    "id": "pd-uuid",
    "key": "order-process",
    "name": "Order Fulfillment",
    "version": 3,
    "deployedAt": "2026-07-15T10:30:00Z",
    "deployedBy": "john.doe"
  }
}
```

---

### Process Instances

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/processes/{key}/instances` | Start a new instance |
| `GET` | `/instances` | List instances (with filters) |
| `GET` | `/instances/{id}` | Get instance details |
| `GET` | `/instances/{id}/variables` | Get instance variables |
| `PUT` | `/instances/{id}/variables` | Update instance variables |
| `GET` | `/instances/{id}/history` | Get instance event history |
| `POST` | `/instances/{id}/cancel` | Cancel a running instance |
| `POST` | `/instances/{id}/retry` | Retry a failed instance |
| `POST` | `/instances/{id}/migrate` | Migrate to new version |

#### Start Instance

```http
POST /api/v1/processes/order-process/instances
Content-Type: application/json

{
  "businessKey": "ORDER-12345",
  "variables": {
    "orderId": "12345",
    "customerId": "cust-789",
    "amount": 99.99,
    "items": ["SKU-001", "SKU-002"]
  }
}
```

Response:
```json
{
  "data": {
    "id": "pi-uuid",
    "processDefinitionKey": "order-process",
    "processDefinitionVersion": 3,
    "businessKey": "ORDER-12345",
    "status": "RUNNING",
    "startedAt": "2026-07-15T10:30:00Z",
    "startedBy": "john.doe",
    "variables": {
      "orderId": "12345",
      "customerId": "cust-789",
      "amount": 99.99
    }
  }
}
```

#### List Instances (with filters)

```http
GET /api/v1/instances?status=RUNNING&processDefinitionKey=order-process&startedAfter=2026-07-01T00:00:00Z&sort=startedAt:desc&pageSize=25
```

---

### Tasks (Human Tasks)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/tasks` | List tasks (inbox) |
| `GET` | `/tasks/{id}` | Get task details |
| `POST` | `/tasks/{id}/claim` | Claim a task |
| `POST` | `/tasks/{id}/unclaim` | Release a claimed task |
| `POST` | `/tasks/{id}/assign` | Assign to another user |
| `POST` | `/tasks/{id}/delegate` | Delegate task |
| `POST` | `/tasks/{id}/complete` | Complete a task |
| `GET` | `/tasks/{id}/variables` | Get task variables |
| `GET` | `/tasks/{id}/form` | Get task form definition |

#### List Tasks (Inbox)

```http
GET /api/v1/tasks?assignee=me&status=CREATED&sort=createdAt:desc
```

Response:
```json
{
  "data": [
    {
      "id": "task-uuid",
      "name": "Approve Order",
      "description": "Review and approve order ORDER-12345",
      "assignee": "john.doe",
      "candidateGroups": ["managers"],
      "priority": 50,
      "dueDate": "2026-07-16T10:00:00Z",
      "createdAt": "2026-07-15T10:30:00Z",
      "processInstanceId": "pi-uuid",
      "processDefinitionKey": "order-process",
      "formKey": "approve-order-form",
      "status": "ASSIGNED",
      "variables": {
        "orderId": "12345",
        "amount": 99.99
      }
    }
  ],
  "pagination": {
    "totalCount": 12,
    "pageSize": 25,
    "hasMore": false
  }
}
```

#### Complete Task

```http
POST /api/v1/tasks/task-uuid/complete
Content-Type: application/json

{
  "variables": {
    "approved": true,
    "reviewNotes": "Looks good, approved."
  }
}
```

---

### Messages

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/messages` | Publish a message (correlate) |

#### Publish Message

```http
POST /api/v1/messages
Content-Type: application/json

{
  "messageName": "payment-received",
  "correlationKey": "ORDER-12345",
  "variables": {
    "paymentId": "pay-xyz",
    "paidAmount": 99.99
  }
}
```

---

### Signals

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/signals` | Broadcast a signal |

```http
POST /api/v1/signals
Content-Type: application/json

{
  "signalName": "end-of-day-close",
  "variables": {
    "closingTime": "2026-07-15T17:00:00Z"
  }
}
```

---

### Connectors

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/connectors` | List available connectors |
| `GET` | `/connectors/{id}` | Get connector details |
| `POST` | `/connectors/{id}/test` | Test connector configuration |
| `GET` | `/connectors/{id}/executions` | List connector executions |

---

### Analytics

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/analytics/processes/{key}/duration` | Process duration metrics |
| `GET` | `/analytics/processes/{key}/throughput` | Throughput metrics |
| `GET` | `/analytics/processes/{key}/heatmap` | Activity duration heatmap |
| `GET` | `/analytics/processes/{key}/bottlenecks` | Bottleneck analysis |
| `GET` | `/analytics/sla` | SLA breach summary |
| `GET` | `/analytics/sla/{id}` | SLA details |
| `POST` | `/analytics/sla` | Define new SLA |
| `GET` | `/analytics/dashboards` | List dashboards |
| `POST` | `/analytics/dashboards` | Create dashboard |
| `GET` | `/analytics/reports/{id}/export` | Export report (CSV/PDF) |

#### Process Duration Heatmap

```http
GET /api/v1/analytics/processes/order-process/heatmap?from=2026-07-01&to=2026-07-15
```

Response:
```json
{
  "data": {
    "processDefinitionKey": "order-process",
    "period": {
      "from": "2026-07-01T00:00:00Z",
      "to": "2026-07-15T23:59:59Z"
    },
    "instancesCompleted": 1523,
    "avgDuration": "PT2H15M",
    "activities": [
      {
        "activityId": "approve-order",
        "activityName": "Approve Order",
        "activityType": "userTask",
        "executionCount": 1523,
        "avgDuration": "PT45M",
        "p50Duration": "PT30M",
        "p95Duration": "PT2H",
        "p99Duration": "PT4H",
        "isBottleneck": true
      },
      {
        "activityId": "process-payment",
        "activityName": "Process Payment",
        "activityType": "serviceTask",
        "executionCount": 1498,
        "avgDuration": "PT3S",
        "p50Duration": "PT2S",
        "p95Duration": "PT8S",
        "p99Duration": "PT15S",
        "isBottleneck": false
      }
    ]
  }
}
```

---

### Admin / Identity

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/users` | List users |
| `POST` | `/admin/users` | Create user |
| `GET` | `/admin/roles` | List roles |
| `POST` | `/admin/roles` | Create role |
| `POST` | `/admin/roles/{id}/permissions` | Add permission to role |
| `GET` | `/admin/tenants` | List tenants |
| `POST` | `/admin/tenants` | Create tenant |
| `GET` | `/admin/api-keys` | List API keys |
| `POST` | `/admin/api-keys` | Generate API key |
| `DELETE` | `/admin/api-keys/{id}` | Revoke API key |

---

### System / Health

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Platform health status |
| `GET` | `/health/services` | Individual service health |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/info` | Platform version info |

---

## WebSocket / Real-Time API

### Connection

```javascript
const ws = new WebSocket('wss://api.orchestration.company.com/ws?token=JWT_TOKEN');
```

### Subscribe to Instance Events

```json
{
  "action": "subscribe",
  "channel": "instance",
  "filter": {
    "processInstanceId": "pi-uuid"
  }
}
```

### Event Message

```json
{
  "channel": "instance",
  "event": "ACTIVITY_COMPLETED",
  "data": {
    "processInstanceId": "pi-uuid",
    "activityId": "approve-order",
    "activityName": "Approve Order",
    "timestamp": "2026-07-15T10:45:00Z"
  }
}
```

### Subscribe to Task Notifications

```json
{
  "action": "subscribe",
  "channel": "tasks",
  "filter": {
    "assignee": "john.doe"
  }
}
```

---

## GraphQL API (Notifications Service)

```graphql
# Subscribe to process instance events
subscription ProcessEvents($processInstanceId: ID!) {
  processInstanceEvents(processInstanceId: $processInstanceId) {
    eventType
    activityId
    activityName
    timestamp
    variables
  }
}

# Subscribe to task assignments
subscription MyTasks($assignee: String!) {
  taskEvents(assignee: $assignee) {
    eventType
    task {
      id
      name
      assignee
      dueDate
      processDefinitionKey
    }
  }
}

# Query process instances
query ProcessInstances($filter: ProcessInstanceFilter!) {
  processInstances(filter: $filter) {
    edges {
      node {
        id
        businessKey
        status
        startedAt
        processDefinitionKey
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

---

## gRPC API (High-Performance Clients)

```protobuf
// proto/gateway/v1/gateway.proto

syntax = "proto3";
package oop.gateway.v1;

service GatewayService {
  // Process operations
  rpc DeployProcess(DeployProcessRequest) returns (DeployProcessResponse);
  rpc StartProcessInstance(StartProcessInstanceRequest) returns (StartProcessInstanceResponse);
  rpc CancelProcessInstance(CancelProcessInstanceRequest) returns (CancelProcessInstanceResponse);
  
  // Job operations (for service task workers)
  rpc ActivateJobs(ActivateJobsRequest) returns (stream ActivateJobsResponse);
  rpc CompleteJob(CompleteJobRequest) returns (CompleteJobResponse);
  rpc FailJob(FailJobRequest) returns (FailJobResponse);
  
  // Message operations
  rpc PublishMessage(PublishMessageRequest) returns (PublishMessageResponse);
  
  // Topology
  rpc GetTopology(GetTopologyRequest) returns (GetTopologyResponse);
}

message StartProcessInstanceRequest {
  string process_definition_key = 1;
  int32 version = 2;  // -1 for latest
  string business_key = 3;
  bytes variables = 4;  // JSON-encoded
}

message ActivateJobsRequest {
  string type = 1;        // Job type to activate
  string worker = 2;      // Worker identifier
  int32 max_jobs = 3;     // Max jobs to receive
  int64 timeout_ms = 4;   // Job lock timeout
  repeated string fetch_variables = 5;  // Variables to include
}
```

---

## Rate Limiting

| Endpoint Category | Default Limit | Burst |
|-------------------|--------------|-------|
| Read (GET) | 1000 req/min | 50 |
| Write (POST/PUT/DELETE) | 200 req/min | 20 |
| Deploy | 10 req/min | 5 |
| Analytics | 100 req/min | 10 |
| WebSocket connections | 50 concurrent | — |
| gRPC streams | 100 concurrent | — |

Rate limit headers:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 987
X-RateLimit-Reset: 1721042400
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `PROCESS_NOT_FOUND` | 404 | Process definition not found |
| `INSTANCE_NOT_FOUND` | 404 | Process instance not found |
| `TASK_NOT_FOUND` | 404 | Task not found |
| `TASK_ALREADY_CLAIMED` | 409 | Task already assigned |
| `INSTANCE_NOT_ACTIVE` | 409 | Instance is not in active state |
| `INVALID_BPMN` | 422 | BPMN file is invalid |
| `CORRELATION_FAILED` | 422 | Message could not be correlated |
| `RATE_LIMITED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Unexpected server error |
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |
