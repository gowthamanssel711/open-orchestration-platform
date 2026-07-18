package com.oop.query.consumer;

import com.oop.query.model.ProcessInstanceEntity;
import com.oop.query.repository.ProcessInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class ProcessEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(ProcessEventConsumer.class);

  private final ProcessInstanceRepository processInstanceRepository;

  public ProcessEventConsumer(ProcessInstanceRepository processInstanceRepository) {
    this.processInstanceRepository = processInstanceRepository;
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> engineEventsConsumer() {
    return message -> {
      Map<String, Object> payload = message.getPayload();
      String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
      log.debug("Received engine event: {}", eventType);

      switch (eventType) {
        case "PROCESS_STARTED" -> handleProcessStarted(payload);
        case "PROCESS_COMPLETED" -> handleProcessCompleted(payload);
        case "PROCESS_CANCELLED" -> handleProcessCancelled(payload);
        default -> log.trace("Unhandled engine event type: {}", eventType);
      }
    };
  }

  @Bean
  public Consumer<Message<Map<String, Object>>> variableEventsConsumer() {
    return message -> {
      log.trace("Received variable event: {}", message.getPayload().get("type"));
    };
  }

  @SuppressWarnings("unchecked")
  private void handleProcessStarted(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String processInstanceId = (String) data.get("processInstanceId");
    String processDefinitionKey = (String) data.get("processDefinitionKey");
    String businessKey = (String) data.get("businessKey");
    String initiator = (String) data.get("initiator");

    ProcessInstanceEntity entity = new ProcessInstanceEntity();
    entity.setProcessInstanceId(processInstanceId);
    entity.setProcessDefinitionKey(processDefinitionKey);
    entity.setBusinessKey(businessKey);
    entity.setInitiator(initiator);
    entity.setStatus("RUNNING");
    entity.setStartedAt(Instant.now());

    processInstanceRepository.save(entity);
    log.info("Process instance started: {} (key={})", processInstanceId, processDefinitionKey);
  }

  @SuppressWarnings("unchecked")
  private void handleProcessCompleted(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String processInstanceId = (String) data.get("processInstanceId");

    processInstanceRepository.findByProcessInstanceId(processInstanceId).ifPresent(entity -> {
      entity.setStatus("COMPLETED");
      entity.setCompletedAt(Instant.now());
      processInstanceRepository.save(entity);
      log.info("Process instance completed: {}", processInstanceId);
    });
  }

  @SuppressWarnings("unchecked")
  private void handleProcessCancelled(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String processInstanceId = (String) data.get("processInstanceId");

    processInstanceRepository.findByProcessInstanceId(processInstanceId).ifPresent(entity -> {
      entity.setStatus("CANCELLED");
      entity.setCompletedAt(Instant.now());
      processInstanceRepository.save(entity);
      log.info("Process instance cancelled: {}", processInstanceId);
    });
  }
}
