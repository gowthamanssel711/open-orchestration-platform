package com.oop.runtime.controller;

import java.util.Map;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProcessController {

  private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

  private final RuntimeService runtimeService;

  public ProcessController(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  @PostMapping("/process-instances")
  public ResponseEntity<Map<String, Object>> startProcess(
      @RequestBody StartProcessRequest request) {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      log.info("Starting process - user: {}, roles: {}, processKey: {}",
          auth != null ? auth.getName() : "null",
          auth != null ? auth.getAuthorities() : "null",
          request.processDefinitionKey());

      ProcessInstance instance = runtimeService.startProcessInstanceByKey(
          request.processDefinitionKey(),
          request.variables());

      log.info("Process started successfully - id: {}", instance.getId());
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of(
              "id", instance.getId(),
              "processDefinitionId", instance.getProcessDefinitionId(),
              "processDefinitionKey", instance.getProcessDefinitionKey(),
              "suspended", instance.isSuspended()));
    } catch (Exception e) {
      log.error("Failed to start process '{}': {}", request.processDefinitionKey(), e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getClass().getSimpleName(),
              "message", e.getMessage() != null ? e.getMessage() : "unknown error",
              "processDefinitionKey", request.processDefinitionKey()));
    }
  }

  @GetMapping("/process-instances/{id}")
  public ResponseEntity<Map<String, Object>> getProcessInstance(@PathVariable String id) {
    try {
      ProcessInstance instance = runtimeService.createProcessInstanceQuery()
          .processInstanceId(id)
          .singleResult();
      if (instance == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NotFound", "message", "Process instance not found: " + id));
      }
      return ResponseEntity.ok(Map.of(
          "id", instance.getId(),
          "processDefinitionId", instance.getProcessDefinitionId(),
          "processDefinitionKey", instance.getProcessDefinitionKey(),
          "suspended", instance.isSuspended()));
    } catch (Exception e) {
      log.error("Failed to get process instance '{}': {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getClass().getSimpleName(),
              "message", e.getMessage() != null ? e.getMessage() : "unknown error"));
    }
  }

  @PostMapping("/process-instances/{id}/suspend")
  public ResponseEntity<Map<String, Object>> suspendProcess(@PathVariable String id) {
    try {
      runtimeService.suspendProcessInstanceById(id);
      return ResponseEntity.ok(Map.of("status", "SUSPENDED", "id", id));
    } catch (Exception e) {
      log.error("Failed to suspend process '{}': {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getClass().getSimpleName(),
              "message", e.getMessage() != null ? e.getMessage() : "unknown error"));
    }
  }

  @PostMapping("/process-instances/{id}/resume")
  public ResponseEntity<Map<String, Object>> resumeProcess(@PathVariable String id) {
    try {
      runtimeService.activateProcessInstanceById(id);
      return ResponseEntity.ok(Map.of("status", "ACTIVE", "id", id));
    } catch (Exception e) {
      log.error("Failed to resume process '{}': {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getClass().getSimpleName(),
              "message", e.getMessage() != null ? e.getMessage() : "unknown error"));
    }
  }

  @PostMapping("/process-instances/{id}/cancel")
  public ResponseEntity<Map<String, Object>> cancelProcess(@PathVariable String id) {
    try {
      runtimeService.deleteProcessInstance(id, "Cancelled via API");
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Failed to cancel process '{}': {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "error", e.getClass().getSimpleName(),
              "message", e.getMessage() != null ? e.getMessage() : "unknown error"));
    }
  }

  record StartProcessRequest(
      String processDefinitionKey, Map<String, Object> variables) {}
}
