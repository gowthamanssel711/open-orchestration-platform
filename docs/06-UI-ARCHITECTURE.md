# 06 — UI Architecture

## Design System

### Component Library: shadcn/ui + Tailwind CSS

```
apps/shared/design-system/
├── components/
│   ├── Button/
│   ├── Card/
│   ├── DataTable/
│   ├── Dialog/
│   ├── Dropdown/
│   ├── Form/
│   ├── Input/
│   ├── Sidebar/
│   ├── Tabs/
│   ├── Toast/
│   └── Badge/
├── layouts/
│   ├── AppShell.tsx        # Sidebar + header + content
│   ├── PageHeader.tsx      # Page title + actions
│   └── EmptyState.tsx      # Empty collection placeholder
├── hooks/
│   ├── useAuth.ts          # Keycloak authentication
│   ├── usePermissions.ts   # RBAC checks
│   ├── useWebSocket.ts     # Real-time subscriptions
│   └── useTheme.ts         # Dark/light mode
└── styles/
    ├── tokens.css          # Design tokens (colors, spacing)
    └── tailwind.config.ts  # Shared Tailwind config
```

### Theme

- **Light mode** by default, dark mode toggle
- **Neutral palette** with process-state accent colors:
  - Running: Blue (`#3B82F6`)
  - Completed: Green (`#22C55E`)
  - Failed/Incident: Red (`#EF4444`)
  - Suspended: Yellow (`#EAB308`)
  - Canceled: Gray (`#6B7280`)

---

## App Architecture (Each UI App)

```
apps/operate/
├── src/
│   ├── main.tsx                    # App entry point
│   ├── App.tsx                     # Router + providers
│   ├── api/                        # TanStack Query hooks
│   │   ├── processes.ts            # Process definition queries
│   │   ├── instances.ts            # Instance queries + mutations
│   │   ├── tasks.ts                # Task queries
│   │   └── client.ts               # Axios/fetch client setup
│   ├── pages/                      # Route pages
│   │   ├── Dashboard/
│   │   ├── Processes/
│   │   ├── Instances/
│   │   ├── InstanceDetail/
│   │   ├── Incidents/
│   │   └── Settings/
│   ├── components/                 # App-specific components
│   │   ├── BpmnDiagram/            # BPMN viewer with overlays
│   │   ├── InstanceTable/
│   │   ├── VariablePanel/
│   │   ├── AuditTimeline/
│   │   └── IncidentCard/
│   ├── hooks/                      # App-specific hooks
│   ├── store/                      # Client state (minimal)
│   └── utils/                      # Helpers
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── vitest.config.ts
```

---

## BPMN Diagram Component (Shared)

The most critical shared component — renders BPMN diagrams with interactive overlays.

```typescript
// apps/shared/bpmn-viewer/BpmnViewer.tsx

interface BpmnViewerProps {
  xml: string;                          // BPMN 2.0 XML
  activeTokens?: ActivityToken[];       // Currently active flow nodes
  completedActivities?: string[];       // Completed activity IDs
  incidentActivities?: string[];        // Activities with incidents
  heatmapData?: HeatmapEntry[];         // Duration heatmap (analytics)
  onActivityClick?: (id: string) => void;
  interactive?: boolean;                 // Allow zoom/pan
}

interface ActivityToken {
  activityId: string;
  instanceCount: number;  // Multiple instances on same node
}

interface HeatmapEntry {
  activityId: string;
  avgDuration: number;    // milliseconds
  percentile: number;     // 0-1 (used for color intensity)
}
```

**Features:**
- Zoom/pan with mouse and touch
- Click on activities for detail panel
- Token visualization (blue dots on active nodes)
- Completed path highlighting (green trail)
- Incident markers (red warning icons)
- Heatmap overlay (color gradient by duration)
- Minimap for navigation
- Fullscreen toggle

---

## Operate UI — Page Breakdown

### Dashboard Page
- Instance count cards (running, completed, incidents today)
- Throughput chart (last 24h)
- Recent incidents list
- Top processes by volume

### Process Definitions Page
- Table: name, key, version, instances count, last deployed
- Actions: view XML, deploy new version, delete
- Filter: search by name/key

### Process Instances Page
- Table: ID, business key, process, status, started, duration
- Filters: status, process definition, date range, business key search
- Bulk actions: cancel selected, retry selected

### Instance Detail Page
```
┌─────────────────────────────────────────────────────────────┐
│ [< Back]  Order Process v3  |  Instance: pi-12345           │
│ Status: RUNNING  |  Started: 2026-07-15 10:30  |  [Cancel]  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────┐  ┌─────────────┐  │
│  │                                     │  │ VARIABLES    │  │
│  │   [BPMN DIAGRAM]                    │  │             │  │
│  │   (with active token markers)       │  │ orderId:    │  │
│  │                                     │  │   "12345"   │  │
│  │   ●─→ [Start] ─→ [Approve] ─→ ...  │  │ amount:     │  │
│  │        (green)    (blue=active)     │  │   99.99     │  │
│  │                                     │  │ status:     │  │
│  └─────────────────────────────────────┘  │   "pending" │  │
│                                           │             │  │
│  ┌─────────────────────────────────────┐  │ [Edit]      │  │
│  │ AUDIT LOG                           │  └─────────────┘  │
│  │                                     │                    │
│  │ 10:30:00  Process started           │  ┌─────────────┐  │
│  │ 10:30:01  Activity: validate-order  │  │ INCIDENTS    │  │
│  │ 10:30:02  Activity: check-stock     │  │             │  │
│  │ 10:30:05  Task: approve-order       │  │ (none)      │  │
│  │           assigned to managers      │  │             │  │
│  └─────────────────────────────────────┘  └─────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Tasklist UI — Page Breakdown

### Task Inbox
```
┌─────────────────────────────────────────────────────────────┐
│ MY TASKS (12)  |  GROUP TASKS (5)  |  ALL TASKS (89)        │
├─────────────────────────────────────────────────────────────┤
│ [Search...]  [Priority ▼]  [Due Date ▼]  [Process ▼]       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ⚡ Approve Order — ORDER-12345                          │ │
│ │ Priority: HIGH  |  Due: Tomorrow  |  Process: order     │ │
│ │ [Claim] [View]                                          │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📋 Review Document — DOC-789                            │ │
│ │ Priority: MEDIUM  |  Due: Jul 18  |  Process: review    │ │
│ │ [Complete] [Delegate]                                   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Task Detail + Form
```
┌─────────────────────────────────────────────────────────────┐
│ Approve Order                                    [Complete]  │
│ Process: order-process  |  Instance: pi-12345               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─── FORM ────────────────────────────────────────────┐     │
│ │                                                     │     │
│ │  Order ID:     12345                                │     │
│ │  Customer:     John Smith                           │     │
│ │  Amount:       $99.99                               │     │
│ │                                                     │     │
│ │  Decision:     ( ) Approve  ( ) Reject              │     │
│ │                                                     │     │
│ │  Notes:        [________________________]           │     │
│ │                [________________________]           │     │
│ │                                                     │     │
│ │            [Submit]  [Save Draft]                    │     │
│ └─────────────────────────────────────────────────────┘     │
│                                                             │
│ ┌─── HISTORY ─────────────────────────────────────────┐     │
│ │ Jul 15 10:30  Task created                          │     │
│ │ Jul 15 10:35  Claimed by john.doe                   │     │
│ └─────────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Analytics UI — Page Breakdown

### Process Heatmap
```
┌─────────────────────────────────────────────────────────────┐
│ Process: order-process  |  Period: Last 30 days             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                                                     │    │
│  │  [BPMN DIAGRAM WITH HEAT OVERLAY]                   │    │
│  │                                                     │    │
│  │  ●─→ [Validate]──→[Approve]──→[Process]──→[Ship]──●│    │
│  │       (green)      (RED/HOT)   (yellow)   (green)   │    │
│  │       avg: 2s      avg: 45min  avg: 5min  avg: 1s   │    │
│  │                                                     │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  LEGEND: 🟢 Fast (<1min)  🟡 Normal  🔴 Slow (>30min)     │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ BOTTLENECK: "Approve Order" — avg 45min             │    │
│  │ Recommendation: Add parallel approval or auto-approve│    │
│  │ for amounts < $50                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## State Management Pattern

```typescript
// Pattern: TanStack Query for server state, minimal client state

// API hook (server state)
function useProcessInstances(filters: InstanceFilters) {
  return useQuery({
    queryKey: ['instances', filters],
    queryFn: () => api.instances.list(filters),
    refetchInterval: 5000, // Poll every 5s for live data
  });
}

// Mutation with optimistic update
function useCancelInstance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (instanceId: string) => api.instances.cancel(instanceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['instances'] });
    },
  });
}

// WebSocket for real-time (replaces polling for active views)
function useInstanceEvents(instanceId: string) {
  const queryClient = useQueryClient();
  
  useWebSocket(`/ws/instances/${instanceId}`, {
    onMessage: (event) => {
      queryClient.setQueryData(
        ['instance', instanceId],
        (old) => applyEvent(old, event)
      );
    },
  });
}
```

---

## Authentication Flow (Frontend)

```typescript
// apps/shared/auth/AuthProvider.tsx

// Uses @react-keycloak/web
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
};

// Auto-refresh token, redirect to login if expired
// All API calls include Authorization header via Axios interceptor
```

---

## Responsive Design

| Breakpoint | Layout |
|------------|--------|
| Mobile (<768px) | Stack layout, hamburger menu, simplified tables |
| Tablet (768-1024px) | Collapsible sidebar, scrollable tables |
| Desktop (>1024px) | Full sidebar, split panes, detail panels |

BPMN diagrams on mobile: pinch-to-zoom, simplified node labels.
