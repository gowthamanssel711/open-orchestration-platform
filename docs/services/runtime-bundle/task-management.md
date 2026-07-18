# Task Management

Endpoints for working with human tasks in the process engine.

## List Tasks

```
GET /api/v1/tasks
```

**Query Parameters:**
| Param | Type | Description |
|---|---|---|
| `processInstanceId` | string | Filter by process instance |
| `processDefinitionKey` | string | Filter by process definition |
| `assignee` | string | Filter by assignee |
| `status` | string | `ALL` (default), `ACTIVE`, `COMPLETED` |

**Response:**
```json
[
  {
    "id": "f7aaca58-81cd-11f1-b84a-00155d010200",
    "name": "Review Order",
    "assignee": "admin",
    "status": "ACTIVE",
    "createTime": "2026-07-17T10:55:04.000Z",
    "processInstanceId": "f7aa2e14-81cd-11f1-b84a-00155d010200",
    "processDefinitionId": "order-process:1:abc123",
    "taskDefinitionKey": "Task_Review",
    "formKey": "review-order",
    "priority": 50,
    "dueDate": null
  }
]
```

## Get Single Task

```
GET /api/v1/tasks/{taskId}
```

Checks active tasks first, then history. Returns task details.

## Claim Task

```
POST /api/v1/tasks/{taskId}/claim?assignee=username
```

Assigns the task to the specified user. Only unassigned tasks can be claimed.

**Response:** Returns updated task object with `assignee` set.

## Release Task

```
POST /api/v1/tasks/{taskId}/release
```

Removes the assignee (unclaim). Task goes back to the pool.

**Response:** Returns updated task object with `assignee: null`.

**Event behavior:** Activiti fires a `TASK_ASSIGNED` event with null assignee on unclaim. The `EngineEventPublisher` converts this to a `TASK_RELEASED` event so the query service can properly reset the status.

## Complete Task

```
POST /api/v1/tasks/{taskId}/complete
Content-Type: application/json
```

**Request body (optional variables):**
```json
{
  "reviewDecision": "approved",
  "reviewNotes": "Looks good, approved for shipment"
}
```

Variables submitted here are stored as process variables and available in subsequent tasks/service tasks.

**Response:**
```json
{
  "status": "COMPLETED",
  "taskId": "f7aaca58-81cd-11f1-b84a-00155d010200"
}
```

## Task Status Flow

```
CREATED → (claim) → ASSIGNED → (complete) → COMPLETED
                  ↕
              (release) → CREATED
```

## Events Published

| Action | Event Type | Channel |
|---|---|---|
| Task created by engine | `TASK_CREATED` | taskEvents-out-0 |
| Task claimed | `TASK_ASSIGNED` | taskEvents-out-0 |
| Task released | `TASK_RELEASED` | taskEvents-out-0 |
| Task completed | `TASK_COMPLETED` | taskEvents-out-0 |

## Implementation

**File:** `com.oop.runtime.controller.TaskController`

Uses Activiti's engine-level `TaskService` and `HistoryService` for both active and completed task queries.
