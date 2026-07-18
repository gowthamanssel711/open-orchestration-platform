package com.oop.runtime.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class ActivitiIdentityConfig {

  @Bean
  public UserGroupManager userGroupManager() {
    return new UserGroupManager() {

      @Override
      public List<String> getUserGroups(String username) {
        return getRolesFromContext();
      }

      @Override
      public List<String> getUserRoles(String username) {
        return getRolesFromContext();
      }

      @Override
      public List<String> getGroups() {
        return getRolesFromContext();
      }

      @Override
      public List<String> getUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
          return List.of(auth.getName());
        }
        return Collections.emptyList();
      }

      @SuppressWarnings("unchecked")
      private List<String> getRolesFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
          Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
          List<String> roles = new java.util.ArrayList<>();
          roles.add("ACTIVITI_USER");
          if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> keycloakRoles = (List<String>) realmAccess.get("roles");
            roles.addAll(keycloakRoles);
            if (keycloakRoles.contains("admin")) {
              roles.add("ACTIVITI_ADMIN");
            }
          }
          return roles;
        }
        return List.of("ACTIVITI_USER");
      }
    };
  }
}
