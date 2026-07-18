# Query Service - Technical Reference

Detailed file-by-file, function-by-function documentation.

---

## `QueryServiceApplication.java`

**Package:** `com.oop.query`

Main Spring Boot application entry point.

| Method | Description |
|---|---|
| `main(String[] args)` | Sets `spring.cloud.function.definition` to `engineEventsConsumer;taskEventsConsumer` before bootstrapping. This is required because Spring Cloud Function auto-detection fails when multiple `Consumer` beans exist |

**Why set in main():** If set in `application.yml`, the property may be overridden or ignored depending on profile loading order. Setting it programmatically guarantees it applies.

---

## Config Package

### `SecurityConfig.java`

**Package:** `com.oop.query.config`

Configures Spring Security with OAuth2 Resource Server (JWT).

| Class/Method | Description |
|---|---|
| `SecurityConfig` | `@Configuration` class |
| `filterChain(HttpSecurity)` | All endpoints require authentication, configures JWT resource server |

Simpler than runtime bundle — no role injection needed since query service only reads data.

---

## Consumer Package

### `ProcessEventConsumer.java`

**Package:** `com.oop.query.consumer`

Consumes engine events from RabbitMQ and updates the process instance read model.

| Method | Description |
|---|---|
| `engineEventsConsumer()` | `@Bean` returning `Consumer<Message<byte[]>>`. Parses payload, routes by `type` field |
| `parsePayload(byte[])` | Converts byte array to String, checks if starts with `{`, deserializes to `Map<String, Object>`. Returns `null` for non-JSON (Activiti internal messages) |
| `handleProcessStarted(Map)` | Extracts `processInstanceId`, `processDefinitionKey`, `businessKey`, `initiator` from `data`. Creates `ProcessInstanceEntity` with status `RUNNING`, saves |
| `handleProcessCompleted(Map)` | Finds entity by processInstanceId, sets status `COMPLETED` + `completedAt` |
| `handleProcessCancelled(Map)` | Finds entity by processInstanceId, sets status `CANCELLED` + `completedAt` |
| `handleFormDeployed(Map)` | Extracts `formKey`, `version`, `schema` from `data`. Creates `FormDefinition` entity, saves. This keeps query service's form store in sync with runtime bundle |

**Event routing:**
```java
switch (eventType) {
  case "PROCESS_STARTED" -> handleProcessStarted(payload);
  case "PROCESS_COMPLETED" -> handleProcessCompleted(payload);
  case "PROCESS_CANCELLED" -> handleProcessCancelled(payload);
  case "FORM_DEPLOYED" -> handleFormDeployed(payload);
  default -> log.trace("Unhandled...");
}
```

---

### `TaskEventConsumer.java`

**Package:** `com.oop.query.consumer`

Consumes task events from RabbitMQ and updates the task read model.

| Method | Description |
|---|---|
| `taskEventsConsumer()` | `@Bean` returning `Consumer<Message<byte[]>>`. Parses payload, routes by `type` field |
| `parsePayload(byte[])` | Same as ProcessEventConsumer — handles non-JSON gracefully |
| `handleTaskCreated(Map)` | Extracts `taskId`, `name`, `processInstanceId`, `processDefinitionKey`, `assignee`, `formKey`. Creates `TaskEntity` with status `CREATED`. Saves blank strings as null for `processDefinitionKey` and `formKey` |
| `handleTaskAssigned(Map)` | **Guard:** Returns early if `assignee` is null or blank (safety net for unclaim events that slip through). Finds entity by taskId, sets `assignee` and status `ASSIGNED` |
| `handleTaskReleased(Map)` | Finds entity by taskId, sets `assignee` to `null` and status back to `CREATED`. This handles the unclaim flow |
| `handleTaskCompleted(Map)` | Finds entity by taskId, sets status `COMPLETED` + `completedAt` |

**TASK_ASSIGNED guard:**
```java
if (assignee == null || assignee.isBlank()) {
  log.trace("Ignoring TASK_ASSIGNED with empty assignee for task: {}", taskId);
  return;
}
```

This is a safety net — normally the runtime bundle already converts null-assignee events to `TASK_RELEASED`, but if any slip through (e.g., from Activiti Cloud's own publisher), they're ignored here.

---

## Controller Package

### `ProcessInstanceController.java`

**Package:** `com.oop.query.controller`  
**Path:** `/api/v1/process-instances`

| Method | Endpoint | Description |
|---|---|---|
| `list(String status, String processDefinitionKey, Pageable)` | `GET /` | Returns paginated list. Filter priority: status > processDefinitionKey > all. Default page size: 20 |
| `get(String processInstanceId)` | `GET /{processInstanceId}` | Finds by processInstanceId, returns 404 if not found |

---

### `TaskController.java`

**Package:** `com.oop.query.controller`  
**Path:** `/api/v1/tasks`

| Method | Endpoint | Description |
|---|---|---|
| `list(assignee, status, processInstanceId, processDefinitionKey, Pageable)` | `GET /` | Returns paginated tasks. Filter priority: processInstanceId > processDefinitionKey+status > processDefinitionKey > assignee+status > assignee > status > all. Default page size: 50 |
| `myTasks(Jwt jwt, String statuses, Pageable)` | `GET /my` | Extracts `preferred_username` from JWT. If statuses contains comma, uses `findByAssigneeAndStatusIn`. Otherwise uses `findByAssigneeAndStatus`. Default statuses: `CREATED,ASSIGNED` |
| `get(String taskId)` | `GET /{taskId}` | Finds by taskId field (not DB id), returns 404 if not found |
| `byProcessInstance(String processInstanceId)` | `GET /process-instance/{processInstanceId}` | Returns unpaginated list of all tasks for a process instance |

---

### `FormController.java`

**Package:** `com.oop.query.controller`  
**Path:** `GET /api/v1/tasks/{taskId}/form`

| Method | Description |
|---|---|
| `getTaskForm(String taskId)` | Finds task entity, reads `formKey` from it, resolves schema, returns response. Returns `{hasForm: false}` if no formKey or schema not found |
| `resolveFormSchema(String formKey)` | **Resolution order:** 1) `formDefinitionRepository.findTopByFormKeyOrderByVersionDesc(formKey)` 2) `ClassPathResource("forms/" + formKey + ".form.json")`. Returns schema string or null |

**Difference from runtime bundle:** Returns `"data": {}` (empty) — query service doesn't have access to live process variables. For pre-filled forms, route through runtime bundle instead.

---

## Model Package

### `ProcessInstanceEntity.java`

**Package:** `com.oop.query.model`  
**Table:** `process_instance`

| Field | Column | Type | Description |
|---|---|---|---|
| `id` | `id` | Long (auto) | Primary key |
| `processInstanceId` | `process_instance_id` | String (unique) | Activiti instance ID |
| `processDefinitionKey` | `process_definition_key` | String | Process definition key |
| `processDefinitionId` | `process_definition_id` | String | Full definition ID (key:version:id) |
| `businessKey` | `business_key` | String | Business correlation key |
| `initiator` | `initiator` | String | User who started the process |
| `status` | `status` | String | RUNNING, COMPLETED, CANCELLED |
| `startedAt` | `started_at` | Instant | When process started |
| `completedAt` | `completed_at` | Instant | When process ended (null if running) |

---

### `TaskEntity.java`

**Package:** `com.oop.query.model`  
**Table:** `task`

| Field | Column | Type | Description |
|---|---|---|---|
| `id` | `id` | Long (auto) | Primary key |
| `taskId` | `task_id` | String (unique) | Activiti task ID |
| `name` | `name` | String | Task display name |
| `description` | `description` | String | Task description |
| `processInstanceId` | `process_instance_id` | String | Parent process instance |
| `processDefinitionKey` | `process_definition_key` | String | Process definition key |
| `assignee` | `assignee` | String | Assigned user (null = unassigned) |
| `formKey` | `form_key` | String | Form key for rendering (null = no form) |
| `candidateGroups` | `candidate_groups` | String | Comma-separated candidate groups |
| `status` | `status` | String | CREATED, ASSIGNED, COMPLETED |
| `priority` | `priority` | Integer | Task priority |
| `dueDate` | `due_date` | Instant | Due date |
| `createdAt` | `created_at` | Instant | When task was created |
| `completedAt` | `completed_at` | Instant | When task was completed |

---

### `FormDefinition.java`

**Package:** `com.oop.query.model`  
**Table:** `form_definition`

| Field | Column | Type | Description |
|---|---|---|---|
| `id` | `id` | Long (auto) | Primary key |
| `formKey` | `form_key` | String | Form identifier |
| `version` | `version` | Integer | Version number |
| `name` | `name` | String | Display name |
| `schemaJson` | `schema_json` | String (TEXT) | Full form JSON |
| `deployedAt` | `deployed_at` | Instant | When deployed |

**No `@Lob`** — uses `columnDefinition = "TEXT"` to avoid PostgreSQL large object issues.

---

## Repository Package

### `ProcessInstanceRepository.java`

**Extends:** `JpaRepository<ProcessInstanceEntity, Long>`

| Method | Description |
|---|---|
| `findByProcessInstanceId(String)` | Find by Activiti instance ID |
| `findByStatus(String, Pageable)` | Filter by status (paginated) |
| `findByProcessDefinitionKey(String, Pageable)` | Filter by definition key |

---

### `TaskRepository.java`

**Extends:** `JpaRepository<TaskEntity, Long>`

| Method | Description |
|---|---|
| `findByTaskId(String)` | Find by Activiti task ID |
| `findByAssignee(String, Pageable)` | Tasks by assignee |
| `findByAssigneeAndStatus(String, String, Pageable)` | Tasks by assignee + status |
| `findByAssigneeAndStatusIn(String, List<String>, Pageable)` | Tasks by assignee + multiple statuses |
| `findByStatus(String, Pageable)` | Filter by status |
| `findByProcessInstanceId(String, Pageable)` | Tasks by process instance (paginated) |
| `findByProcessInstanceId(String)` | Tasks by process instance (list) |
| `findByProcessDefinitionKey(String, Pageable)` | Tasks by definition key |
| `findByProcessDefinitionKeyAndStatus(String, String, Pageable)` | Tasks by definition key + status |

---

### `FormDefinitionRepository.java`

**Extends:** `JpaRepository<FormDefinition, Long>`

| Method | Description |
|---|---|
| `findTopByFormKeyOrderByVersionDesc(String)` | Latest version of a form |

---

## Exception Package

### `GlobalExceptionHandler.java`

**Package:** `com.oop.query.exception`

`@ControllerAdvice` — catches unhandled exceptions, logs full stack trace, returns JSON with actual exception class name and message (not generic "Internal Server Error").
