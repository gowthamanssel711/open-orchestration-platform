package com.oop.notification.consumer;

import com.oop.notification.model.Notification;
import com.oop.notification.service.NotificationPublisher;
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
public class EventToNotificationConsumer {

  private static final Logger log = LoggerFactory.getLogger(EventToNotificationConsumer.class);

  private final NotificationPublisher publisher;

  public EventToNotificationConsumer(NotificationPublisher publisher) {
    this.publisher = publisher;
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> notificationProcessEvents() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");

      switch (eventType) {
        case "PROCESS_STARTED", "PROCESS_COMPLETED", "PROCESS_CANCELLED" -> {
          @SuppressWarnings("unchecked")
          Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
          publisher.publish(new Notification(
              UUID.randomUUID().toString(),
              eventType,
              formatTitle(eventType),
              formatMessage(eventType, data),
              (String) data.get("processInstanceId"),
              null,
              (String) data.get("initiator"),
              Instant.now()));
        }
        default -> {}
      }
    };
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> notificationTaskEvents() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");

      if ("TASK_ASSIGNED".equals(eventType) || "TASK_CREATED".equals(eventType)) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
        String assignee = (String) data.get("assignee");
        publisher.publish(new Notification(
            UUID.randomUUID().toString(),
            eventType,
            "Task Assigned",
            "Task '" + data.getOrDefault("name", "Unknown") + "' has been assigned to you",
            (String) data.get("processInstanceId"),
            (String) data.get("taskId"),
            assignee,
            Instant.now()));
      }
    };
  }

  private String formatTitle(String eventType) {
    return switch (eventType) {
      case "PROCESS_STARTED" -> "Process Started";
      case "PROCESS_COMPLETED" -> "Process Completed";
      case "PROCESS_CANCELLED" -> "Process Cancelled";
      default -> eventType;
    };
  }

  private String formatMessage(String eventType, Map<String, Object> data) {
    String key = (String) data.getOrDefault("processDefinitionKey", "unknown");
    return switch (eventType) {
      case "PROCESS_STARTED" -> "Process '" + key + "' has started";
      case "PROCESS_COMPLETED" -> "Process '" + key + "' has completed successfully";
      case "PROCESS_CANCELLED" -> "Process '" + key + "' has been cancelled";
      default -> eventType;
    };
  }
}
