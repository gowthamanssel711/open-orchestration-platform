package com.oop.query.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.oop.query.model.ProcessInstanceEntity;
import com.oop.query.model.TaskEntity;
import com.oop.query.repository.ProcessInstanceRepository;
import com.oop.query.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestChannelBinderConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CqrsEventFlowIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("oop_query")
          .withUsername("oop")
          .withPassword("oop-dev");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private InputDestination inputDestination;
  @Autowired private ProcessInstanceRepository processInstanceRepository;
  @Autowired private TaskRepository taskRepository;

  private final String processInstanceId = UUID.randomUUID().toString();
  private final String taskId = UUID.randomUUID().toString();

  @Test
  @Order(1)
  void shouldPersistProcessStartedEvent() {
    Map<String, Object> event =
        Map.of(
            "eventType", "PROCESS_STARTED",
            "processInstanceId", processInstanceId,
            "processDefinitionKey", "orderProcess",
            "initiator", "admin",
            "timestamp", Instant.now().toString());

    inputDestination.send(
        new GenericMessage<>(event), "oop.engine.process.events");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var entity = processInstanceRepository.findByProcessInstanceId(processInstanceId);
              assertThat(entity).isPresent();
              assertThat(entity.get().getStatus()).isEqualTo("RUNNING");
              assertThat(entity.get().getProcessDefinitionKey()).isEqualTo("orderProcess");
              assertThat(entity.get().getInitiator()).isEqualTo("admin");
            });
  }

  @Test
  @Order(2)
  void shouldPersistTaskCreatedEvent() {
    Map<String, Object> event =
        Map.of(
            "eventType", "TASK_CREATED",
            "taskId", taskId,
            "processInstanceId", processInstanceId,
            "name", "Review Order",
            "assignee", "operator1",
            "timestamp", Instant.now().toString());

    inputDestination.send(
        new GenericMessage<>(event), "oop.engine.task.events");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var entity = taskRepository.findByTaskId(taskId);
              assertThat(entity).isPresent();
              assertThat(entity.get().getName()).isEqualTo("Review Order");
              assertThat(entity.get().getAssignee()).isEqualTo("operator1");
              assertThat(entity.get().getStatus()).isEqualTo("CREATED");
            });
  }

  @Test
  @Order(3)
  void shouldUpdateTaskOnAssignedEvent() {
    Map<String, Object> event =
        Map.of(
            "eventType", "TASK_ASSIGNED",
            "taskId", taskId,
            "processInstanceId", processInstanceId,
            "assignee", "admin",
            "timestamp", Instant.now().toString());

    inputDestination.send(
        new GenericMessage<>(event), "oop.engine.task.events");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var entity = taskRepository.findByTaskId(taskId);
              assertThat(entity).isPresent();
              assertThat(entity.get().getAssignee()).isEqualTo("admin");
              assertThat(entity.get().getStatus()).isEqualTo("ASSIGNED");
            });
  }

  @Test
  @Order(4)
  void shouldUpdateTaskOnCompletedEvent() {
    Map<String, Object> event =
        Map.of(
            "eventType", "TASK_COMPLETED",
            "taskId", taskId,
            "processInstanceId", processInstanceId,
            "timestamp", Instant.now().toString());

    inputDestination.send(
        new GenericMessage<>(event), "oop.engine.task.events");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var entity = taskRepository.findByTaskId(taskId);
              assertThat(entity).isPresent();
              assertThat(entity.get().getStatus()).isEqualTo("COMPLETED");
            });
  }

  @Test
  @Order(5)
  void shouldUpdateProcessOnCompletedEvent() {
    Map<String, Object> event =
        Map.of(
            "eventType", "PROCESS_COMPLETED",
            "processInstanceId", processInstanceId,
            "timestamp", Instant.now().toString());

    inputDestination.send(
        new GenericMessage<>(event), "oop.engine.process.events");

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var entity = processInstanceRepository.findByProcessInstanceId(processInstanceId);
              assertThat(entity).isPresent();
              assertThat(entity.get().getStatus()).isEqualTo("COMPLETED");
            });
  }
}
