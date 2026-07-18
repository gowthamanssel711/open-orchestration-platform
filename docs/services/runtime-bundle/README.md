# Runtime Bundle

The Runtime Bundle is the **write side** of the CQRS architecture — the BPMN execution engine powered by Activiti Cloud 8.4.0.

## Purpose

- Execute BPMN process definitions
- Manage process instance lifecycle (start, suspend, resume, cancel)
- Handle human task operations (claim, release, complete)
- Store and serve form definitions
- Publish engine events to RabbitMQ for downstream consumers

## Technology

| Component | Technology |
|---|---|
| Engine | Activiti Cloud 8.4.0 |
| Framework | Spring Boot 3.2.4 |
| Database | PostgreSQL (Activiti schema + form_definition) |
| Messaging | RabbitMQ via Spring Cloud Stream |
| Auth | Keycloak JWT (OAuth2 Resource Server) |

## Port

`8081` (default)

## Package Structure

```
com.oop.runtime/
├── RuntimeBundleApplication.java     # Main class
├── config/
│   ├── SecurityConfig.java           # JWT validation + role injection
│   ├── ActivitiIdentityConfig.java   # UserGroupManager for Activiti
│   ├── ActivitiConfig.java           # Engine configuration
│   ├── EngineEventConfig.java        # Registers event listener
│   └── StreamBindingConfig.java      # RabbitMQ stream bindings
├── controller/
│   ├── DeployController.java         # Unified deploy (BPMN + forms)
│   ├── ProcessController.java        # Process instance operations
│   ├── ProcessDefinitionController.java  # List definitions
│   ├── TaskController.java           # Task operations
│   └── FormController.java           # Form schema retrieval
├── event/
│   └── EngineEventPublisher.java     # Publishes events to RabbitMQ
├── model/
│   └── FormDefinition.java           # JPA entity
├── repository/
│   └── FormDefinitionRepository.java
└── exception/
    └── GlobalExceptionHandler.java
```

## Running

```bash
# From project root
cd platform/runtime-bundle
mvn spring-boot:run

# Or from IDE — run RuntimeBundleApplication.java
```

**Prerequisites:** PostgreSQL, RabbitMQ, Keycloak must be running (see infrastructure docs).

## Configuration

Key properties in `application.yml`:
- `spring.datasource.*` — PostgreSQL connection (database: `oop_runtime`)
- `spring.cloud.stream.*` — RabbitMQ bindings
- `spring.security.oauth2.resourceserver.jwt.*` — Keycloak JWT validation
- `activiti.cloud.services.oauth2.*` — Service-to-service auth for Activiti internals

## Features

- [Deployment API](deployment-api.md) — Deploy BPMN processes and forms
- [Process Management](process-management.md) — Start, cancel, suspend, resume
- [Task Management](task-management.md) — Claim, release, complete
- [Form System](form-system.md) — Form storage and retrieval
- [Event Publishing](event-publishing.md) — RabbitMQ event publishing
- [Security](security.md) — Authentication and authorization
