# Operate UI

Process operations dashboard for monitoring and managing running processes.

## Status

**Not yet active** — scaffolded but not implemented.

## Planned Features

- Real-time process instance monitoring
- Process instance detail with BPMN diagram overlay
- Incident management (failed jobs, retries)
- Variable inspection and modification
- Batch operations (cancel multiple instances)
- Process definition heatmaps

## Technology

| Component | Technology |
|---|---|
| Framework | React 18 |
| Build Tool | Vite |
| BPMN Viewer | @oop/bpmn-viewer (shared package) |

## Location

```
apps/operate/
├── src/
├── index.html
├── package.json
├── vite.config.ts
└── tailwind.config.js
```
