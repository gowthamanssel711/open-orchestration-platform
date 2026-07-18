package com.oop.audit.consumer;

import com.oop.audit.model.AuditEventEntity;
import com.oop.audit.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class AuditEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

  private final AuditEventRepository repository;

  public AuditEventConsumer(AuditEventRepository repository) {
    this.repository = repository;
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> auditProcessEvents() {
    return message -> persistEvent(message.getPayload(), "process");
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> auditTaskEvents() {
    return message -> persistEvent(message.getPayload(), "task");
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> auditActivityEvents() {
    return message -> persistEvent(message.getPayload(), "activity");
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> auditVariableEvents() {
    return message -> persistEvent(message.getPayload(), "variable");
  }

  @SuppressWarnings("unchecked")
  private void persistEvent(Map<String, Object> payload, String category) {
    String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
    String eventId = (String) payload.getOrDefault("id", UUID.randomUUID().toString());
    String source = (String) payload.getOrDefault("source", "unknown");

    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String processInstanceId = (String) data.get("processInstanceId");
    String processDefinitionKey = (String) data.get("processDefinitionKey");
    String taskId = (String) data.get("taskId");
    String activityId = (String) data.get("activityId");
    String userId = (String) data.get("initiator");

    String timeStr = (String) payload.get("time");
    Instant timestamp = timeStr != null ? Instant.parse(timeStr) : Instant.now();

    AuditEventEntity entity = new AuditEventEntity();
    entity.setEventId(eventId);
    entity.setEventType(eventType);
    entity.setSourceService(source);
    entity.setProcessInstanceId(processInstanceId);
    entity.setProcessDefinitionKey(processDefinitionKey);
    entity.setTaskId(taskId);
    entity.setActivityId(activityId);
    entity.setUserId(userId);
    entity.setPayload(payload);
    entity.setTimestamp(timestamp);
    entity.setReceivedAt(Instant.now());

    repository.save(entity);
    log.debug("Audit event persisted: {} [{}] (category={})", eventType, eventId, category);
  }
}
