# PostgreSQL

Database for all platform services.

## Access

| | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Username | oop |
| Password | oop-dev |

## Databases

Created automatically by `deploy/docker/init-databases.sql`:

| Database | Used By | Schema Management |
|---|---|---|
| `oop_runtime` | Runtime Bundle | Activiti Liquibase + Flyway (form_definition) |
| `oop_query` | Query Service | Flyway |
| `oop_keycloak` | Keycloak | Keycloak internal |
| `oop_audit` | Audit Service | Flyway (not active) |
| `oop_analytics` | Analytics Service | Flyway (not active) |

## Connecting

```bash
# psql
psql -h localhost -U oop -d oop_query

# JDBC URLs
jdbc:postgresql://localhost:5432/oop_runtime
jdbc:postgresql://localhost:5432/oop_query
```

## Runtime Bundle Database

The runtime bundle database contains:
- **Activiti engine tables** (ACT_*) — managed by Activiti's Liquibase migrations
- **form_definition** — managed by our Flyway migration

These coexist because Flyway tracks state in `flyway_schema_history` while Activiti uses `ACT_GE_PROPERTY` for its version.

## Query Service Database

Fully managed by Flyway:

| Table | Purpose |
|---|---|
| `process_instance` | Denormalized process instance read model |
| `task` | Denormalized task read model |
| `form_definition` | Form schemas (synced from runtime bundle) |

## Data Volume

Data persists in a Docker volume (`postgres-data`). To reset:

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

## Inspecting Tables

```sql
-- Query service tasks
SELECT task_id, name, status, assignee, form_key FROM task ORDER BY created_at DESC;

-- Process instances
SELECT process_instance_id, process_definition_key, status FROM process_instance;

-- Deployed forms
SELECT form_key, version, deployed_at FROM form_definition ORDER BY deployed_at DESC;
```

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| Connection refused | PostgreSQL not running | `docker compose up -d postgres` |
| Database doesn't exist | init-databases.sql didn't run | Down -v then up again |
| Flyway checksum mismatch | Migration file was modified | Reset flyway or fix migration |
| Large Objects error | `@Lob` annotation on entity | Use `columnDefinition = "TEXT"` instead |
