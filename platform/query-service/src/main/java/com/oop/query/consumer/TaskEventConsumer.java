package com.oop.query.consumer;

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

  public TaskEventConsumer(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> taskEventsConsumer() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
      log.debug("Received task event: {}", eventType);

      switch (eventType) {
        case "TASK_CREATED" -> handleTaskCreated(payload);
        case "TASK_ASSIGNED" -> handleTaskAssigned(payload);
        case "TASK_COMPLETED" -> handleTaskCompleted(payload);
        default -> log.trace("Unhandled task event type: {}", eventType);
      }
    };
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> activityEventsConsumer() {
    return message -> {
      log.trace("Received activity event: {}", message.getPayload().get("type"));
    };
  }

  @SuppressWarnings("unchecked")
  private void handleTaskCreated(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");
    String taskName = (String) data.get("name");
    String processInstanceId = (String) data.get("processInstanceId");
    String assignee = (String) data.get("assignee");

    TaskEntity entity = new TaskEntity();
    entity.setTaskId(taskId);
    entity.setName(taskName);
    entity.setProcessInstanceId(processInstanceId);
    entity.setAssignee(assignee);
    entity.setStatus("CREATED");
    entity.setCreatedAt(Instant.now());

    taskRepository.save(entity);
    log.info("Task created: {} (name={})", taskId, taskName);
  }

  @SuppressWarnings("unchecked")
  private void handleTaskAssigned(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String taskId = (String) data.get("taskId");
    String assignee = (String) data.get("assignee");

    taskRepository.findByTaskId(taskId).ifPresent(entity -> {
      entity.setAssignee(assignee);
      entity.setStatus("ASSIGNED");
      taskRepository.save(entity);
      log.info("Task assigned: {} -> {}", taskId, assignee);
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
