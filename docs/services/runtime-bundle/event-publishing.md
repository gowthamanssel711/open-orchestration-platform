# Event Publishing

The Runtime Bundle publishes engine events to RabbitMQ so downstream services (Query Service) can build read models.

## Architecture

```
Activiti Engine
    │ (fires ActivitiEvent)
    ▼
EngineEventPublisher (implements ActivitiEventListener)
    │ (converts to JSON)
    ▼
StreamBridge.send()
    │
    ▼
RabbitMQ Exchange
    │
    ▼
Query Service Consumers
```

## Channels

| Channel | Events | Purpose |
|---|---|---|
| `engineEvents-out-0` | PROCESS_*, FORM_DEPLOYED | Process lifecycle + forms |
| `taskEvents-out-0` | TASK_* | Task lifecycle |

## Event Types

### Process Events

**PROCESS_STARTED**
```json
{
  "type": "PROCESS_STARTED",
  "timestamp": "2026-07-17T10:00:00.000Z",
  "data": {
    "processInstanceId": "f7aa2e14-...",
    "processDefinitionKey": "order-process",
    "businessKey": "ORD-12345",
    "initiator": "admin"
  }
}
```

**PROCESS_COMPLETED**
```json
{
  "type": "PROCESS_COMPLETED",
  "timestamp": "2026-07-17T11:00:00.000Z",
  "data": {
    "processInstanceId": "f7aa2e14-..."
  }
}
```

**PROCESS_CANCELLED**
```json
{
  "type": "PROCESS_CANCELLED",
  "timestamp": "2026-07-17T11:00:00.000Z",
  "data": {
    "processInstanceId": "f7aa2e14-..."
  }
}
```

### Task Events

**TASK_CREATED**
```json
{
  "type": "TASK_CREATED",
  "timestamp": "2026-07-17T10:00:00.000Z",
  "data": {
    "taskId": "f7aaca58-...",
    "name": "Review Order",
    "processInstanceId": "f7aa2e14-...",
    "processDefinitionKey": "order-process",
    "assignee": "",
    "formKey": "review-order"
  }
}
```

**TASK_ASSIGNED**
```json
{
  "type": "TASK_ASSIGNED",
  "timestamp": "2026-07-17T10:05:00.000Z",
  "data": {
    "taskId": "f7aaca58-...",
    "assignee": "admin"
  }
}
```

**TASK_RELEASED**

Sent when `assignee` is null/blank (unclaim operation):
```json
{
  "type": "TASK_RELEASED",
  "timestamp": "2026-07-17T10:10:00.000Z",
  "data": {
    "taskId": "f7aaca58-...",
    "assignee": ""
  }
}
```

**TASK_COMPLETED**
```json
{
  "type": "TASK_COMPLETED",
  "timestamp": "2026-07-17T10:15:00.000Z",
  "data": {
    "taskId": "f7aaca58-..."
  }
}
```

### Form Events

**FORM_DEPLOYED**
```json
{
  "type": "FORM_DEPLOYED",
  "timestamp": "2026-07-17T10:00:00.000Z",
  "data": {
    "formKey": "review-order",
    "version": 1,
    "schema": "{...full form JSON...}"
  }
}
```

## TASK_ASSIGNED vs TASK_RELEASED

Activiti engine fires `TASK_ASSIGNED` for both claim and unclaim operations. The publisher distinguishes:
- If `task.getAssignee()` is non-null and non-blank → publishes `TASK_ASSIGNED`
- If `task.getAssignee()` is null or blank → publishes `TASK_RELEASED`

This allows the query service to properly reset task status on unclaim.

## processDefinitionKey Extraction

The `Task` object doesn't have a direct `getProcessDefinitionKey()` method. The publisher extracts it from `processDefinitionId` (format: `key:version:id`):

```java
String[] parts = task.getProcessDefinitionId().split(":");
String processDefinitionKey = parts[0];
```

## Configuration

The event listener is registered with RuntimeService in `EngineEventConfig`:

```java
@PostConstruct
public void registerListener() {
    runtimeService.addEventListener(engineEventPublisher);
}
```

## Failure Handling

`isFailOnException()` returns `false` — if event publishing fails, it does not roll back the engine operation. Events are best-effort; the query service may lag but the engine continues.

## Implementation

**Files:**
- `com.oop.runtime.event.EngineEventPublisher` — event listener + publisher
- `com.oop.runtime.config.EngineEventConfig` — registers listener
- `com.oop.runtime.config.StreamBindingConfig` — RabbitMQ stream configuration
