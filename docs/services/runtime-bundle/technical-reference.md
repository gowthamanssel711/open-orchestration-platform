# Runtime Bundle - Technical Reference

Detailed file-by-file, function-by-function documentation.

---

## `RuntimeBundleApplication.java`

**Package:** `com.oop.runtime`

Main Spring Boot application entry point.

| Method | Description |
|---|---|
| `main(String[] args)` | Bootstraps the Spring Boot application |

---

## Config Package

### `SecurityConfig.java`

**Package:** `com.oop.runtime.config`

Configures Spring Security with OAuth2 Resource Server (JWT).

| Class/Method | Description |
|---|---|
| `SecurityConfig` | `@Configuration` class |
| `filterChain(HttpSecurity)` | Defines security filter chain — all endpoints require authentication, configures JWT resource server with custom role converter |
| `KeycloakRealmRoleConverter` | Inner class implementing `Converter<Jwt, Collection<GrantedAuthority>>` |
| `KeycloakRealmRoleConverter.convert(Jwt)` | Extracts `realm_access.roles` from JWT payload, maps each to `ROLE_` prefixed authority, always injects `ROLE_ACTIVITI_USER` |

**Why `ROLE_ACTIVITI_USER` is always added:** Activiti Cloud's `ProcessRuntime` and `TaskRuntime` check for this role. Without it, all API calls return 403.

---

### `ActivitiIdentityConfig.java`

**Package:** `com.oop.runtime.config`

Bridges Keycloak JWT identity into Activiti's internal identity system.

| Class/Method | Description |
|---|---|
| `ActivitiIdentityConfig` | `@Configuration` class |
| `userGroupManager()` | `@Bean` — returns a `UserGroupManager` implementation that reads username from JWT `preferred_username` claim |
| `UserGroupManager.getUserGroups(String)` | Returns groups for a user — always includes `ACTIVITI_USER`, adds `ACTIVITI_ADMIN` for admin users |
| `UserGroupManager.getUserRoles(String)` | Returns roles extracted from the security context |

**Why needed:** Activiti Cloud uses `UserGroupManager` to determine what processes/tasks a user can see. Without this bean, the engine has no identity context.

---

### `ActivitiConfig.java`

**Package:** `com.oop.runtime.config`

General Activiti engine configuration.

| Class/Method | Description |
|---|---|
| `ActivitiConfig` | `@Configuration` class for engine-level settings |

---

### `EngineEventConfig.java`

**Package:** `com.oop.runtime.config`

Registers the event publisher with the Activiti engine.

| Class/Method | Description |
|---|---|
| `EngineEventConfig` | `@Configuration` class |
| `runtimeService` | Injected `RuntimeService` |
| `engineEventPublisher` | Injected `EngineEventPublisher` |
| `@PostConstruct registerListener()` | Calls `runtimeService.addEventListener(engineEventPublisher)` to register our custom listener for all engine events |

**Why `@PostConstruct`:** The listener must be registered after the engine is initialized. Spring ensures this runs after bean construction.

---

### `StreamBindingConfig.java`

**Package:** `com.oop.runtime.config`

Configures Spring Cloud Stream bindings for RabbitMQ.

| Class/Method | Description |
|---|---|
| `StreamBindingConfig` | `@Configuration` class for stream binding customization |

---

## Controller Package

### `DeployController.java`

**Package:** `com.oop.runtime.controller`  
**Path:** `POST /api/v1/deploy`

Unified deployment endpoint for BPMN and form files.

| Method | Description |
|---|---|
| `deploy(MultipartFile[] files, Jwt jwt)` | Main handler — separates files by extension, deploys each type, returns summary |
| `deployBpmn(List<MultipartFile>)` | Creates a single Activiti deployment containing all BPMN files. Uses `RepositoryService.createDeployment().addInputStream()` for each file. Returns list of deployment results |
| `deployForms(List<MultipartFile>, String deployedBy)` | For each form file: parses JSON, extracts `"id"` as formKey, saves to DB with auto-incremented version, publishes `FORM_DEPLOYED` event. Returns list of form results |
| `publishFormDeployedEvent(FormDefinition)` | Sends `FORM_DEPLOYED` event via `StreamBridge` to `engineEvents-out-0` containing formKey, version, and full schema JSON |

**File type detection logic:**
```java
if (filename.endsWith(".bpmn") || filename.endsWith(".bpmn20.xml")) → bpmnFiles
else if (filename.endsWith(".form") || filename.endsWith(".form.json")) → formFiles
else → skipped
```

---

### `ProcessController.java`

**Package:** `com.oop.runtime.controller`  
**Path:** `/api/v1/process-instances`

Process instance lifecycle management.

| Method | Endpoint | Description |
|---|---|---|
| `startProcess(StartProcessRequest)` | `POST /process-instances` | Calls `runtimeService.startProcessInstanceByKey(key, variables)`. Logs security context for debugging. Returns instance ID and metadata |
| `getProcessInstance(String id)` | `GET /process-instances/{id}` | Queries `runtimeService.createProcessInstanceQuery().processInstanceId(id)`. Returns 404 if not found |
| `suspendProcess(String id)` | `POST /process-instances/{id}/suspend` | Calls `runtimeService.suspendProcessInstanceById(id)` |
| `resumeProcess(String id)` | `POST /process-instances/{id}/resume` | Calls `runtimeService.activateProcessInstanceById(id)` |
| `cancelProcess(String id)` | `POST /process-instances/{id}/cancel` | Calls `runtimeService.deleteProcessInstance(id, "Cancelled via API")`. Returns 204 No Content |

**Inner record:** `StartProcessRequest(String processDefinitionKey, Map<String, Object> variables)`

---

### `ProcessDefinitionController.java`

**Package:** `com.oop.runtime.controller`  
**Path:** `/api/v1/processes`

Lists deployed process definitions.

| Method | Endpoint | Description |
|---|---|---|
| `listDefinitions()` | `GET /processes` | Uses Cloud API `ProcessRuntime.processDefinitions(Pageable.of(0, 100))`. Returns id, key, name, version for each definition |

---

### `TaskController.java`

**Package:** `com.oop.runtime.controller`  
**Path:** `/api/v1/tasks`

Task operations (claim, release, complete, list).

| Method | Endpoint | Description |
|---|---|---|
| `listTasks(processInstanceId, processDefinitionKey, assignee, status)` | `GET /tasks` | Routes to `listActiveTasks()`, `listHistoricTasks()`, or both based on `status` param |
| `getTask(String taskId)` | `GET /tasks/{taskId}` | Checks active tasks first (`TaskService`), then history (`HistoryService`). Returns 404 if not found |
| `claimTask(String taskId, String assignee)` | `POST /tasks/{taskId}/claim` | Calls `taskService.claim(taskId, assignee)`. Returns updated task |
| `releaseTask(String taskId)` | `POST /tasks/{taskId}/release` | Calls `taskService.unclaim(taskId)`. Returns updated task |
| `completeTask(String taskId, Map variables)` | `POST /tasks/{taskId}/complete` | Calls `taskService.complete(taskId, variables)` or `taskService.complete(taskId)` if no variables |
| `listActiveTasks(...)` | private | Builds `TaskQuery` with optional filters, returns list via `toMap()` |
| `listHistoricTasks(...)` | private | Builds `HistoricTaskInstanceQuery` with optional filters |
| `toMap(Task)` | private | Converts engine `Task` to response map. Includes: id, name, assignee, status, createTime, processInstanceId, processDefinitionId, taskDefinitionKey, **formKey**, priority, dueDate |
| `toHistoricMap(HistoricTaskInstance)` | private | Converts history task. Includes endTime and duration |

---

### `FormController.java`

**Package:** `com.oop.runtime.controller`  
**Path:** `GET /api/v1/tasks/{taskId}/form`

Serves form schema and process variables for a task.

| Method | Description |
|---|---|
| `getTaskForm(String taskId)` | Queries task via `TaskService`, gets `formKey` from task, resolves schema via `resolveFormSchema()`, gets variables via `taskService.getVariables(taskId)`. Returns `{hasForm, taskId, formKey, schema, data}` |
| `resolveFormSchema(String formKey)` | **Resolution order:** 1) DB lookup via `formRepository.findTopByFormKeyOrderByVersionDesc(formKey)` 2) Classpath fallback via `ClassPathResource("forms/" + formKey + ".form.json")`. Returns schema JSON string or null |

**Key difference from query service version:** This endpoint returns actual process variables in the `data` field (for pre-filling form fields).

---

## Event Package

### `EngineEventPublisher.java`

**Package:** `com.oop.runtime.event`  
**Implements:** `ActivitiEventListener`

Listens to Activiti engine events and publishes JSON messages to RabbitMQ.

| Method | Trigger | Output Channel | Description |
|---|---|---|---|
| `onEvent(ActivitiEvent)` | Any engine event | — | Switch on `event.getType()`, routes to handler |
| `isFailOnException()` | — | — | Returns `false` — publishing failures don't roll back engine operations |
| `handleProcessStarted(event)` | `PROCESS_STARTED` | `engineEvents-out-0` | Extracts processInstanceId, processDefinitionKey, businessKey, initiator from `ExecutionEntity` |
| `handleProcessCompleted(event)` | `PROCESS_COMPLETED` | `engineEvents-out-0` | Extracts processInstanceId from `ExecutionEntity` |
| `handleProcessCancelled(event)` | `PROCESS_CANCELLED` | `engineEvents-out-0` | Extracts processInstanceId from `ExecutionEntity` |
| `handleTaskCreated(event)` | `TASK_CREATED` | `taskEvents-out-0` | Extracts taskId, name, processInstanceId, assignee, formKey. **Extracts processDefinitionKey** from `processDefinitionId` by splitting on `:` |
| `handleTaskAssigned(event)` | `TASK_ASSIGNED` | `taskEvents-out-0` | Checks `task.getAssignee()` — if non-blank publishes `TASK_ASSIGNED`, if null/blank publishes `TASK_RELEASED` |
| `handleTaskCompleted(event)` | `TASK_COMPLETED` | `taskEvents-out-0` | Extracts taskId |
| `send(String binding, Map payload)` | private | — | Wraps payload in `MessageBuilder.withPayload()` and sends via `StreamBridge` |

**processDefinitionKey extraction:**
```java
String[] parts = task.getProcessDefinitionId().split(":"); // "key:version:id"
String processDefinitionKey = parts[0];
```

**TASK_ASSIGNED vs TASK_RELEASED logic:**
```java
String assignee = task.getAssignee();
String eventType = (assignee != null && !assignee.isBlank()) ? "TASK_ASSIGNED" : "TASK_RELEASED";
```

---

## Model Package

### `FormDefinition.java`

**Package:** `com.oop.runtime.model`  
**Table:** `form_definition`

JPA entity for deployed form definitions.

| Field | Column | Type | Description |
|---|---|---|---|
| `id` | `id` | Long (auto) | Primary key |
| `formKey` | `form_key` | String | Form identifier (matches BPMN formKey) |
| `version` | `version` | Integer | Auto-incremented per formKey |
| `name` | `name` | String | Display name |
| `schemaJson` | `schema_json` | String (TEXT) | Full form JSON schema |
| `deployedAt` | `deployed_at` | Instant | Deployment timestamp |
| `deployedBy` | `deployed_by` | String | Username who deployed |

**Important:** Uses `columnDefinition = "TEXT"`, **not** `@Lob`. PostgreSQL's `@Lob` uses large objects which require transactions and fail in auto-commit mode.

---

## Repository Package

### `FormDefinitionRepository.java`

**Package:** `com.oop.runtime.repository`  
**Extends:** `JpaRepository<FormDefinition, Long>`

| Method | Description |
|---|---|
| `findTopByFormKeyOrderByVersionDesc(String formKey)` | Returns latest version of a form |
| `findByFormKeyOrderByVersionDesc(String formKey)` | Returns all versions (for deletion) |
| `findByFormKeyAndVersion(String formKey, Integer version)` | Returns specific version |

---

## Exception Package

### `GlobalExceptionHandler.java`

**Package:** `com.oop.runtime.exception`

`@ControllerAdvice` for handling unhandled exceptions.

| Method | Description |
|---|---|
| Exception handlers | Catches exceptions, logs full stack trace, returns JSON error response with exception class name and message |
