package com.oop.runtime.controller;

import java.util.Map;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProcessController {

  private final ProcessRuntime processRuntime;

  public ProcessController(ProcessRuntime processRuntime) {
    this.processRuntime = processRuntime;
  }

  @PostMapping("/process-instances")
  public ResponseEntity<Map<String, Object>> startProcess(
      @RequestBody StartProcessRequest request) {
    ProcessInstance instance =
        processRuntime.start(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey(request.processDefinitionKey())
                .withVariables(request.variables())
                .build());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            Map.of(
                "id", instance.getId(),
                "processDefinitionKey", instance.getProcessDefinitionKey(),
                "status", instance.getStatus().name(),
                "startDate", instance.getStartDate().toString()));
  }

  @GetMapping("/process-instances/{id}")
  public ResponseEntity<Map<String, Object>> getProcessInstance(@PathVariable String id) {
    ProcessInstance instance =
        processRuntime.processInstance(id);

    return ResponseEntity.ok(
        Map.of(
            "id", instance.getId(),
            "processDefinitionKey", instance.getProcessDefinitionKey(),
            "status", instance.getStatus().name(),
            "startDate", instance.getStartDate().toString()));
  }

  @PostMapping("/process-instances/{id}/suspend")
  public ResponseEntity<Void> suspendProcess(@PathVariable String id) {
    processRuntime.suspend(
        ProcessPayloadBuilder.suspend(id));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/process-instances/{id}/resume")
  public ResponseEntity<Void> resumeProcess(@PathVariable String id) {
    processRuntime.resume(
        ProcessPayloadBuilder.resume(id));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/process-instances/{id}/cancel")
  public ResponseEntity<Void> cancelProcess(@PathVariable String id) {
    processRuntime.delete(
        ProcessPayloadBuilder.delete(id));
    return ResponseEntity.noContent().build();
  }

  record StartProcessRequest(
      String processDefinitionKey, Map<String, Object> variables) {}
}
