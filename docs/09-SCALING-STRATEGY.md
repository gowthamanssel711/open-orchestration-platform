# 09 — Scaling Strategy

## Performance Targets

| Metric | Small (Startup) | Medium (Growth) | Large (Enterprise) |
|--------|-----------------|-----------------|-------------------|
| Instances started/sec | 100 | 1,000 | 10,000 |
| Concurrent running instances | 10K | 100K | 1M+ |
| Active human tasks | 1K | 50K | 500K |
| API requests/sec | 500 | 5,000 | 50,000 |
| Event throughput (Kafka) | 1K msg/sec | 50K msg/sec | 500K msg/sec |
| Process start latency (p99) | <100ms | <200ms | <500ms |
| Query latency (p99) | <50ms | <100ms | <200ms |
| Monthly cost (cloud) | $200-500 | $1K-3K | $5K-15K |

---

## Scaling Tiers

### Tier 1: Single Node (Development / Small Production)

```
┌────────────────────────────────────────┐
│         Single Docker Compose           │
│                                        │
│  Gateway ─→ Runtime Bundle ─→ Kafka    │
│          ─→ Query Service  ─→ PG       │
│                                        │
│  Capacity: ~100 instances/sec          │
│  Infra: 1 VM (4 CPU, 16GB RAM)        │
└────────────────────────────────────────┘
```

**When to use:** <100 instances/day, development, demos
**Cost:** ~$50-150/month (single VM)

### Tier 2: Multi-Instance (Small-Medium Production)

```
┌────────────────────────────────────────────────┐
│              Kubernetes (3 nodes)                │
│                                                │
│  Gateway ×2 ─→ Runtime Bundle ×2 ─→ Kafka ×3  │
│             ─→ Query Service ×2  ─→ PG (HA)   │
│                                                │
│  Capacity: ~1,000 instances/sec                │
│  Infra: 3 nodes (4 CPU, 8GB each)             │
└────────────────────────────────────────────────┘
```

**When to use:** 100-10K instances/day, production workloads
**Cost:** ~$500-1500/month

### Tier 3: Scaled (Medium-Large Production)

```
┌──────────────────────────────────────────────────────┐
│              Kubernetes (6+ nodes, multi-AZ)           │
│                                                      │
│  Gateway ×3 ─→ Runtime Bundle ×5 ─→ Kafka ×3        │
│             ─→ Query Service ×3  ─→ PG Primary+2 Rep │
│             ─→ Analytics ×2      ─→ ES Cluster ×3    │
│                                                      │
│  + Redis Sentinel ×3                                 │
│  + Horizontal Pod Autoscaler on all services         │
│                                                      │
│  Capacity: ~10,000 instances/sec                     │
│  Infra: 6 nodes (8 CPU, 16GB each)                  │
└──────────────────────────────────────────────────────┘
```

**When to use:** 10K-1M instances/day, high availability required
**Cost:** ~$3K-8K/month

### Tier 4: Enterprise (Large Scale)

```
┌────────────────────────────────────────────────────────────┐
│          Kubernetes (12+ nodes, multi-region)               │
│                                                            │
│  Gateway ×5+ (HPA) ─→ Runtime Bundle ×10+ (HPA)           │
│                     ─→ Query Service ×5+ (HPA)             │
│                     ─→ Analytics ×3+                        │
│                                                            │
│  Kafka ×5 (multi-partition, compaction)                     │
│  PG: Primary + 3 Replicas + Standby in DR region           │
│  ES: 6+ data nodes + 3 master nodes                        │
│  Redis: Cluster mode (6 nodes)                             │
│                                                            │
│  + Sharded Runtime Bundles (by tenant/process)             │
│  + Read replicas in secondary region                        │
│  + CDN for frontend UIs                                     │
│                                                            │
│  Capacity: 100,000+ instances/sec                          │
│  Infra: 12+ nodes (16 CPU, 32GB each)                     │
└────────────────────────────────────────────────────────────┘
```

**When to use:** >1M instances/day, multi-tenant SaaS, global deployment
**Cost:** ~$10K-30K/month

---

## Scaling Each Component

### Runtime Bundle (Process Engine)

| Strategy | When | How |
|----------|------|-----|
| Vertical | CPU-bound (complex expressions) | Increase memory/CPU limits |
| Horizontal | Throughput-bound | Add replicas (stateless, DB-coordinated) |
| Sharding | Tenant isolation needed | Dedicated Runtime Bundle per tenant |
| Read separation | Mixed read/write load | Route reads to Query Service, not Runtime |

**Scaling limits:**
- Single instance: ~500-1000 instances/sec (DB write throughput bound)
- PostgreSQL write scaling: Connection pooling (PgBouncer) → Read replicas → Partitioning
- Maximum practical: ~10K instances/sec with connection pooling + fast SSD

**Auto-scaling policy:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  scaleTargetRef:
    name: runtime-bundle
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
    - type: Pods
      pods:
        metric:
          name: oop_active_instances
        target:
          type: AverageValue
          averageValue: 5000
```

### Query Service

| Strategy | When | How |
|----------|------|-----|
| Horizontal | High read load | Add replicas (all hit same DB) |
| Read replicas | DB read throughput saturated | Route to PG replicas |
| Elasticsearch | Full-text search / high-volume queries | Add ES as query backend |
| Caching | Repeated queries (dashboard widgets) | Redis cache layer |

### Kafka

| Strategy | When | How |
|----------|------|-----|
| Add partitions | Consumer throughput insufficient | More partitions = more parallel consumers |
| Add brokers | Storage/network saturated | Scale cluster |
| Topic compaction | Event store growing unbounded | Enable log compaction for state topics |
| Tiered storage | Long retention needed | S3-backed tiered storage |

### PostgreSQL

| Strategy | When | How |
|----------|------|-----|
| Connection pooling | Connection limits hit | PgBouncer (transaction mode) |
| Read replicas | Read load > write load | Streaming replication |
| Table partitioning | Large tables (>100M rows) | Partition by time (audit) or tenant |
| Archival | Historical data growing | Move completed instances to archive tables |

---

## Data Retention & Archival

### Retention Policies

| Data Type | Active Retention | Archive | Delete |
|-----------|-----------------|---------|--------|
| Running instances | Forever (until complete) | N/A | N/A |
| Completed instances | 30 days | Move to archive after 30d | After 1 year |
| Audit events | 90 days (hot) | Move to cold storage | After 3 years |
| Analytics aggregates | 1 year (hot) | N/A | After 5 years |
| Task history | 30 days | Archive after 30d | After 1 year |
| Process definitions | Forever | N/A | Manual only |

### Archival Strategy

```sql
-- Daily archival job (runs at 2 AM)
-- Move completed instances older than 30 days to archive

INSERT INTO process_instances_archive
SELECT * FROM process_instances
WHERE status = 'COMPLETED'
  AND completed_at < NOW() - INTERVAL '30 days';

DELETE FROM process_instances
WHERE status = 'COMPLETED'
  AND completed_at < NOW() - INTERVAL '30 days';

-- Partition audit events by month
CREATE TABLE audit_events_2026_07 PARTITION OF audit_events
  FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
```

---

## Load Testing

### Tools

- **k6** for API load testing
- **Gatling** for complex scenario simulation
- **Custom Kafka producer** for event throughput testing

### Scenarios

```javascript
// test/load/scenarios/basic-process.js (k6)

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up
    { duration: '5m', target: 100 },   // Steady state
    { duration: '2m', target: 500 },   // Spike
    { duration: '5m', target: 500 },   // Sustained high
    { duration: '2m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Start process instance
  const startRes = http.post(
    `${BASE_URL}/api/v1/processes/order-process/instances`,
    JSON.stringify({
      businessKey: `ORDER-${Date.now()}-${__VU}`,
      variables: { orderId: `${__ITER}`, amount: 99.99 },
    }),
    { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` } }
  );

  check(startRes, {
    'instance started': (r) => r.status === 201,
    'latency < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);
}
```

### Benchmark Results (Target)

| Scenario | Instances/sec | p50 Latency | p99 Latency | Error Rate |
|----------|--------------|-------------|-------------|------------|
| Simple linear process (5 tasks) | 5,000 | 15ms | 80ms | <0.01% |
| Process with parallel gateway | 3,000 | 25ms | 120ms | <0.01% |
| Process with human tasks | 2,000 | 20ms | 100ms | <0.01% |
| Process with external connector | 1,000 | 50ms | 300ms | <0.1% |
| Mixed workload (realistic) | 2,500 | 30ms | 150ms | <0.05% |

---

## Cost Optimization

### Recommendations by Stage

| Stage | Strategy | Savings |
|-------|----------|---------|
| **Startup** | Single node, no ES, no Redis | Minimum cost |
| **Growth** | Right-size pods, spot instances for non-critical | 30-50% |
| **Scale** | Reserved instances, tiered storage, archival | 40-60% |
| **Enterprise** | Multi-tenant density, shared infrastructure | Per-tenant cost drops |

### Cloud Provider Comparison (Tier 2 workload)

| Provider | Monthly Estimate | Notes |
|----------|-----------------|-------|
| AWS (EKS) | ~$800-1200 | t3.xlarge nodes, RDS, MSK |
| GCP (GKE) | ~$700-1100 | e2-standard-4 nodes, Cloud SQL, Pub/Sub |
| Azure (AKS) | ~$750-1150 | D4s_v3 nodes, Azure DB, Event Hubs |
| Hetzner (k3s) | ~$200-400 | Dedicated servers, self-managed |
| Self-hosted | Hardware cost only | Requires ops team |

---

## Capacity Planning Formula

```
Required Runtime Bundle instances =
  (Peak instances per second × Average process duration in seconds)
  ÷ Max concurrent instances per Runtime Bundle (default: 5000)

Required Query Service instances =
  Peak read requests per second ÷ 2000 (queries/sec per instance)

Required Kafka partitions per topic =
  Peak messages per second ÷ 5000 (messages/sec per partition)

Required PostgreSQL connections =
  (Runtime instances × 20) + (Query instances × 30) + (Audit × 10)
  → Use PgBouncer if > 100 total
```
