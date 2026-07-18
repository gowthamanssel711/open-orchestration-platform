# Form System

The runtime bundle stores, resolves, and serves form definitions for user tasks.

## How Forms Work

1. Forms are deployed via the `/api/v1/deploy` endpoint (`.form` files)
2. BPMN user tasks reference forms via `activiti:formKey="form-id"`
3. When a task is active, `GET /api/v1/tasks/{taskId}/form` returns the form schema + process variables
4. The frontend renders the form using `@bpmn-io/form-js-viewer`
5. On submit, variables are sent to `POST /api/v1/tasks/{taskId}/complete`

## Get Task Form

```
GET /api/v1/tasks/{taskId}/form
```

**Response (form exists):**
```json
{
  "hasForm": true,
  "taskId": "f7aaca58-81cd-11f1-b84a-00155d010200",
  "formKey": "review-order",
  "schema": "{\"id\":\"review-order\",\"components\":[...],\"schemaVersion\":19}",
  "data": {
    "orderId": "ORD-12345",
    "customerName": "John Doe"
  }
}
```

**Response (no form configured):**
```json
{
  "hasForm": false,
  "taskId": "f7aaca58-81cd-11f1-b84a-00155d010200",
  "taskDefinitionKey": "Task_Review"
}
```

## Form Resolution Order

1. **Database** — Checks `form_definition` table for the latest version matching the formKey
2. **Classpath** — Falls back to `resources/forms/{formKey}.form.json`

Database takes priority, allowing runtime updates without redeployment.

## Form Data (Variables)

The `data` field in the response contains the current process variables for that task. This allows forms to:
- Display read-only data from previous steps
- Pre-fill fields with existing values

## List Deployed Forms

```
GET /api/v1/forms
```

**Response:**
```json
[
  {
    "formKey": "review-order",
    "version": 2,
    "name": "review-order",
    "deployedAt": "2026-07-17T10:00:00.000Z"
  }
]
```

## Get Specific Form

```
GET /api/v1/forms/{formKey}
GET /api/v1/forms/{formKey}?version=1
```

**Response:**
```json
{
  "formKey": "review-order",
  "version": 2,
  "schema": "{...}",
  "deployedAt": "2026-07-17T10:00:00.000Z"
}
```

## Delete Form

```
DELETE /api/v1/forms/{formKey}
```

Removes all versions of the form.

## Database Schema

```sql
CREATE TABLE form_definition (
    id BIGSERIAL PRIMARY KEY,
    form_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(500),
    schema_json TEXT NOT NULL,
    deployed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deployed_by VARCHAR(255),
    UNIQUE (form_key, version)
);
```

## Implementation

**Files:**
- `com.oop.runtime.controller.FormController` — serves form for task
- `com.oop.runtime.controller.DeployController` — handles form deployment
- `com.oop.runtime.model.FormDefinition` — JPA entity
- `com.oop.runtime.repository.FormDefinitionRepository` — data access
