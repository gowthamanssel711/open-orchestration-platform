package com.oop.runtime.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oop.runtime.model.FormDefinition;
import com.oop.runtime.repository.FormDefinitionRepository;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/deploy")
public class DeployController {

  private static final Logger log = LoggerFactory.getLogger(DeployController.class);

  private final RepositoryService repositoryService;
  private final FormDefinitionRepository formRepository;
  private final StreamBridge streamBridge;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DeployController(RepositoryService repositoryService,
                          FormDefinitionRepository formRepository,
                          StreamBridge streamBridge) {
    this.repositoryService = repositoryService;
    this.formRepository = formRepository;
    this.streamBridge = streamBridge;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> deploy(
      @RequestParam("files") MultipartFile[] files,
      @AuthenticationPrincipal Jwt jwt) {
    try {
      String deployedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "anonymous";

      List<MultipartFile> bpmnFiles = new ArrayList<>();
      List<MultipartFile> formFiles = new ArrayList<>();
      List<String> skipped = new ArrayList<>();

      for (MultipartFile file : files) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
          skipped.add("unnamed file");
          continue;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".bpmn") || lower.endsWith(".bpmn20.xml")) {
          bpmnFiles.add(file);
        } else if (lower.endsWith(".form") || lower.endsWith(".form.json")) {
          formFiles.add(file);
        } else {
          skipped.add(filename);
        }
      }

      List<Map<String, Object>> deployedProcesses = deployBpmn(bpmnFiles);
      List<Map<String, Object>> deployedForms = deployForms(formFiles, deployedBy);

      Map<String, Object> result = new HashMap<>();
      result.put("processes", deployedProcesses);
      result.put("forms", deployedForms);
      if (!skipped.isEmpty()) {
        result.put("skipped", skipped);
      }
      result.put("summary", Map.of(
          "processCount", deployedProcesses.size(),
          "formCount", deployedForms.size(),
          "skippedCount", skipped.size()));

      return ResponseEntity.status(HttpStatus.CREATED).body(result);
    } catch (Exception e) {
      log.error("Deployment failed: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  private List<Map<String, Object>> deployBpmn(List<MultipartFile> files) throws Exception {
    List<Map<String, Object>> results = new ArrayList<>();
    if (files.isEmpty()) return results;

    var builder = repositoryService.createDeployment().name("api-deployment");
    for (MultipartFile file : files) {
      builder.addInputStream(file.getOriginalFilename(), file.getInputStream());
    }
    Deployment deployment = builder.deploy();

    for (MultipartFile file : files) {
      results.add(Map.of(
          "type", "process",
          "file", file.getOriginalFilename(),
          "deploymentId", deployment.getId(),
          "deployedAt", deployment.getDeploymentTime().toString()));
    }
    log.info("Deployed {} BPMN file(s), deploymentId={}", files.size(), deployment.getId());
    return results;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> deployForms(List<MultipartFile> files, String deployedBy) throws Exception {
    List<Map<String, Object>> results = new ArrayList<>();

    for (MultipartFile file : files) {
      String content = new String(file.getBytes(), StandardCharsets.UTF_8);
      Map<String, Object> formJson = objectMapper.readValue(content, Map.class);

      String formKey = (String) formJson.get("id");
      if (formKey == null || formKey.isBlank()) {
        results.add(Map.of(
            "type", "form",
            "file", file.getOriginalFilename(),
            "error", "Missing 'id' field in form file"));
        continue;
      }

      int nextVersion = formRepository.findTopByFormKeyOrderByVersionDesc(formKey)
          .map(f -> f.getVersion() + 1)
          .orElse(1);

      FormDefinition form = new FormDefinition();
      form.setFormKey(formKey);
      form.setVersion(nextVersion);
      form.setName(formKey);
      form.setSchemaJson(content);
      form.setDeployedAt(Instant.now());
      form.setDeployedBy(deployedBy);
      formRepository.save(form);

      publishFormDeployedEvent(form);

      results.add(Map.of(
          "type", "form",
          "file", file.getOriginalFilename(),
          "formKey", formKey,
          "version", nextVersion));
    }

    log.info("Deployed {} form file(s)", results.size());
    return results;
  }

  private void publishFormDeployedEvent(FormDefinition form) {
    Map<String, Object> event = Map.of(
        "type", "FORM_DEPLOYED",
        "timestamp", Instant.now().toString(),
        "data", Map.of(
            "formKey", form.getFormKey(),
            "version", form.getVersion(),
            "schema", form.getSchemaJson()));
    streamBridge.send("engineEvents-out-0", MessageBuilder.withPayload(event).build());
  }
}
