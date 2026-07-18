# Task Views

The Tasklist UI has two main task listing pages.

## My Tasks Page (`/`)

Shows tasks assigned to the currently logged-in user.

**API:** `GET /api/v1/tasks/my`

**Features:**
- Auto-fetches on login (when `user.username` is available)
- Shows task name, process instance, creation time
- **Complete** — completes the task (removes from list)
- **Release** — unclaims the task (removes from list)
- Click task name → navigates to task detail page
- Refresh button for manual re-fetch

**Actions available:**
| Action | When | Effect |
|---|---|---|
| Complete | Always | Completes task with empty variables |
| Release | Always | Unclaims task, returns to pool |

## All Tasks Page (`/all`)

Shows all tasks with status filter.

**API:** `GET /api/v1/tasks` with optional `?status=` param

**Features:**
- Status filter buttons: ALL, CREATED, ASSIGNED, COMPLETED
- Table with: Task name, Status badge, Assignee, Process ID, Created time, Actions
- Click task name → navigates to task detail page
- Refresh button

**Actions available (contextual):**
| Status | Assignee | Actions |
|---|---|---|
| CREATED | None | Claim |
| ASSIGNED | Current user | Complete, Release |
| ASSIGNED | Other user | None |
| COMPLETED | — | None |

## Response Handling

Both pages handle paginated Spring responses:

```typescript
const data = await apiFetch(`/api/v1/tasks${params}`);
setTasks(data.content ?? data);
```

`data.content` extracts the array from Spring's `Page` object. Falls back to `data` if it's already an array.

## Optimistic Updates

- **My Tasks:** Remove task from list immediately after complete/release (don't wait for refetch)
- **All Tasks:** Full refetch after actions (shows updated status)

## Navigation to Detail

Both pages make task names clickable links to the detail page:

```tsx
<button onClick={() => navigate(`/task/${task.taskId}`)}>
  {task.name || task.taskId}
</button>
```
