package com.oop.gateway.filter;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TenantEnrichmentFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(TenantEnrichmentFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .map(ctx -> ctx.getAuthentication())
        .filter(auth -> auth instanceof JwtAuthenticationToken)
        .cast(JwtAuthenticationToken.class)
        .map(JwtAuthenticationToken::getToken)
        .flatMap(jwt -> {
          ServerHttpRequest mutatedRequest = enrichRequest(exchange.getRequest(), jwt);
          ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
          return chain.filter(mutatedExchange);
        })
        .switchIfEmpty(chain.filter(exchange));
  }

  private ServerHttpRequest enrichRequest(ServerHttpRequest request, Jwt jwt) {
    ServerHttpRequest.Builder builder = request.mutate();

    String tenantId = extractTenantId(jwt);
    if (tenantId != null) {
      builder.header("X-Tenant-Id", tenantId);
    }

    String userId = jwt.getSubject();
    if (userId != null) {
      builder.header("X-User-Id", userId);
    }

    String username = jwt.getClaimAsString("preferred_username");
    if (username != null) {
      builder.header("X-User-Name", username);
    }

    List<String> roles = extractRoles(jwt);
    if (!roles.isEmpty()) {
      builder.header("X-User-Roles", String.join(",", roles));
    }

    List<String> groups = jwt.getClaimAsStringList("groups");
    if (groups != null && !groups.isEmpty()) {
      builder.header("X-User-Groups", String.join(",", groups));
    }

    log.debug("Enriched request for user={} tenant={} roles={}",
        username, tenantId, roles);

    return builder.build();
  }

  private String extractTenantId(Jwt jwt) {
    String tenantId = jwt.getClaimAsString("tenant_id");
    if (tenantId != null) {
      return tenantId;
    }
    // Fallback: check groups for tenant pattern /tenant-{id}
    List<String> groups = jwt.getClaimAsStringList("groups");
    if (groups != null) {
      return groups.stream()
          .filter(g -> g.startsWith("/tenant-"))
          .map(g -> g.substring("/tenant-".length()))
          .findFirst()
          .orElse("default");
    }
    return "default";
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.containsKey("roles")) {
      return (List<String>) realmAccess.get("roles");
    }
    return List.of();
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
