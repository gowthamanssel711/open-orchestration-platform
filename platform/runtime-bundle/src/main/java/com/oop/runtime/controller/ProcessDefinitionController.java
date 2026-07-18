package com.oop.runtime.controller;

import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessDefinitionController {

  private final ProcessRuntime processRuntime;

  public ProcessDefinitionController(ProcessRuntime processRuntime) {
    this.processRuntime = processRuntime;
  }

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listDefinitions() {
    Page<ProcessDefinition> definitions = processRuntime.processDefinitions(Pageable.of(0, 100));
    List<Map<String, Object>> result =
        definitions.getContent().stream()
            .map(
                d ->
                    Map.<String, Object>of(
                        "id", d.getId(),
                        "key", d.getKey(),
                        "name", d.getName() != null ? d.getName() : d.getKey(),
                        "version", d.getVersion()))
            .toList();
    return ResponseEntity.ok(result);
  }
}
