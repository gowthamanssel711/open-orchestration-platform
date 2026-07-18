package com.oop.gateway.config;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class OpenApiConfig {

  @Value("${RUNTIME_BUNDLE_URL:http://localhost:8081}")
  private String runtimeBundleUrl;

  @Value("${QUERY_SERVICE_URL:http://localhost:8082}")
  private String queryServiceUrl;

  @Value("${AUDIT_SERVICE_URL:http://localhost:8083}")
  private String auditServiceUrl;

  @Value("${ANALYTICS_SERVICE_URL:http://localhost:8084}")
  private String analyticsServiceUrl;

  @Value("${IDENTITY_SERVICE_URL:http://localhost:8087}")
  private String identityServiceUrl;

  @Bean
  public RouterFunction<ServerResponse> apiDocsRoute() {
    return RouterFunctions.route()
        .GET(
            "/api/docs",
            request ->
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(buildApiIndex()))
        .build();
  }

  private Map<String, Object> buildApiIndex() {
    return Map.of(
        "openapi", "3.0.3",
        "info", Map.of(
            "title", "Open Orchestration Platform API",
            "version", "0.1.0",
            "description", "Unified API gateway for all OOP services"),
        "servers", List.of(Map.of("url", "/", "description", "Gateway")),
        "paths", Map.of(),
        "x-service-docs", List.of(
            Map.of("name", "Runtime Bundle", "url", runtimeBundleUrl + "/v3/api-docs"),
            Map.of("name", "Query Service", "url", queryServiceUrl + "/v3/api-docs"),
            Map.of("name", "Audit Service", "url", auditServiceUrl + "/v3/api-docs"),
            Map.of("name", "Analytics Service", "url", analyticsServiceUrl + "/v3/api-docs"),
            Map.of("name", "Identity Service", "url", identityServiceUrl + "/v3/api-docs")));
  }
}
