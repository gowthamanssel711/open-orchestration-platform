# Building Camunda 8-Style Architecture on Activiti

## The Good News

**Activiti Cloud already has 60-70% of the Camunda 8 architecture patterns built in.** It's Apache 2.0, actively maintained (last commit July 2026), and designed for exactly this use case.

---

## Why Activiti Cloud (Not Plain Activiti or Flowable)

| Engine | Why / Why Not |
|--------|---------------|
| **Activiti Cloud** | Already has CQRS, event sourcing, microservices, Kubernetes-native, Spring Cloud Stream (Kafka/RabbitMQ). **Best fit.** |
| **Plain Activiti** | Monolithic, no CQRS, no event streaming. Would need too much rearchitecting. |
| **Flowable** | Better community, but monolithic. No CQRS, no microservices separation. Would need to build everything from scratch. |

---

## Architecture Mapping: Camunda 8 → Activiti Cloud

| Camunda 8 Component | Activiti Cloud Equivalent | Gap? |
|---------------------|--------------------------|------|
| **Zeebe Broker** (engine) | **Runtime Bundle** | Activiti uses RDBMS, not event log. No Raft consensus. Scale via DB, not partitions. |
| **Zeebe Gateway** (gRPC/REST) | **Runtime Bundle REST API** | Already has REST. Need to add gRPC. |
| **Operate** (monitoring) | **Query Service** + custom UI | Query service exists. Need to build React UI. |
| **Tasklist** (human tasks) | **Query Service** + custom UI | Task data available via query. Need UI. |
| **Optimize** (analytics) | Nothing | Build from scratch on top of Audit Service events. |
| **Identity** (IAM) | **Keycloak integration** | Already supports Keycloak. Need RBAC layer. |
| **Connectors** | **Cloud Connectors** | Already has connector framework. |
| **Exporters** | **Audit Service + Spring Cloud Stream** | Events already published to message broker. |
| **CQRS separation** | **Built-in** (Runtime Bundle = write, Query Service = read) | Already done. |
| **Event streaming** | **Spring Cloud Stream** (RabbitMQ/Kafka) | Already done. |
| **Kubernetes deployment** | **Native support** | Already done. |

---

## What Activiti Cloud Already Gives You (Free)

```
┌─────────────────────────────────────────────────────────────┐
│                  ACTIVITI CLOUD (Apache 2.0)                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ Runtime Bundle          = Process execution engine       │
│  ✅ Query Service           = CQRS read side                 │
│  ✅ Audit Service           = Event sourcing / history       │
│  ✅ Cloud Connectors        = External system integration    │
│  ✅ GraphQL Notifications   = Real-time subscriptions        │
│  ✅ Spring Cloud Stream     = Kafka/RabbitMQ messaging       │
│  ✅ Keycloak Integration    = OAuth2/OIDC authentication     │
│  ✅ REST APIs               = Full CRUD for process/tasks    │
│  ✅ Kubernetes-native       = Helm charts, scaling           │
│  ✅ Multi-tenancy           = Basic support                  │
│  ✅ Full BPMN 2.0           = All elements supported         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## What You Need to Build (The Gaps)

```
┌─────────────────────────────────────────────────────────────┐
│                     MUST BUILD                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ❌ Operate UI              = Process monitoring dashboard   │
│  ❌ Tasklist UI             = Human task inbox + forms        │
│  ❌ Analytics UI            = Process analytics (Optimize)   │
│  ❌ Modeler (collaborative) = Web-based BPMN editor          │
│  ❌ Admin Console           = Cluster/deployment management  │
│  ❌ Enhanced RBAC           = Resource-level permissions      │
│  ❌ DMN Engine              = Decision table execution        │
│  ❌ Elastic/OpenSearch exp. = High-volume search backend      │
│  ❌ gRPC Gateway            = High-perf binary protocol       │
│  ❌ CLI Tool                = Developer experience            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Target Architecture

```
                         ┌──────────────────────┐
                         │   API Gateway        │
                         │   (Spring Cloud GW)  │
                         │   REST + gRPC + WS   │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
          ┌─────────▼──┐    ┌──────▼─────┐   ┌────▼──────────┐
          │  Runtime    │    │  Query     │   │  Connectors   │
          │  Bundle     │    │  Service   │   │  Service      │
          │  (Engine)   │    │  (CQRS     │   │  (Outbound/   │
          │             │    │   Read)    │   │   Inbound)    │
          └──────┬──────┘    └──────▲─────┘   └───────────────┘
                 │                  │
                 │    ┌─────────────┤
                 ▼    │             │
          ┌──────────────┐  ┌──────┴──────┐
          │ Message      │  │  Query DB   │
          │ Broker       │  │  (Postgres  │
          │ (Kafka/      │──│   + ES opt) │
          │  RabbitMQ)   │  └─────────────┘
          └──────┬───────┘
                 │
       ┌─────────┼──────────┐
       │         │          │
┌──────▼──┐ ┌───▼────┐ ┌───▼──────────┐
│  Audit  │ │Notif.  │ │ Analytics    │
│  Service│ │Service │ │ Service      │
│  (Event │ │(GraphQL│ │ (Aggregation │
│   Store)│ │ + WS)  │ │  + Metrics)  │
└─────────┘ └────────┘ └──────────────┘

                    │
        ┌───────────┼───────────┐
        │           │           │
  ┌─────▼───┐ ┌────▼────┐ ┌───▼──────┐
  │ Operate │ │Tasklist │ │Analytics │
  │ UI      │ │ UI      │ │ UI       │
  │ (React) │ │(React)  │ │(React)   │
  └─────────┘ └─────────┘ └──────────┘

        ┌───────────────────────┐
        │   Identity Service    │
        │   (Keycloak + RBAC)   │
        └───────────────────────┘
```

---

## Detailed Implementation Plan

### Layer 1: Foundation (Use Activiti Cloud As-Is)

These already work. Just configure and deploy:

| Service | Activiti Cloud Module | Action |
|---------|----------------------|--------|
| Process Engine | `activiti-cloud-runtime-bundle-service` | Deploy as-is |
| Query Service | `activiti-cloud-query-service` | Deploy as-is |
| Audit Service | `activiti-cloud-audit-service` | Deploy as-is |
| Notifications | `activiti-cloud-notifications-graphql-service` | Deploy as-is |
| Connectors | `activiti-cloud-connectors` | Deploy as-is |
| Message Broker | Spring Cloud Stream | Configure Kafka or RabbitMQ |
| Auth | Keycloak | Deploy Keycloak instance |
| Database | PostgreSQL | Deploy for runtime + query |

**Effort: 1-2 weeks** (configuration + Helm charts + Docker Compose)

### Layer 2: API Gateway Enhancement

Build a unified API gateway on top of the services:

```java
// Spring Cloud Gateway routing
@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("process-commands", r -> r.path("/v1/processes/**")
                .uri("lb://runtime-bundle"))
            .route("queries", r -> r.path("/v1/query/**")
                .uri("lb://query-service"))
            .route("tasks", r -> r.path("/v1/tasks/**")
                .uri("lb://query-service"))
            .build();
    }
}
```

**Add:**
- Unified REST API (combine runtime + query endpoints)
- gRPC gateway (Protobuf definitions + gRPC-Spring-Boot-Starter)
- WebSocket endpoint for real-time instance events
- Rate limiting, request validation
- API versioning

**Effort: 3-4 weeks**

### Layer 3: Operate UI (Process Monitoring)

**Tech:** React + TypeScript + bpmn.io

**Features to build:**
1. Process definition list (versions, deployments)
2. Process instance list (running, completed, incidents)
3. BPMN diagram viewer with active token overlay
4. Instance detail view (variables, audit trail, flow history)
5. Incident management (view error, retry, cancel)
6. Variable inspection and modification
7. Batch operations (cancel/retry multiple)

**Data source:** Activiti Cloud Query Service REST API + GraphQL Notifications (real-time)

**Effort: 6-8 weeks**

### Layer 4: Tasklist UI (Human Tasks)

**Tech:** React + TypeScript

**Features to build:**
1. Task inbox with filters (assignee, group, process, priority)
2. Task detail view with variables
3. Dynamic form rendering (JSON Schema → React form)
4. Task claiming, assignment, delegation
5. Task completion with variable submission
6. Task comments and history
7. Task notifications (WebSocket push)

**Data source:** Activiti Cloud Query Service (task endpoints)

**Effort: 4-6 weeks**

### Layer 5: Analytics Service (Replaces Camunda Optimize)

**Tech:** Spring Boot + PostgreSQL aggregations

**New microservice** that consumes Audit Service events and builds analytics:

```java
@Service
public class AnalyticsEventConsumer {
    
    @StreamListener("auditConsumer")
    public void processEvent(CloudRuntimeEvent event) {
        switch (event.getEventType()) {
            case PROCESS_STARTED -> processStartMetric(event);
            case PROCESS_COMPLETED -> processCompletionMetric(event);
            case ACTIVITY_STARTED -> activityDurationStart(event);
            case ACTIVITY_COMPLETED -> activityDurationEnd(event);
            case TASK_ASSIGNED -> taskAssignmentMetric(event);
        }
    }
}
```

**Analytics features:**
1. Process duration heatmaps (overlay on BPMN diagram)
2. Bottleneck detection (slowest activities)
3. SLA monitoring (define targets, track breaches)
4. Throughput metrics (instances/hour, tasks/day)
5. User performance (tasks completed per assignee)
6. Custom reports (SQL-based query builder)
7. Dashboard builder (drag-and-drop widgets)
8. Export (CSV, PDF)

**Effort: 8-10 weeks**

### Layer 6: Collaborative Modeler

**Tech:** React + bpmn.io + Yjs (CRDT collaboration)

**Features:**
1. BPMN 2.0 editor (bpmn-js — already open-source, MIT)
2. DMN editor (dmn-js — already open-source, MIT)
3. Form editor (JSON Schema builder)
4. Real-time collaboration (Yjs + WebSocket provider)
5. Version history with visual diff
6. Deploy to engine directly from modeler
7. Validation and linting
8. Template library

**Effort: 6-8 weeks** (bpmn.io does heavy lifting)

### Layer 7: Enhanced Identity & RBAC

Extend existing Keycloak integration:

```java
@Service
public class ResourcePermissionService {
    
    public boolean canAccessProcess(String userId, String processDefKey) {
        return permissionRepository.hasPermission(
            userId, ResourceType.PROCESS_DEFINITION, processDefKey, Action.READ
        );
    }
    
    public boolean canStartProcess(String userId, String processDefKey) {
        return permissionRepository.hasPermission(
            userId, ResourceType.PROCESS_DEFINITION, processDefKey, Action.CREATE_INSTANCE
        );
    }
}
```

**Permission model:**
```
Role → Permission → Resource → Action
  │
  ├── admin        → process:*        → *:all
  ├── operator     → process:*        → *:read, instance:cancel
  ├── developer    → process:deploy   → own-ns:deploy
  └── task-user    → task:*           → assigned:complete
```

**Effort: 3-4 weeks**

### Layer 8: Elasticsearch/OpenSearch Exporter (Optional)

For high-volume deployments (>1M instances), add ES as secondary query backend:

```java
@Service
public class ElasticsearchExporter {
    
    @StreamListener("auditConsumer")
    public void exportToElasticsearch(CloudRuntimeEvent event) {
        IndexRequest request = new IndexRequest("process-instances")
            .id(event.getEntityId())
            .source(mapToDocument(event));
        elasticsearchClient.index(request);
    }
}
```

**Effort: 2-3 weeks**

### Layer 9: CLI Tool

```bash
# Developer CLI for the platform
flow deploy ./order-process.bpmn          # Deploy process
flow start order-process --var orderId=123 # Start instance  
flow instances list --status=running       # List instances
flow tasks list --assignee=john            # List tasks
flow tasks complete <taskId> --var approved=true
flow analytics report --process=order      # Get metrics
flow cluster status                        # Health check
flow dev                                   # Local dev mode
```

**Tech:** Java (Spring Shell) or Go (Cobra)

**Effort: 2-3 weeks**

### Layer 10: DMN Decision Engine

Activiti has basic DMN. Enhance with:
- Full DMN 1.3 specification support
- Decision table editor in Modeler
- Decision history and analytics
- A/B testing for decision tables

**Options:**
1. Use Activiti's built-in DMN (limited but functional)
2. Integrate Flowable DMN engine (Apache 2.0, more complete)
3. Use open-source FEEL evaluator (e.g., camunda/feel-scala — Apache 2.0)

**Effort: 3-5 weeks**

---

## Technology Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Process Engine | Activiti Cloud Runtime Bundle | Apache 2.0, BPMN 2.0 complete |
| Backend Services | Spring Boot 4.x + Java 21+ | Matches Activiti Cloud stack |
| Message Broker | Apache Kafka | High throughput, replay capability |
| Primary Database | PostgreSQL 16 | ACID, JSON support, proven |
| Search (optional) | Elasticsearch/OpenSearch | High-volume text search |
| Frontend | React 18 + TypeScript | Modern, bpmn.io compatible |
| BPMN Rendering | bpmn.io (bpmn-js, dmn-js) | MIT, industry standard |
| Collaboration | Yjs (CRDT) | Real-time collaborative editing |
| Authentication | Keycloak | Apache 2.0, full OIDC |
| API Gateway | Spring Cloud Gateway | Native Spring integration |
| Service Discovery | Kubernetes DNS / Eureka | Cloud-native |
| Observability | Micrometer + Prometheus + Grafana | Spring Boot native |
| CI/CD | GitHub Actions | Free for OSS |
| Container | Docker + Helm | Kubernetes standard |
| CLI | Spring Shell or Go + Cobra | Developer experience |

---

## Repository Structure

```
/
├── platform/                          # Platform services
│   ├── runtime-bundle/                # Process engine (Activiti Cloud)
│   │   ├── src/
│   │   └── pom.xml
│   ├── query-service/                 # CQRS query side (Activiti Cloud)
│   │   ├── src/
│   │   └── pom.xml
│   ├── audit-service/                 # Event store (Activiti Cloud)
│   │   ├── src/
│   │   └── pom.xml
│   ├── analytics-service/             # NEW: Process analytics
│   │   ├── src/
│   │   └── pom.xml
│   ├── gateway/                       # NEW: Unified API Gateway
│   │   ├── src/
│   │   └── pom.xml
│   ├── identity/                      # NEW: Enhanced RBAC service
│   │   ├── src/
│   │   └── pom.xml
│   ├── notifications/                 # GraphQL notifications (Activiti Cloud)
│   │   ├── src/
│   │   └── pom.xml
│   └── connectors/                    # Connector runtime (Activiti Cloud)
│       ├── http-connector/
│       ├── kafka-connector/
│       ├── email-connector/
│       └── sdk/
├── apps/                              # Frontend applications
│   ├── operate/                       # NEW: Process monitoring UI
│   │   ├── src/
│   │   ├── package.json
│   │   └── vite.config.ts
│   ├── tasklist/                      # NEW: Task management UI
│   │   ├── src/
│   │   ├── package.json
│   │   └── vite.config.ts
│   ├── analytics/                     # NEW: Analytics dashboard
│   │   ├── src/
│   │   ├── package.json
│   │   └── vite.config.ts
│   ├── modeler/                       # NEW: Collaborative BPMN editor
│   │   ├── src/
│   │   ├── package.json
│   │   └── vite.config.ts
│   └── shared/                        # Shared UI components
│       ├── bpmn-viewer/               # BPMN diagram overlay component
│       ├── design-system/             # UI component library
│       └── auth/                      # Auth context/hooks
├── cli/                               # NEW: Developer CLI
│   ├── cmd/
│   └── main.go
├── proto/                             # gRPC Protobuf definitions
│   ├── gateway.proto
│   ├── process.proto
│   └── task.proto
├── deploy/                            # Deployment configs
│   ├── docker-compose.yml             # Local development
│   ├── docker-compose.full.yml        # Full stack
│   ├── helm/                          # Kubernetes Helm chart
│   │   ├── Chart.yaml
│   │   ├── values.yaml
│   │   └── templates/
│   └── terraform/                     # Cloud infrastructure
├── db/                                # Database schemas
│   ├── analytics-migrations/          # Analytics service schema
│   ├── identity-migrations/           # RBAC schema
│   └── seeds/                         # Dev seed data
├── docs/                              # Documentation
│   ├── architecture/
│   ├── api/
│   └── guides/
├── test/                              # Integration tests
│   ├── e2e/
│   └── load/
├── pom.xml                            # Root Maven POM
├── pnpm-workspace.yaml                # Frontend workspace
├── docker-compose.yml                 # Quick start
├── Makefile                           # Build commands
├── LICENSE                            # Apache 2.0
└── README.md
```

---

## Phased Delivery

### Phase 1: Foundation (Weeks 1-4)

**Goal:** Get Activiti Cloud running with all services, accessible via unified API.

- [x] Fork/import Activiti Cloud services
- [ ] Docker Compose with all services (Runtime Bundle, Query, Audit, Notifications)
- [ ] PostgreSQL + Kafka configuration
- [ ] Keycloak setup with default realm
- [ ] Spring Cloud Gateway with unified routing
- [ ] Basic health checks and Prometheus metrics
- [ ] CI/CD pipeline (build + test)
- [ ] Developer documentation (getting started)

**Deliverable:** `docker compose up` starts entire platform. REST API accessible.

### Phase 2: Operate UI (Weeks 5-10)

**Goal:** Visual process monitoring equivalent to Camunda Operate.

- [ ] React app scaffolding (Vite + TypeScript)
- [ ] Design system / component library
- [ ] Process definition list page
- [ ] Process instance list with filters
- [ ] BPMN diagram viewer with active tokens (bpmn-js)
- [ ] Instance detail page (variables, history, flow)
- [ ] Incident list and management
- [ ] Retry/cancel operations
- [ ] Real-time updates via GraphQL subscriptions
- [ ] Authentication integration (Keycloak OIDC)

**Deliverable:** Fully functional process monitoring dashboard.

### Phase 3: Tasklist UI (Weeks 8-12)

**Goal:** Human task management equivalent to Camunda Tasklist.

- [ ] Task inbox page with filters
- [ ] Task detail view
- [ ] JSON Schema form renderer
- [ ] Task claim/unclaim/assign/delegate
- [ ] Task completion with variable submission
- [ ] Form builder (in modeler)
- [ ] Task notifications (push via WebSocket)
- [ ] Mobile-responsive design

**Deliverable:** Users can view, claim, and complete tasks with forms.

### Phase 4: Analytics Service + UI (Weeks 10-16)

**Goal:** Open-source alternative to Camunda Optimize.

- [ ] Analytics microservice (consumes audit events)
- [ ] Metrics aggregation and storage (PostgreSQL)
- [ ] Process duration heatmap API
- [ ] Bottleneck detection algorithm
- [ ] SLA definition and monitoring
- [ ] Analytics React UI
- [ ] Dashboard builder
- [ ] Report export (CSV/PDF)
- [ ] Scheduled report delivery (email)

**Deliverable:** Full process analytics with visual heatmaps and SLA tracking.

### Phase 5: Collaborative Modeler (Weeks 12-18)

**Goal:** Self-hosted alternative to Camunda Web Modeler.

- [ ] bpmn-js integration in React
- [ ] dmn-js integration
- [ ] Form editor (JSON Schema visual builder)
- [ ] Yjs CRDT backend (WebSocket provider)
- [ ] Multi-user real-time editing
- [ ] Version history with diff
- [ ] Deploy-to-engine button
- [ ] Template library
- [ ] Process validation and linting

**Deliverable:** Collaborative BPMN editor deployable on-premise.

### Phase 6: Production Hardening (Weeks 16-22)

**Goal:** Ready for production workloads.

- [ ] Enhanced RBAC (resource-level permissions)
- [ ] Multi-tenancy (tenant isolation)
- [ ] Elasticsearch exporter (optional high-volume search)
- [ ] gRPC gateway
- [ ] CLI tool
- [ ] Helm chart (production-grade)
- [ ] Horizontal scaling load tests (k6)
- [ ] Backup and restore tooling
- [ ] Security audit
- [ ] Documentation site

**Deliverable:** Production-deployable platform with enterprise features.

---

## Scaling Comparison

| Scale | Camunda 8 Approach | Our Approach (Activiti Cloud) |
|-------|-------------------|-------------------------------|
| **Small** (<1K inst/day) | Single Zeebe broker | Single Runtime Bundle + PG |
| **Medium** (1K-100K/day) | 3-node Zeebe cluster | 3 Runtime Bundle replicas + PG read replicas |
| **Large** (100K-1M/day) | Multi-partition Zeebe + ES | Multiple Runtime Bundles + Kafka + ES query |
| **Very Large** (>1M/day) | Enterprise Zeebe cluster | Sharded Runtime Bundles + Kafka partitions + ES cluster |

**Key difference:** Camunda 8 uses partition-based Raft consensus (no central DB). Activiti Cloud uses RDBMS coordination. For most workloads (<100K/day), this difference is irrelevant. For very high throughput, consider adding a distributed coordination layer.

---

## Limitations vs Camunda 8

| Feature | Camunda 8 | Activiti Cloud-Based Platform |
|---------|-----------|-------------------------------|
| **Throughput ceiling** | 100K+ instances/sec (partition-based) | ~10K instances/sec (DB-bound, horizontally scalable) |
| **Consensus** | Raft (no DB dependency) | RDBMS (PostgreSQL) |
| **Event replay** | Native (append-only log) | Via Audit Service (reconstructible) |
| **Cold start** | Slow (JVM) | Slow (JVM) — same stack |
| **Memory** | High (JVM) | High (JVM) — same stack |
| **Recovery** | Raft snapshot + log replay | DB point-in-time recovery |

**For 95% of use cases, Activiti Cloud's approach is sufficient.** The Raft-based approach only matters at extreme scale (>100K instances/second).

---

## Cost Comparison (Self-Hosted)

| Component | Camunda 8 Enterprise | Our Platform (Open Source) |
|-----------|---------------------|---------------------------|
| License | $$$$$ (annual subscription) | $0 (Apache 2.0) |
| Elasticsearch | Required (3+ nodes) | Optional (PG by default) |
| Infra (small) | ~$500-1000/month | ~$200-400/month |
| Infra (medium) | ~$2000-5000/month | ~$800-1500/month |
| Support | Included in license | Community / self-managed |

---

## Quick Start (Target Developer Experience)

```bash
# Clone the repo
git clone https://github.com/<org>/open-orchestration-platform.git
cd open-orchestration-platform

# Start everything locally
docker compose up -d

# Verify services are running
curl http://localhost:8080/actuator/health

# Deploy a process
curl -X POST http://localhost:8080/v1/processes \
  -F "file=@my-process.bpmn"

# Start a process instance
curl -X POST http://localhost:8080/v1/processes/my-process/instances \
  -H "Content-Type: application/json" \
  -d '{"variables": {"orderId": "12345"}}'

# Open UIs
# Operate:    http://localhost:3000
# Tasklist:   http://localhost:3001
# Analytics:  http://localhost:3002
# Modeler:    http://localhost:3003
```

---

## Summary

**You don't need to build a workflow engine from scratch.** Activiti Cloud gives you:
- Full BPMN 2.0 execution (Apache 2.0)
- CQRS architecture (already separated)
- Event streaming (Kafka/RabbitMQ)
- Audit trail (event sourcing)
- Kubernetes-native deployment
- Connector framework

**What you build on top:**
- Modern React UIs (Operate, Tasklist, Analytics, Modeler)
- Unified API Gateway
- Enhanced RBAC
- Analytics engine
- Developer CLI
- Production Helm charts

**Total estimated effort: ~22 weeks (5-6 months)** with a team of 3-4 developers to reach feature parity with Camunda 8 core capabilities — at zero licensing cost.
