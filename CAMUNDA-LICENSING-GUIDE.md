# Camunda 8 — Commercial Use Licensing Guide

## TL;DR

Camunda 8 uses a **dual-license model**. Most of the core platform is under the proprietary **"Camunda License 1.0"** which **prohibits production/commercial use** without purchasing an Enterprise Edition. Only a handful of components are truly open-source (Apache 2.0 / MIT).

---

## Components You CAN Use Commercially (Free, No Restrictions)

These are under **Apache 2.0** or **MIT** — fully permissive for commercial/production use:

| Component | License | What It Is |
|-----------|---------|------------|
| **Java Client** (`clients/java`) | Apache 2.0 | SDK to interact with Zeebe from Java applications |
| **Spring Boot Starter** (`clients/camunda-spring-boot-starter`) | Apache 2.0 | Spring Boot integration for Camunda |
| **Connector SDK** (`connector-sdk/`) | Apache 2.0 | Framework for building custom connectors |
| **REST Connector** | Apache 2.0 | Built-in HTTP/REST connector |
| **Connector Runtime Docker Image** | Apache 2.0 | Container image for running connectors |
| **Exporter API** (`zeebe/exporter-api`) | Apache 2.0 | SPI for building custom Zeebe exporters |
| **Protocol Definitions** (`zeebe/protocol`) | Apache 2.0 | SBE protocol message definitions |
| **Gateway Protocol** (`zeebe/gateway-protocol-impl`) | Apache 2.0 | gRPC protocol definitions and generated stubs |
| **BPMN Model API** (`zeebe/bpmn-model`) | Apache 2.0 | Java API for reading/writing BPMN XML |
| **Desktop Modeler** | MIT | Desktop BPMN/DMN/Form editor application |
| **gRPC Generated Stubs** | Exempt | Generated code is explicitly NOT a derivative work |

### What You Can Do With These:
- Build commercial applications that talk to Zeebe via the Java Client
- Build and sell custom connectors using the Connector SDK
- Build custom exporters using the Exporter API
- Use the Desktop Modeler in your commercial workflow
- Generate client stubs from the protocol definitions
- Parse/create BPMN files using the BPMN Model API
- Deploy the Connector Runtime in production

---

## Components You CANNOT Use Commercially (Without Paying)

These are under **Camunda License 1.0** — restricted to non-production environments only:

| Component | License | What It Is | Restriction |
|-----------|---------|------------|-------------|
| **Zeebe Engine** (`zeebe/` core) | Camunda License 1.0 | The workflow execution engine | No production use |
| **Operate** (`operate/`) | Camunda License 1.0 | Process monitoring & troubleshooting UI | No production use |
| **Tasklist** (`tasklist/`) | Camunda License 1.0 | Human task management UI | No production use |
| **Optimize** (`optimize/`) | Camunda License 1.0 | Process analytics & reporting | No production use |
| **Identity** (`identity/`) | Camunda License 1.0 | Authentication & authorization service | No production use |
| **Other Connectors** (non-REST) | Camunda License 1.0 | Kafka, Slack, email, etc. connectors | No production use |
| **Web Modeler** | Proprietary (closed) | Collaborative browser BPMN editor | SaaS-only, requires subscription |
| **Console** | Proprietary (closed) | Cluster management UI | SaaS-only, requires subscription |

---

## What "Camunda License 1.0" Actually Says

### Allowed (Free):
- Development and testing
- Staging and QA environments
- Demo and evaluation
- Viewing and modifying source code
- Learning and education

### Prohibited (Without Enterprise License):
- Production deployment
- Processing real/live business data
- Revenue-generating services
- Providing access to third parties
- Sublicensing or reselling
- Building competitive products
- Disabling or circumventing license key checks

### If You Violate:
- License terminates automatically
- Must cease all use within 30 days
- Maximum liability under free license: **$50**

---

## What You Need to Buy for Production

### Camunda Self-Managed Enterprise Edition

Required for on-premise/self-hosted production deployment of:
- Zeebe (workflow engine)
- Operate (monitoring)
- Tasklist (human tasks)
- Identity (auth)
- Optimize (analytics)

**Pricing:** Contact Camunda sales (not publicly listed, typically annual subscription based on cluster size)

### Camunda Cloud (SaaS)

Required for:
- Web Modeler (collaborative editing)
- Console (cluster management)
- Fully managed infrastructure

**Pricing:** Subscription-based, 30-day free trial available

---

## Practical Scenarios

### Scenario 1: "I want to build a commercial app that orchestrates workflows"

**What you need to pay for:**
- Zeebe Engine (Camunda License 1.0) — needs Enterprise Edition
- Operate (if you want monitoring) — needs Enterprise Edition

**What's free:**
- Java Client / Spring Boot Starter (Apache 2.0) — your app's code
- Custom connectors via SDK (Apache 2.0)
- Desktop Modeler for designing processes (MIT)

### Scenario 2: "I want to build and sell connectors"

**Fully free** — The Connector SDK is Apache 2.0. You can build, distribute, and sell connectors commercially.

### Scenario 3: "I want to use Zeebe for internal production workflows"

**Must purchase Enterprise Edition.** Even internal use in production requires a license.

### Scenario 4: "I want to evaluate Camunda for my team"

**Fully free** — All components can be used for development, testing, and evaluation without payment.

### Scenario 5: "I want to build a competing BPM platform"

**Cannot use Camunda License 1.0 components.** The license explicitly prohibits competitive use. You CAN use Apache 2.0 components (client libs, protocol definitions) as reference.

### Scenario 6: "I want to offer Camunda as a managed service (like a SaaS)"

**Prohibited under Camunda License 1.0.** You cannot sublicense or provide third-party access to the restricted components without an Enterprise agreement.

---

## What's Safe to Use as Building Blocks

If you're building your own platform, here's what you can legally reuse from Camunda:

### Safe to Use (Apache 2.0 / MIT):
| Asset | How You Can Use It |
|-------|-------------------|
| Protocol Buffers / gRPC definitions | Generate client stubs for any language |
| BPMN Model API | Parse/validate BPMN files in your application |
| Exporter API interfaces | Design compatible exporter plugins |
| Connector SDK patterns | Build your own connector framework |
| Desktop Modeler | Embed or extend for BPMN editing |
| bpmn.io libraries | Build web-based BPMN viewers/editors (separate repo, MIT/Apache) |

### NOT Safe to Use:
| Asset | Why Not |
|-------|---------|
| Zeebe engine source code | Cannot use in production or competitive products |
| Operate/Tasklist UI code | Camunda License 1.0 — restricted |
| Optimize analytics code | Camunda License 1.0 — restricted |
| Identity service code | Camunda License 1.0 — restricted |
| Internal algorithms/patterns | If copied, constitutes derivative work |

---

## Alternative Open-Source Components

For components you can't use commercially, here are open-source alternatives:

| Camunda Component | Open-Source Alternative | License |
|-------------------|----------------------|---------|
| Zeebe (engine) | Temporal, Netflix Conductor, Apache Airflow | MIT, Apache 2.0 |
| Operate (monitoring) | Build custom with bpmn.io + PostgreSQL | — |
| Tasklist (human tasks) | Build custom or use Flowable (Apache 2.0) | Apache 2.0 |
| Optimize (analytics) | Metabase, Apache Superset, custom | AGPL / Apache 2.0 |
| Identity (auth) | Keycloak, Zitadel, Authentik | Apache 2.0 |
| Web Modeler | bpmn.io + Yjs (collaboration) | MIT |
| Connectors | Apache Camel, n8n | Apache 2.0, custom |

---

## Summary Table

```
┌─────────────────────────────────────────────────────────────────┐
│                    CAMUNDA 8 LICENSE MAP                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─── APACHE 2.0 / MIT (FREE FOR COMMERCIAL) ───────────────┐  │
│  │                                                           │  │
│  │  • Java Client          • Connector SDK                   │  │
│  │  • Spring Boot Starter  • REST Connector                  │  │
│  │  • Exporter API         • BPMN Model API                  │  │
│  │  • Protocol Definitions • Desktop Modeler (MIT)           │  │
│  │  • Gateway Protocol     • Generated gRPC Stubs            │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─── CAMUNDA LICENSE 1.0 (NO PRODUCTION WITHOUT PAYING) ───┐  │
│  │                                                           │  │
│  │  • Zeebe Engine         • Operate                         │  │
│  │  • Tasklist             • Optimize                        │  │
│  │  • Identity             • Built-in Connectors             │  │
│  │                                                           │  │
│  │  Allowed: dev, test, QA, staging, evaluation              │  │
│  │  Prohibited: production, commercial, revenue-generating   │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─── PROPRIETARY / CLOSED SOURCE (SAAS ONLY) ──────────────┐  │
│  │                                                           │  │
│  │  • Web Modeler          • Console                         │  │
│  │                                                           │  │
│  │  Not available for self-hosting at any price tier         │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Disclaimer

This document is for informational purposes and is not legal advice. Always review the actual license text at:
- `/licenses/CAMUNDA-LICENSE-1.0.txt` in the Camunda repo
- `/licenses/APACHE-2.0.txt` in the Camunda repo
- https://docs.camunda.io/docs/reference/licenses/

Consult legal counsel before making commercial decisions based on licensing interpretations.
