package com.oop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.reactive.function.client.WebClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthCheckIT {

  private WebClient webClient;

  @BeforeAll
  void setup() {
    webClient = WebClient.builder().build();
  }

  static Stream<String> serviceUrls() {
    return Stream.of(
        System.getProperty("gateway.url", "http://localhost:8080"),
        System.getProperty("runtime.url", "http://localhost:8081"),
        System.getProperty("query.url", "http://localhost:8082"),
        System.getProperty("audit.url", "http://localhost:8083"),
        System.getProperty("analytics.url", "http://localhost:8084"),
        System.getProperty("notification.url", "http://localhost:8085"),
        System.getProperty("identity.url", "http://localhost:8087"));
  }

  @ParameterizedTest(name = "Service at {0} should be healthy")
  @MethodSource("serviceUrls")
  void serviceShouldBeHealthy(String baseUrl) {
    Map<?, ?> response =
        webClient
            .get()
            .uri(baseUrl + "/actuator/health")
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(10));

    assertThat(response).isNotNull();
    assertThat(response.get("status")).isEqualTo("UP");
  }
}
