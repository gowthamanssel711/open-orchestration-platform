# Task Queries

Query endpoints for tasks with various filters.

## List All Tasks

```
GET /api/v1/tasks
```

**Query Parameters:**
| Param | Type | Description |
|---|---|---|
| `status` | string | Filter: `CREATED`, `ASSIGNED`, `COMPLETED` |
| `assignee` | string | Filter by assignee username |
| `processInstanceId` | string | Filter by process instance |
| `processDefinitionKey` | string | Filter by process definition |
| `page` | int | Page number (0-based) |
| `size` | int | Page size (default: 50) |

**Response (paginated):**
```json
{
  "content": [
    {
      "id": 15,
      "taskId": "f7aaca58-81cd-11f1-b84a-00155d010200",
      "name": "Review Order",
      "description": null,
      "processInstanceId": "f7aa2e14-81cd-11f1-b84a-00155d010200",
      "processDefinitionKey": "order-process",
      "assignee": "admin",
      "formKey": "review-order",
      "candidateGroups": null,
      "status": "ASSIGNED",
      "priority": null,
      "dueDate": null,
      "createdAt": "2026-07-17T10:55:04.868Z",
      "completedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 50,
  "number": 0
}
```

## My Tasks

```
GET /api/v1/tasks/my
```

Returns tasks assigned to the currently authenticated user. Extracts username from JWT `preferred_username` claim.

**Query Parameters:**
| Param | Type | Default | Description |
|---|---|---|---|
| `statuses` | string | `CREATED,ASSIGNED` | Comma-separated status filter |

## Get Single Task

```
GET /api/v1/tasks/{taskId}
```

Returns `404` if not found.

## Tasks by Process Instance

```
GET /api/v1/tasks/process-instance/{processInstanceId}
```

Returns all tasks (unpaginated list) for a specific process instance.

## Filter Priority

When multiple filters are provided, they apply in this priority:
1. `processInstanceId` (most specific)
2. `processDefinitionKey` + `status`
3. `processDefinitionKey`
4. `assignee` + `status`
5. `assignee`
6. `status`
7. No filter (all tasks)

## Task Status Values

| Status | Meaning |
|---|---|
| `CREATED` | Task exists, unassigned |
| `ASSIGNED` | Task is claimed by a user |
| `COMPLETED` | Task has been completed |

## Implementation

**File:** `com.oop.query.controller.TaskController`
