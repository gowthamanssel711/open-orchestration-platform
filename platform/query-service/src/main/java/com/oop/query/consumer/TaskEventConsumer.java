package com.oop.query.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oop.query.model.TaskEntity;
import com.oop.query.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class TaskEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

  private final TaskRepository taskRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TaskEventConsumer(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Bean
  public Consumer<Message<byte[]>> taskEventsConsumer() {
    return message -> {
      try {
        Map<String, Object> payload = parsePayload(message.getPayload());
        if (payload == null) {
          log.trace("Skipping non-JSON task event");
          return;
        }
        String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
        log.debug("Received task event: {}", eventType);

        switch (eventType) {
          case "TASK_CREATED" -> handleTaskCreated(payload);
          case "TASK_ASSIGNED" -> handleTaskAssigned(payload);
          case "TASK_RELEASED" -> handleTaskReleased(payload);
          case "TASK_COMPLETED" -> handleTaskCompleted(payload);
          default -> log.trace("Unhandled task event type: {}", eventType);
        }
      } catch (Exception e) {
        log.warn("Failed to process task event: {}", e.getMessage());
      }
    };
  }

  private Map<String, Object> parsePayload(byte[] bytes) {
    try {
      String json = new String(bytes);
      if (!json.trim().startsWith("{")) {
        return null;
      }
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void handleTaskCreated(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");
    String taskName = (String) data.get("name");
    String processInstanceId = (String) data.get("processInstanceId");
    String processDefinitionKey = (String) data.get("processDefinitionKey");
    String assignee = (String) data.get("assignee");
    String formKey = (String) data.get("formKey");

    TaskEntity entity = new TaskEntity();
    entity.setTaskId(taskId);
    entity.setName(taskName);
    entity.setProcessInstanceId(processInstanceId);
    entity.setProcessDefinitionKey(processDefinitionKey != null && !processDefinitionKey.isBlank() ? processDefinitionKey : null);
    entity.setAssignee(assignee);
    entity.setFormKey(formKey != null && !formKey.isBlank() ? formKey : null);
    entity.setStatus("CREATED");
    entity.setCreatedAt(Instant.now());

    taskRepository.save(entity);
    log.info("Task created: {} (name={}, formKey={}, defKey={})", taskId, taskName, formKey, processDefinitionKey);
  }

  @SuppressWarnings("unchecked")
  private void handleTaskAssigned(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");
    String assignee = (String) data.get("assignee");

    if (assignee == null || assignee.isBlank()) {
      log.trace("Ignoring TASK_ASSIGNED with empty assignee for task: {}", taskId);
      return;
    }

    taskRepository.findByTaskId(taskId).ifPresent(entity -> {
      entity.setAssignee(assignee);
      entity.setStatus("ASSIGNED");
      taskRepository.save(entity);
      log.info("Task assigned: {} -> {}", taskId, assignee);
    });
  }

  @SuppressWarnings("unchecked")
  private void handleTaskReleased(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");

    taskRepository.findByTaskId(taskId).ifPresent(entity -> {
      entity.setAssignee(null);
      entity.setStatus("CREATED");
      taskRepository.save(entity);
      log.info("Task released: {}", taskId);
    });
  }

  @SuppressWarnings("unchecked")
  private void handleTaskCompleted(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");

    taskRepository.findByTaskId(taskId).ifPresent(entity -> {
      entity.setStatus("COMPLETED");
      entity.setCompletedAt(Instant.now());
      taskRepository.save(entity);
      log.info("Task completed: {}", taskId);
    });
  }
}
