package com.oop.query.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oop.query.model.FormDefinition;
import com.oop.query.model.ProcessInstanceEntity;
import com.oop.query.repository.FormDefinitionRepository;
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
  private final FormDefinitionRepository formDefinitionRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ProcessEventConsumer(ProcessInstanceRepository processInstanceRepository,
                              FormDefinitionRepository formDefinitionRepository) {
    this.processInstanceRepository = processInstanceRepository;
    this.formDefinitionRepository = formDefinitionRepository;
  }

  @Bean
  public Consumer<Message<byte[]>> engineEventsConsumer() {
    return message -> {
      try {
        Map<String, Object> payload = parsePayload(message.getPayload());
        if (payload == null) {
          log.trace("Skipping non-JSON engine event");
          return;
        }
        String eventType = (String) payload.getOrDefault("type", "UNKNOWN");
        log.debug("Received engine event: {}", eventType);

        switch (eventType) {
          case "PROCESS_STARTED" -> handleProcessStarted(payload);
          case "PROCESS_COMPLETED" -> handleProcessCompleted(payload);
          case "PROCESS_CANCELLED" -> handleProcessCancelled(payload);
          case "FORM_DEPLOYED" -> handleFormDeployed(payload);
          default -> log.trace("Unhandled engine event type: {}", eventType);
        }
      } catch (Exception e) {
        log.warn("Failed to process engine event: {}", e.getMessage());
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

  @SuppressWarnings("unchecked")
  private void handleFormDeployed(Map<String, Object> payload) {
    Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
    String formKey = (String) data.get("formKey");
    Integer version = data.get("version") instanceof Number n ? n.intValue() : 1;
    String name = (String) data.get("name");
    String schema = (String) data.get("schema");

    if (formKey == null || schema == null) {
      log.warn("FORM_DEPLOYED event missing formKey or schema");
      return;
    }

    FormDefinition entity = new FormDefinition();
    entity.setFormKey(formKey);
    entity.setVersion(version);
    entity.setName(name);
    entity.setSchemaJson(schema);
    entity.setDeployedAt(Instant.now());
    formDefinitionRepository.save(entity);
    log.info("Form definition stored: {} v{}", formKey, version);
  }
}
