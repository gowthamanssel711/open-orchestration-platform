# Event Consumers

The Query Service consumes events from RabbitMQ to keep its read model in sync with the runtime bundle.

## Consumer Beans

| Bean Name | Channel | Events Handled |
|---|---|---|
| `engineEventsConsumer` | engineEvents-in-0 | PROCESS_STARTED, PROCESS_COMPLETED, PROCESS_CANCELLED, FORM_DEPLOYED |
| `taskEventsConsumer` | taskEvents-in-0 | TASK_CREATED, TASK_ASSIGNED, TASK_RELEASED, TASK_COMPLETED |

## Function Definition

Both beans must be declared in the function definition:

```java
System.setProperty("spring.cloud.function.definition", "engineEventsConsumer;taskEventsConsumer");
```

This is set in `QueryServiceApplication.main()` because Spring Cloud Function auto-detection fails with multiple `Consumer` beans.

## Message Format

All messages arrive as `Message<byte[]>`. The consumers:
1. Parse the byte array as a UTF-8 string
2. Check if it starts with `{` (JSON) — skip non-JSON messages
3. Deserialize to `Map<String, Object>`
4. Route by the `"type"` field

This handles Activiti Cloud's internal `CloudRuntimeEvent[]` messages (binary, non-JSON) that arrive on the same exchange.

## Process Event Consumer

**File:** `com.oop.query.consumer.ProcessEventConsumer`

### PROCESS_STARTED
Creates a new `ProcessInstanceEntity` with status `RUNNING`.

### PROCESS_COMPLETED
Sets status to `COMPLETED` and records `completedAt`.

### PROCESS_CANCELLED
Sets status to `CANCELLED` and records `completedAt`.

### FORM_DEPLOYED
Stores the form schema in `form_definition` table.

## Task Event Consumer

**File:** `com.oop.query.consumer.TaskEventConsumer`

### TASK_CREATED
Creates a new `TaskEntity` with:
- `taskId`, `name`, `processInstanceId`, `processDefinitionKey`
- `formKey` (if present)
- `assignee` (if pre-assigned)
- Status: `CREATED`

### TASK_ASSIGNED
- **Guard:** Ignores events with empty assignee (safety net for unclaim)
- Sets `assignee` and status to `ASSIGNED`

### TASK_RELEASED
- Clears `assignee` (sets to null)
- Resets status to `CREATED`

### TASK_COMPLETED
- Sets status to `COMPLETED`
- Records `completedAt`

## Error Handling

- Parse failures log a warning and skip the message (no retry)
- Unknown event types are logged at TRACE level
- Non-JSON messages (Activiti internal) are silently skipped

## Eventual Consistency

The query service is **eventually consistent** — there's a brief delay (typically < 1 second) between an action on the runtime bundle and the data appearing in queries. The frontend handles this by:
- Optimistically updating the UI after write operations
- Providing "Refresh" buttons for users to re-fetch
