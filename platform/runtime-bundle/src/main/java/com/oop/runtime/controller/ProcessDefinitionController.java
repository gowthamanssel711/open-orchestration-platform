package com.oop.runtime.controller;

import java.util.List;
import java.util.Map;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessDefinitionController {

  private final ProcessRuntime processRuntime;
  private final RepositoryService repositoryService;

  public ProcessDefinitionController(
      ProcessRuntime processRuntime, RepositoryService repositoryService) {
    this.processRuntime = processRuntime;
    this.repositoryService = repositoryService;
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

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> deployProcessFile(
      @RequestParam("file") MultipartFile file) {
    try {
      String filename = file.getOriginalFilename();
      if (filename == null) {
        filename = "process.bpmn";
      }
      Deployment deployment =
          repositoryService
              .createDeployment()
              .addInputStream(filename, file.getInputStream())
              .name(filename.replace(".bpmn", ""))
              .deploy();

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              Map.of(
                  "id", deployment.getId(),
                  "name", deployment.getName(),
                  "deploymentTime", deployment.getDeploymentTime().toString()));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Failed to deploy: " + e.getMessage()));
    }
  }

  @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<Map<String, Object>> deployProcessXml(@RequestBody String bpmnXml) {
    Deployment deployment =
        repositoryService
            .createDeployment()
            .addString("process.bpmn20.xml", bpmnXml)
            .name("api-deployment")
            .deploy();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            Map.of(
                "id", deployment.getId(),
                "name", deployment.getName(),
                "deploymentTime", deployment.getDeploymentTime().toString()));
  }
}
