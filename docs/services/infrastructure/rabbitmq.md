# RabbitMQ

Message broker for CQRS event streaming between services.

## Access

| | Value |
|---|---|
| AMQP | `localhost:5672` |
| Management UI | http://localhost:15672 |
| Username | guest |
| Password | guest |

## Event Flow

```
Runtime Bundle
    │
    ├── engineEvents-out-0 ──▶ Exchange ──▶ Queue ──▶ Query Service (engineEventsConsumer)
    │
    └── taskEvents-out-0 ───▶ Exchange ──▶ Queue ──▶ Query Service (taskEventsConsumer)
```

## Exchanges & Queues

Spring Cloud Stream auto-creates exchanges and queues based on binding configuration.

### Engine Events Channel

| Component | Name |
|---|---|
| Producer binding | `engineEvents-out-0` |
| Consumer binding | `engineEventsConsumer-in-0` |
| Exchange | Created automatically by Spring Cloud Stream |
| Events | PROCESS_STARTED, PROCESS_COMPLETED, PROCESS_CANCELLED, FORM_DEPLOYED |

### Task Events Channel

| Component | Name |
|---|---|
| Producer binding | `taskEvents-out-0` |
| Consumer binding | `taskEventsConsumer-in-0` |
| Exchange | Created automatically by Spring Cloud Stream |
| Events | TASK_CREATED, TASK_ASSIGNED, TASK_RELEASED, TASK_COMPLETED |

## Configuration

### Producer (Runtime Bundle)

```yaml
spring:
  cloud:
    stream:
      bindings:
        engineEvents-out-0:
          destination: engineEvents
        taskEvents-out-0:
          destination: taskEvents
```

### Consumer (Query Service)

```yaml
spring:
  cloud:
    function:
      definition: engineEventsConsumer;taskEventsConsumer
    stream:
      bindings:
        engineEventsConsumer-in-0:
          destination: engineEvents
          group: query-service
        taskEventsConsumer-in-0:
          destination: taskEvents
          group: query-service
```

The `group` ensures only one instance of the query service processes each message (competing consumers pattern).

## Message Format

All messages are JSON objects with `type` and `data` fields:

```json
{
  "type": "TASK_CREATED",
  "timestamp": "2026-07-17T10:00:00.000Z",
  "data": {
    "taskId": "...",
    "name": "..."
  }
}
```

## Activiti Cloud Internal Messages

Activiti Cloud also publishes its own `CloudRuntimeEvent[]` messages as byte arrays on the same exchange. The query service consumers handle this by:
1. Receiving as `Message<byte[]>`
2. Attempting JSON parse
3. Skipping messages that don't start with `{`

## Monitoring

RabbitMQ Management UI (http://localhost:15672) shows:
- Queue depths (messages waiting to be consumed)
- Consumer status
- Message rates
- Connection status

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| Messages not consumed | Function definition wrong | Check `spring.cloud.function.definition` |
| MessageHandlingException | Activiti binary messages | Consumers use `byte[]` + manual parse |
| Queue not created | Consumer not started | Start query service |
| Messages accumulating | Consumer crashed | Check query service logs |
