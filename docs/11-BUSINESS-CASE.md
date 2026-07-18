# 11 — Business Case & Market Analysis

## The Market Opportunity

### Process Automation Market Size

| Year | Market Size | Growth Rate |
|------|------------|-------------|
| 2024 | $13.8B | — |
| 2025 | $16.2B | 17.4% CAGR |
| 2027 | $22.4B | 17.4% CAGR |
| 2030 | $37.8B | 17.4% CAGR |

**Source:** Industry analyst reports (Gartner, Forrester)

### Who's Paying for Process Automation Today

| Vendor | Revenue (est.) | Pricing Model | Target |
|--------|---------------|---------------|--------|
| Camunda | ~$100M+ ARR | $50K-500K/year per cluster | Enterprise |
| Pega | ~$1.4B ARR | $100K-1M/year | Large Enterprise |
| Appian | ~$545M ARR | $75-200/user/month | Enterprise |
| Temporal | ~$100M+ ARR | Usage-based | Engineering teams |
| ServiceNow (workflows) | Part of $8B+ | Per-user | Enterprise |

### The Gap: No Good Open-Source SaaS Option

```
                    ENTERPRISE ($$$$)
                    ┌──────────────────────────┐
                    │ Camunda 8, Pega, Appian  │
                    │ $50K-500K/year           │
                    └──────────────────────────┘

                    DEVELOPER TOOLS ($$)
                    ┌──────────────────────────┐
                    │ Temporal, Conductor       │
                    │ Code-first, no visual UI  │
                    └──────────────────────────┘

        ──── GAP ────────────────────────────────────
        
                    OPEN SOURCE + SAAS ($)
                    ┌──────────────────────────┐
                    │ ??? (THIS IS US)          │
                    │ Visual + Code, $99-999/mo │
                    └──────────────────────────┘

                    FREE (BUT LIMITED)
                    ┌──────────────────────────┐
                    │ Activiti, Flowable        │
                    │ No UIs, limited cloud     │
                    └──────────────────────────┘
```

---

## Revenue Model

### Pricing Strategy: "The Cheap Alternative That's Actually Good"

| Tier | Monthly Price | What's Included | Target Customer |
|------|--------------|-----------------|-----------------|
| **Community** | $0 (forever) | Full platform, self-hosted | Developers, startups |
| **Starter SaaS** | $99/mo | Managed hosting, 5K instances/mo, 5 users | Small teams |
| **Growth SaaS** | $299/mo | 50K instances/mo, 25 users, email support | Growing companies |
| **Business SaaS** | $999/mo | 500K instances/mo, unlimited users, priority support | Mid-market |
| **Enterprise** | Custom ($3K-10K/mo) | Dedicated infra, SLA, SSO, audit, compliance | Enterprise |

### Revenue Projections (Conservative)

| Month | Customers | MRR | ARR |
|-------|-----------|-----|-----|
| 6 (v1.0 launch) | 0 | $0 | $0 (building) |
| 9 | 10 | $2K | $24K |
| 12 | 50 | $15K | $180K |
| 18 | 200 | $60K | $720K |
| 24 | 500 | $150K | **$1.8M** |
| 36 | 1,500 | $500K | **$6M** |

### How We Get to $1M ARR

```
$1M ARR = 
  50 × Starter ($99)     = $5K/mo    = $60K/year
  100 × Growth ($299)    = $30K/mo   = $360K/year
  30 × Business ($999)   = $30K/mo   = $360K/year
  3 × Enterprise ($5K)   = $15K/mo   = $180K/year
  ─────────────────────────────────────────────────
  183 customers           = $80K/mo   ≈ $960K/year ✓
```

**183 paying customers to hit $1M ARR.** With a TAM of millions of companies using process automation, this is achievable in 18-24 months.

---

## Competitive Advantages

### Why Customers Choose Us Over Camunda 8

| Factor | Camunda 8 | Our Platform | Impact |
|--------|-----------|-------------|--------|
| **Price** | $50K-500K/year | $0-12K/year | **10-50x cheaper** |
| **Lock-in** | Proprietary license | Apache 2.0, self-host anytime | No risk |
| **Complexity** | Elasticsearch required | PostgreSQL only | Less infra |
| **Self-host option** | Need Enterprise license | Always free | Full control |
| **Time to value** | Weeks (complex setup) | Minutes (Docker Compose) | Faster |
| **Analytics** | Extra $$ (Optimize) | Built-in free | Included |
| **Modeler** | SaaS-only (Web Modeler) | Self-hosted | On-prem OK |

### Why Customers Choose Us Over Temporal

| Factor | Temporal | Our Platform | Impact |
|--------|----------|-------------|--------|
| **Visual design** | Code-only | BPMN visual + code | Business users |
| **Business users** | Developers only | All stakeholders | Wider adoption |
| **Human tasks** | Build yourself | Built-in Tasklist | Time savings |
| **Analytics** | Build yourself | Built-in dashboards | Immediate value |
| **Standards** | Proprietary SDK | BPMN 2.0 standard | Portability |

---

## Total Cost of Ownership: Us vs Camunda 8

### Scenario: Mid-size company, 50K processes/month

**Camunda 8 Enterprise:**
```
License:                    $80,000/year
Elasticsearch (3 nodes):    $3,600/year (cloud)
Kubernetes infra:           $12,000/year
Operations engineer (0.5):  $75,000/year
─────────────────────────────────────────
Total:                      $170,600/year
```

**Our Platform (Self-Hosted):**
```
License:                    $0
PostgreSQL (managed):       $1,200/year
Kubernetes infra:           $8,000/year
Operations engineer (0.25): $37,500/year
─────────────────────────────────────────
Total:                      $46,700/year
Savings:                    $123,900/year (73% less)
```

**Our Platform (Managed SaaS):**
```
Subscription:               $3,600/year (Growth tier)
Infrastructure:             $0 (included)
Operations:                 $0 (managed)
─────────────────────────────────────────
Total:                      $3,600/year
Savings:                    $167,000/year (98% less)
```

---

## Go-To-Market Strategy

### Phase 1: Community-Led Growth (Months 1-9)

| Activity | Goal |
|----------|------|
| Open-source on GitHub | Build credibility and trust |
| Technical blog posts | SEO + developer awareness |
| Dev.to / Hashnode articles | "How to run BPMN without paying $100K" |
| YouTube tutorials | Visual learners, process modeling |
| Discord community | Support + feedback loop |
| Hacker News / Reddit launches | Initial traffic spike |
| Conference talks (QCon, KubeCon) | Enterprise visibility |
| Comparison pages | "vs Camunda", "vs Temporal" SEO |

### Phase 2: Product-Led Growth (Months 6-12)

| Activity | Goal |
|----------|------|
| Free SaaS tier (with limits) | Frictionless trial |
| One-click deploy buttons (Railway, Render, Fly.io) | Easy adoption |
| GitHub template repo | Clone and run in 5 minutes |
| Starter templates (HR, Finance, IT) | Show value immediately |
| Usage-based upgrade nudges | Convert free → paid |
| Integrations (Slack, Teams notifications) | Stickiness |

### Phase 3: Sales-Assisted (Months 12+)

| Activity | Goal |
|----------|------|
| Case studies (first enterprise customers) | Social proof |
| Migration tool (Camunda → Us) | Remove switching cost |
| Partner program (SI firms) | Indirect sales channel |
| Compliance certifications (SOC2, ISO27001) | Enterprise readiness |
| White-glove onboarding (Enterprise tier) | High-touch sales |

---

## Investment Required

### Bootstrap Path (3-4 developers, part-time)

| Period | Investment | Deliverable |
|--------|-----------|-------------|
| Months 1-6 | $0 (sweat equity) | v1.0 — Core platform |
| Months 6-9 | $0 (sweat equity) | v1.5 — Modeler + Connectors |
| Month 9 | First revenue | Starter SaaS customers |
| Month 12 | Break-even? | Growth tier customers |

**Total cash needed: $0 if team works on this part-time alongside jobs.**

### Funded Path (Seed Round)

| Investment | Team | Timeline to $1M ARR |
|-----------|------|---------------------|
| $500K seed | 4 FTE engineers | 18 months |
| $2M seed | 7 FTE (eng + sales + ops) | 12 months |

### Cost Structure (Monthly, at scale)

| Cost | Amount | Notes |
|------|--------|-------|
| Cloud infrastructure | $3K-10K | Multi-tenant shared infra |
| Team (7 people) | $80K-120K | Depending on location |
| Tools/SaaS | $2K | GitHub, monitoring, etc. |
| Marketing | $5K | Content, ads, events |
| **Total burn** | **$90K-137K/month** | |
| **Break-even at** | **~$100K MRR** | ~300 customers |

---

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Camunda drops price dramatically | Low | High | OSS core always free; compete on features |
| Temporal adds visual UI | Medium | Medium | Our business user focus is broader |
| Low adoption | Medium | High | Focus on specific niche first (e.g., fintech) |
| Technical debt slows development | Medium | Medium | Strong architecture, automated testing |
| Key person leaves | Medium | High | Document everything, multiple maintainers |
| Big cloud provider builds competing product | Low | High | Community + portability as differentiator |

---

## Answer: Is This a Million-Dollar Project?

### YES. Here's why:

1. **Market is $13B+ and growing 17% annually** — plenty of room
2. **Camunda charges $50K-500K/year** — massive price disruption opportunity
3. **183 customers at blended $450/mo = $1M ARR** — achievable in 18-24 months
4. **Open source creates organic growth** — developers discover, try, adopt, then buy SaaS
5. **Switching cost from Camunda is LOW** — standard BPMN 2.0, similar APIs
6. **Infra cost per customer is LOW** — multi-tenant, shared PostgreSQL, Kafka
7. **Gross margin is HIGH** — ~80-90% (SaaS standard)

### To be a million-dollar project, you need:

```
✅ Working product (6 months to v1.0)
✅ 10x cheaper than incumbents (we're 50-100x cheaper)
✅ Comparable features (achievable in 12 months)
✅ Developer trust (open source + community)
✅ Easy trial (free tier + Docker Compose + one-click deploy)
✅ Upgrade path (free → starter → growth → enterprise)
```

### Conservative math:

```
Year 1:  $180K ARR  (50 customers, mostly Starter/Growth)
Year 2:  $1.8M ARR  (500 customers, mix of tiers)
Year 3:  $6M ARR    (1500 customers + enterprise)
Year 4:  $15M ARR   (3000 customers + 50 enterprise)
```

**This is absolutely a million-dollar project.** The question is execution speed and market timing. The window is NOW — Camunda just tightened their license, Temporal is code-only, and enterprises are looking for cheaper alternatives.
