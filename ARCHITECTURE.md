# Open-Source Process Orchestration Platform — Architecture Document

## 1. Overview

This document defines the architecture for a fully open-source (Apache 2.0) alternative to Camunda 8 SaaS. The platform provides cloud-native, distributed process orchestration with BPMN/DMN execution, human task management, process monitoring, analytics, and extensible integrations.

---

## 2. Camunda 8 Analysis

### What Camunda 8 Is

A **CQRS + Event-Sourced process orchestration platform** with these core components:

| Component | Purpose | Key Tech |
|-----------|---------|----------|
| **Zeebe** | Distributed workflow engine (BPMN/DMN execution) | RocksDB, Raft consensus, SBE protocol |
| **Operate** | Process monitoring & troubleshooting UI | Elasticsearch/OpenSearch queries |
| **Tasklist** | Human task management UI | REST API, forms |
| **Optimize** | Process analytics (enterprise-only) | ES aggregations |
| **Identity** | IAM / RBAC | Keycloak, OAuth2 |
| **Connectors** | External system integrations | Outbound/Inbound adapters |
| **Gateways** | API entry points | gRPC, REST, MCP |

### Key Architectural Patterns in Camunda 8

1. **Event-Sourced State Machine**: Zeebe reconstructs state through event replay. Append-only logs provide durability with snapshot optimization for recovery speed.
2. **CQRS (Command Query Responsibility Segregation)**: Write path (commands) flows through Zeebe broker/engine. Read path (queries) goes directly to secondary storage (Elasticsearch/OpenSearch/RDBMS).
3. **Partition-Based Distributed Processing**: Workload is split across partitions, each managed by a Raft consensus group for fault tolerance.
4. **No Central Database for Execution**: RocksDB on each broker stores state locally. No relational database bottleneck.
5. **Exporter Pattern**: Events are streamed from the engine to secondary storage via pluggable exporters.

### What Makes Camunda 8 "Not Fully Open Source"

| Component | Issue |
|-----------|-------|
| **Optimize** | Enterprise-only, closed source analytics |
| **Web Modeler** | SaaS-only collaborative BPMN designer |
| **Console** | SaaS cluster management |
| **Licensing** | Parts are source-available but SSPL/proprietary |

---

## 3. Design Principles

1. **Event-sourced, partition-based execution** — same proven pattern as Zeebe
2. **CQRS separation** — writes hit the engine, reads hit secondary storage
3. **PostgreSQL-first** — lower operational cost than Elasticsearch dependency
4. **Fully permissive license** — Apache 2.0 throughout
5. **Polyglot-friendly** — Go for engine (performance + single binary), TypeScript for UIs
6. **Kubernetes-native** but runnable locally with a single binary
7. **OpenTelemetry-native observability** — traces, metrics, logs from day one

---

## 4. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (Go)                          │
│              REST + gRPC + GraphQL + WebSocket                   │
└──────────┬──────────────────────────────────┬───────────────────┘
           │ (commands)                       │ (queries)
┌──────────▼──────────────┐      ┌───────────▼───────────────────┐
│   Orchestration Engine   │      │      Query Service (Go)       │
│        (Go + Raft)       │      │   Reads from secondary store  │
│                          │      └───────────▲───────────────────┘
│  ┌────────────────────┐  │                  │
│  │ Partition 1 (Raft) │  │      ┌───────────┴───────────────────┐
│  │ Partition 2 (Raft) │  │      │     Secondary Storage          │
│  │ Partition N (Raft) │  │──────▶  PostgreSQL (default)          │
│  └────────────────────┘  │      │  or Elasticsearch/OpenSearch   │
│                          │      └───────────────────────────────┘
│  State: Pebble/BadgerDB  │
│  Log: WAL + Snapshots    │
└──────────┬───────────────┘
           │ (events via exporters)
┌──────────▼───────────────┐
│       Exporter SPI        │
│  PostgreSQL | ES | Kafka  │
│  Webhook | Custom         │
└───────────────────────────┘
```

### Data Flow

```
┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌────────────┐
│  Client  │────▶│ Gateway  │────▶│   Engine     │────▶│  Exporter  │
│  (SDK)   │     │ REST/gRPC│     │  (Raft +     │     │  (PG/ES/   │
└──────────┘     └──────────┘     │   Pebble)    │     │   Kafka)   │
                                  └──────────────┘     └─────┬──────┘
                                                             │
                 ┌──────────┐     ┌──────────────┐           │
                 │  Web UI  │◀────│ Query Service │◀──────────┘
                 │ (React)  │     │   (Go)       │    (secondary store)
                 └──────────┘     └──────────────┘
```

---

## 5. Component Architecture

### 5.1 Orchestration Engine (`engine/`)

The heart of the platform. A distributed, event-sourced BPMN/DMN execution engine.

| Sub-module | Responsibility |
|------------|---------------|
| `engine/core` | BPMN state machine, token-based execution |
| `engine/dmn` | Decision table evaluation (FEEL expressions) |
| `engine/raft` | Consensus via Dragonboat (multi-group Raft) |
| `engine/store` | PebbleDB for partition state |
| `engine/log` | Append-only event log with compaction + snapshots |
| `engine/scheduler` | Job scheduling, timers, retries, deadlines |
| `engine/exporter` | SPI for streaming events to secondary stores |
| `engine/cluster` | Membership, partition assignment, rebalancing |

**Execution Model:**
- Each partition runs as an independent Raft group
- Commands are appended to the partition's log
- State machine applies commands deterministically
- Events emitted to exporters asynchronously
- Snapshots taken periodically for fast recovery

**Supported BPMN Elements (Target: Full BPMN 2.0):**
- Start/End events (none, message, timer, signal, error)
- Tasks (service, user, script, send, receive, business rule)
- Gateways (exclusive, parallel, inclusive, event-based)
- Subprocesses (embedded, call activity, event subprocess)
- Boundary events (timer, message, error, escalation, compensation)
- Multi-instance (parallel, sequential)

### 5.2 API Gateway (`gateway/`)

| Sub-module | Responsibility |
|------------|---------------|
| `gateway/rest` | OpenAPI 3.1 REST endpoints |
| `gateway/grpc` | Protobuf service definitions |
| `gateway/ws` | WebSocket for real-time process events |
| `gateway/auth` | JWT validation, API key management |
| `gateway/router` | Request routing to partitions (hash-based) |

**Key APIs:**
- `POST /processes` — Deploy process definition
- `POST /processes/{key}/instances` — Start process instance
- `POST /jobs/{key}/complete` — Complete a job
- `GET /instances/{key}` — Get process instance state
- `GET /tasks` — List human tasks (with filters)
- `POST /messages` — Publish a message to correlate

### 5.3 Query Service (`query/`)

Handles all read operations against secondary storage.

- Abstracts over PostgreSQL and Elasticsearch backends
- Cursor-based pagination for large result sets
- Real-time subscriptions via WebSocket/SSE
- Full-text search on process variables
- Aggregation queries for analytics

### 5.4 Process Monitor — Operate (`apps/operate/`)

**Tech:** React + TypeScript

**Features:**
- Real-time process instance visualization with BPMN diagram overlay
- Active token positions shown on diagram
- Incident management (view, resolve, retry)
- Variable inspection and modification
- Process definition version management
- Batch operations (cancel, retry multiple instances)
- Audit log with full event history

### 5.5 Human Task Manager — Tasklist (`apps/tasklist/`)

**Tech:** React + TypeScript

**Features:**
- Task inbox with filtering, sorting, search
- Dynamic form rendering (JSON Schema-based forms)
- Task assignment, delegation, and claiming
- Task groups and queues
- Variable editor for task completion
- REST API for programmatic task interaction
- Notifications (WebSocket push)

### 5.6 Process Analytics (`apps/analytics/`)

**Replaces proprietary Camunda Optimize — fully open-source.**

**Tech:** React + TypeScript

**Features:**
- Process heatmaps (duration overlay on BPMN diagrams)
- SLA monitoring and breach alerts
- Bottleneck detection (cycle time analysis per element)
- Custom dashboards with SQL-based widget queries
- Process comparison (version A vs version B)
- Flow node statistics (frequency, duration percentiles)
- Built on PostgreSQL aggregations (no ES dependency required)
- Export reports as CSV/PDF

### 5.7 Collaborative Modeler (`apps/modeler/`)

**Tech:** React + TypeScript, bpmn.io (already open-source)

**Features:**
- BPMN 2.0 diagram editor (based on bpmn-js)
- DMN decision table editor (based on dmn-js)
- Form editor (JSON Schema-based)
- Real-time collaboration via CRDTs (Yjs)
- Version history with diff visualization
- Deploy directly to engine from modeler
- Template library for common patterns
- Validation and linting

### 5.8 Identity & Access Management (`identity/`)

**Tech:** Go

**Features:**
- OAuth2 / OIDC provider (built-in or delegate to external IdP)
- Supported external IdPs: Keycloak, Auth0, Zitadel, Okta, Azure AD
- Role-based access control (RBAC) with resource-level permissions
- API key management for M2M communication
- Multi-tenancy support (tenant isolation at partition level)
- User/group management API
- Session management

**Permission Model:**
```
Tenant → Organization → Project → Resource
                                      ↓
                              Process Definition
                              Process Instance
                              Task
                              Decision
                              Connector
```

### 5.9 Connector Framework (`connectors/`)

**Tech:** Go (runtime) + Multi-language SDK

| Sub-module | Responsibility |
|------------|---------------|
| `connectors/runtime` | Connector execution, lifecycle management |
| `connectors/sdk-go` | Go SDK for building connectors |
| `connectors/sdk-java` | Java SDK for building connectors |
| `connectors/sdk-python` | Python SDK for building connectors |
| `connectors/sdk-node` | Node.js SDK for building connectors |
| `connectors/builtin/` | Pre-built connectors |

**Built-in Connectors:**
- HTTP/REST (outbound + webhook inbound)
- Kafka (produce + consume)
- AMQP / RabbitMQ
- gRPC
- Email (SMTP + IMAP)
- S3 / GCS / Azure Blob
- Database (PostgreSQL, MySQL, MongoDB)
- Slack, Microsoft Teams
- AWS Lambda, Google Cloud Functions

**Connector Features:**
- Rate limiting per connector instance
- Circuit breaker pattern
- Retry with exponential backoff
- Secret management integration (Vault, AWS Secrets Manager)
- Input/output variable mapping
- Error handling with BPMN error codes

### 5.10 Developer CLI (`cli/`)

**Tech:** Go (Cobra framework)

```bash
flow deploy ./processes/           # Deploy process definitions
flow start order-process           # Start a process instance
flow status                        # Cluster health and metrics
flow instances list                # List process instances
flow jobs list                     # List available jobs
flow jobs complete <key>           # Complete a job
flow connectors list               # List registered connectors
flow dev                           # Start local dev mode (single binary)
flow migrate                       # Run schema migrations
flow backup create                 # Create cluster backup
flow topology                      # Show cluster partition topology
```

---

## 6. Technology Choices

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Engine language | **Go** | Single binary, excellent concurrency, Raft libs |
| Frontend | **React + TypeScript** | Ecosystem, bpmn.io compatibility |
| Primary state store | **PebbleDB** | Modern RocksDB alternative, CockroachDB-maintained |
| Secondary storage | **PostgreSQL** | Ubiquitous, lower ops cost than ES |
| Optional secondary | **Elasticsearch/OpenSearch** | For high-volume search workloads |
| Consensus | **Dragonboat** | Battle-tested multi-group Raft in Go |
| Serialization | **Protobuf** | Cross-language, schema evolution |
| Expression language | **FEEL** | BPMN standard compliance |
| Forms | **JSON Schema + RJSF** | Standard, extensible |
| Auth protocol | **OIDC** | Plug into any identity provider |
| Observability | **OpenTelemetry** | Vendor-neutral traces, metrics, logs |
| Streaming | **NATS JetStream** | Lightweight alternative to Kafka for events |
| Build system | **Go modules + pnpm workspaces** | Fast, reproducible |
| Container | **Docker + Helm** | Standard Kubernetes deployment |

---

## 7. Key Architectural Decisions

### ADR-001: Go Over Java

**Context:** Camunda 8 uses Java/Spring Boot. JVM has high memory overhead (512MB–2GB per service).

**Decision:** Use Go for all backend services.

**Consequences:**
- Single binary deployment (~20–50MB)
- ~50MB RSS memory footprint vs 512MB+ for JVM
- Sub-second cold start (important for serverless/edge)
- Excellent Raft libraries available (Dragonboat handles thousands of groups)
- Trade-off: Less enterprise library ecosystem than Java

### ADR-002: PostgreSQL-First Over Elasticsearch-First

**Context:** Camunda 8 requires Elasticsearch for Operate/Tasklist. ES adds significant operational complexity.

**Decision:** PostgreSQL as default secondary storage. Elasticsearch as optional.

**Consequences:**
- 90% of deployments need only PostgreSQL
- Full-text search via `tsvector`/`tsquery`
- Analytics via window functions and CTEs
- ES available for high-volume (>10M instances) deployments
- Trade-off: Slightly less performant full-text search than ES

### ADR-003: PebbleDB Over RocksDB

**Context:** Camunda uses RocksDB (via JNI) for Zeebe's partition state.

**Decision:** Use PebbleDB (Go-native LSM tree by CockroachDB).

**Consequences:**
- No CGo dependency (pure Go, easier cross-compilation)
- Actively maintained by CockroachDB team
- Battle-tested in CockroachDB production
- API-compatible patterns with RocksDB
- Better Go integration and garbage collection behavior

### ADR-004: Dragonboat for Multi-Group Raft

**Context:** Each partition needs its own Raft group. Need efficient management of many groups.

**Decision:** Use Dragonboat library for consensus.

**Consequences:**
- Handles 100,000+ Raft groups in a single process
- Built-in snapshotting and log compaction
- Pipelining and batching for high throughput
- Proven in production at scale
- Trade-off: Less community adoption than Hashicorp Raft (but better multi-group support)

### ADR-005: Protobuf Over SBE

**Context:** Camunda 8 uses Simple Binary Encoding (SBE) for internal protocol. SBE is faster but harder to evolve.

**Decision:** Use Protocol Buffers for all serialization.

**Consequences:**
- Schema evolution with backward/forward compatibility
- Cross-language code generation (Go, Java, Python, TypeScript)
- Extensive tooling (grpcurl, buf, protoc plugins)
- Trade-off: ~10-20% slower serialization than SBE (negligible for our throughput targets)

### ADR-006: Single Binary Dev Mode

**Context:** Camunda 8 requires Docker Compose with 5+ services for local development.

**Decision:** Provide a `flow dev` command that runs everything in-process.

**Consequences:**
- Engine, gateway, query service, and embedded PostgreSQL in one binary
- Zero external dependencies for development
- Start in <2 seconds
- Automatic hot-reload of process definitions
- Same code paths as production (not a separate implementation)

---

## 8. Repository Structure

```
/
├── engine/                    # Core orchestration engine (Go)
│   ├── core/                  # BPMN state machine, execution
│   ├── dmn/                   # DMN decision engine
│   ├── raft/                  # Dragonboat consensus integration
│   ├── store/                 # PebbleDB state management
│   ├── log/                   # Append-only event log
│   ├── scheduler/             # Timers, jobs, retries
│   ├── exporter/              # Exporter SPI + implementations
│   │   ├── postgres/
│   │   ├── elasticsearch/
│   │   ├── kafka/
│   │   └── webhook/
│   ├── cluster/               # Membership, partition management
│   └── feel/                  # FEEL expression evaluator
├── gateway/                   # API gateway (Go)
│   ├── rest/                  # REST API handlers
│   ├── grpc/                  # gRPC service implementation
│   ├── ws/                    # WebSocket real-time events
│   ├── auth/                  # Authentication middleware
│   └── router/                # Partition-aware request routing
├── query/                     # Query service (Go)
│   ├── postgres/              # PostgreSQL query implementation
│   ├── elasticsearch/         # ES query implementation
│   └── api/                   # Query API definitions
├── identity/                  # IAM service (Go)
│   ├── oidc/                  # OIDC provider/consumer
│   ├── rbac/                  # Role-based access control
│   ├── tenant/                # Multi-tenancy
│   └── apikey/                # API key management
├── connectors/                # Connector framework
│   ├── runtime/               # Connector execution runtime (Go)
│   ├── sdk-go/                # Go connector SDK
│   ├── sdk-java/              # Java connector SDK
│   ├── sdk-python/            # Python connector SDK
│   ├── sdk-node/              # Node.js connector SDK
│   └── builtin/               # Pre-built connectors
│       ├── http/
│       ├── kafka/
│       ├── amqp/
│       ├── email/
│       ├── s3/
│       └── slack/
├── cli/                       # Developer CLI (Go)
│   ├── cmd/                   # CLI commands
│   └── dev/                   # Embedded dev mode
├── apps/                      # Web applications (React/TypeScript)
│   ├── operate/               # Process monitor
│   │   ├── src/
│   │   └── package.json
│   ├── tasklist/              # Task manager
│   │   ├── src/
│   │   └── package.json
│   ├── analytics/             # Process analytics (replaces Optimize)
│   │   ├── src/
│   │   └── package.json
│   ├── modeler/               # BPMN/DMN collaborative editor
│   │   ├── src/
│   │   └── package.json
│   └── shared/                # Shared UI components
│       ├── bpmn-viewer/
│       ├── design-system/
│       └── auth/
├── proto/                     # Protobuf definitions (shared)
│   ├── engine/
│   ├── gateway/
│   ├── query/
│   └── identity/
├── sdk/                       # Client SDKs
│   ├── go/
│   ├── java/
│   ├── python/
│   └── node/
├── deploy/                    # Deployment configurations
│   ├── docker/                # Dockerfiles + docker-compose
│   ├── helm/                  # Kubernetes Helm charts
│   │   ├── charts/
│   │   └── values.yaml
│   └── terraform/             # Cloud infrastructure modules
│       ├── aws/
│       ├── gcp/
│       └── azure/
├── db/                        # Database schemas
│   ├── migrations/            # PostgreSQL migrations (golang-migrate)
│   └── seeds/                 # Development seed data
├── test/                      # Integration & E2E tests
│   ├── integration/
│   ├── e2e/
│   ├── load/                  # Load testing (k6)
│   └── conformance/           # BPMN conformance test suite
├── docs/                      # Documentation
│   ├── architecture/          # ADRs and design docs
│   ├── api/                   # API reference (generated from proto)
│   ├── guides/                # User guides
│   └── contributing/          # Contributor docs
├── .github/
│   └── workflows/             # CI/CD pipelines
├── Makefile                   # Build orchestration
├── go.work                    # Go workspace (multi-module)
├── pnpm-workspace.yaml        # Frontend workspace
├── LICENSE                    # Apache 2.0
└── README.md
```

---

## 9. Communication Patterns

### Internal (Engine Cluster)

```
Broker A ◄──── Raft Replication ────► Broker B
    │                                      │
    │          Raft Replication             │
    └──────────────┬───────────────────────┘
                   │
                   ▼
               Broker C
```

- **Raft consensus** for log replication within each partition
- **gRPC** for inter-node RPC (snapshot transfer, admin)
- **SWIM protocol** for cluster membership gossip

### External (Client ↔ Platform)

```
Client SDK ──── gRPC (binary, streaming) ────► Gateway
Browser    ──── REST/JSON ──────────────────► Gateway
Real-time  ──── WebSocket ─────────────────► Gateway
```

### Event Export (Engine → Secondary Storage)

```
Engine ──── Async batch export ────► PostgreSQL
       ──── Async batch export ────► Elasticsearch (optional)
       ──── Streaming ─────────────► Kafka/NATS (optional)
       ──── HTTP POST ─────────────► Webhook (optional)
```

---

## 10. Scalability Model

### Horizontal Scaling

| Component | Scaling Strategy |
|-----------|-----------------|
| Engine brokers | Add nodes, rebalance partitions |
| Gateway | Stateless, scale with load balancer |
| Query service | Stateless, scale with read replicas |
| Web apps | Static assets on CDN, API calls to gateway |
| Connectors | Scale independently per connector type |
| PostgreSQL | Read replicas, partitioning by tenant |

### Partition Strategy

- Default: 3 partitions with replication factor 3
- Scale up to 256 partitions for high-throughput workloads
- Round-robin partition assignment for new instances
- Correlation-based routing for message events

### Performance Targets

| Metric | Target |
|--------|--------|
| Process instance throughput | 10,000+ instances/sec (per partition) |
| Job completion latency (p99) | < 10ms |
| Process start latency (p99) | < 20ms |
| Recovery time (partition failover) | < 5 seconds |
| Single binary memory (dev mode) | < 100MB RSS |

---

## 11. Security Architecture

### Authentication Flow

```
┌────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐
│ Client │────▶│ Gateway  │────▶│ Identity │────▶│  IdP    │
│        │◀────│ (JWT     │◀────│ (verify) │◀────│(Keycloak│
│        │     │  check)  │     │          │     │ Auth0)  │
└────────┘     └──────────┘     └──────────┘     └─────────┘
```

### Security Layers

1. **Transport**: TLS 1.3 for all communication (mTLS for inter-node)
2. **Authentication**: JWT/OIDC tokens or API keys
3. **Authorization**: RBAC with resource-level permissions
4. **Secrets**: Integration with HashiCorp Vault, AWS Secrets Manager
5. **Audit**: Full audit log of all administrative actions
6. **Encryption at rest**: AES-256 for PebbleDB state files
7. **Network policies**: Kubernetes NetworkPolicy for pod isolation
8. **Rate limiting**: Per-tenant rate limits at gateway level

### Multi-Tenancy Isolation

- Partition-level tenant isolation (dedicated partitions per tenant)
- Row-level security in PostgreSQL for shared partitions
- Tenant-scoped API keys and JWT claims
- Resource quotas per tenant (instances, jobs, storage)

---

## 12. Observability

### OpenTelemetry Integration

```yaml
Traces:  Process instance lifecycle, job execution, connector calls
Metrics: Instance throughput, job latency, partition health, exporter lag
Logs:    Structured JSON logs with correlation IDs
```

### Built-in Dashboards (Grafana-compatible)

- Cluster health overview
- Partition leader distribution
- Exporter lag monitoring
- Process instance throughput
- Job worker utilization
- Error rates and incident tracking

### Health Endpoints

```
GET /health/live     # Liveness probe
GET /health/ready    # Readiness probe (includes partition health)
GET /health/startup  # Startup probe (Raft group initialized)
GET /metrics         # Prometheus metrics endpoint
```

---

## 13. Deployment Models

### Development (Single Binary)

```bash
flow dev  # Starts everything: engine + gateway + query + embedded PG + UIs
```

### Docker Compose (Small Teams)

```yaml
services:
  engine:
    image: flowengine/engine:latest
    ports: ["26500:26500"]
  gateway:
    image: flowengine/gateway:latest
    ports: ["8080:8080"]
  postgres:
    image: postgres:16
  operate:
    image: flowengine/operate:latest
    ports: ["3000:3000"]
```

### Kubernetes (Production)

```bash
helm install flow-platform flowengine/flow-platform \
  --set engine.replicas=3 \
  --set engine.partitions=12 \
  --set engine.replicationFactor=3 \
  --set gateway.replicas=2 \
  --set postgresql.enabled=true
```

---

## 14. Migration Path from Camunda 8

For teams currently on Camunda 8 who want to migrate:

| Concern | Migration Strategy |
|---------|-------------------|
| BPMN models | Direct import (standard BPMN 2.0 XML) |
| DMN models | Direct import (standard DMN XML) |
| Job workers | Adapter layer for Zeebe client protocol |
| REST API | Compatibility shim for Zeebe REST API |
| Elasticsearch data | Migration tool for historical data |
| Connectors | Re-implement using our SDK (similar patterns) |
| Identity/Auth | Map Keycloak roles to our RBAC model |

---

## 15. Comparison: This Platform vs Camunda 8

| Concern | Camunda 8 | This Platform |
|---------|-----------|---------------|
| License | Mixed (SSPL + proprietary) | Apache 2.0 |
| Language | Java (JVM) | Go (single binary) |
| Memory per service | 512MB–2GB | 50–100MB |
| Cold start | 10–30 seconds | < 2 seconds |
| Required infra | Elasticsearch mandatory | PostgreSQL only |
| Analytics | Enterprise-only (Optimize) | Open-source (built-in) |
| Modeler | SaaS-only (Web Modeler) | Self-hosted + collaborative |
| Dev experience | Docker Compose with 5+ services | Single binary (`flow dev`) |
| Deployment | Complex multi-service | Single binary → Helm chart |
| Observability | Custom metrics | OpenTelemetry native |
| Multi-tenancy | Enterprise feature | Built-in |

---

## 16. Phased Delivery Plan

### Phase 1: Foundation (Months 1–3)

**Goal:** Minimal viable engine that can execute simple processes.

- [ ] Single-node engine executing linear BPMN processes
- [ ] Service tasks, exclusive gateways, parallel gateways
- [ ] REST gateway with deploy/start/complete operations
- [ ] PostgreSQL exporter (process instances, jobs, variables)
- [ ] CLI tool (`flow deploy`, `flow start`, `flow dev`)
- [ ] Docker Compose for local development
- [ ] Basic integration test suite
- [ ] CI/CD pipeline (GitHub Actions)

**Deliverable:** Deploy a BPMN process, start instances, complete jobs via REST.

### Phase 2: Clustering & UIs (Months 3–6)

**Goal:** Multi-node fault tolerance and user-facing applications.

- [ ] Multi-partition Raft clustering (Dragonboat)
- [ ] Partition rebalancing on node join/leave
- [ ] Snapshot and restore for partitions
- [ ] Operate UI (process instance list, diagram overlay, incidents)
- [ ] Tasklist UI (task inbox, form rendering, completion)
- [ ] Timer events (intermediate, boundary, start)
- [ ] Message events (correlation, buffering)
- [ ] Connector SDK (Go) + HTTP connector
- [ ] gRPC gateway
- [ ] Go client SDK

### Phase 3: Production Readiness (Months 6–9)

**Goal:** Ready for production workloads.

- [ ] Identity service with RBAC
- [ ] API key management
- [ ] Analytics dashboard (heatmaps, SLA tracking, bottleneck detection)
- [ ] Full backup/restore to S3/GCS/Azure Blob
- [ ] Horizontal scaling validated at load (k6 load tests)
- [ ] Helm chart for Kubernetes deployment
- [ ] Elasticsearch exporter (optional)
- [ ] Multi-instance activities (parallel + sequential)
- [ ] Error events, escalation events
- [ ] Compensation handling
- [ ] Python and Node.js client SDKs

### Phase 4: Ecosystem (Months 9–12)

**Goal:** Full-featured platform with ecosystem.

- [ ] Collaborative modeler (Yjs-based real-time editing)
- [ ] Connector marketplace / registry
- [ ] Multi-tenancy (partition-level isolation)
- [ ] Signal events, link events
- [ ] Event subprocesses
- [ ] Call activities (nested process invocation)
- [ ] Full BPMN 2.0 conformance test suite passing
- [ ] Terraform modules for AWS/GCP/Azure
- [ ] Java and Python connector SDKs
- [ ] Plugin system for custom extensions
- [ ] Documentation site

---

## 17. Open Questions / Future Considerations

1. **FEEL implementation**: Build custom Go parser or embed existing (e.g., via WASM from Java implementation)?
2. **Form engine**: JSON Schema Forms vs custom DSL vs embed existing (Formio)?
3. **Event streaming**: NATS JetStream vs embedded Kafka (Redpanda) vs none?
4. **Process versioning**: Migrate running instances to new version or version-lock?
5. **Cloud marketplace**: Offer managed version on AWS/GCP/Azure marketplaces?
6. **AI integration**: Built-in LLM connector for AI-assisted process decisions?

---

## 18. Contributing

This project is licensed under Apache 2.0. Contributions welcome.

**Development Prerequisites:**
- Go 1.22+
- Node.js 20+ with pnpm
- Docker and Docker Compose
- PostgreSQL 16+ (or use `flow dev` for embedded)
- Protocol Buffers compiler (protoc)
- Buf CLI for proto management

**Quick Start:**
```bash
git clone https://github.com/<org>/flow-engine.git
cd flow-engine
make build
./bin/flow dev        # Starts everything locally
# Visit http://localhost:3000 for Operate UI
```
