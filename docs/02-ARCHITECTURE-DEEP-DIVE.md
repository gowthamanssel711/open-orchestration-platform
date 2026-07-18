# 02 — Architecture Deep Dive

## System Context

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL ACTORS                                │
│                                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Process  │  │ Task     │  │ External │  │ Admin    │              │
│  │ Developer│  │ Worker   │  │ System   │  │ Operator │              │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘              │
│       │              │              │              │                    │
└───────┼──────────────┼──────────────┼──────────────┼────────────────────┘
        │              │              │              │
        ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    OPEN ORCHESTRATION PLATFORM                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Core Architecture Pattern: CQRS + Event Sourcing

```
                    COMMAND SIDE                    QUERY SIDE
                    ──────────                     ──────────
                         │                              │
   ┌─────────────┐      │      ┌────────────────┐     │    ┌─────────────┐
   │ REST/gRPC   │──────┼─────▶│ Runtime Bundle │     │    │ Query       │
   │ Gateway     │      │      │ (Activiti)     │     │    │ Service     │
   └─────────────┘      │      └───────┬────────┘     │    └──────▲──────┘
                         │              │              │           │
                         │              │ Events       │           │
                         │              ▼              │           │
                         │      ┌────────────────┐    │    ┌──────┴──────┐
                         │      │ Apache Kafka   │────┼───▶│ PostgreSQL  │
                         │      │ (Event Bus)    │    │    │ (Read Model)│
                         │      └───────┬────────┘    │    └─────────────┘
                         │              │              │
                         │         ┌────┴────┐        │
                         │         │         │        │
                         │    ┌────▼───┐ ┌───▼────┐   │
                         │    │ Audit  │ │Analytic│   │
                         │    │Service │ │Service │   │
                         │    └────────┘ └────────┘   │
                         │                            │
```

**Write Path:** Client → Gateway → Runtime Bundle → Kafka (events emitted)  
**Read Path:** Client → Gateway → Query Service → PostgreSQL (read model)  
**Analytics Path:** Kafka → Analytics Service → PostgreSQL (aggregated metrics)  
**Audit Path:** Kafka → Audit Service → PostgreSQL (immutable event log)  

---

## Service Architecture

### Service Catalog

| Service | Type | Source | Port | Database | Description |
|---------|------|--------|------|----------|-------------|
| **api-gateway** | Custom | New build | 8080 | None | Unified API routing + auth |
| **runtime-bundle** | Activiti | Activiti Cloud | 8081 | `oop_runtime` | BPMN execution engine |
| **query-service** | Activiti | Activiti Cloud | 8082 | `oop_query` | CQRS read model |
| **audit-service** | Activiti | Activiti Cloud | 8083 | `oop_audit` | Event store |
| **analytics-service** | Custom | New build | 8084 | `oop_analytics` | Metrics + aggregation |
| **notification-service** | Activiti | Activiti Cloud | 8085 | None | GraphQL subscriptions |
| **connector-service** | Custom | New build | 8086 | None | Connector runtime |
| **identity-service** | Custom | New build | 8087 | `oop_identity` | RBAC + tenant mgmt |
| **operate-ui** | Custom | New build | 3000 | None | Process monitoring |
| **tasklist-ui** | Custom | New build | 3001 | None | Task management |
| **analytics-ui** | Custom | New build | 3002 | None | Dashboards |
| **modeler-ui** | Custom | New build | 3003 | None | BPMN editor |

---

## Data Flow: Process Instance Lifecycle

```
1. DEPLOY PROCESS
   Developer → Gateway → Runtime Bundle → Process stored in DB
                                        → Event: PROCESS_DEPLOYED → Kafka
                                                                      │
                                        Query Service ← ─ ─ ─ ─ ─ ─ ─┘
                                        Audit Service ← ─ ─ ─ ─ ─ ─ ─┘

2. START INSTANCE
   Client → Gateway → Runtime Bundle → Instance created in DB
                                      → Event: PROCESS_STARTED → Kafka
                                      → Event: ACTIVITY_STARTED → Kafka
                                                                    │
                                      Query Service (updates) ← ─ ─ ┘
                                      Analytics Service ← ─ ─ ─ ─ ─ ┘
                                      Notification Service ← ─ ─ ─ ─┘

3. EXECUTE SERVICE TASK
   Runtime Bundle → Event: TASK_CREATED → Kafka → Connector Service
                                                         │
   Connector Service calls external API                  │
                                                         │
   Connector Service → Runtime Bundle: task complete      │
   Runtime Bundle → Event: TASK_COMPLETED → Kafka        │
                                                         │
                   Query/Audit/Analytics update ← ─ ─ ─ ┘

4. HUMAN TASK
   Runtime Bundle → Event: USER_TASK_CREATED → Kafka → Query Service
                                                            │
   Tasklist UI polls Query Service for tasks                │
   User completes task via Gateway → Runtime Bundle         │
   Runtime Bundle → Event: USER_TASK_COMPLETED → Kafka     │

5. PROCESS COMPLETE
   Runtime Bundle → Event: PROCESS_COMPLETED → Kafka
                   → All services update final state
                   → Analytics records duration metrics
```

---

## Database Architecture

### Separation of Concerns

```
┌─────────────────────────────────────────────────────────┐
│                    PostgreSQL Cluster                     │
├──────────────┬──────────────┬──────────────┬────────────┤
│  oop_runtime │  oop_query   │  oop_audit   │oop_analytic│
│              │              │              │            │
│ Process defs │ Denormalized │ Immutable    │ Aggregated │
│ Instances    │ process view │ event log    │ metrics    │
│ Tasks        │ Task view    │ Full history │ Time-series│
│ Variables    │ Variable view│              │ SLA data   │
│ Jobs         │              │              │ Heatmaps   │
│              │              │              │            │
│ WRITE-       │ READ-        │ APPEND-      │ AGGREGATE- │
│ OPTIMIZED    │ OPTIMIZED    │ ONLY         │ OPTIMIZED  │
└──────────────┴──────────────┴──────────────┴────────────┘
```

### Key Schema Concepts

**Runtime DB** (Activiti-managed):
- `ACT_RU_*` tables — running process/task state
- `ACT_HI_*` tables — historical data
- `ACT_RE_*` tables — repository (deployments, definitions)

**Query DB** (denormalized for fast reads):
- `process_instance` — flat view of all instances
- `task` — flat view of all tasks
- `variable` — process variables
- `process_definition` — deployed definitions

**Audit DB** (append-only event log):
- `cloud_runtime_event` — every event from the engine
- Partitioned by timestamp for efficient retention

**Analytics DB** (custom aggregations):
- `process_metrics` — duration, throughput per definition
- `activity_metrics` — per-activity timing
- `sla_tracking` — SLA definitions and breach records
- `dashboard_widget` — user-configured dashboards

---

## Messaging Architecture

### Kafka Topic Design

```
Topic Naming Convention: oop.<service>.<entity>.<action>

Core Topics:
├── oop.engine.process.events          # All process lifecycle events
├── oop.engine.task.events             # All task lifecycle events
├── oop.engine.activity.events         # Activity-level events
├── oop.engine.variable.events         # Variable change events
├── oop.engine.signal.events           # BPMN signal events
├── oop.engine.message.events          # BPMN message events
├── oop.connector.integration.input    # Commands TO connectors
├── oop.connector.integration.output   # Results FROM connectors
├── oop.notification.events            # UI notification triggers
└── oop.analytics.metrics              # Processed metrics
```

### Event Schema (CloudEvents Specification)

```json
{
  "specversion": "1.0",
  "type": "org.activiti.process.started",
  "source": "runtime-bundle-1",
  "id": "evt-uuid-here",
  "time": "2026-07-15T10:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "processInstanceId": "pi-123",
    "processDefinitionId": "order-process:3",
    "processDefinitionKey": "order-process",
    "businessKey": "ORDER-456",
    "initiator": "john.doe",
    "variables": {
      "orderId": "456",
      "amount": 99.99
    }
  }
}
```

---

## API Gateway Architecture

```
┌─────────────────────────────────────────────────────┐
│                   API GATEWAY                         │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────┐  ┌────────────┐  ┌─────────────┐ │
│  │ Rate Limiter│  │ JWT/OIDC   │  │ Request     │ │
│  │ (Redis)     │  │ Validation │  │ Routing     │ │
│  └─────────────┘  └────────────┘  └─────────────┘ │
│                                                     │
│  ROUTING RULES:                                     │
│  /api/v1/processes/**   → runtime-bundle            │
│  /api/v1/query/**       → query-service             │
│  /api/v1/tasks/**       → query-service (GET)       │
│  /api/v1/tasks/**       → runtime-bundle (POST)     │
│  /api/v1/analytics/**   → analytics-service         │
│  /api/v1/connectors/**  → connector-service         │
│  /api/v1/admin/**       → identity-service          │
│  /ws/**                 → notification-service       │
│  /graphql               → notification-service       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Authentication & Authorization Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │────▶│ Gateway  │────▶│ Keycloak │     │ Identity │
│          │     │          │     │ (OIDC)   │     │ Service  │
│          │◀────│          │◀────│          │     │ (RBAC)   │
│          │ JWT │          │Token│          │     │          │
└──────────┘     └──────┬───┘     └──────────┘     └──────────┘
                        │                                 ▲
                        │ JWT (validated)                  │
                        ▼                                  │
                 ┌──────────────┐     Permission check    │
                 │ Runtime/Query│─────────────────────────┘
                 │ Service      │
                 └──────────────┘
```

**Flow:**
1. Client authenticates with Keycloak → receives JWT
2. Client sends request to Gateway with Bearer token
3. Gateway validates JWT signature and expiry
4. Gateway forwards request with user context headers
5. Service checks permissions via Identity Service (cached)
6. Service executes if authorized

---

## Connector Architecture

```
┌───────────────────────────────────────────────┐
│              CONNECTOR SERVICE                  │
├───────────────────────────────────────────────┤
│                                               │
│  ┌─────────────────────────────────────────┐  │
│  │         Connector Runtime                │  │
│  │                                         │  │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌───────┐  │  │
│  │  │ HTTP │ │Kafka │ │Email │ │Custom │  │  │
│  │  │Conn. │ │Conn. │ │Conn. │ │Conn.  │  │  │
│  │  └──┬───┘ └──┬───┘ └──┬───┘ └──┬────┘  │  │
│  │     │        │        │        │       │  │
│  │  ┌──▼────────▼────────▼────────▼────┐  │  │
│  │  │      Connector SDK                │  │  │
│  │  │  - Input/Output mapping           │  │  │
│  │  │  - Error handling → BPMN errors   │  │  │
│  │  │  - Retry with backoff             │  │  │
│  │  │  - Circuit breaker                │  │  │
│  │  │  - Secret resolution              │  │  │
│  │  └──────────────────────────────────┘  │  │
│  └─────────────────────────────────────────┘  │
│                                               │
│  INBOUND (Webhook/Polling):                   │
│  External System → Connector → Kafka →        │
│  → Runtime Bundle (correlate message/signal)  │
│                                               │
│  OUTBOUND (Task execution):                   │
│  Runtime Bundle → Kafka → Connector →         │
│  → External System → Result → Kafka →         │
│  → Runtime Bundle (complete task)             │
│                                               │
└───────────────────────────────────────────────┘
```

---

## High Availability Architecture

### Multi-Instance Deployment

```
                    ┌─────────────────────┐
                    │   Load Balancer     │
                    │   (Ingress/Traefik) │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──┐   ┌────────▼───┐   ┌───────▼────┐
    │ Gateway-1  │   │ Gateway-2  │   │ Gateway-3  │
    └─────────┬──┘   └────────┬───┘   └───────┬────┘
              │                │                │
              └────────────────┼────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──┐   ┌────────▼───┐   ┌───────▼────┐
    │ Runtime-1  │   │ Runtime-2  │   │ Runtime-3  │
    └─────────┬──┘   └────────┬───┘   └───────┬────┘
              │                │                │
              └────────────────┼────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──┐   ┌────────▼───┐   ┌───────▼────┐
    │ Kafka-1    │   │ Kafka-2    │   │ Kafka-3    │
    └────────────┘   └────────────┘   └────────────┘
              │                │                │
              └────────────────┼────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                                │
    ┌─────────▼──────┐              ┌──────────▼─────┐
    │ PostgreSQL     │              │ PostgreSQL     │
    │ Primary        │─── Repl ────▶│ Replica        │
    └────────────────┘              └────────────────┘
```

### Failure Scenarios

| Failure | Impact | Recovery |
|---------|--------|----------|
| Single Gateway dies | None (load balanced) | Auto-restart by K8s |
| Single Runtime Bundle dies | In-flight requests fail | Retry via client; DB state consistent |
| Kafka broker dies | Event lag increases | Kafka replication handles; consumers catch up |
| PostgreSQL primary dies | Write operations fail | Automatic failover to replica (CloudNativePG) |
| Query Service dies | Read operations fail | Auto-restart; no data loss (reads only) |
| Full zone failure | Partial outage | Multi-AZ deployment resumes in other zone |

---

## Multi-Tenancy Architecture

```
┌───────────────────────────────────────────────────────┐
│                    TENANT ISOLATION                     │
├───────────────────────────────────────────────────────┤
│                                                       │
│  LEVEL 1: Shared Infrastructure (Default)             │
│  ┌─────────────────────────────────────────────────┐  │
│  │  All tenants share services + database           │  │
│  │  Isolation via: tenant_id column + RLS policies  │  │
│  │  Best for: SaaS with many small tenants          │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  LEVEL 2: Shared Services, Separate Databases         │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Services shared, each tenant has own DB schema  │  │
│  │  Isolation via: dynamic datasource routing       │  │
│  │  Best for: Compliance-sensitive tenants           │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  LEVEL 3: Dedicated Infrastructure                    │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Separate service instances per tenant            │  │
│  │  Isolation via: Kubernetes namespaces             │  │
│  │  Best for: Enterprise on-prem / regulated         │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
└───────────────────────────────────────────────────────┘
```

---

## Extension Points (Plugin Architecture)

| Extension Point | Mechanism | Use Case |
|----------------|-----------|----------|
| **Custom Connectors** | Connector SDK (Java interface) | Integrate with any external system |
| **Custom Exporters** | Spring Cloud Stream consumer | Export events to custom destinations |
| **Custom Task Listeners** | Activiti listener SPI | Execute logic on task lifecycle events |
| **Custom Execution Listeners** | Activiti listener SPI | Execute logic on process element events |
| **Custom REST Endpoints** | Spring MVC controllers | Add domain-specific APIs |
| **Custom UI Widgets** | React component registry | Add dashboard/form components |
| **Custom Auth Providers** | Spring Security adapter | Integrate non-OIDC auth |
| **Custom Analytics** | Kafka consumer + custom aggregation | Domain-specific metrics |
| **Process Interceptors** | Gateway filter chain | Request transformation, validation |
| **AI Plugins** | Connector SDK + LLM adapter | AI-powered decisions |
