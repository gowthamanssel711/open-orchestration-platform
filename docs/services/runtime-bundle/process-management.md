# Process Management

Endpoints for managing process instance lifecycle.

## Start Process Instance

```
POST /api/v1/process-instances
```

**Request:**
```json
{
  "processDefinitionKey": "order-process",
  "variables": {
    "orderId": "ORD-12345",
    "customerName": "John Doe",
    "orderAmount": 150.00
  }
}
```

**Response (201):**
```json
{
  "id": "f7aa2e14-81cd-11f1-b84a-00155d010200",
  "processDefinitionId": "order-process:1:abc123",
  "processDefinitionKey": "order-process",
  "suspended": false
}
```

Variables passed here become available in the process execution and can pre-fill form fields.

## Get Process Instance

```
GET /api/v1/process-instances/{id}
```

**Response:**
```json
{
  "id": "f7aa2e14-81cd-11f1-b84a-00155d010200",
  "processDefinitionId": "order-process:1:abc123",
  "processDefinitionKey": "order-process",
  "suspended": false
}
```

Returns `404` if instance not found (may have been completed or cancelled).

## Cancel Process Instance

```
POST /api/v1/process-instances/{id}/cancel
```

Terminates the process immediately. All active tasks are removed. Publishes `PROCESS_CANCELLED` event.

**Response:** `204 No Content`

## Suspend Process Instance

```
POST /api/v1/process-instances/{id}/suspend
```

Pauses the process. No tasks can be completed while suspended.

**Response:**
```json
{
  "status": "SUSPENDED",
  "id": "f7aa2e14-81cd-11f1-b84a-00155d010200"
}
```

## Resume Process Instance

```
POST /api/v1/process-instances/{id}/resume
```

Resumes a previously suspended process.

**Response:**
```json
{
  "status": "ACTIVE",
  "id": "f7aa2e14-81cd-11f1-b84a-00155d010200"
}
```

## List Process Definitions

```
GET /api/v1/processes
```

Returns all deployed process definitions (from Activiti Cloud ProcessRuntime).

**Response:**
```json
[
  {
    "id": "order-process:1:abc123",
    "key": "order-process",
    "name": "Order Fulfillment",
    "version": 1
  }
]
```

## Events Published

| Action | Event Type | Channel |
|---|---|---|
| Start | `PROCESS_STARTED` | engineEvents-out-0 |
| Complete (auto) | `PROCESS_COMPLETED` | engineEvents-out-0 |
| Cancel | `PROCESS_CANCELLED` | engineEvents-out-0 |

## Implementation

**File:** `com.oop.runtime.controller.ProcessController`

Uses Activiti's engine-level `RuntimeService` directly (not Cloud `ProcessRuntime`) to avoid application-level filtering issues.
