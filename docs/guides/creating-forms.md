# Creating Forms

How to create form definitions for user tasks using the bpmn.io form-js format.

## Tools

- **Camunda Modeler** (desktop app) — visual form editor
- **Manual** — write JSON directly

## Form File Structure

```json
{
  "id": "my-form-key",
  "components": [
    { "type": "textfield", "key": "fieldName", "label": "Field Label" }
  ],
  "type": "default",
  "schemaVersion": 19,
  "exporter": {
    "name": "Camunda Modeler",
    "version": "5.42.0"
  },
  "executionPlatform": "Camunda Platform",
  "executionPlatformVersion": "7.24.0"
}
```

### Required Fields

| Field | Purpose |
|---|---|
| `id` | Form key — used to link to BPMN user tasks |
| `components` | Array of form field definitions |
| `type` | Always `"default"` |
| `schemaVersion` | Form-js schema version (use `19`) |

## Component Types

### Text Field

```json
{
  "type": "textfield",
  "key": "customerName",
  "label": "Customer Name",
  "id": "Field_1",
  "validate": {
    "required": true,
    "minLength": 2,
    "maxLength": 100
  }
}
```

### Number

```json
{
  "type": "number",
  "key": "orderAmount",
  "label": "Order Amount",
  "id": "Field_2",
  "validate": { "required": true, "min": 0 }
}
```

### Text Area

```json
{
  "type": "textarea",
  "key": "notes",
  "label": "Notes",
  "id": "Field_3"
}
```

### Checkbox

```json
{
  "type": "checkbox",
  "key": "confirmed",
  "label": "I confirm this is correct",
  "id": "Field_4",
  "validate": { "required": true }
}
```

### Select (Dropdown)

```json
{
  "type": "select",
  "key": "decision",
  "label": "Decision",
  "id": "Field_5",
  "values": [
    { "label": "Approve", "value": "approved" },
    { "label": "Reject", "value": "rejected" }
  ],
  "validate": { "required": true }
}
```

### Date/Time

```json
{
  "type": "datetime",
  "key": "dueDate",
  "label": "Due Date",
  "id": "Field_6",
  "subtype": "date"
}
```

### Static Text (Display Only)

```json
{
  "type": "text",
  "text": "## Instructions\nPlease review the details below.",
  "id": "Text_1"
}
```

### Tag List

```json
{
  "type": "taglist",
  "key": "tags",
  "label": "Tags",
  "id": "Field_7",
  "values": [
    { "label": "Urgent", "value": "urgent" },
    { "label": "VIP", "value": "vip" }
  ]
}
```

## Read-Only Fields

Add `"disabled": true` to display data without allowing edits:

```json
{
  "type": "textfield",
  "key": "orderId",
  "label": "Order ID",
  "id": "Field_1",
  "disabled": true
}
```

These show pre-filled data from process variables.

## Layout

Use `layout.row` to group fields on the same row:

```json
{
  "type": "textfield",
  "key": "firstName",
  "label": "First Name",
  "layout": { "row": "Row_1", "columns": null }
},
{
  "type": "textfield",
  "key": "lastName",
  "label": "Last Name",
  "layout": { "row": "Row_1", "columns": null }
}
```

## Linking to BPMN

In your BPMN file, add the `activiti` namespace and `formKey`:

```xml
<bpmn:definitions xmlns:activiti="http://activiti.org/bpmn" ...>
  <bpmn:userTask id="task1" name="Review" activiti:formKey="my-form-key">
  </bpmn:userTask>
</bpmn:definitions>
```

The `activiti:formKey` value **must match** the form's `"id"` field.

## Data Flow

```
Process Variables (set at start or by previous tasks)
    │
    ▼
GET /api/v1/tasks/{id}/form → returns { schema, data: {variables} }
    │
    ▼
form-js renders form, pre-fills "disabled" fields with variable values
    │
    ▼
User fills editable fields, submits
    │
    ▼
POST /api/v1/tasks/{id}/complete → body = form field values
    │
    ▼
Field values stored as process variables for next steps
```

## Example: Complete Form

```json
{
  "id": "order-review",
  "components": [
    {
      "type": "text",
      "text": "## Review Order",
      "id": "heading"
    },
    {
      "type": "textfield",
      "key": "orderId",
      "label": "Order ID",
      "id": "Field_1",
      "disabled": true,
      "layout": { "row": "Row_1", "columns": null }
    },
    {
      "type": "number",
      "key": "amount",
      "label": "Amount",
      "id": "Field_2",
      "disabled": true,
      "layout": { "row": "Row_1", "columns": null }
    },
    {
      "type": "select",
      "key": "decision",
      "label": "Decision",
      "id": "Field_3",
      "values": [
        { "label": "Approve", "value": "approved" },
        { "label": "Reject", "value": "rejected" }
      ],
      "validate": { "required": true }
    },
    {
      "type": "textarea",
      "key": "reason",
      "label": "Reason",
      "id": "Field_4",
      "validate": { "required": true, "minLength": 10 }
    }
  ],
  "type": "default",
  "schemaVersion": 19
}
```

## Deploying

Save as `.form` file and deploy:

```bash
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@order-review.form"
```
