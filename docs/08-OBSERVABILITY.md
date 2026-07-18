# 08 — Observability

## Three Pillars

```
┌─────────────────────────────────────────────────────────────┐
│                     OBSERVABILITY STACK                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  METRICS              TRACES              LOGS               │
│  ────────             ──────              ────               │
│  Micrometer     →     OpenTelemetry  →    Structured JSON   │
│  Prometheus     ←     Jaeger/Tempo   ←    Loki              │
│  Grafana              Grafana             Grafana            │
│                                                             │
│  "What's happening?"  "Why is it slow?"   "What went wrong?"│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Metrics (Prometheus + Grafana)

### Business Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `oop_process_instances_started_total` | Counter | process_key, version | Total instances started |
| `oop_process_instances_completed_total` | Counter | process_key, version | Total instances completed |
| `oop_process_instances_failed_total` | Counter | process_key, version, error | Total instances failed |
| `oop_process_instance_duration_seconds` | Histogram | process_key | End-to-end process duration |
| `oop_tasks_created_total` | Counter | process_key, task_name | Human tasks created |
| `oop_tasks_completed_total` | Counter | process_key, task_name, assignee | Human tasks completed |
| `oop_task_duration_seconds` | Histogram | process_key, task_name | Time from creation to completion |
| `oop_connectors_execution_total` | Counter | connector_type, status | Connector calls |
| `oop_connectors_duration_seconds` | Histogram | connector_type | Connector execution time |
| `oop_active_instances` | Gauge | process_key | Currently running instances |
| `oop_active_tasks` | Gauge | process_key | Currently open tasks |
| `oop_incidents_active` | Gauge | process_key | Unresolved incidents |

### Infrastructure Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `oop_kafka_consumer_lag` | Gauge | Consumer group lag (events behind) |
| `oop_kafka_messages_produced_total` | Counter | Events published |
| `oop_db_connection_pool_active` | Gauge | Active DB connections |
| `oop_db_query_duration_seconds` | Histogram | Database query latency |
| `oop_gateway_requests_total` | Counter | API requests by endpoint |
| `oop_gateway_request_duration_seconds` | Histogram | API latency |
| `oop_gateway_rate_limited_total` | Counter | Rate-limited requests |

### Grafana Dashboards

**Dashboard 1: Platform Overview**
- Total instances (today vs yesterday)
- Throughput chart (instances/min over time)
- Active incidents count (with trend)
- Service health matrix (green/yellow/red)
- Top 5 processes by volume

**Dashboard 2: Process Performance**
- Duration percentiles per process (p50, p95, p99)
- Activity-level timing breakdown
- SLA compliance percentage
- Failure rate by process

**Dashboard 3: Infrastructure Health**
- Kafka consumer lag (critical — indicates system falling behind)
- DB connection pool utilization
- Memory/CPU per service
- JVM GC pauses
- Pod restarts

**Dashboard 4: API Gateway**
- Request rate (req/sec)
- Error rate (4xx, 5xx)
- Latency distribution
- Rate limiting events
- Top consumers (by API key)

---

## Distributed Tracing (OpenTelemetry → Jaeger)

### Trace Context Propagation

```
Client Request (trace-id: abc123)
  └── Gateway (span: route-request)
       └── Runtime Bundle (span: execute-command)
            ├── DB Query (span: persist-instance)
            └── Kafka Produce (span: emit-event)
                 └── Query Service (span: update-read-model)
                      └── DB Query (span: upsert-instance-view)
```

### Key Traces

| Trace | Spans | Purpose |
|-------|-------|---------|
| Process Start | Gateway → Runtime → DB → Kafka | End-to-end instance creation |
| Task Complete | Gateway → Runtime → DB → Kafka → Query | Task completion flow |
| Connector Call | Runtime → Kafka → Connector → External → Kafka → Runtime | External integration timing |
| Query Request | Gateway → Query → DB | Read path performance |

### Instrumentation

```java
// Spring Boot auto-instrumentation handles most spans
// Custom spans for business logic:

@Observed(name = "process.execution")
public ProcessInstance executeProcess(StartCommand command) {
    // Automatically creates span with duration + attributes
}
```

### Trace Sampling Strategy

| Environment | Strategy | Rate |
|-------------|----------|------|
| Development | Always sample | 100% |
| Staging | Always sample | 100% |
| Production (low traffic) | Always sample | 100% |
| Production (high traffic) | Probabilistic | 10% |
| Production (errors) | Always sample | 100% (all errors traced) |

---

## Logging (Structured JSON → Loki)

### Log Format

```json
{
  "timestamp": "2026-07-15T10:30:00.123Z",
  "level": "INFO",
  "service": "runtime-bundle",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "tenantId": "tenant-abc",
  "userId": "john.doe",
  "message": "Process instance started",
  "context": {
    "processDefinitionKey": "order-process",
    "processInstanceId": "pi-12345",
    "businessKey": "ORDER-456"
  }
}
```

### Log Levels by Environment

| Level | Development | Staging | Production |
|-------|-------------|---------|------------|
| TRACE | Optional | Off | Off |
| DEBUG | On | Optional | Off |
| INFO | On | On | On |
| WARN | On | On | On |
| ERROR | On | On | On |

### Key Log Events

```
INFO  — Process instance started/completed
INFO  — Task created/assigned/completed
INFO  — Connector execution started/completed
WARN  — Retry attempt on failed job
WARN  — Rate limit applied to client
WARN  — SLA breach detected
ERROR — Process instance failed (incident created)
ERROR — Connector execution failed
ERROR — Database connection error
ERROR — Kafka producer/consumer error
```

---

## Alerting (Prometheus Alertmanager)

### Critical Alerts (PagerDuty/Slack)

```yaml
groups:
  - name: platform-critical
    rules:
      - alert: ServiceDown
        expr: up{job=~"runtime-bundle|query-service|gateway"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"

      - alert: HighIncidentRate
        expr: rate(oop_incidents_active[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High incident rate: {{ $value }}/min"

      - alert: KafkaConsumerLagCritical
        expr: oop_kafka_consumer_lag > 10000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Kafka consumer lag critical: {{ $value }} messages behind"

      - alert: DatabaseConnectionPoolExhausted
        expr: oop_db_connection_pool_active / oop_db_connection_pool_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool > 90% utilized"
```

### Warning Alerts (Slack only)

```yaml
      - alert: HighErrorRate
        expr: rate(oop_gateway_requests_total{status=~"5.."}[5m]) / rate(oop_gateway_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API error rate above 5%"

      - alert: SlowProcessDuration
        expr: histogram_quantile(0.95, oop_process_instance_duration_seconds) > 3600
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "p95 process duration exceeds 1 hour"

      - alert: PodHighMemory
        expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.85
        for: 5m
        labels:
          severity: warning
```

---

## Health Checks

### Endpoint Hierarchy

```
GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "activiti": { "status": "UP", "details": { "running": 1523 } }
  }
}

GET /actuator/health/liveness    → 200 if JVM is alive
GET /actuator/health/readiness   → 200 if ready to serve traffic
GET /actuator/health/startup     → 200 if initialization complete
```

### Kubernetes Probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health/startup
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30  # Allow up to 150s for startup
```

---

## Runbook: Common Issues

| Symptom | Dashboard | Root Cause | Fix |
|---------|-----------|------------|-----|
| Kafka lag increasing | Infrastructure | Consumer too slow or dead | Scale query-service replicas |
| High API latency | API Gateway | DB connection pool exhausted | Increase pool size or add read replicas |
| Incidents spiking | Platform Overview | External service down | Check connector dashboard, fix integration |
| OOM kills | Infrastructure | Memory leak or undersized | Increase limits, investigate heap dump |
| 5xx errors | API Gateway | Service crash loop | Check logs, fix bug, rollback if needed |
