# Tasklist UI - Technical Reference

Detailed file-by-file, function-by-function documentation.

---

## `main.tsx`

Entry point. Mounts the React app.

| Import | Purpose |
|---|---|
| `@bpmn-io/form-js-viewer/dist/assets/form-js.css` | Loaded **before** index.css so form-js styles apply first, then Tailwind overrides |
| `./index.css` | Tailwind base + form-js element fixes |

```tsx
createRoot(document.getElementById('root')!).render(
  <StrictMode><App /></StrictMode>
);
```

---

## `App.tsx`

Application shell — auth, routing, layout.

| Component/Function | Description |
|---|---|
| `keycloak` | Singleton keycloak-js instance created with `clientId: 'tasklist-ui'` |
| `queryClient` | TanStack Query client with `staleTime: 3000ms`, `retry: 1` |
| `App()` | Root component — wraps with `AuthProvider` → `AuthGuard` → `QueryClientProvider` → `BrowserRouter` → `AppLayout` |
| `AppLayout()` | Sidebar + main content area. Contains navigation items and route definitions |

**Routes:**
| Path | Component |
|---|---|
| `/` | `MyTasksPage` |
| `/all` | `AllTasksPage` |
| `/instances` | `ProcessInstancesPage` |
| `/task/:taskId` | `TaskDetailPage` |

**Nav items:** My Tasks, All Tasks, Instances

**Sidebar:** Shows current user (username, email) from `useAuth()` + logout button.

---

## `api.ts`

API client hook with automatic Bearer token injection.

| Export | Description |
|---|---|
| `useApiClient()` | Hook returning `{ apiFetch }` |
| `apiFetch(path, options)` | Fetches `${API_BASE}${path}` with Authorization header. Throws on non-OK response. Returns parsed JSON |

**API_BASE:** `import.meta.env.VITE_API_URL || 'http://localhost:8080'`

**Headers always set:**
- `Content-Type: application/json`
- `Authorization: Bearer ${token}` (if token exists)

**Error handling:** Throws `Error(`API error: ${status} ${statusText}`)` on non-2xx response.

---

## `index.css`

Tailwind CSS + form-js override styles.

| Section | Purpose |
|---|---|
| `@tailwind base/components/utilities` | Tailwind framework |
| `.fjs-container *` | box-sizing reset for form-js container |
| `.fjs-container input/textarea/select` | Restores borders, padding, border-radius that Tailwind's Preflight strips. Sets consistent font-size (14px), line-height, colors |
| `.fjs-container input[type="checkbox"]` | Prevents checkbox from being full-width |
| `.fjs-container input:focus` | Blue focus ring (`#0f62fe`) matching form-js design system |
| `.fjs-container label` | Consistent label styling (0.875rem, medium weight, gray) |

**Why needed:** Tailwind's Preflight resets all form elements to borderless/unstyled. form-js expects native browser styles. These overrides restore what form-js needs.

---

## Pages

### `MyTasksPage.tsx`

Tasks assigned to the current user.

| Function/State | Description |
|---|---|
| `navigate` | From `useNavigate()` — for task detail navigation |
| `user` | From `useAuth()` — current user |
| `apiFetch` | From `useApiClient()` — API calls |
| `tasks` | State: array of Task objects |
| `isLoading` | State: loading indicator |
| `actionId` | State: task ID currently being acted on (disables buttons) |
| `useEffect([user?.username])` | Auto-fetches tasks when user is available |
| `fetchMyTasks()` | `GET /api/v1/tasks/my`. Sets tasks from `data.content ?? data` |
| `completeTask(taskId)` | `POST /api/v1/tasks/${taskId}/complete` with `{}` body. Optimistically removes task from list |
| `releaseTask(taskId)` | `POST /api/v1/tasks/${taskId}/release`. Optimistically removes from list |

**Task name is clickable** — navigates to `/task/${task.taskId}`.

---

### `AllTasksPage.tsx`

All tasks with status filter.

| Function/State | Description |
|---|---|
| `navigate` | For task detail navigation |
| `statusFilter` | State: `ALL | CREATED | ASSIGNED | COMPLETED` |
| `useEffect([statusFilter])` | Refetches when filter changes |
| `fetchTasks()` | `GET /api/v1/tasks?status=${filter}` (omits param for ALL) |
| `claimTask(taskId)` | `POST /api/v1/tasks/${taskId}/claim?assignee=${username}`. Refetches all |
| `releaseTask(taskId)` | `POST /api/v1/tasks/${taskId}/release`. Refetches all |
| `completeTask(taskId)` | `POST /api/v1/tasks/${taskId}/complete` with `{}`. Refetches all |

**Action visibility logic:**
- `CREATED` + no assignee → Show "Claim"
- `ASSIGNED` + current user → Show "Complete" + "Release"
- Otherwise → No actions

**Task name is clickable** — navigates to `/task/${task.taskId}`.

---

### `TaskDetailPage.tsx`

Full task detail with form rendering.

| Function/State | Description |
|---|---|
| `taskId` | From `useParams()` |
| `task` | State: task detail object |
| `form` | State: `{hasForm, schema, data, formKey}` or null |
| `loading, submitting, error` | UI states |
| `actionId` | Tracks claim/release actions |
| `useEffect([taskId])` | Loads task + form on mount |
| `loadTask()` | Fetches `GET /api/v1/tasks/${taskId}` then `GET /api/v1/tasks/${taskId}/form`. Catches form fetch errors gracefully |
| `handleFormSubmit(formData)` | `POST /api/v1/tasks/${taskId}/complete` with form data as body. Navigates back on success |
| `handleCompleteWithoutForm()` | `POST /api/v1/tasks/${taskId}/complete` with `{}`. For tasks without forms |
| `claimTask()` | `POST /api/v1/tasks/${taskId}/claim?assignee=${username}`. Reloads task |
| `releaseTask()` | `POST /api/v1/tasks/${taskId}/release`. Reloads task |

**Rendering logic:**
```
if loading → spinner
if error && !task → error message + back button
if unassigned → "Claim" prompt
if assigned to me + has form → <FormRenderer>
if assigned to me + no form → "Complete Task" button
if assigned to other → read-only message
if completed → "completed" message
```

---

### `ProcessInstancesPage.tsx`

Process instance list with cancel.

| Function/State | Description |
|---|---|
| `statusFilter` | State: `ALL | RUNNING | COMPLETED | CANCELLED` |
| `useEffect([statusFilter])` | Refetches when filter changes |
| `fetchInstances()` | `GET /api/v1/process-instances?status=${filter}` |
| `cancelInstance(processInstanceId)` | Shows `confirm()` dialog. On confirm: `POST /api/v1/process-instances/${id}/cancel`. **Optimistic update:** immediately sets local status to `CANCELLED` (button disappears) |

**Cancel button only shown** when `pi.status === 'RUNNING'`.

---

## Components

### `FormRenderer.tsx`

Wrapper around `@bpmn-io/form-js-viewer`'s `Form` class.

| Prop | Type | Description |
|---|---|---|
| `schema` | string | Form JSON schema (stringified) |
| `data` | Record<string, unknown> | Pre-fill data for form fields |
| `onSubmit` | function | Called with validated form data |
| `onCancel` | function | Called when cancel clicked |
| `submitting` | boolean | Disables buttons |

| Internal | Description |
|---|---|
| `containerRef` | Ref to the DOM div where form-js renders |
| `formRef` | Ref to the `Form` instance (for submit/destroy) |
| `error` | State for form load errors |

**Lifecycle:**
```
useEffect([schema, data]):
  1. Create Form instance: new Form({ container: containerRef.current })
  2. Parse schema (string → object)
  3. Import: form.importSchema(parsed, data)
  4. Cleanup: form.destroy()
```

**Submit flow:**
```
handleSubmit():
  1. form.submit() → { errors, data }
  2. If errors has keys → return (form shows inline errors)
  3. If valid → call onSubmit(data)
```

**Error display:** If `importSchema` fails, shows error message with close button.

**CSS class:** Container div has `fjs-container` class for CSS targeting.
