# Form Resolution

The Query Service can serve form schemas for tasks, enabling the frontend to render forms without hitting the runtime bundle.

## Get Task Form

```
GET /api/v1/tasks/{taskId}/form
```

**Response (form exists):**
```json
{
  "hasForm": true,
  "taskId": "f7aaca58-...",
  "formKey": "review-order",
  "schema": "{\"id\":\"review-order\",\"components\":[...]}",
  "data": {}
}
```

**Response (no form):**
```json
{
  "hasForm": false,
  "taskId": "f7aaca58-...",
  "taskName": "Review Order"
}
```

## Resolution Order

1. **Database** — Checks `form_definition` table for the latest version matching the `formKey`
2. **Classpath** — Falls back to `resources/forms/{formKey}.form.json`

## Difference from Runtime Bundle's Form Endpoint

| | Query Service | Runtime Bundle |
|---|---|---|
| `data` field | Always `{}` (no variable access) | Returns actual process variables |
| Source of truth | `form_key` column in task table | `task.getFormKey()` from engine |
| Use case | Gateway routes GETs here | Direct runtime access |

The query service version is sufficient when forms don't need pre-filled data. For forms that require process variables (read-only display fields), configure the gateway to route `GET /api/v1/tasks/*/form` to the runtime bundle instead.

## How formKey Gets Stored

1. Runtime Bundle's `EngineEventPublisher` includes `formKey` in `TASK_CREATED` events
2. Query Service's `TaskEventConsumer.handleTaskCreated()` saves it to the `form_key` column
3. `FormController` reads it from the `TaskEntity`

## How Form Schemas Get Stored

1. When forms are deployed via `/api/v1/deploy`, the runtime bundle publishes `FORM_DEPLOYED`
2. Query Service's `ProcessEventConsumer.handleFormDeployed()` stores the schema in `form_definition` table
3. Alternatively, classpath forms in `resources/forms/` serve as fallback

## Implementation

**File:** `com.oop.query.controller.FormController`
