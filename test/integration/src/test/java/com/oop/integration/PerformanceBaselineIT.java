package com.oop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceBaselineIT {

  private static final String GATEWAY_URL =
      System.getProperty("gateway.url", "http://localhost:8080");
  private static final String KEYCLOAK_URL =
      System.getProperty("keycloak.url", "http://localhost:8180");
  private static final int WARMUP_REQUESTS = 5;
  private static final int BENCHMARK_REQUESTS = 50;
  private static final int CONCURRENT_USERS = 5;

  private WebClient webClient;
  private String accessToken;

  @BeforeAll
  void setup() {
    webClient = WebClient.builder().baseUrl(GATEWAY_URL).build();
    accessToken = obtainToken();
  }

  @Test
  void shouldStartProcessInstancesWithAcceptableThroughput() {
    // Warmup
    for (int i = 0; i < WARMUP_REQUESTS; i++) {
      startProcessInstance("warmup-" + i);
    }

    // Benchmark
    Instant start = Instant.now();
    List<Long> latencies = new ArrayList<>();

    for (int i = 0; i < BENCHMARK_REQUESTS; i++) {
      Instant reqStart = Instant.now();
      startProcessInstance("perf-" + i);
      latencies.add(Duration.between(reqStart, Instant.now()).toMillis());
    }

    Duration total = Duration.between(start, Instant.now());
    double throughput = (double) BENCHMARK_REQUESTS / total.toSeconds();
    long p50 = percentile(latencies, 50);
    long p95 = percentile(latencies, 95);
    long p99 = percentile(latencies, 99);

    System.out.println("╔════════════════════════════════════════════════╗");
    System.out.println("║         PERFORMANCE BASELINE RESULTS           ║");
    System.out.println("╠════════════════════════════════════════════════╣");
    System.out.printf("║  Total requests:    %d%n", BENCHMARK_REQUESTS);
    System.out.printf("║  Total time:        %d ms%n", total.toMillis());
    System.out.printf("║  Throughput:        %.1f req/s%n", throughput);
    System.out.printf("║  Latency p50:       %d ms%n", p50);
    System.out.printf("║  Latency p95:       %d ms%n", p95);
    System.out.printf("║  Latency p99:       %d ms%n", p99);
    System.out.println("╚════════════════════════════════════════════════╝");

    // Baseline assertions — these should pass on any reasonable setup
    assertThat(throughput).as("Throughput should be > 1 req/s").isGreaterThan(1.0);
    assertThat(p95).as("p95 latency should be < 5000ms").isLessThan(5000);
  }

  @Test
  void shouldHandleConcurrentProcessStarts() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);

    Instant start = Instant.now();
    List<CompletableFuture<Long>> futures = new ArrayList<>();

    for (int i = 0; i < BENCHMARK_REQUESTS; i++) {
      final int idx = i;
      futures.add(
          CompletableFuture.supplyAsync(
              () -> {
                Instant reqStart = Instant.now();
                startProcessInstance("concurrent-" + idx);
                return Duration.between(reqStart, Instant.now()).toMillis();
              },
              executor));
    }

    List<Long> latencies =
        futures.stream().map(CompletableFuture::join).toList();

    Duration total = Duration.between(start, Instant.now());
    double throughput = (double) BENCHMARK_REQUESTS / total.toSeconds();
    long p50 = percentile(latencies, 50);
    long p95 = percentile(latencies, 95);

    System.out.println();
    System.out.println("╔════════════════════════════════════════════════╗");
    System.out.println("║     CONCURRENT PERFORMANCE RESULTS             ║");
    System.out.printf("║  Concurrent users:  %d%n", CONCURRENT_USERS);
    System.out.printf("║  Total requests:    %d%n", BENCHMARK_REQUESTS);
    System.out.printf("║  Total time:        %d ms%n", total.toMillis());
    System.out.printf("║  Throughput:        %.1f req/s%n", throughput);
    System.out.printf("║  Latency p50:       %d ms%n", p50);
    System.out.printf("║  Latency p95:       %d ms%n", p95);
    System.out.println("╚════════════════════════════════════════════════╝");

    executor.shutdown();

    assertThat(throughput).as("Concurrent throughput should be > 2 req/s").isGreaterThan(2.0);
  }

  @Test
  void shouldQueryProcessInstancesWithAcceptableLatency() {
    // Start some processes first
    for (int i = 0; i < 10; i++) {
      startProcessInstance("query-test-" + i);
    }

    // Benchmark query endpoint
    List<Long> latencies = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      Instant reqStart = Instant.now();
      webClient
          .get()
          .uri("/api/v1/query/process-instances?size=20")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .bodyToMono(Map.class)
          .block(Duration.ofSeconds(10));
      latencies.add(Duration.between(reqStart, Instant.now()).toMillis());
    }

    long p50 = percentile(latencies, 50);
    long p95 = percentile(latencies, 95);

    System.out.println();
    System.out.printf("  Query p50: %d ms, p95: %d ms%n", p50, p95);

    assertThat(p95).as("Query p95 latency should be < 2000ms").isLessThan(2000);
  }

  private void startProcessInstance(String orderId) {
    Map<String, Object> request =
        Map.of(
            "processDefinitionKey", "sampleOrderProcess",
            "variables", Map.of("orderId", orderId, "amount", 99.99));

    webClient
        .post()
        .uri("/api/v1/process-instances")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Map.class)
        .block(Duration.ofSeconds(30));
  }

  private long percentile(List<Long> latencies, int percentile) {
    List<Long> sorted = latencies.stream().sorted().toList();
    int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
    return sorted.get(Math.max(0, index));
  }

  private String obtainToken() {
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
                    .with("username", "admin")
                    .with("password", "admin"))
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(10));

    return (String) tokenResponse.get("access_token");
  }
}
