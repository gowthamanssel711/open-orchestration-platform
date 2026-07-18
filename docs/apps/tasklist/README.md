# Tasklist UI

React single-page application for task management — claim, complete, and manage human tasks with dynamic form rendering.

## Purpose

- View and manage tasks assigned to the current user
- Browse and claim available tasks
- Render dynamic forms (bpmn.io form-js) for task completion
- View and cancel process instances

## Technology

| Component | Technology |
|---|---|
| Framework | React 18 |
| Language | TypeScript |
| Build Tool | Vite |
| Styling | Tailwind CSS |
| Data Fetching | @tanstack/react-query |
| Form Rendering | @bpmn-io/form-js-viewer |
| Auth | Keycloak (via @oop/auth) |
| Routing | react-router-dom v6 |

## Port

`5173` (Vite dev server)

## Running

```bash
cd apps/tasklist
pnpm dev
```

Open http://localhost:5173 — redirects to Keycloak login.

## Project Structure

```
apps/tasklist/
├── src/
│   ├── main.tsx                        # Entry point
│   ├── App.tsx                         # Routes, layout, auth wrapper
│   ├── api.ts                          # API client hook (Bearer token)
│   ├── index.css                       # Tailwind + form-js overrides
│   ├── pages/
│   │   ├── MyTasksPage.tsx             # Tasks assigned to me
│   │   ├── AllTasksPage.tsx            # All tasks with filters
│   │   ├── TaskDetailPage.tsx          # Task detail + form
│   │   └── ProcessInstancesPage.tsx    # Instance list + cancel
│   └── components/
│       └── FormRenderer.tsx            # form-js wrapper component
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── postcss.config.js
```

## Features

- [Authentication](authentication.md) — Keycloak integration
- [Task Views](task-views.md) — My Tasks, All Tasks, filters
- [Task Detail & Forms](task-detail-forms.md) — Form rendering with form-js
- [Process Instances](process-instances.md) — List and cancel instances

## API Base

Configured via environment variable or defaults to gateway:

```typescript
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';
```

All requests go through the gateway which routes to the correct backend service.

## Shared Packages

| Package | Location | Purpose |
|---|---|---|
| `@oop/auth` | `apps/shared/auth` | Keycloak wrapper (AuthProvider, AuthGuard, useAuth) |
| `@oop/design-system` | `apps/shared/design-system` | Shared UI components |
