# Process Instances

The Instances page lists all process instances and allows cancelling running ones.

## Page: `/instances`

### API

- **List:** `GET /api/v1/process-instances` (routed to Query Service)
- **Cancel:** `POST /api/v1/process-instances/{id}/cancel` (routed to Runtime Bundle)

### Features

- Status filter: ALL, RUNNING, COMPLETED, CANCELLED
- Table with: Instance ID, Definition Key, Status, Initiator, Started time, Actions
- Cancel button on RUNNING instances (with confirmation dialog)
- Refresh button

### Status Colors

| Status | Color |
|---|---|
| RUNNING | Blue |
| COMPLETED | Green |
| CANCELLED | Red |

### Cancel Flow

```
User clicks "Cancel"
    │
    ▼
confirm() dialog: "Cancel process instance {id}?"
    │ (user confirms)
    ▼
POST /api/v1/process-instances/{id}/cancel
    │
    ▼
Optimistic update: set status to 'CANCELLED' in local state
(button disappears immediately)
```

### Instance ID Display

Instance IDs are UUIDs — displayed truncated in the table:

```tsx
{pi.processInstanceId.substring(0, 8)}...
```

### Response Handling

Handles paginated Spring response:

```typescript
const data = await apiFetch(`/api/v1/process-instances${params}`);
setInstances(data.content ?? data);
```
