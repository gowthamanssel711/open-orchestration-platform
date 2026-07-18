# OOP (Open Orchestration Platform) - Project Guide

A CQRS-based process orchestration platform built on Activiti Cloud, providing BPMN process execution, task management, and dynamic form rendering.

---

## Architecture Overview

```
┌──────────────┐     ┌──────────────────────────────────────────────────────┐
│   Frontend   │     │                    Backend                            │
│              │     │                                                      │
│  Tasklist UI │────▶│  API Gateway (:8080)                                 │
│  (React)     │     │    │                                                 │
│              │     │    ├── GET  ──▶ Query Service (:8082)                │
│              │     │    └── POST ──▶ Runtime Bundle (:8081)               │
└──────────────┘     │                                                      │
                     │  Runtime Bundle ──▶ RabbitMQ ──▶ Query Service       │
                     │        │                              │               │
                     │        ▼                              ▼               │
                     │   PostgreSQL (rb)             PostgreSQL (query)      │
                     └──────────────────────────────────────────────────────┘
                                        │
                              ┌─────────┴─────────┐
                              │    Keycloak        │
                              │  (:8180) - Auth    │
                              └───────────────────┘
```

**CQRS Pattern:**
- **Writes** (start process, claim/complete task, deploy) → Runtime Bundle
- **Reads** (list tasks, list instances, get form) → Query Service
- **Sync** → Runtime Bundle publishes events to RabbitMQ → Query Service consumes and updates its read model

---

## Services

### 1. API Gateway (`platform/gateway` — port 8080)

The single entry point for all frontend requests. Routes by HTTP method and path.

| Route Pattern | Method | Target |
|---|---|---|
| `/api/v1/tasks/**` | GET | Query Service |
| `/api/v1/tasks/**` | POST/PUT/DELETE | Runtime Bundle |
| `/api/v1/process-instances/**` | GET | Query Service |
| `/api/v1/process-instances/**` | POST | Runtime Bundle |
| `/api/v1/processes/**` | ALL | Runtime Bundle |
| `/api/v1/deploy` | POST | Runtime Bundle |
| `/api/v1/forms/**` | GET | Runtime Bundle |

**Key files:**
```
gateway/
├── src/main/java/com/oop/gateway/
│   ├── GatewayApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java        # JWT validation, CORS
│   │   ├── RateLimiterConfig.java     # Redis-based rate limiting
│   │   └── OpenApiConfig.java
│   ├── filter/
│   │   ├── RequestLoggingFilter.java  # Logs all requests
│   │   ├── ApiKeyAuthFilter.java      # API key auth (optional)
│   │   └── TenantEnrichmentFilter.java
│   └── exception/
│       └── GlobalErrorWebExceptionHandler.java
└── src/main/resources/
    └── application.yml                # Route definitions
```

---

### 2. Runtime Bundle (`platform/runtime-bundle` — port 8081)

The **write side** — BPMN execution engine powered by Activiti Cloud. Handles all mutations: starting processes, claiming/completing tasks, deploying definitions and forms.

**Key files:**
```
runtime-bundle/
├── src/main/java/com/oop/runtime/
│   ├── RuntimeBundleApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          # JWT + ROLE_ACTIVITI_USER injection
│   │   ├── ActivitiIdentityConfig.java  # UserGroupManager for Activiti identity
│   │   ├── ActivitiConfig.java
│   │   ├── EngineEventConfig.java       # Registers event listener with RuntimeService
│   │   └── StreamBindingConfig.java     # RabbitMQ stream bindings
│   ├── controller/
│   │   ├── DeployController.java        # POST /api/v1/deploy (BPMN + forms)
│   │   ├── ProcessController.java       # POST /api/v1/process-instances
│   │   ├── ProcessDefinitionController.java  # GET /api/v1/processes
│   │   ├── TaskController.java          # POST claim/release/complete
│   │   └── FormController.java          # GET /api/v1/tasks/{id}/form
│   ├── event/
│   │   └── EngineEventPublisher.java    # Publishes events to RabbitMQ
│   ├── model/
│   │   └── FormDefinition.java          # JPA entity for deployed forms
│   └── repository/
│       └── FormDefinitionRepository.java
├── src/main/resources/
│   ├── application.yml
│   ├── forms/                           # Classpath form fallback
│   │   ├── review-order.form.json
│   │   └── confirm-delivery.form.json
│   └── db/migration/
│       └── V1__create_form_definition.sql
└── pom.xml
```

**API Endpoints:**

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/deploy` | POST | Deploy BPMN + .form files (multipart) |
| `/api/v1/processes` | GET | List process definitions |
| `/api/v1/process-instances` | POST | Start a process instance |
| `/api/v1/process-instances/{id}` | GET | Get instance details |
| `/api/v1/process-instances/{id}/cancel` | POST | Cancel (delete) instance |
| `/api/v1/process-instances/{id}/suspend` | POST | Suspend instance |
| `/api/v1/process-instances/{id}/resume` | POST | Resume instance |
| `/api/v1/tasks` | GET | List active tasks |
| `/api/v1/tasks/{id}` | GET | Get task details |
| `/api/v1/tasks/{id}/claim?assignee=X` | POST | Claim a task |
| `/api/v1/tasks/{id}/release` | POST | Release (unclaim) a task |
| `/api/v1/tasks/{id}/complete` | POST | Complete task with variables |
| `/api/v1/tasks/{id}/form` | GET | Get form schema + variables |
| `/api/v1/forms` | GET | List deployed forms |
| `/api/v1/forms/{formKey}` | GET | Get specific form by key |

**Event Publishing (RabbitMQ):**

The `EngineEventPublisher` listens to Activiti engine events and publishes:
- `PROCESS_STARTED`, `PROCESS_COMPLETED`, `PROCESS_CANCELLED` → `engineEvents-out-0`
- `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_RELEASED`, `TASK_COMPLETED` → `taskEvents-out-0`
- `FORM_DEPLOYED` → `engineEvents-out-0`

---

### 3. Query Service (`platform/query-service` — port 8082)

The **read side** — optimized denormalized read models. Consumes events from RabbitMQ and maintains its own PostgreSQL tables for fast lookups.

**Key files:**
```
query-service/
├── src/main/java/com/oop/query/
│   ├── QueryServiceApplication.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── consumer/
│   │   ├── ProcessEventConsumer.java    # Handles PROCESS_* + FORM_DEPLOYED events
│   │   └── TaskEventConsumer.java       # Handles TASK_* events
│   ├── controller/
│   │   ├── ProcessInstanceController.java
│   │   ├── TaskController.java
│   │   └── FormController.java          # Returns form schema for task
│   ├── model/
│   │   ├── ProcessInstanceEntity.java
│   │   ├── TaskEntity.java
│   │   └── FormDefinition.java
│   ├── repository/
│   │   ├── ProcessInstanceRepository.java
│   │   ├── TaskRepository.java
│   │   └── FormDefinitionRepository.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   ├── forms/                           # Classpath form fallback
│   └── db/migration/
│       ├── V1__create_query_schema.sql  # process_instance + task tables
│       └── V2__add_forms.sql           # form_key column + form_definition table
└── pom.xml
```

**API Endpoints:**

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/process-instances` | GET | List instances (filter: status, processDefinitionKey) |
| `/api/v1/process-instances/{id}` | GET | Get single instance |
| `/api/v1/tasks` | GET | List tasks (filter: status, assignee, processInstanceId) |
| `/api/v1/tasks/my` | GET | My assigned tasks |
| `/api/v1/tasks/{taskId}` | GET | Get single task |
| `/api/v1/tasks/{taskId}/form` | GET | Get form schema for task |
| `/api/v1/tasks/process-instance/{id}` | GET | Tasks by process instance |

**Database Schema:**
```sql
-- process_instance
process_instance_id, process_definition_key, business_key, initiator, status, started_at, completed_at

-- task
task_id, name, description, process_instance_id, process_definition_key, assignee,
form_key, candidate_groups, status, priority, due_date, created_at, completed_at

-- form_definition
form_key, version, name, schema_json, deployed_at
```

---

### 4. Tasklist UI (`apps/tasklist` — port 5173 dev)

React SPA for task management. Authenticates via Keycloak, communicates through gateway.

**Key files:**
```
tasklist/
├── src/
│   ├── main.tsx                   # Entry point, imports form-js CSS
│   ├── App.tsx                    # Routes + layout + auth
│   ├── api.ts                    # useApiClient hook (Bearer token)
│   ├── index.css                 # Tailwind + form-js overrides
│   ├── pages/
│   │   ├── MyTasksPage.tsx       # Tasks assigned to current user
│   │   ├── AllTasksPage.tsx      # All tasks with status filter
│   │   ├── TaskDetailPage.tsx    # Task detail + form rendering
│   │   └── ProcessInstancesPage.tsx  # List/cancel instances
│   └── components/
│       └── FormRenderer.tsx      # Wraps @bpmn-io/form-js-viewer
├── package.json
├── vite.config.ts
└── tailwind.config.js
```

**Dependencies:**
- `react`, `react-router-dom` — routing/UI
- `@tanstack/react-query` — data fetching
- `@bpmn-io/form-js-viewer` — renders Camunda form-js schemas
- `@oop/auth` — shared Keycloak auth wrapper
- `tailwindcss` — styling

**Pages:**

| Page | Path | Description |
|---|---|---|
| My Tasks | `/` | Tasks assigned to logged-in user (complete, release) |
| All Tasks | `/all` | All tasks with status filter (claim, complete, release) |
| Task Detail | `/task/:taskId` | Full detail view with form rendering |
| Instances | `/instances` | Process instances list with cancel |

---

## Infrastructure (docker-compose.dev.yml)

| Service | Port | Purpose |
|---|---|---|
| PostgreSQL | 5432 | Database for all services |
| RabbitMQ | 5672 / 15672 | Event streaming (CQRS sync) |
| Keycloak | 8180 | OAuth2 / OpenID Connect |
| Redis | 6379 | Rate limiting (gateway) |

**Databases** (created by `deploy/docker/init-databases.sql`):
- `oop_runtime` — Runtime bundle (Activiti engine tables + form_definition)
- `oop_query` — Query service (process_instance, task, form_definition)
- `oop_keycloak` — Keycloak internal

---

## Authentication

- **Provider:** Keycloak (realm: `oop`)
- **Frontend:** `keycloak-js` with client `tasklist-ui`
- **Backend:** All services validate JWT via `spring.security.oauth2.resourceserver.jwt`
- **Key role:** `ROLE_ACTIVITI_USER` — injected by SecurityConfig for all authenticated users
- **Service-to-service:** `client_credentials` grant with `activiti-keycloak` client

---

## Deployment Flow

```bash
# 1. Start infrastructure
docker compose -f docker-compose.dev.yml up -d

# 2. Start backend services (from IDE or CLI)
# Runtime Bundle → port 8081
# Query Service  → port 8082
# Gateway        → port 8080

# 3. Start frontend
cd apps/tasklist && pnpm dev  # → port 5173

# 4. Deploy process + forms
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@my-process.bpmn" \
  -F "files=@my-form.form"

# 5. Start instance
curl -X POST http://localhost:8080/api/v1/process-instances \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionKey": "my-process", "variables": {}}'
```

---

## Form System

Forms use the **bpmn.io form-js** format (same as Camunda Modeler exports).

**Linking form to user task:**
```xml
<bpmn:userTask id="task1" name="Review" activiti:formKey="review-order">
```

The `activiti:formKey` value must match the `"id"` field in the `.form` file:
```json
{
  "id": "review-order",
  "components": [...],
  "schemaVersion": 19
}
```

**Resolution order:** Database (deployed via API) → Classpath fallback (`resources/forms/`)

**Deploy supports:** `.bpmn`, `.bpmn20.xml`, `.form`, `.form.json`

---

## Event Flow (CQRS)

```
User action (start process, claim task, etc.)
    │
    ▼
Runtime Bundle (Activiti engine executes)
    │
    ▼
EngineEventPublisher (listens to engine events)
    │
    ▼
RabbitMQ (engineEvents / taskEvents exchanges)
    │
    ▼
Query Service consumers (ProcessEventConsumer, TaskEventConsumer)
    │
    ▼
PostgreSQL (denormalized read tables)
    │
    ▼
Frontend reads from Query Service via Gateway
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Process Engine | Activiti Cloud 8.4.0 |
| Backend Framework | Spring Boot 3.2.4 |
| API Gateway | Spring Cloud Gateway |
| Message Broker | RabbitMQ (Spring Cloud Stream) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | Keycloak 24 (OAuth2/OIDC) |
| Frontend | React 18 + TypeScript + Vite |
| Form Rendering | @bpmn-io/form-js-viewer |
| Styling | Tailwind CSS |
| Data Fetching | @tanstack/react-query |
| Monorepo | pnpm workspaces + Maven multi-module |
| Java | 21 |
| Build | Maven (backend), Vite (frontend) |

---

## Other Services (Scaffolded, Not Active)

| Service | Purpose |
|---|---|
| `platform/audit-service` | Audit trail for all operations |
| `platform/analytics-service` | Process analytics and reporting |
| `platform/notification-service` | WebSocket/GraphQL notifications |
| `platform/identity-service` | User/group management |
| `platform/connector-service` | External system connectors |
| `apps/operate` | Process operations dashboard |
| `apps/modeler` | BPMN modeler UI |
| `apps/analytics` | Analytics dashboard |
