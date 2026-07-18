# 04 — Infrastructure Setup

## Local Development (Docker Compose)

### Prerequisites

| Tool | Version | Installation |
|------|---------|-------------|
| Docker Desktop | 4.25+ | docker.com |
| Docker Compose | 2.23+ | Included with Docker Desktop |
| Java JDK | 21 | `sdkman install java 21-tem` |
| Maven | 3.9+ | `sdkman install maven` |
| Node.js | 20 LTS | `nvm install 20` |
| pnpm | 9.x | `npm install -g pnpm` |
| Go | 1.22+ | go.dev (for CLI only) |

### Docker Compose Architecture

```yaml
# docker-compose.yml — Full local development stack

version: '3.8'

services:
  # ============================================================
  # INFRASTRUCTURE
  # ============================================================

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: oop
      POSTGRES_PASSWORD: oop-dev
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./deploy/docker/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U oop"]
      interval: 5s
      timeout: 3s
      retries: 5

  kafka:
    image: apache/kafka:3.7.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:29092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,EXTERNAL://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "29092:29092"
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions.sh --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    command: start-dev --import-realm
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/oop_keycloak
      KC_DB_USERNAME: oop
      KC_DB_PASSWORD: oop-dev
    ports:
      - "8180:8080"
    volumes:
      - ./deploy/keycloak/realm-export.json:/opt/keycloak/data/import/realm.json
    depends_on:
      postgres:
        condition: service_healthy

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  # ============================================================
  # PLATFORM SERVICES
  # ============================================================

  runtime-bundle:
    build: ./platform/runtime-bundle
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/oop_runtime
      SPRING_DATASOURCE_USERNAME: oop
      SPRING_DATASOURCE_PASSWORD: oop-dev
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      KEYCLOAK_AUTH_SERVER_URL: http://keycloak:8080
      ACTIVITI_CLOUD_APPLICATION_NAME: runtime-bundle
    ports:
      - "8081:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
      keycloak:
        condition: service_started

  query-service:
    build: ./platform/query-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/oop_query
      SPRING_DATASOURCE_USERNAME: oop
      SPRING_DATASOURCE_PASSWORD: oop-dev
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      KEYCLOAK_AUTH_SERVER_URL: http://keycloak:8080
    ports:
      - "8082:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy

  audit-service:
    build: ./platform/audit-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/oop_audit
      SPRING_DATASOURCE_USERNAME: oop
      SPRING_DATASOURCE_PASSWORD: oop-dev
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      KEYCLOAK_AUTH_SERVER_URL: http://keycloak:8080
    ports:
      - "8083:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy

  analytics-service:
    build: ./platform/analytics-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/oop_analytics
      SPRING_DATASOURCE_USERNAME: oop
      SPRING_DATASOURCE_PASSWORD: oop-dev
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
    ports:
      - "8084:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy

  notification-service:
    build: ./platform/notification-service
    environment:
      SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      KEYCLOAK_AUTH_SERVER_URL: http://keycloak:8080
    ports:
      - "8085:8080"
    depends_on:
      kafka:
        condition: service_healthy

  gateway:
    build: ./platform/gateway
    environment:
      SPRING_CLOUD_GATEWAY_ROUTES_RUNTIME_URI: http://runtime-bundle:8080
      SPRING_CLOUD_GATEWAY_ROUTES_QUERY_URI: http://query-service:8080
      SPRING_CLOUD_GATEWAY_ROUTES_AUDIT_URI: http://audit-service:8080
      SPRING_CLOUD_GATEWAY_ROUTES_ANALYTICS_URI: http://analytics-service:8080
      SPRING_REDIS_HOST: redis
      KEYCLOAK_AUTH_SERVER_URL: http://keycloak:8080
    ports:
      - "8080:8080"
    depends_on:
      - runtime-bundle
      - query-service
      - redis

  # ============================================================
  # FRONTEND APPLICATIONS
  # ============================================================

  operate-ui:
    build: ./apps/operate
    environment:
      VITE_API_URL: http://localhost:8080
      VITE_KEYCLOAK_URL: http://localhost:8180
      VITE_KEYCLOAK_REALM: oop
      VITE_KEYCLOAK_CLIENT_ID: operate-ui
    ports:
      - "3000:80"

  tasklist-ui:
    build: ./apps/tasklist
    environment:
      VITE_API_URL: http://localhost:8080
      VITE_KEYCLOAK_URL: http://localhost:8180
      VITE_KEYCLOAK_REALM: oop
      VITE_KEYCLOAK_CLIENT_ID: tasklist-ui
    ports:
      - "3001:80"

  analytics-ui:
    build: ./apps/analytics
    environment:
      VITE_API_URL: http://localhost:8080
      VITE_KEYCLOAK_URL: http://localhost:8180
      VITE_KEYCLOAK_REALM: oop
      VITE_KEYCLOAK_CLIENT_ID: analytics-ui
    ports:
      - "3002:80"

  modeler-ui:
    build: ./apps/modeler
    environment:
      VITE_API_URL: http://localhost:8080
      VITE_KEYCLOAK_URL: http://localhost:8180
      VITE_KEYCLOAK_REALM: oop
      VITE_KEYCLOAK_CLIENT_ID: modeler-ui
    ports:
      - "3003:80"

volumes:
  postgres-data:
```

### Database Initialization Script

```sql
-- deploy/docker/init-databases.sql

CREATE DATABASE oop_runtime;
CREATE DATABASE oop_query;
CREATE DATABASE oop_audit;
CREATE DATABASE oop_analytics;
CREATE DATABASE oop_identity;
CREATE DATABASE oop_keycloak;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE oop_runtime TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_query TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_audit TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_analytics TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_identity TO oop;
GRANT ALL PRIVILEGES ON DATABASE oop_keycloak TO oop;
```

### Makefile

```makefile
# Makefile

.PHONY: help build up down logs test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build all services
	mvn clean package -DskipTests -pl platform/ -am
	cd apps && pnpm install && pnpm build

up: ## Start all services (Docker Compose)
	docker compose up -d
	@echo "Platform starting..."
	@echo "  Gateway:    http://localhost:8080"
	@echo "  Operate:    http://localhost:3000"
	@echo "  Tasklist:   http://localhost:3001"
	@echo "  Analytics:  http://localhost:3002"
	@echo "  Modeler:    http://localhost:3003"
	@echo "  Keycloak:   http://localhost:8180"
	@echo "  Kafka:      localhost:29092"
	@echo "  PostgreSQL: localhost:5432"

down: ## Stop all services
	docker compose down

down-clean: ## Stop all services and remove volumes
	docker compose down -v

logs: ## Tail logs for all services
	docker compose logs -f

logs-engine: ## Tail logs for runtime bundle
	docker compose logs -f runtime-bundle

test: ## Run all tests
	mvn test -pl platform/ -am
	cd apps && pnpm test

test-integration: ## Run integration tests (requires Docker)
	mvn verify -pl test/integration -am -Pintegration

test-e2e: ## Run E2E tests (requires running stack)
	cd test/e2e && pnpm playwright test

load-test: ## Run load tests
	k6 run test/load/scenarios/basic-process.js

deploy-sample: ## Deploy sample process
	curl -X POST http://localhost:8080/api/v1/processes \
	  -H "Authorization: Bearer $$(make _get-token)" \
	  -F "file=@samples/order-process.bpmn"

status: ## Check service health
	@echo "Checking services..."
	@curl -s http://localhost:8080/actuator/health | jq .status
	@curl -s http://localhost:8081/actuator/health | jq .status
	@curl -s http://localhost:8082/actuator/health | jq .status

_get-token: ## Get Keycloak access token (internal)
	@curl -s -X POST http://localhost:8180/realms/oop/protocol/openid-connect/token \
	  -d "client_id=admin-cli" \
	  -d "username=admin" \
	  -d "password=admin" \
	  -d "grant_type=password" | jq -r .access_token
```

---

## Kubernetes Deployment (Production)

### Helm Chart Structure

```
deploy/helm/
├── Chart.yaml              # Umbrella chart metadata
├── values.yaml             # Default configuration
├── values-prod.yaml        # Production overrides
├── values-staging.yaml     # Staging overrides
├── templates/
│   ├── _helpers.tpl        # Template helpers
│   ├── namespace.yaml      # Namespace creation
│   ├── configmap.yaml      # Shared configuration
│   └── secrets.yaml        # Secret references
└── charts/                 # Sub-charts (one per service)
    ├── runtime-bundle/
    │   ├── Chart.yaml
    │   ├── values.yaml
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── hpa.yaml
    │       └── pdb.yaml
    ├── query-service/
    ├── audit-service/
    ├── analytics-service/
    ├── gateway/
    ├── operate-ui/
    ├── tasklist-ui/
    ├── analytics-ui/
    └── modeler-ui/
```

### Production Values Example

```yaml
# values-prod.yaml

global:
  namespace: oop-production
  domain: orchestration.company.com
  tls:
    enabled: true
    issuer: letsencrypt-prod

gateway:
  replicas: 3
  resources:
    requests:
      memory: "256Mi"
      cpu: "200m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  hpa:
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilization: 70

runtimeBundle:
  replicas: 3
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"
  hpa:
    minReplicas: 3
    maxReplicas: 20
    targetCPUUtilization: 60
  javaOpts: "-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms512m -Xmx1536m"

queryService:
  replicas: 3
  resources:
    requests:
      memory: "512Mi"
      cpu: "300m"
    limits:
      memory: "1Gi"
      cpu: "600m"
  hpa:
    minReplicas: 3
    maxReplicas: 15
    targetCPUUtilization: 70

postgresql:
  # Using CloudNativePG operator
  operator: cloudnative-pg
  instances: 3  # 1 primary + 2 replicas
  storage:
    size: 100Gi
    storageClass: gp3
  backup:
    enabled: true
    schedule: "0 2 * * *"
    retention: 30d
    destination: s3://company-backups/oop/

kafka:
  # Using Strimzi operator
  operator: strimzi
  replicas: 3
  storage:
    type: persistent-claim
    size: 200Gi
    storageClass: gp3
  config:
    log.retention.hours: 168  # 7 days
    num.partitions: 12
    default.replication.factor: 3

keycloak:
  replicas: 2
  resources:
    requests:
      memory: "512Mi"
    limits:
      memory: "1Gi"
  database:
    vendor: postgres

redis:
  architecture: replication
  replica:
    replicaCount: 2
  sentinel:
    enabled: true

monitoring:
  prometheus:
    enabled: true
  grafana:
    enabled: true
    dashboards: true
  jaeger:
    enabled: true
  loki:
    enabled: true
```

### Deployment Command

```bash
# Install operators first
kubectl apply -f https://strimzi.io/install/latest
kubectl apply -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/main/releases/cnpg-1.22.yaml

# Deploy platform
helm install oop ./deploy/helm \
  --namespace oop-production \
  --create-namespace \
  -f deploy/helm/values-prod.yaml \
  --set global.domain=orchestration.company.com

# Verify
kubectl get pods -n oop-production
helm status oop -n oop-production
```

---

## Resource Requirements

### Development (Docker Compose)

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 4 cores | 8 cores |
| RAM | 8 GB | 16 GB |
| Disk | 10 GB | 20 GB |
| Network | N/A | N/A |

### Production (Kubernetes)

| Component | CPU (request) | RAM (request) | Instances | Total CPU | Total RAM |
|-----------|--------------|---------------|-----------|-----------|-----------|
| Gateway | 200m | 256Mi | 3 | 600m | 768Mi |
| Runtime Bundle | 500m | 1Gi | 3 | 1500m | 3Gi |
| Query Service | 300m | 512Mi | 3 | 900m | 1.5Gi |
| Audit Service | 200m | 256Mi | 2 | 400m | 512Mi |
| Analytics Service | 300m | 512Mi | 2 | 600m | 1Gi |
| Notification Service | 100m | 256Mi | 2 | 200m | 512Mi |
| Kafka (Strimzi) | 500m | 2Gi | 3 | 1500m | 6Gi |
| PostgreSQL (CNPG) | 500m | 2Gi | 3 | 1500m | 6Gi |
| Keycloak | 300m | 512Mi | 2 | 600m | 1Gi |
| Redis Sentinel | 100m | 128Mi | 3 | 300m | 384Mi |
| Frontend UIs | 50m | 64Mi | 4×2 | 400m | 512Mi |
| **TOTAL** | | | | **~8.5 CPU** | **~21 GB** |

**Recommended Kubernetes cluster:** 3 nodes × 4 CPU × 8GB RAM (small), or 3 nodes × 8 CPU × 16GB RAM (medium workloads).

---

## Environment Configuration

### Environment Variables (All Services)

```bash
# Common
OOP_ENVIRONMENT=production|staging|development
OOP_LOG_LEVEL=INFO|DEBUG

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname
SPRING_DATASOURCE_USERNAME=oop
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}

# Kafka
SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=kafka-bootstrap:9092
SPRING_CLOUD_STREAM_KAFKA_BINDER_CONFIGURATION_SECURITY_PROTOCOL=SASL_SSL

# Keycloak
KEYCLOAK_AUTH_SERVER_URL=https://auth.company.com
KEYCLOAK_REALM=oop
KEYCLOAK_RESOURCE=backend-service

# Redis
SPRING_REDIS_HOST=redis-sentinel
SPRING_REDIS_SENTINEL_MASTER=mymaster

# Observability
MANAGEMENT_TRACING_ENABLED=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```

---

## Networking

### Port Assignments

| Service | Internal Port | External Port (dev) | Protocol |
|---------|--------------|--------------------:|----------|
| API Gateway | 8080 | 8080 | HTTP/REST |
| Runtime Bundle | 8080 | 8081 | HTTP/REST |
| Query Service | 8080 | 8082 | HTTP/REST |
| Audit Service | 8080 | 8083 | HTTP/REST |
| Analytics Service | 8080 | 8084 | HTTP/REST |
| Notification Service | 8080 | 8085 | HTTP/GraphQL/WS |
| Connector Service | 8080 | 8086 | HTTP/REST |
| Identity Service | 8080 | 8087 | HTTP/REST |
| Operate UI | 80 | 3000 | HTTP |
| Tasklist UI | 80 | 3001 | HTTP |
| Analytics UI | 80 | 3002 | HTTP |
| Modeler UI | 80 | 3003 | HTTP |
| PostgreSQL | 5432 | 5432 | TCP |
| Kafka | 9092 | 29092 | TCP |
| Keycloak | 8080 | 8180 | HTTP |
| Redis | 6379 | 6379 | TCP |
| Prometheus | 9090 | 9090 | HTTP |
| Grafana | 3000 | 3100 | HTTP |
| Jaeger | 16686 | 16686 | HTTP |

### Production Ingress Rules

```yaml
# Only the gateway and UIs are externally accessible
rules:
  - host: api.orchestration.company.com
    paths:
      - path: /
        service: gateway
  - host: operate.orchestration.company.com
    paths:
      - path: /
        service: operate-ui
  - host: tasks.orchestration.company.com
    paths:
      - path: /
        service: tasklist-ui
  - host: analytics.orchestration.company.com
    paths:
      - path: /
        service: analytics-ui
  - host: modeler.orchestration.company.com
    paths:
      - path: /
        service: modeler-ui
  - host: auth.orchestration.company.com
    paths:
      - path: /
        service: keycloak
```
