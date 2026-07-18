package com.oop.runtime.event;

import java.time.Instant;
import java.util.Map;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class EngineEventPublisher implements ActivitiEventListener {

  private static final Logger log = LoggerFactory.getLogger(EngineEventPublisher.class);

  private final StreamBridge streamBridge;

  public EngineEventPublisher(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  @Override
  public void onEvent(ActivitiEvent event) {
    switch (event.getType()) {
      case PROCESS_STARTED -> handleProcessStarted(event);
      case PROCESS_COMPLETED -> handleProcessCompleted(event);
      case PROCESS_CANCELLED -> handleProcessCancelled(event);
      case TASK_CREATED -> handleTaskCreated(event);
      case TASK_ASSIGNED -> handleTaskAssigned(event);
      case TASK_COMPLETED -> handleTaskCompleted(event);
      default -> {}
    }
  }

  @Override
  public boolean isFailOnException() {
    return false;
  }

  private void handleProcessStarted(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof ExecutionEntity execution) {
      Map<String, Object> payload = Map.of(
          "type", "PROCESS_STARTED",
          "timestamp", Instant.now().toString(),
          "data", Map.of(
              "processInstanceId", execution.getProcessInstanceId(),
              "processDefinitionKey", execution.getProcessDefinitionKey() != null ? execution.getProcessDefinitionKey() : "",
              "businessKey", execution.getBusinessKey() != null ? execution.getBusinessKey() : "",
              "initiator", execution.getStartUserId() != null ? execution.getStartUserId() : ""));
      send("engineEvents-out-0", payload);
      log.debug("Published PROCESS_STARTED: {}", execution.getProcessInstanceId());
    }
  }

  private void handleProcessCompleted(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof ExecutionEntity execution) {
      Map<String, Object> payload = Map.of(
          "type", "PROCESS_COMPLETED",
          "timestamp", Instant.now().toString(),
          "data", Map.of("processInstanceId", execution.getProcessInstanceId()));
      send("engineEvents-out-0", payload);
      log.debug("Published PROCESS_COMPLETED: {}", execution.getProcessInstanceId());
    }
  }

  private void handleProcessCancelled(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof ExecutionEntity execution) {
      Map<String, Object> payload = Map.of(
          "type", "PROCESS_CANCELLED",
          "timestamp", Instant.now().toString(),
          "data", Map.of("processInstanceId", execution.getProcessInstanceId()));
      send("engineEvents-out-0", payload);
      log.debug("Published PROCESS_CANCELLED: {}", execution.getProcessInstanceId());
    }
  }

  private void handleTaskCreated(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof Task task) {
      String processDefinitionKey = "";
      if (task.getProcessDefinitionId() != null) {
        String[] parts = task.getProcessDefinitionId().split(":");
        if (parts.length > 0) processDefinitionKey = parts[0];
      }
      Map<String, Object> payload = Map.of(
          "type", "TASK_CREATED",
          "timestamp", Instant.now().toString(),
          "data", Map.ofEntries(
              Map.entry("taskId", task.getId()),
              Map.entry("name", task.getName() != null ? task.getName() : ""),
              Map.entry("processInstanceId", task.getProcessInstanceId() != null ? task.getProcessInstanceId() : ""),
              Map.entry("processDefinitionKey", processDefinitionKey),
              Map.entry("assignee", task.getAssignee() != null ? task.getAssignee() : ""),
              Map.entry("formKey", task.getFormKey() != null ? task.getFormKey() : "")));
      send("taskEvents-out-0", payload);
      log.debug("Published TASK_CREATED: {}", task.getId());
    }
  }

  private void handleTaskAssigned(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof Task task) {
      String assignee = task.getAssignee();
      String eventType = (assignee != null && !assignee.isBlank()) ? "TASK_ASSIGNED" : "TASK_RELEASED";
      Map<String, Object> payload = Map.of(
          "type", eventType,
          "timestamp", Instant.now().toString(),
          "data", Map.of(
              "taskId", task.getId(),
              "assignee", assignee != null ? assignee : ""));
      send("taskEvents-out-0", payload);
      log.debug("Published {}: {} -> {}", eventType, task.getId(), assignee);
    }
  }

  private void handleTaskCompleted(ActivitiEvent event) {
    if (event instanceof ActivitiEntityEvent entityEvent
        && entityEvent.getEntity() instanceof Task task) {
      Map<String, Object> payload = Map.of(
          "type", "TASK_COMPLETED",
          "timestamp", Instant.now().toString(),
          "data", Map.of("taskId", task.getId()));
      send("taskEvents-out-0", payload);
      log.debug("Published TASK_COMPLETED: {}", task.getId());
    }
  }

  private void send(String binding, Map<String, Object> payload) {
    streamBridge.send(binding, MessageBuilder.withPayload(payload).build());
  }
}
