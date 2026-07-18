import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import Keycloak from 'keycloak-js';
import type { AuthContextType, AuthUser } from './types';

const AuthContext = createContext<AuthContextType | null>(null);

interface AuthProviderProps {
  keycloak: Keycloak;
  children: React.ReactNode;
  onTokenRefresh?: (token: string) => void;
}

export function AuthProvider({ keycloak, children, onTokenRefresh }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    keycloak
      .init({ onLoad: 'login-required', checkLoginIframe: false })
      .then((authenticated) => {
        if (authenticated) {
          updateAuthState();
          startTokenRefresh();
        }
        setIsLoading(false);
      })
      .catch(() => {
        setIsLoading(false);
      });
  }, [keycloak]);

  function updateAuthState() {
    if (keycloak.tokenParsed) {
      const parsed = keycloak.tokenParsed as Record<string, unknown>;
      const realmAccess = parsed.realm_access as { roles?: string[] } | undefined;

      setUser({
        id: parsed.sub as string,
        username: parsed.preferred_username as string,
        email: (parsed.email as string) || '',
        firstName: (parsed.given_name as string) || '',
        lastName: (parsed.family_name as string) || '',
        roles: realmAccess?.roles || [],
        tenantId: (parsed.tenant_id as string) || 'default',
      });
    }
    setToken(keycloak.token || null);
    if (keycloak.token && onTokenRefresh) {
      onTokenRefresh(keycloak.token);
    }
  }

  function startTokenRefresh() {
    setInterval(() => {
      keycloak.updateToken(30).then((refreshed) => {
        if (refreshed) {
          updateAuthState();
        }
      });
    }, 30000);
  }

  const login = useCallback(() => {
    keycloak.login();
  }, [keycloak]);

  const logout = useCallback(() => {
    keycloak.logout({ redirectUri: window.location.origin });
  }, [keycloak]);

  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role) ?? false,
    [user]
  );

  const hasAnyRole = useCallback(
    (roles: string[]) => roles.some((r) => user?.roles.includes(r)),
    [user]
  );

  const contextValue: AuthContextType = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: !!user,
      isLoading,
      login,
      logout,
      hasRole,
      hasAnyRole,
    }),
    [user, token, isLoading, login, logout, hasRole, hasAnyRole]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
