# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-alpha] - 2026-07-16

### Added

#### Platform Services
- **Runtime Bundle** — BPMN execution engine powered by Activiti Cloud 8.4.0
  - Process deployment via REST API (file upload and XML body)
  - Process instance lifecycle management (start, suspend, resume, cancel)
  - User task operations (claim, release, complete)
  - Service task connectors (orderValidation, processPayment, shipOrder)
  - Kafka event producers for all engine events

- **Query Service** — CQRS read-side for process and task queries
  - Kafka consumers for process, task, activity, and variable events
  - REST API with pagination and filtering
  - Process instance and task entity projections

- **Audit Service** — Immutable event log
  - Persists all engine events to append-only store with JSONB payload
  - Query by process instance, event type, or user
  - GIN-indexed JSONB for fast payload queries

- **Analytics Service** — Process metrics aggregation
  - Real-time metric updates via Kafka consumers
  - Daily process metrics with UPSERT
  - SLA tracking schema (definitions + breach records)
  - REST API for metrics queries

- **Notification Service** — Real-time event push
  - GraphQL subscriptions via WebSocket
  - Kafka-to-WebSocket bridge using Reactor Sinks
  - User-specific notification filtering

- **Identity Service** — RBAC and permissions
  - Permission check API
  - Seed data with 4 default roles (admin, operator, developer, user)
  - API key management for service accounts
  - Tenant management schema

- **API Gateway** — Unified entry point
  - Route configuration for all backend services
  - JWT validation via Keycloak OAuth2
  - Redis-based rate limiting
  - Structured request/response logging
  - CORS configuration
  - OpenAPI aggregation endpoint

#### Infrastructure
- Docker Compose full-stack setup (7 services + 4 infrastructure)
- Docker Compose dev mode (infrastructure only)
- PostgreSQL 16 with 6 isolated databases
- Apache Kafka 3.7 (KRaft mode, no ZooKeeper)
- Keycloak 24 with pre-configured realm, roles, users, and clients
- Redis 7 for rate limiting
- Health checks on all services

#### Security
- OAuth2/OIDC via Keycloak with JWT validation
- Keycloak realm role extraction (realm_access.roles → Spring Security)
- Pre-configured realm with 4 roles, 4 users, 5 clients, 4 groups

#### Developer Experience
- Makefile with common commands (build, up, down, test, status, verify)
- Multi-stage Dockerfiles (Java and React)
- GitHub Actions CI (backend, frontend, docker, integration tests)
- Spotless code formatting (Google Java Style)
- SpringDoc OpenAPI on all services
- Unified error response format across all services

#### Testing
- Integration test module (Testcontainers + Failsafe)
- CQRS event flow tests (process events → query model)
- Auth flow tests (Keycloak token validation)
- Performance baseline tests (throughput + latency)
- Health check verification tests

#### Frontend (Scaffolded)
- React 18 + TypeScript 5 + Vite monorepo
- 4 apps: Operate, Tasklist, Analytics, Modeler
- pnpm workspaces
- Tailwind CSS + shadcn/ui ready

#### Documentation
- Getting Started guide
- Architecture documentation
- Implementation roadmap
- Security model documentation
- Sample BPMN process (Order Fulfillment)

### Infrastructure Versions
- Java 21 (Eclipse Temurin)
- Spring Boot 3.3.5
- Spring Cloud 2023.0.3
- Activiti Cloud 8.4.0
- PostgreSQL 16
- Apache Kafka 3.7
- Keycloak 24
- Redis 7
- Node.js 20
- React 18

[0.1.0-alpha]: https://github.com/open-orchestration/oop/releases/tag/v0.1.0-alpha
