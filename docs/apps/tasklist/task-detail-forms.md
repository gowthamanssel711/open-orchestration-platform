# Task Detail & Forms

The Task Detail page shows full task information and renders dynamic forms using bpmn.io form-js.

## Page: `/task/:taskId`

### Loading Flow

```
1. Fetch task details:     GET /api/v1/tasks/{taskId}
2. If task is active:      GET /api/v1/tasks/{taskId}/form
3. If form exists:         Render form-js viewer
4. If no form:             Show "Complete Task" button directly
```

### Task Information Displayed

- Task name and ID
- Status badge (color-coded)
- Process Instance ID
- Assignee
- Task Definition Key
- Form Key (if configured)
- Creation time

### Actions

| Condition | Available Actions |
|---|---|
| Unassigned task | Claim |
| Assigned to me, has form | Fill form + Complete, Release |
| Assigned to me, no form | Complete, Release |
| Assigned to other | None (read-only) |
| Completed | None (read-only) |

## Form Rendering

### FormRenderer Component

Wraps `@bpmn-io/form-js-viewer`:

```tsx
import { Form } from '@bpmn-io/form-js-viewer';

// Create form instance
const form = new Form({ container: containerRef.current });

// Import schema + data
await form.importSchema(parsedSchema, data);

// Submit (validates + returns data)
const { errors, data: formData } = form.submit();
```

### Props

| Prop | Type | Description |
|---|---|---|
| `schema` | string (JSON) | Form schema from API |
| `data` | object | Pre-fill data (process variables) |
| `onSubmit` | function | Called with form data on valid submit |
| `onCancel` | function | Called when cancel clicked |
| `submitting` | boolean | Disables buttons during submission |

### Validation

form-js handles validation internally based on the schema:
- `required` fields must be filled
- `minLength` / `maxLength` constraints
- Type validation (number, email, etc.)

On submit, if `errors` object has keys, the form shows inline error messages and `onSubmit` is not called.

### Submission Flow

```
User fills form → clicks "Complete Task"
    │
    ▼
FormRenderer.handleSubmit()
    │ (form.submit() validates)
    ▼
If errors → show inline validation
If valid  → onSubmit(formData)
    │
    ▼
TaskDetailPage.handleFormSubmit()
    │
    ▼
POST /api/v1/tasks/{taskId}/complete
Body: { ...formData }  (form field values become process variables)
    │
    ▼
navigate(-1)  (back to task list)
```

### CSS Integration

form-js requires its own CSS. Loaded in `main.tsx` before Tailwind:

```tsx
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css';
```

Custom overrides in `index.css` restore form element styles that Tailwind's Preflight strips (borders, padding, focus rings).

## Supported Form Components

form-js supports these component types:
- `textfield` — single line text input
- `textarea` — multi-line text
- `number` — numeric input
- `checkbox` — boolean toggle
- `select` — dropdown
- `radio` — radio buttons
- `datetime` — date/time picker
- `taglist` — tag selection
- `text` — static text/markdown display

## No Form Configured

If a task has no `formKey`, or the form definition isn't found:

```json
{ "hasForm": false, "taskId": "...", "taskDefinitionKey": "..." }
```

The UI shows a simple "Complete Task" button that submits empty variables.
