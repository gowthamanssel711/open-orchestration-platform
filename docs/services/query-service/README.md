# Query Service

The Query Service is the **read side** of the CQRS architecture — an optimized read model for fast task and process instance lookups.

## Purpose

- Provide fast, filterable queries for process instances and tasks
- Maintain denormalized read models by consuming events from RabbitMQ
- Serve form schemas for task rendering
- Decouple read load from the process engine

## Why a Separate Service?

The Activiti engine (Runtime Bundle) is optimized for execution, not querying. Its tables are normalized and transaction-heavy. The Query Service:
- Has denormalized tables optimized for list/filter operations
- Can scale independently of the engine
- Doesn't add read-load to the process engine
- Can survive engine restarts (data persists)

## Technology

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.2.4 |
| Database | PostgreSQL (dedicated `oop_query` database) |
| Messaging | RabbitMQ via Spring Cloud Stream |
| Auth | Keycloak JWT (OAuth2 Resource Server) |
| Migrations | Flyway |

## Port

`8082` (default)

## Package Structure

```
com.oop.query/
├── QueryServiceApplication.java
├── config/
│   └── SecurityConfig.java
├── consumer/
│   ├── ProcessEventConsumer.java    # PROCESS_* + FORM_DEPLOYED
│   └── TaskEventConsumer.java       # TASK_*
├── controller/
│   ├── ProcessInstanceController.java
│   ├── TaskController.java
│   └── FormController.java
├── model/
│   ├── ProcessInstanceEntity.java
│   ├── TaskEntity.java
│   └── FormDefinition.java
├── repository/
│   ├── ProcessInstanceRepository.java
│   ├── TaskRepository.java
│   └── FormDefinitionRepository.java
└── exception/
    └── GlobalExceptionHandler.java
```

## Running

```bash
cd platform/query-service
mvn spring-boot:run
```

**Prerequisites:** PostgreSQL, RabbitMQ, Keycloak must be running.

## Configuration

Key properties in `application.yml`:
- `spring.datasource.*` — PostgreSQL connection (database: `oop_query`)
- `spring.cloud.stream.*` — RabbitMQ consumer bindings
- `spring.cloud.function.definition` — `engineEventsConsumer;taskEventsConsumer`
- `spring.security.oauth2.resourceserver.jwt.*` — Keycloak JWT validation

## Features

- [Process Instances](process-instances.md) — Query process instances
- [Task Queries](task-queries.md) — List and filter tasks
- [Form Resolution](form-resolution.md) — Serve form schemas
- [Event Consumers](event-consumers.md) — RabbitMQ event handling
- [Database Schema](database-schema.md) — Tables, migrations, indexes
