# Open Orchestration Platform (OOP)

## Vision

A fully open-source (Apache 2.0), cloud-native process orchestration platform that delivers Camunda 8 SaaS-equivalent capabilities — built on Activiti Cloud, with modern React UIs, enhanced observability, and zero licensing costs.

---

## Why This Exists

| Problem | Solution |
|---------|----------|
| Camunda 8 core engine is not free for production | Activiti Cloud (Apache 2.0) as engine |
| No open-source process analytics (Optimize is paid) | Custom analytics service |
| No self-hosted collaborative modeler | bpmn.io + Yjs-based modeler |
| Complex deployment (5+ services + Elasticsearch required) | Single Docker Compose + optional scale-out |
| Vendor lock-in | Standard BPMN 2.0, portable processes |

---

## Platform Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                     OPEN ORCHESTRATION PLATFORM                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  EXECUTION LAYER (Activiti Cloud - Apache 2.0)                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ Runtime      │ │ Query        │ │ Audit        │               │
│  │ Bundle       │ │ Service      │ │ Service      │               │
│  │ (Engine)     │ │ (CQRS Read)  │ │ (Events)     │               │
│  └──────────────┘ └──────────────┘ └──────────────┘               │
│                                                                     │
│  PLATFORM SERVICES (Custom - Apache 2.0)                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ API Gateway  │ │ Analytics    │ │ Identity     │               │
│  │ (Unified)    │ │ Service      │ │ (RBAC)       │               │
│  └──────────────┘ └──────────────┘ └──────────────┘               │
│                                                                     │
│  USER INTERFACES (Custom - Apache 2.0)                              │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────┐│
│  │ Operate      │ │ Tasklist     │ │ Analytics    │ │ Modeler   ││
│  │ (Monitor)    │ │ (Tasks)      │ │ (Dashboard)  │ │ (Design)  ││
│  └──────────────┘ └──────────────┘ └──────────────┘ └───────────┘│
│                                                                     │
│  INFRASTRUCTURE                                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ PostgreSQL   │ │ Kafka        │ │ Keycloak     │               │
│  │ (Storage)    │ │ (Messaging)  │ │ (Auth)       │               │
│  └──────────────┘ └──────────────┘ └──────────────┘               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Future-Proof Design Principles

1. **Standard-first**: BPMN 2.0, DMN 1.3, OIDC, OpenTelemetry, CloudEvents
2. **Modular**: Each component independently deployable and replaceable
3. **API-driven**: Every capability accessible via REST/gRPC
4. **Event-native**: All state changes produce events (CQRS + event sourcing)
5. **Database-agnostic**: PostgreSQL default, pluggable to any JDBC/ES/OpenSearch
6. **Cloud-agnostic**: Runs on any Kubernetes (AWS/GCP/Azure/on-prem)
7. **AI-ready**: Extension points for LLM-based decision making and process generation
8. **Scale-down-friendly**: Single binary dev mode, scales up to distributed cluster

---

## Document Index

| Document | Purpose |
|----------|---------|
| [01-TECH-STACK.md](./01-TECH-STACK.md) | Technology choices and justifications |
| [02-ARCHITECTURE-DEEP-DIVE.md](./02-ARCHITECTURE-DEEP-DIVE.md) | Detailed architecture with data flows |
| [03-IMPLEMENTATION-ROADMAP.md](./03-IMPLEMENTATION-ROADMAP.md) | Step-by-step build plan (24 weeks) |
| [04-INFRASTRUCTURE-SETUP.md](./04-INFRASTRUCTURE-SETUP.md) | Local dev, Docker, Kubernetes setup |
| [05-API-DESIGN.md](./05-API-DESIGN.md) | Unified API specification |
| [06-UI-ARCHITECTURE.md](./06-UI-ARCHITECTURE.md) | Frontend architecture and design system |
| [07-SECURITY-MODEL.md](./07-SECURITY-MODEL.md) | Auth, RBAC, multi-tenancy |
| [08-OBSERVABILITY.md](./08-OBSERVABILITY.md) | Monitoring, tracing, alerting |
| [09-SCALING-STRATEGY.md](./09-SCALING-STRATEGY.md) | Performance targets and scaling patterns |
| [10-FUTURE-ROADMAP.md](./10-FUTURE-ROADMAP.md) | Post-v1.0 evolution plan |

---

## Quick Reference

**License:** Apache 2.0 (all components)  
**Engine:** Activiti Cloud 9.x (Java 21+, Spring Boot 4.x)  
**Frontend:** React 18 + TypeScript 5 + Vite  
**Database:** PostgreSQL 16 (primary), Elasticsearch 8 (optional)  
**Messaging:** Apache Kafka 3.x (via Spring Cloud Stream)  
**Auth:** Keycloak 24+ (OIDC/OAuth2)  
**Orchestration:** Kubernetes 1.28+ with Helm 3  
**Observability:** OpenTelemetry → Prometheus + Grafana + Jaeger  
