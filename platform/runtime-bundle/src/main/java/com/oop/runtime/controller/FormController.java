package com.oop.runtime.controller;

import com.oop.runtime.repository.FormDefinitionRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class FormController {

  private static final Logger log = LoggerFactory.getLogger(FormController.class);

  private final TaskService taskService;
  private final FormDefinitionRepository formRepository;

  public FormController(TaskService taskService, FormDefinitionRepository formRepository) {
    this.taskService = taskService;
    this.formRepository = formRepository;
  }

  @GetMapping(value = "/{taskId}/form", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getTaskForm(@PathVariable String taskId) {
    try {
      Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
      if (task == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NotFound", "message", "Task not found: " + taskId));
      }

      String formKey = task.getFormKey();
      if (formKey == null || formKey.isBlank()) {
        return ResponseEntity.ok(Map.of(
            "hasForm", false,
            "taskId", taskId,
            "taskDefinitionKey", task.getTaskDefinitionKey()));
      }

      String formJson = resolveFormSchema(formKey);
      if (formJson == null) {
        return ResponseEntity.ok(Map.of(
            "hasForm", false,
            "taskId", taskId,
            "formKey", formKey,
            "message", "Form definition not found: " + formKey));
      }

      Map<String, Object> variables = taskService.getVariables(taskId);

      return ResponseEntity.ok(Map.of(
          "hasForm", true,
          "taskId", taskId,
          "formKey", formKey,
          "schema", formJson,
          "data", variables != null ? variables : Map.of()));

    } catch (Exception e) {
      log.error("Failed to get form for task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  private String resolveFormSchema(String formKey) {
    var dbForm = formRepository.findTopByFormKeyOrderByVersionDesc(formKey);
    if (dbForm.isPresent()) {
      return dbForm.get().getSchemaJson();
    }

    String formPath = "forms/" + formKey + ".form.json";
    ClassPathResource resource = new ClassPathResource(formPath);
    if (!resource.exists()) {
      return null;
    }
    try (InputStream is = resource.getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.warn("Failed to read classpath form '{}': {}", formKey, e.getMessage());
      return null;
    }
  }
}
