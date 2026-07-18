package com.oop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowIT {

  private static final String GATEWAY_URL =
      System.getProperty("gateway.url", "http://localhost:8080");
  private static final String KEYCLOAK_URL =
      System.getProperty("keycloak.url", "http://localhost:8180");

  private WebClient webClient;

  @BeforeAll
  void setup() {
    webClient = WebClient.builder().baseUrl(GATEWAY_URL).build();
  }

  @Test
  void shouldRejectRequestWithoutToken() {
    int status =
        webClient
            .get()
            .uri("/api/v1/query/process-instances")
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(401);
  }

  @Test
  void shouldRejectRequestWithInvalidToken() {
    int status =
        webClient
            .get()
            .uri("/api/v1/query/process-instances")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-here")
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(401);
  }

  @Test
  void shouldAcceptRequestWithValidAdminToken() {
    String token = obtainToken("admin", "admin");

    int status =
        webClient
            .get()
            .uri("/api/v1/query/process-instances")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(200);
  }

  @Test
  void shouldAcceptRequestWithValidOperatorToken() {
    String token = obtainToken("operator", "operator");

    int status =
        webClient
            .get()
            .uri("/api/v1/query/process-instances")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(200);
  }

  @Test
  void shouldReturnTokenWithRealmRoles() {
    String token = obtainToken("admin", "admin");
    assertThat(token).isNotBlank();

    String[] parts = token.split("\\.");
    assertThat(parts).hasSize(3);

    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
    assertThat(payload).contains("realm_access");
    assertThat(payload).contains("admin");
  }

  @Test
  void shouldAccessActuatorWithoutToken() {
    int status =
        webClient
            .get()
            .uri("/actuator/health")
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block(Duration.ofSeconds(10));

    assertThat(status).isEqualTo(200);
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
