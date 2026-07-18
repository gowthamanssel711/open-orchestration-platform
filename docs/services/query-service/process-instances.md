# Process Instances

Query endpoints for process instances.

## List Process Instances

```
GET /api/v1/process-instances
```

**Query Parameters:**
| Param | Type | Description |
|---|---|---|
| `status` | string | Filter: `RUNNING`, `COMPLETED`, `CANCELLED` |
| `processDefinitionKey` | string | Filter by process definition |
| `page` | int | Page number (0-based) |
| `size` | int | Page size (default: 20) |

**Response (paginated):**
```json
{
  "content": [
    {
      "id": 1,
      "processInstanceId": "f7aa2e14-81cd-11f1-b84a-00155d010200",
      "processDefinitionKey": "order-process",
      "businessKey": "ORD-12345",
      "initiator": "admin",
      "status": "RUNNING",
      "startedAt": "2026-07-17T10:00:00.000Z",
      "completedAt": null
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Get Single Instance

```
GET /api/v1/process-instances/{processInstanceId}
```

**Response:**
```json
{
  "id": 1,
  "processInstanceId": "f7aa2e14-81cd-11f1-b84a-00155d010200",
  "processDefinitionKey": "order-process",
  "businessKey": "ORD-12345",
  "initiator": "admin",
  "status": "RUNNING",
  "startedAt": "2026-07-17T10:00:00.000Z",
  "completedAt": null
}
```

Returns `404` if not found.

## Status Values

| Status | Meaning |
|---|---|
| `RUNNING` | Process is actively executing |
| `COMPLETED` | Process reached end event naturally |
| `CANCELLED` | Process was terminated via cancel API |

## Data Source

Data comes from `PROCESS_STARTED`, `PROCESS_COMPLETED`, `PROCESS_CANCELLED` events consumed from RabbitMQ. There may be a slight delay (typically < 1 second) between the action on the runtime bundle and the data appearing here.

## Implementation

**File:** `com.oop.query.controller.ProcessInstanceController`
