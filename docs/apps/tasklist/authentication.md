# Authentication

The Tasklist UI uses Keycloak for authentication via the `@oop/auth` shared package.

## Flow

```
User visits app
    │
    ▼
AuthProvider initializes keycloak-js
    │
    ▼
AuthGuard checks if authenticated
    │ (not authenticated)
    ▼
Redirect to Keycloak login page
    │ (user logs in)
    ▼
Redirect back with auth code
    │
    ▼
keycloak-js exchanges code for tokens
    │
    ▼
App renders, token available via useAuth()
```

## Setup

```tsx
// App.tsx
import { AuthProvider, AuthGuard, createKeycloakInstance, useAuth } from '@oop/auth';

const keycloak = createKeycloakInstance({ clientId: 'tasklist-ui' });

export default function App() {
  return (
    <AuthProvider keycloak={keycloak}>
      <AuthGuard>
        {/* App content - only rendered when authenticated */}
      </AuthGuard>
    </AuthProvider>
  );
}
```

## useAuth Hook

```tsx
const { user, token, logout } = useAuth();

// user.username — Keycloak preferred_username
// user.email — Keycloak email
// token — Current access token (auto-refreshed)
// logout() — Triggers Keycloak logout
```

## API Requests

The `useApiClient` hook automatically attaches the Bearer token:

```tsx
const { apiFetch } = useApiClient();

// Token automatically included
const tasks = await apiFetch('/api/v1/tasks');
```

```typescript
// api.ts
headers: {
  'Content-Type': 'application/json',
  ...(token ? { Authorization: `Bearer ${token}` } : {}),
}
```

## Keycloak Client

| Setting | Value |
|---|---|
| Client ID | `tasklist-ui` |
| Access Type | Public |
| Standard Flow | Enabled |
| Redirect URIs | `http://localhost:*` |

## User Info Display

The sidebar shows current user info and logout button:

```tsx
<p>{user?.username}</p>
<p>{user?.email}</p>
<button onClick={logout}>Logout</button>
```

## Token Refresh

`keycloak-js` handles token refresh automatically. The `useAuth` hook exposes the current valid token — no manual refresh needed.
