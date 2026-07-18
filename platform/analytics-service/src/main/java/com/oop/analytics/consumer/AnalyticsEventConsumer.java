package com.oop.analytics.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class AnalyticsEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

  private final JdbcTemplate jdbc;

  public AnalyticsEventConsumer(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> analyticsProcessEvents() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");

      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
      String processDefinitionKey = (String) data.get("processDefinitionKey");

      if (processDefinitionKey == null) return;

      switch (eventType) {
        case "PROCESS_STARTED" -> incrementMetric(processDefinitionKey, "instances_started");
        case "PROCESS_COMPLETED" -> incrementMetric(processDefinitionKey, "instances_completed");
        case "PROCESS_CANCELLED" -> incrementMetric(processDefinitionKey, "instances_cancelled");
        default -> {}
      }
    };
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> analyticsActivityEvents() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
      log.trace("Analytics received activity event: {}", eventType);
    };
  }

  private void incrementMetric(String processDefinitionKey, String column) {
    LocalDate today = LocalDate.now();
    String sql = String.format("""
        INSERT INTO process_metrics (process_definition_key, metric_date, %s)
        VALUES (?, ?, 1)
        ON CONFLICT (process_definition_key, metric_date)
        DO UPDATE SET %s = process_metrics.%s + 1
        """, column, column, column);

    jdbc.update(sql, processDefinitionKey, today);
    log.debug("Incremented {} for {} on {}", column, processDefinitionKey, today);
  }
}
