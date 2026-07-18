# Deployment API

Unified endpoint to deploy BPMN process definitions and form schemas in a single request.

## Endpoint

```
POST /api/v1/deploy
Content-Type: multipart/form-data
```

## How It Works

1. Accepts multiple files in a single request
2. Detects file type by extension:
   - `.bpmn`, `.bpmn20.xml` → Activiti process deployment
   - `.form`, `.form.json` → Form definition stored in database
   - Other extensions → Skipped (reported in response)
3. BPMN files are deployed together in one Activiti deployment
4. Form files are versioned individually (auto-increments version per formKey)
5. Each deployed form publishes a `FORM_DEPLOYED` event to RabbitMQ

## Request Example

```bash
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@order-process.bpmn" \
  -F "files=@review-order.form" \
  -F "files=@confirm-delivery.form"
```

## Response Example

```json
{
  "processes": [
    {
      "type": "process",
      "file": "order-process.bpmn",
      "deploymentId": "abc-123",
      "deployedAt": "2026-07-17T10:00:00.000Z"
    }
  ],
  "forms": [
    {
      "type": "form",
      "file": "review-order.form",
      "formKey": "review-order",
      "version": 1
    },
    {
      "type": "form",
      "file": "confirm-delivery.form",
      "formKey": "confirm-delivery",
      "version": 1
    }
  ],
  "summary": {
    "processCount": 1,
    "formCount": 2,
    "skippedCount": 0
  }
}
```

## Form File Format

Form files follow the bpmn.io form-js schema (exported by Camunda Modeler):

```json
{
  "id": "review-order",
  "components": [
    {
      "label": "Customer Name",
      "type": "textfield",
      "key": "customerName",
      "id": "Field_1"
    }
  ],
  "type": "default",
  "schemaVersion": 19
}
```

The `"id"` field becomes the `formKey` used to link the form to a BPMN user task.

## Linking Form to User Task

In your BPMN file, add `activiti:formKey` matching the form's `"id"`:

```xml
<bpmn:userTask id="task1" name="Review Order" activiti:formKey="review-order">
```

## Versioning

- Each form deployment auto-increments the version for that formKey
- When resolving forms, the latest version is always returned
- Previous versions can be queried via `GET /api/v1/forms/{formKey}?version=1`

## Error Handling

- If a form file is missing the `"id"` field, it's reported as an error in the response (not deployed)
- If BPMN XML is invalid, the entire BPMN deployment fails (forms still deploy)
- Files with unrecognized extensions are skipped and listed in `"skipped"`

## Implementation

**File:** `com.oop.runtime.controller.DeployController`

The controller:
1. Separates files by extension
2. Deploys BPMN files via Activiti `RepositoryService.createDeployment()`
3. Parses form JSON, extracts `id` as formKey, saves to `form_definition` table
4. Publishes `FORM_DEPLOYED` event so query service stays in sync
