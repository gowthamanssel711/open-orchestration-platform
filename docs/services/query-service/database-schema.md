# Database Schema

The Query Service uses PostgreSQL with Flyway-managed migrations.

## Database

Name: `oop_query`

## Migrations

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_query_schema.sql` | Initial tables: process_instance, task |
| V2 | `V2__add_forms.sql` | Add form_key to task + form_definition table |

## Tables

### process_instance

Stores process instance read model.

```sql
CREATE TABLE process_instance (
    id BIGSERIAL PRIMARY KEY,
    process_instance_id VARCHAR(255) UNIQUE NOT NULL,
    process_definition_key VARCHAR(255),
    process_definition_id VARCHAR(255),
    business_key VARCHAR(255),
    initiator VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);
```

**Indexes:**
- `idx_pi_status` — filter by status
- `idx_pi_definition_key` — filter by process definition
- `idx_pi_initiator` — filter by who started it
- `idx_pi_started_at` — sort by start time

### task

Stores task read model.

```sql
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(500),
    description TEXT,
    process_instance_id VARCHAR(255),
    process_definition_key VARCHAR(255),
    assignee VARCHAR(255),
    form_key VARCHAR(255),
    candidate_groups VARCHAR(1000),
    status VARCHAR(50) NOT NULL,
    priority INTEGER DEFAULT 0,
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);
```

**Indexes:**
- `idx_task_assignee` — my tasks query
- `idx_task_status` — filter by status
- `idx_task_assignee_status` — my tasks with status filter
- `idx_task_process_instance` — tasks by process instance
- `idx_task_created_at` — sort by creation time
- `idx_task_form_key` — form-related queries

### form_definition

Stores deployed form schemas (synced from runtime bundle via events).

```sql
CREATE TABLE form_definition (
    id BIGSERIAL PRIMARY KEY,
    form_key VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR(500),
    schema_json TEXT NOT NULL,
    deployed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (form_key, version)
);
```

**Indexes:**
- `idx_form_key` — lookup by key
- `idx_form_key_version` — lookup latest version

## JPA Entities

| Entity | Table | Repository |
|---|---|---|
| `ProcessInstanceEntity` | process_instance | `ProcessInstanceRepository` |
| `TaskEntity` | task | `TaskRepository` |
| `FormDefinition` | form_definition | `FormDefinitionRepository` |

## Important Notes

- `schema_json` uses `TEXT` column type, **not** `@Lob` — PostgreSQL Large Objects require transactions and cause issues with auto-commit mode
- Flyway is configured to run on startup automatically
- The schema is purely a read model — it's rebuilt entirely from events
