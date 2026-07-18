# Infrastructure

Local development infrastructure managed via Docker Compose.

## Services

| Service | Image | Port | Purpose |
|---|---|---|---|
| PostgreSQL | postgres:16-alpine | 5432 | Database for all services |
| RabbitMQ | rabbitmq:3.13-management-alpine | 5672 / 15672 | Event streaming |
| Keycloak | quay.io/keycloak/keycloak:24.0 | 8180 | Authentication |
| Redis | redis:7-alpine | 6379 | Rate limiting (gateway) |

## Quick Start

```bash
docker compose -f docker-compose.dev.yml up -d
```

Wait for all services to be healthy:
```bash
docker compose -f docker-compose.dev.yml ps
```

## Management UIs

| Service | URL | Credentials |
|---|---|---|
| RabbitMQ Management | http://localhost:15672 | guest / guest |
| Keycloak Admin | http://localhost:8180 | admin / admin |

## Stopping

```bash
docker compose -f docker-compose.dev.yml down

# To also remove volumes (fresh start):
docker compose -f docker-compose.dev.yml down -v
```

## Detailed Guides

- [Keycloak](keycloak.md) — Realm, clients, roles, users
- [RabbitMQ](rabbitmq.md) — Exchanges, queues, event flow
- [PostgreSQL](postgresql.md) — Databases, initialization, connections
