package com.oop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProcessLifecycleIT {

  private static final String GATEWAY_URL =
      System.getProperty("gateway.url", "http://localhost:8080");
  private static final String KEYCLOAK_URL =
      System.getProperty("keycloak.url", "http://localhost:8180");
  private static final String REALM = "oop";
  private static final String CLIENT_ID = "admin-cli";
  private static final String USERNAME = "admin";
  private static final String PASSWORD = "admin";

  private WebClient webClient;
  private String accessToken;
  private String processInstanceId;
  private String taskId;

  @BeforeAll
  void setup() {
    webClient = WebClient.builder().baseUrl(GATEWAY_URL).build();
    accessToken = obtainToken();
  }

  @Test
  @Order(1)
  void shouldDeployBpmnProcess() {
    String bpmnXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:activiti="http://activiti.org/bpmn"
                     targetNamespace="http://oop.com/test">
          <process id="integrationTestProcess" name="Integration Test Process" isExecutable="true">
            <startEvent id="start"/>
            <sequenceFlow id="flow1" sourceRef="start" targetRef="userTask1"/>
            <userTask id="userTask1" name="Review Order" activiti:assignee="admin"/>
            <sequenceFlow id="flow2" sourceRef="userTask1" targetRef="end"/>
            <endEvent id="end"/>
          </process>
        </definitions>
        """;

    String response =
        webClient
            .post()
            .uri("/api/v1/processes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(bpmnXml)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(30));

    assertThat(response).isNotNull();
  }

  @Test
  @Order(2)
  void shouldStartProcessInstance() {
    Map<String, Object> request =
        Map.of(
            "processDefinitionKey", "integrationTestProcess",
            "variables", Map.of("orderId", "ORD-001", "amount", 150.00));

    Map<?, ?> response =
        webClient
            .post()
            .uri("/api/v1/process-instances")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(30));

    assertThat(response).isNotNull();
    processInstanceId = (String) response.get("id");
    assertThat(processInstanceId).isNotNull();
  }

  @Test
  @Order(3)
  void shouldFindProcessInstanceInQueryService() {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              Map<?, ?> response =
                  webClient
                      .get()
                      .uri("/api/v1/query/process-instances/{id}", processInstanceId)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                      .retrieve()
                      .bodyToMono(Map.class)
                      .block(Duration.ofSeconds(10));

              assertThat(response).isNotNull();
              assertThat(response.get("processInstanceId")).isEqualTo(processInstanceId);
              assertThat(response.get("status")).isEqualTo("RUNNING");
            });
  }

  @Test
  @Order(4)
  void shouldFindTaskAssignedToUser() {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              Map<?, ?> response =
                  webClient
                      .get()
                      .uri(
                          "/api/v1/query/tasks/process-instance/{id}",
                          processInstanceId)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                      .retrieve()
                      .bodyToMono(Map.class)
                      .block(Duration.ofSeconds(10));

              assertThat(response).isNotNull();
            });

    Map<?, ?> tasksResponse =
        webClient
            .get()
            .uri("/api/v1/query/tasks?assignee=admin")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(10));

    assertThat(tasksResponse).isNotNull();
    var content = (java.util.List<?>) tasksResponse.get("content");
    assertThat(content).isNotEmpty();

    Map<?, ?> task = (Map<?, ?>) content.get(0);
    taskId = (String) task.get("taskId");
    assertThat(taskId).isNotNull();
  }

  @Test
  @Order(5)
  void shouldCompleteTask() {
    Map<String, Object> completeRequest = Map.of("outcome", "approved");

    webClient
        .post()
        .uri("/api/v1/tasks/{taskId}/complete", taskId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(completeRequest)
        .retrieve()
        .bodyToMono(Void.class)
        .block(Duration.ofSeconds(30));
  }

  @Test
  @Order(6)
  void shouldShowProcessInstanceAsCompleted() {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              Map<?, ?> response =
                  webClient
                      .get()
                      .uri("/api/v1/query/process-instances/{id}", processInstanceId)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                      .retrieve()
                      .bodyToMono(Map.class)
                      .block(Duration.ofSeconds(10));

              assertThat(response).isNotNull();
              assertThat(response.get("status")).isEqualTo("COMPLETED");
            });
  }

  @Test
  @Order(7)
  void shouldHaveAuditTrail() {
    await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              Map<?, ?> response =
                  webClient
                      .get()
                      .uri(
                          "/api/v1/audit/events?processInstanceId={id}",
                          processInstanceId)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                      .retrieve()
                      .bodyToMono(Map.class)
                      .block(Duration.ofSeconds(10));

              assertThat(response).isNotNull();
              var content = (java.util.List<?>) response.get("content");
              assertThat(content).hasSizeGreaterThanOrEqualTo(3);
            });
  }

  private String obtainToken() {
    Map<?, ?> tokenResponse =
        WebClient.builder()
            .baseUrl(KEYCLOAK_URL)
            .build()
            .post()
            .uri("/realms/{realm}/protocol/openid-connect/token", REALM)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", CLIENT_ID)
                    .with("username", USERNAME)
                    .with("password", PASSWORD))
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(30));

    assertThat(tokenResponse).isNotNull();
    return (String) tokenResponse.get("access_token");
  }
}
