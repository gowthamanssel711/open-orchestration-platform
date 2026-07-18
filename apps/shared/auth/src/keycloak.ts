import Keycloak from 'keycloak-js';

export interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

const DEFAULT_CONFIG: KeycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'oop',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'operate-ui',
};

export function createKeycloakInstance(config?: Partial<KeycloakConfig>): Keycloak {
  const mergedConfig = { ...DEFAULT_CONFIG, ...config };
  return new Keycloak(mergedConfig);
}
