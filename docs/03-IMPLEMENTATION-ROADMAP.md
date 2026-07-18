# 03 — Implementation Roadmap (24 Weeks)

## Overview

```
Week:  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
       ├──────────┤                                                              
       Phase 1: Foundation                                                       
                  ├────────────────┤                                             
                  Phase 2: API Gateway + Auth                                    
                              ├──────────────────────┤                           
                              Phase 3: Operate UI                                
                                       ├──────────────────┤                     
                                       Phase 4: Tasklist UI                      
                                                ├──────────────────────┤        
                                                Phase 5: Analytics                
                                                          ├──────────────────┤  
                                                          Phase 6: Modeler + Polish
```

---

## Phase 1: Foundation (Weeks 1-4)

### Goal
Get the full Activiti Cloud stack running with Docker Compose, all services communicating, and a basic health-check passing.

### Week 1: Project Setup

| Task | Details | Deliverable |
|------|---------|-------------|
| 1.1 | Initialize Git repository with monorepo structure | Repo with folder structure |
| 1.2 | Create root Maven POM (parent for all Java services) | `pom.xml` with dependency management |
| 1.3 | Create pnpm workspace for frontend apps | `pnpm-workspace.yaml` |
| 1.4 | Set up GitHub Actions CI (build + test) | `.github/workflows/ci.yml` |
| 1.5 | Create base Dockerfiles for Java services | `deploy/docker/Dockerfile.java` |
| 1.6 | Create base Dockerfile for React apps | `deploy/docker/Dockerfile.react` |
| 1.7 | Set up code formatting (Spotless + Prettier) | Auto-format on commit |
| 1.8 | Write `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` | Developer onboarding docs |

### Week 2: Activiti Cloud Services

| Task | Details | Deliverable |
|------|---------|-------------|
| 2.1 | Create Runtime Bundle service (Spring Boot app with Activiti Cloud starter) | `platform/runtime-bundle/` |
| 2.2 | Create Query Service (Spring Boot app with Activiti Cloud query starter) | `platform/query-service/` |
| 2.3 | Create Audit Service (Spring Boot app with Activiti Cloud audit starter) | `platform/audit-service/` |
| 2.4 | Create Notification Service (GraphQL subscriptions) | `platform/notification-service/` |
| 2.5 | Configure shared `application.yml` properties | Database, Kafka, Keycloak settings |
| 2.6 | Set up Flyway migrations for custom schemas | `db/migrations/` |

### Week 3: Infrastructure

| Task | Details | Deliverable |
|------|---------|-------------|
| 3.1 | Docker Compose: PostgreSQL (4 databases) | `docker-compose.yml` |
| 3.2 | Docker Compose: Apache Kafka (KRaft mode, no ZK) | Kafka 3.7+ |
| 3.3 | Docker Compose: Keycloak with pre-configured realm | Realm JSON import |
| 3.4 | Docker Compose: All application services | Full stack compose |
| 3.5 | Create Keycloak realm with default users/roles | `deploy/keycloak/realm-export.json` |
| 3.6 | Create `Makefile` with common commands | `make up`, `make down`, `make build` |
| 3.7 | Verify end-to-end: Deploy BPMN → Start instance → Complete task | Integration test |

### Week 4: Validation & Documentation

| Task | Details | Deliverable |
|------|---------|-------------|
| 4.1 | Write integration tests (Testcontainers) | `test/integration/` |
| 4.2 | Create sample BPMN process (order fulfillment) | `samples/order-process.bpmn` |
| 4.3 | Verify CQRS: event flows from Runtime → Kafka → Query | Event flow test |
| 4.4 | Verify Auth: Keycloak token → service access | Auth flow test |
| 4.5 | Performance baseline: simple process throughput | Benchmark report |
| 4.6 | Write getting-started guide | `docs/guides/getting-started.md` |
| 4.7 | Tag release: `v0.1.0-alpha` | GitHub release |

### Phase 1 Exit Criteria
- [ ] `docker compose up` starts all services in < 2 minutes
- [ ] Can deploy a BPMN process via REST API
- [ ] Can start a process instance and see events in Query Service
- [ ] Can complete a user task via REST
- [ ] Keycloak authentication works end-to-end
- [ ] CI pipeline builds and tests all services

---

## Phase 2: API Gateway + Enhanced Auth (Weeks 3-6)

### Week 3-4: API Gateway

| Task | Details | Deliverable |
|------|---------|-------------|
| 5.1 | Create Spring Cloud Gateway service | `platform/gateway/` |
| 5.2 | Configure routing rules (process, query, task, admin) | Route definitions |
| 5.3 | Add JWT validation filter (Keycloak public key) | Security filter |
| 5.4 | Add rate limiting (Redis + token bucket) | Rate limiter |
| 5.5 | Add request/response logging (structured) | Access logs |
| 5.6 | Add CORS configuration for frontend apps | CORS filter |
| 5.7 | Create unified error response format | Error handler |
| 5.8 | Add OpenAPI spec aggregation (all services) | Swagger UI at `/api/docs` |

### Week 5-6: Identity & RBAC

| Task | Details | Deliverable |
|------|---------|-------------|
| 6.1 | Create Identity Service (Spring Boot) | `platform/identity-service/` |
| 6.2 | Design permission model (Role → Resource → Action) | DB schema |
| 6.3 | Implement permission check API | `GET /permissions/check` |
| 6.4 | Create default roles (admin, operator, developer, user) | Seed data |
| 6.5 | Add tenant management (create, configure, assign users) | Tenant API |
| 6.6 | Integrate with Gateway (permission header enrichment) | Gateway filter |
| 6.7 | Add API key support (for M2M/service accounts) | API key endpoints |
| 6.8 | Write RBAC integration tests | Permission tests |

### Phase 2 Exit Criteria
- [ ] All API calls route through single gateway endpoint
- [ ] JWT authentication required for all endpoints
- [ ] Rate limiting prevents abuse (configurable per client)
- [ ] RBAC: admin can do everything, user can only see own tasks
- [ ] API keys work for service-to-service calls
- [ ] OpenAPI docs available at `/api/docs`

---

## Phase 3: Operate UI (Weeks 5-10)

### Week 5-6: UI Foundation

| Task | Details | Deliverable |
|------|---------|-------------|
| 7.1 | Scaffold React app (Vite + TypeScript + Tailwind) | `apps/operate/` |
| 7.2 | Set up shared design system (shadcn/ui) | `apps/shared/design-system/` |
| 7.3 | Implement auth flow (Keycloak OIDC login) | `apps/shared/auth/` |
| 7.4 | Create app shell (sidebar nav, header, layout) | Shell component |
| 7.5 | Set up TanStack Query for API consumption | API hooks |
| 7.6 | Create shared BPMN viewer component (bpmn-js) | `apps/shared/bpmn-viewer/` |
| 7.7 | Set up Vitest for unit testing | Test configuration |

### Week 7-8: Core Pages

| Task | Details | Deliverable |
|------|---------|-------------|
| 8.1 | Process Definitions page (list, filter, deploy) | Definitions page |
| 8.2 | Process Instances page (list, filter, status badges) | Instances page |
| 8.3 | Instance Detail page (BPMN diagram + active tokens) | Detail page |
| 8.4 | Variables panel (view/edit instance variables) | Variables component |
| 8.5 | Audit Log panel (event timeline for instance) | Audit component |
| 8.6 | Incidents page (list active incidents) | Incidents page |

### Week 9-10: Advanced Features

| Task | Details | Deliverable |
|------|---------|-------------|
| 9.1 | BPMN diagram overlay (color-coded flow nodes by state) | Diagram overlay |
| 9.2 | Real-time updates (GraphQL subscription / WebSocket) | Live status |
| 9.3 | Batch operations (cancel, retry selected instances) | Batch UI |
| 9.4 | Instance migration (move tokens between versions) | Migration wizard |
| 9.5 | Process statistics (instance counts per state) | Stats cards |
| 9.6 | Incident resolution (retry, cancel, modify variables) | Incident actions |
| 9.7 | Responsive design + dark mode | Theme support |
| 9.8 | E2E tests (Playwright) | `test/e2e/operate/` |

### Phase 3 Exit Criteria
- [ ] Can view all process definitions and their versions
- [ ] Can see running instances with BPMN diagram showing active tokens
- [ ] Can inspect variables and audit trail for any instance
- [ ] Can resolve incidents (retry, cancel)
- [ ] Real-time updates when instance state changes
- [ ] Responsive and accessible (WCAG 2.1 AA)

---

## Phase 4: Tasklist UI (Weeks 8-12)

### Week 8-9: Task Core

| Task | Details | Deliverable |
|------|---------|-------------|
| 10.1 | Scaffold Tasklist React app | `apps/tasklist/` |
| 10.2 | Task inbox page (list with filters) | Inbox page |
| 10.3 | Task detail panel (variables, history, actions) | Detail panel |
| 10.4 | Claim/Unclaim/Assign task actions | Task actions |
| 10.5 | Task completion with variable input | Completion form |
| 10.6 | Filter system (my tasks, group tasks, all tasks) | Filter tabs |

### Week 10-11: Forms Engine

| Task | Details | Deliverable |
|------|---------|-------------|
| 11.1 | JSON Schema form renderer (React JSON Schema Form) | Form renderer |
| 11.2 | Form definition storage (linked to user tasks) | Form API |
| 11.3 | Custom form widgets (date picker, file upload, etc.) | Widget library |
| 11.4 | Form validation (client + server side) | Validation |
| 11.5 | Form versioning (linked to process definition version) | Version management |

### Week 11-12: Polish

| Task | Details | Deliverable |
|------|---------|-------------|
| 12.1 | Task notifications (WebSocket push on assignment) | Push notifications |
| 12.2 | Task delegation and transfer | Delegation flow |
| 12.3 | Task comments/notes | Comments panel |
| 12.4 | Task priority and due date display | Priority badges |
| 12.5 | Mobile-responsive layout | Mobile design |
| 12.6 | E2E tests (Playwright) | `test/e2e/tasklist/` |

### Phase 4 Exit Criteria
- [ ] Users see tasks assigned to them or their groups
- [ ] Can claim, complete, and delegate tasks
- [ ] Dynamic forms render from JSON Schema definitions
- [ ] Real-time notifications on task assignment
- [ ] Works on mobile devices

---

## Phase 5: Analytics Service + UI (Weeks 10-18)

### Week 10-12: Analytics Backend

| Task | Details | Deliverable |
|------|---------|-------------|
| 13.1 | Create Analytics Service (Spring Boot) | `platform/analytics-service/` |
| 13.2 | Kafka consumer (process + activity events) | Event consumer |
| 13.3 | Metrics aggregation tables (PostgreSQL) | DB schema |
| 13.4 | Process duration calculation (avg, p50, p95, p99) | Duration metrics |
| 13.5 | Activity-level timing (per flow node) | Activity metrics |
| 13.6 | Throughput tracking (instances started/completed per hour) | Throughput metrics |
| 13.7 | SLA definition API (define target durations) | SLA endpoints |
| 13.8 | SLA breach detection (real-time alerting) | Alert logic |

### Week 13-15: Analytics UI

| Task | Details | Deliverable |
|------|---------|-------------|
| 14.1 | Scaffold Analytics React app | `apps/analytics/` |
| 14.2 | Process heatmap (BPMN diagram with duration overlay) | Heatmap view |
| 14.3 | Throughput charts (line chart, bar chart) | Chart components |
| 14.4 | SLA dashboard (target vs actual, breach list) | SLA page |
| 14.5 | Bottleneck identification (highlight slowest nodes) | Bottleneck view |
| 14.6 | Process comparison (version A vs B performance) | Comparison view |
| 14.7 | Custom report builder (SQL-based widget creation) | Report builder |
| 14.8 | Dashboard builder (drag-and-drop layout) | Dashboard page |

### Week 16-18: Advanced Analytics

| Task | Details | Deliverable |
|------|---------|-------------|
| 15.1 | Export reports (CSV, PDF) | Export functionality |
| 15.2 | Scheduled reports (email delivery via cron) | Report scheduler |
| 15.3 | User performance metrics (tasks/day per assignee) | User metrics |
| 15.4 | Process prediction (estimated completion time) | ML-basic prediction |
| 15.5 | Anomaly detection (unusual duration spikes) | Anomaly alerts |
| 15.6 | Historical trend analysis (week-over-week) | Trend charts |
| 15.7 | E2E tests | `test/e2e/analytics/` |

### Phase 5 Exit Criteria
- [ ] Process heatmaps show duration per activity visually
- [ ] SLA breaches detected and alerted in real-time
- [ ] Custom dashboards with drag-and-drop widgets
- [ ] Export reports as CSV/PDF
- [ ] Bottleneck auto-detection highlights slow nodes

---

## Phase 6: Modeler + Production Readiness (Weeks 16-24)

### Week 16-18: Collaborative Modeler

| Task | Details | Deliverable |
|------|---------|-------------|
| 16.1 | Scaffold Modeler React app | `apps/modeler/` |
| 16.2 | Integrate bpmn-js (BPMN 2.0 editing) | BPMN editor |
| 16.3 | Integrate dmn-js (DMN decision tables) | DMN editor |
| 16.4 | Form designer (JSON Schema visual builder) | Form editor |
| 16.5 | Process versioning and history | Version list |
| 16.6 | Deploy to engine (one-click from modeler) | Deploy button |
| 16.7 | Yjs CRDT backend for real-time collaboration | Collab server |
| 16.8 | Multi-cursor awareness (see who's editing where) | Awareness UI |
| 16.9 | BPMN validation and linting | Lint rules |

### Week 19-20: CLI + Developer Experience

| Task | Details | Deliverable |
|------|---------|-------------|
| 17.1 | Create CLI tool (Go + Cobra) | `cli/` |
| 17.2 | `oop deploy <file>` — deploy process | Deploy command |
| 17.3 | `oop start <key>` — start instance | Start command |
| 17.4 | `oop instances list` — list instances | List command |
| 17.5 | `oop tasks list/complete` — manage tasks | Task commands |
| 17.6 | `oop dev` — start local dev environment | Dev command |
| 17.7 | `oop status` — cluster health | Status command |
| 17.8 | Shell completions (bash, zsh, fish) | Completions |
| 17.9 | npm/brew/apt distribution | Package publishing |

### Week 19-21: Connector Ecosystem

| Task | Details | Deliverable |
|------|---------|-------------|
| 18.1 | Connector runtime service | `platform/connector-service/` |
| 18.2 | HTTP/REST connector (outbound) | HTTP connector |
| 18.3 | Kafka connector (produce/consume) | Kafka connector |
| 18.4 | Email connector (SMTP send) | Email connector |
| 18.5 | Webhook connector (inbound, trigger processes) | Webhook connector |
| 18.6 | Slack connector | Slack connector |
| 18.7 | Database connector (query/insert) | DB connector |
| 18.8 | Connector SDK documentation | SDK guide |

### Week 21-22: Kubernetes + Helm

| Task | Details | Deliverable |
|------|---------|-------------|
| 19.1 | Create Helm chart (umbrella chart) | `deploy/helm/` |
| 19.2 | Configure HPA for each service | Auto-scaling |
| 19.3 | Configure PDB (pod disruption budgets) | HA guarantees |
| 19.4 | Ingress configuration (NGINX/Traefik) | Ingress rules |
| 19.5 | TLS with cert-manager | Certificate automation |
| 19.6 | Strimzi Kafka operator integration | Managed Kafka |
| 19.7 | CloudNativePG operator integration | Managed PostgreSQL |
| 19.8 | ArgoCD application manifests | GitOps deployment |

### Week 22-23: Production Hardening

| Task | Details | Deliverable |
|------|---------|-------------|
| 20.1 | Load testing with k6 (10K instances/sec target) | Load test scripts |
| 20.2 | Chaos testing (kill pods, network partitions) | Chaos experiments |
| 20.3 | Security audit (OWASP top 10, dependency scan) | Security report |
| 20.4 | Backup and restore tooling | Backup scripts |
| 20.5 | Disaster recovery runbook | DR documentation |
| 20.6 | Monitoring dashboards (Grafana) | Dashboard JSON |
| 20.7 | Alerting rules (Prometheus) | Alert rules |
| 20.8 | OpenTelemetry integration (traces + metrics) | OTel config |

### Week 23-24: Documentation + Launch

| Task | Details | Deliverable |
|------|---------|-------------|
| 21.1 | Documentation site (Docusaurus) | `docs-site/` |
| 21.2 | API reference (generated from OpenAPI) | API docs |
| 21.3 | Architecture decision records (ADRs) | ADR documents |
| 21.4 | Migration guide (from Camunda 7/8) | Migration guide |
| 21.5 | Video walkthroughs (getting started, deploy, monitor) | Video links |
| 21.6 | Performance benchmark report | Benchmark doc |
| 21.7 | Security hardening guide | Security guide |
| 21.8 | Tag release: `v1.0.0` | GA release |

### Phase 6 Exit Criteria
- [ ] Collaborative BPMN editing works with multiple users
- [ ] CLI tool published and installable via common package managers
- [ ] 6+ built-in connectors functional
- [ ] Helm chart deploys full platform on any Kubernetes cluster
- [ ] Load tested to 10K instances/second
- [ ] Security audit passed with no critical/high findings
- [ ] Documentation site live with all guides

---

## Team Structure (Recommended)

| Role | Count | Focus |
|------|-------|-------|
| **Tech Lead / Architect** | 1 | Architecture, code review, integration |
| **Backend Engineer (Java)** | 2 | Activiti integration, services, Kafka |
| **Frontend Engineer (React)** | 2 | Operate, Tasklist, Analytics, Modeler UIs |
| **DevOps / Platform Engineer** | 1 | Docker, Helm, CI/CD, monitoring |
| **QA Engineer** | 1 | Integration tests, E2E, load testing |

**Total: 7 engineers for 24 weeks**

For a smaller team (3-4 developers), extend timeline to 36-40 weeks.

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Activiti Cloud version incompatibility | High | Medium | Pin to stable version, test upgrades in CI |
| Kafka operational complexity | Medium | Medium | Use KRaft mode (no ZooKeeper), consider Redpanda |
| bpmn-js API changes | Low | Low | Pin version, thin wrapper layer |
| Performance not meeting targets | High | Low | Benchmark early (Phase 1), optimize bottlenecks |
| Keycloak upgrade breaking changes | Medium | Low | Pin version, test upgrades quarterly |
| Team attrition | High | Medium | Document everything, pair programming |
| Scope creep | High | High | Strict phase gates, MVP mindset |

---

## Definition of Done (Per Phase)

Every phase must satisfy:
- [ ] All tasks completed and code merged to main
- [ ] Unit test coverage ≥ 80%
- [ ] Integration tests passing
- [ ] No critical/high security vulnerabilities
- [ ] Documentation updated
- [ ] Docker Compose stack works end-to-end
- [ ] Demo recorded/presented to stakeholders
- [ ] Release tagged in Git
