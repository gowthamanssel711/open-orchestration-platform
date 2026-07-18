package com.oop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RbacIT {

  private static final String GATEWAY_URL =
      System.getProperty("gateway.url", "http://localhost:8080");
  private static final String KEYCLOAK_URL =
      System.getProperty("keycloak.url", "http://localhost:8180");

  private WebClient webClient;
  private String adminToken;
  private String userToken;

  @BeforeAll
  void setup() {
    webClient = WebClient.builder().baseUrl(GATEWAY_URL).build();
    adminToken = obtainToken("admin", "admin");
    userToken = obtainToken("user", "user");
  }

  @Test
  @Order(1)
  void adminShouldHaveFullPermissions() {
    Map<?, ?> result = checkPermission(adminToken, "*", "*");
    assertThat(result.get("allowed")).isEqualTo(true);
  }

  @Test
  @Order(2)
  void userShouldHaveTaskReadPermission() {
    Map<?, ?> result = checkPermission(userToken, "task", "read");
    assertThat(result.get("allowed")).isEqualTo(true);
  }

  @Test
  @Order(3)
  void userShouldHaveTaskCompletePermission() {
    Map<?, ?> result = checkPermission(userToken, "task", "complete");
    assertThat(result.get("allowed")).isEqualTo(true);
  }

  @Test
  @Order(4)
  void userShouldNotHaveProcessDeployPermission() {
    Map<?, ?> result = checkPermission(userToken, "process-definition", "deploy");
    assertThat(result.get("allowed")).isEqualTo(false);
  }

  @Test
  @Order(5)
  void userShouldNotHaveAdminAccess() {
    Map<?, ?> result = checkPermission(userToken, "admin", "read");
    assertThat(result.get("allowed")).isEqualTo(false);
  }

  @Test
  @Order(6)
  void adminShouldListTenants() {
    int status =
        webClient
            .get()
            .uri("/api/v1/admin/tenants")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(200);
  }

  @Test
  @Order(7)
  void adminShouldCreateTenant() {
    int status =
        webClient
            .post()
            .uri("/api/v1/admin/tenants")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("tenantId", "test-tenant", "name", "Test Tenant"))
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(201);
  }

  @Test
  @Order(8)
  void adminShouldAddMemberToTenant() {
    int status =
        webClient
            .post()
            .uri("/api/v1/admin/tenants/test-tenant/members")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "userId", "user-001",
                "username", "testuser",
                "role", "operator"))
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(201);
  }

  @Test
  @Order(9)
  void userShouldNotAccessTenantAdmin() {
    int status =
        webClient
            .get()
            .uri("/api/v1/admin/tenants")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(403);
  }

  @Test
  @Order(10)
  void adminShouldGenerateApiKey() {
    Map<?, ?> result =
        webClient
            .post()
            .uri("/api/v1/admin/api-keys")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "name", "ci-pipeline",
                "description", "CI/CD automation key",
                "tenantId", "default",
                "roles", List.of("developer", "operator")))
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(10));

    assertThat(result).isNotNull();
    assertThat((String) result.get("key")).startsWith("oop_live_sk_");
    assertThat(result.get("warning")).isNotNull();
  }

  @Test
  @Order(11)
  void requestShouldContainTenantHeaders() {
    // The gateway enrichment filter adds X-Tenant-Id, X-User-Roles headers
    // We verify this indirectly by checking the permission endpoint returns user info
    Map<?, ?> result = checkPermission(adminToken, "task", "read");
    assertThat(result.get("user")).isEqualTo("admin");
  }

  private Map<?, ?> checkPermission(String token, String resource, String action) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/admin/permissions/check")
            .queryParam("resource", resource)
            .queryParam("action", action)
            .build())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .bodyToMono(Map.class)
        .block(Duration.ofSeconds(10));
  }

  private String obtainToken(String username, String password) {
    Map<?, ?> tokenResponse =
        WebClient.builder()
            .baseUrl(KEYCLOAK_URL)
            .build()
            .post()
            .uri("/realms/oop/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", "admin-cli")
                    .with("username", username)
                    .with("password", password))
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(10));

    assertThat(tokenResponse).isNotNull();
    return (String) tokenResponse.get("access_token");
  }
}
