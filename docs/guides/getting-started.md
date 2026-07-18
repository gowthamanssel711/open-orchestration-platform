# Getting Started with Open Orchestration Platform

This guide walks you through setting up OOP locally and running your first BPMN process.

## Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Docker** and **Docker Compose v2**
- **Node.js 20+** and **pnpm 9+** (for frontend)
- **curl** or **httpie** (for testing APIs)

## Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL, Kafka, Keycloak, and Redis
make up-infra

# Or using docker compose directly
docker compose -f docker-compose.dev.yml up -d
```

Wait ~30 seconds for all containers to be ready.

### 2. Build Backend Services

```bash
mvn clean package -DskipTests
```

### 3. Run Services

You can run all services via Docker Compose:

```bash
make up
```

Or run individual services from your IDE:

```bash
# Run the process engine
mvn spring-boot:run -pl platform/runtime-bundle

# Run the query service
mvn spring-boot:run -pl platform/query-service

# Run the API gateway
mvn spring-boot:run -pl platform/gateway
```

### 4. Verify Everything is Running

```bash
make status
```

You should see all services reporting `UP`.

## Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| Keycloak Admin | admin | admin |
| OOP Admin User | admin | admin |
| OOP Operator | operator | operator |
| OOP Developer | developer | developer |
| OOP User | user | user |

Keycloak is available at: http://localhost:8180

## Your First Process

### Step 1: Get an Access Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/oop/protocol/openid-connect/token \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r .access_token)
```

### Step 2: Deploy a BPMN Process

```bash
curl -X POST http://localhost:8080/api/v1/processes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@samples/order-process.bpmn"
```

### Step 3: List Available Process Definitions

```bash
curl -s http://localhost:8080/api/v1/processes \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Step 4: Start a Process Instance

```bash
curl -X POST http://localhost:8080/api/v1/process-instances \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "sampleOrderProcess",
    "variables": {
      "orderId": "ORD-001",
      "amount": 149.99,
      "customer": "acme-corp"
    }
  }'
```

Save the returned `id` — that's your process instance ID.

### Step 5: Query Process Instances

```bash
curl -s http://localhost:8080/api/v1/query/process-instances \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Step 6: View Available Tasks

```bash
curl -s http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Step 7: Complete a User Task

```bash
TASK_ID="<task-id-from-step-6>"

curl -X POST "http://localhost:8080/api/v1/tasks/$TASK_ID/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"approved": true}'
```

### Step 8: Check Audit Trail

```bash
PROCESS_ID="<process-instance-id-from-step-4>"

curl -s "http://localhost:8080/api/v1/audit/events?processInstanceId=$PROCESS_ID" \
  -H "Authorization: Bearer $TOKEN" | jq
```

## Service Endpoints

| Service | Direct URL | Via Gateway |
|---------|-----------|-------------|
| API Gateway | http://localhost:8080 | — |
| Runtime Bundle | http://localhost:8081 | /api/v1/processes, /api/v1/process-instances, /api/v1/tasks |
| Query Service | http://localhost:8082 | /api/v1/query/** |
| Audit Service | http://localhost:8083 | /api/v1/audit/** |
| Analytics Service | http://localhost:8084 | /api/v1/analytics/** |
| Notification Service | http://localhost:8085 | /ws/graphql (WebSocket) |
| Identity Service | http://localhost:8087 | /api/v1/admin/** |
| Keycloak | http://localhost:8180 | — |

## API Documentation

Each service exposes Swagger UI:
- http://localhost:8081/swagger-ui.html (Runtime Bundle)
- http://localhost:8082/swagger-ui.html (Query Service)
- http://localhost:8083/swagger-ui.html (Audit Service)
- http://localhost:8084/swagger-ui.html (Analytics)
- http://localhost:8087/swagger-ui.html (Identity)

Aggregated API index at the gateway: http://localhost:8080/api/docs

## Running Tests

```bash
# Unit tests
make test-backend

# Integration tests (requires Docker)
make test-integration

# Full stack verification
make verify
```

## Project Structure

```
├── platform/
│   ├── runtime-bundle/     # BPMN engine (Activiti Cloud)
│   ├── query-service/      # CQRS read model
│   ├── audit-service/      # Immutable event log
│   ├── analytics-service/  # Process metrics & SLA
│   ├── notification-service/ # Real-time WebSocket notifications
│   ├── identity-service/   # RBAC & tenant management
│   ├── gateway/            # API gateway (routing, auth, rate limiting)
│   └── connector-service/  # External system integrations
├── apps/
│   ├── operate/            # Process monitoring UI
│   ├── tasklist/           # Task management UI
│   ├── analytics/          # Analytics dashboard
│   └── modeler/            # BPMN modeler
├── test/
│   ├── integration/        # Integration tests
│   ├── e2e/                # End-to-end tests
│   └── load/               # Load/performance tests
├── deploy/
│   ├── docker/             # Dockerfiles
│   └── keycloak/           # Realm configuration
└── samples/                # Sample BPMN processes
```

## Troubleshooting

### Services won't start
- Check that infrastructure is running: `docker compose -f docker-compose.dev.yml ps`
- Verify Keycloak imported the realm: http://localhost:8180/admin (admin/admin)
- Check logs: `docker compose logs -f <service-name>`

### Authentication errors
- Token may have expired (default: 5 min). Get a fresh token.
- Ensure Keycloak realm "oop" exists with the expected clients.

### Kafka consumer lag
- Check consumer groups: `docker compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list`
- View lag: `docker compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group query-service`

## Next Steps

- Deploy your own BPMN processes (use any BPMN 2.0 compliant modeler)
- Configure connector services for external integrations
- Set up monitoring with Prometheus + Grafana
- Explore the frontend UIs (start with `make dev-operate`)
