package com.oop.gateway.filter;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
  private static final String API_KEY_HEADER = "X-API-Key";

  private final WebClient webClient;

  public ApiKeyAuthFilter(
      @Value("${IDENTITY_SERVICE_URL:http://localhost:8087}") String identityServiceUrl) {
    this.webClient = WebClient.builder().baseUrl(identityServiceUrl).build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

    if (apiKey == null || apiKey.isBlank()) {
      return chain.filter(exchange);
    }

    // If API key is present, skip JWT auth and validate via Identity Service
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return chain.filter(exchange);
    }

    return validateApiKey(apiKey)
        .flatMap(result -> {
          if (!Boolean.TRUE.equals(result.get("valid"))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
          }

          @SuppressWarnings("unchecked")
          List<String> roles = (List<String>) result.get("roles");
          String tenantId = (String) result.getOrDefault("tenantId", "default");
          String name = (String) result.get("name");

          ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
              .header("X-User-Id", "apikey:" + name)
              .header("X-User-Name", name)
              .header("X-User-Roles", String.join(",", roles))
              .header("X-Tenant-Id", tenantId)
              .header("X-Auth-Type", "api-key")
              .build();

          log.debug("API key auth successful: name={} tenant={} roles={}",
              name, tenantId, roles);

          return chain.filter(exchange.mutate().request(mutatedRequest).build());
        })
        .onErrorResume(e -> {
          log.error("API key validation failed", e);
          exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
          return exchange.getResponse().setComplete();
        });
  }

  @SuppressWarnings("unchecked")
  private Mono<Map<String, Object>> validateApiKey(String key) {
    return webClient.post()
        .uri("/api/v1/admin/api-keys/validate")
        .bodyValue(Map.of("key", key))
        .retrieve()
        .bodyToMono(Map.class)
        .map(m -> (Map<String, Object>) m);
  }

  @Override
  public int getOrder() {
    return -10;
  }
}
