# 01 — Technology Stack

## Core Decisions

### Why Activiti Cloud Over Building From Scratch

| Factor | Build From Scratch (Go) | Activiti Cloud (Java) | Winner |
|--------|------------------------|----------------------|--------|
| Time to market | 12+ months | 5-6 months | Activiti |
| BPMN 2.0 completeness | Must implement all elements | Already complete | Activiti |
| Battle-tested engine | Unproven | 10+ years in production | Activiti |
| CQRS/Event sourcing | Must build | Already implemented | Activiti |
| Kafka/RabbitMQ | Must integrate | Spring Cloud Stream ready | Activiti |
| Team hiring | Go + distributed systems (rare) | Java/Spring (abundant) | Activiti |
| Memory footprint | 50MB (Go) | 512MB+ (JVM) | Go |
| Cold start | <1s | 5-15s | Go |
| Throughput ceiling | Higher (no GC pauses) | Good enough (10K/sec) | Go |

**Verdict:** For 95% of use cases, Activiti Cloud's time-to-market and proven engine win. The JVM overhead is acceptable given modern container orchestration (scale horizontally, not vertically).

---

## Backend Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Runtime** | Java | 21 (LTS) | Engine execution |
| **Framework** | Spring Boot | 3.3+ | Application framework |
| **Process Engine** | Activiti Cloud | 8.x stable | BPMN execution, CQRS |
| **API Gateway** | Spring Cloud Gateway | 4.x | Unified API routing |
| **Messaging** | Apache Kafka | 3.7+ | Event streaming (CQRS) |
| **Messaging (alt)** | RabbitMQ | 3.13+ | Lightweight alternative |
| **Database** | PostgreSQL | 16+ | Primary data store |
| **Search (optional)** | Elasticsearch | 8.x | High-volume search |
| **Search (alt)** | OpenSearch | 2.x | ES-compatible, Apache 2.0 |
| **Cache** | Redis | 7.x | Session cache, rate limiting |
| **Auth** | Keycloak | 24+ | OIDC provider |
| **Migrations** | Flyway | 10.x | Schema versioning |
| **ORM/SQL** | MyBatis | 3.5+ | Query mapping (Activiti native) |
| **gRPC** | grpc-spring-boot-starter | 3.x | Binary protocol |
| **Observability** | Micrometer + OTel | Latest | Metrics + traces |

### Why These Choices

**Java 21 (not 25):**
- LTS release with long-term support
- Virtual threads (Project Loom) for better concurrency
- Pattern matching, records, sealed classes
- Widest cloud provider support

**PostgreSQL 16 (not MySQL):**
- Superior JSON support (JSONB with indexing) for process variables
- Window functions and CTEs for analytics queries
- LISTEN/NOTIFY for lightweight event notification
- Row-level security for multi-tenancy
- Partitioning for time-series analytics data

**Kafka (not RabbitMQ as default):**
- Replay capability (consumers can re-read events)
- Higher throughput for event streaming
- Natural fit for event sourcing / audit
- Compaction for state snapshots
- Better Kubernetes operation (Strimzi operator)

**Keycloak (not custom auth):**
- Apache 2.0 license
- Full OIDC/OAuth2/SAML support
- User federation (LDAP, Active Directory)
- Admin UI included
- Social login, MFA out of box
- Activiti Cloud already integrates with it

---

## Frontend Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Framework** | React | 18+ | UI framework |
| **Language** | TypeScript | 5.4+ | Type safety |
| **Build** | Vite | 5.x | Fast dev server + bundler |
| **State** | TanStack Query | 5.x | Server state management |
| **Routing** | React Router | 6.x | Client-side navigation |
| **UI Library** | shadcn/ui | Latest | Component primitives |
| **Styling** | Tailwind CSS | 3.x | Utility-first CSS |
| **BPMN** | bpmn-js | 17.x | BPMN diagram rendering |
| **DMN** | dmn-js | 16.x | DMN table rendering |
| **Forms** | React JSON Schema Form | 5.x | Dynamic form rendering |
| **Charts** | Recharts | 2.x | Analytics visualizations |
| **Collaboration** | Yjs | 13.x | CRDT real-time editing |
| **WebSocket** | Socket.io | 4.x | Real-time updates |
| **Testing** | Vitest + Playwright | Latest | Unit + E2E testing |
| **Monorepo** | pnpm workspaces | 9.x | Multi-app management |

### Why These Choices

**React (not Vue/Svelte/Angular):**
- bpmn.io ecosystem is React-friendly
- Largest talent pool
- TanStack Query handles server state excellently
- shadcn/ui provides accessible, customizable components

**TanStack Query (not Redux/Zustand):**
- Server state ≠ client state (this is mostly server state)
- Automatic caching, refetching, optimistic updates
- Eliminates 90% of state management boilerplate
- Perfect for CQRS read-side consumption

**Yjs (not Automerge/OT):**
- Battle-tested CRDT library
- Sub-framework (works with any editor)
- Awareness protocol for cursor positions
- Multiple providers (WebSocket, WebRTC, IndexedDB)
- Used by major editors (Tiptap, BlockNote, etc.)

---

## Infrastructure Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Container Runtime** | Docker | Service containerization |
| **Orchestration** | Kubernetes 1.28+ | Production deployment |
| **Helm Charts** | Helm 3 | Package management |
| **Kafka Operator** | Strimzi | Kafka on Kubernetes |
| **DB Operator** | CloudNativePG | PostgreSQL on Kubernetes |
| **Ingress** | NGINX Ingress / Traefik | Traffic routing |
| **TLS** | cert-manager | Automatic certificates |
| **GitOps** | ArgoCD | Declarative deployment |
| **CI/CD** | GitHub Actions | Build + test pipeline |
| **Registry** | GitHub Container Registry | Image storage |
| **Secrets** | External Secrets Operator | Secret management |
| **Monitoring** | Prometheus + Grafana | Metrics + dashboards |
| **Tracing** | Jaeger / Tempo | Distributed tracing |
| **Logging** | Loki + Promtail | Log aggregation |

---

## Development Tools

| Tool | Purpose |
|------|---------|
| **Maven** | Backend build (matches Activiti Cloud) |
| **pnpm** | Frontend package management |
| **Docker Compose** | Local development environment |
| **Testcontainers** | Integration testing |
| **k6** | Load testing |
| **Playwright** | E2E UI testing |
| **Bruno / Insomnia** | API testing |
| **buf** | Protobuf management |
| **SonarQube** | Code quality (optional) |
| **Renovate** | Dependency updates |

---

## Version Compatibility Matrix

| Component | Minimum | Recommended | Maximum |
|-----------|---------|-------------|---------|
| Java | 21 | 21 | 23 |
| Spring Boot | 3.2 | 3.3 | 4.x (when Activiti supports) |
| PostgreSQL | 14 | 16 | 17 |
| Kafka | 3.5 | 3.7 | 3.8 |
| Kubernetes | 1.26 | 1.29 | 1.30 |
| Elasticsearch | 8.8 | 8.13 | 8.x |
| Keycloak | 22 | 24 | 25 |
| Node.js | 18 | 20 | 22 |
| React | 18 | 18.3 | 19 (when stable) |

---

## Future-Proofing Considerations

### What's Coming (2025-2027)

| Technology Shift | Our Readiness |
|-----------------|---------------|
| **Java Virtual Threads (Loom)** | Spring Boot 3.2+ supports them. Enable when Activiti is compatible. |
| **GraalVM Native Image** | Spring Boot AOT ready. Native compilation reduces startup to <1s. Monitor Activiti compatibility. |
| **Kafka replacement (Redpanda/WarpStream)** | Spring Cloud Stream abstraction means zero code change to swap. |
| **PostgreSQL 17+ features** | JSON table functions, incremental backup — adopt progressively. |
| **React Server Components** | Monitor — may improve initial load for dashboard UIs. |
| **WebAssembly (WASM)** | Potential for running FEEL expressions in browser (modeler validation). |
| **AI/LLM integration** | Extension points ready for AI-powered process generation, anomaly detection. |

### Upgrade Path

1. **Activiti Cloud upgrades:** Track `develop` branch. Minor versions should be drop-in. Major versions may require migration.
2. **Spring Boot upgrades:** Follow Spring Boot release cadence. Test with Activiti compatibility.
3. **Kafka upgrades:** Managed by Strimzi operator. Rolling upgrades with zero downtime.
4. **Frontend upgrades:** Renovate bot for automated PRs. Vite makes React upgrades painless.
5. **Kubernetes upgrades:** Follow cloud provider cadence. Helm charts abstract API version differences.
