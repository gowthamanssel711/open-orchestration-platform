package com.oop.query.controller;

import com.oop.query.model.TaskEntity;
import com.oop.query.repository.FormDefinitionRepository;
import com.oop.query.repository.TaskRepository;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class FormController {

  private static final Logger log = LoggerFactory.getLogger(FormController.class);

  private final TaskRepository taskRepository;
  private final FormDefinitionRepository formDefinitionRepository;

  public FormController(TaskRepository taskRepository, FormDefinitionRepository formDefinitionRepository) {
    this.taskRepository = taskRepository;
    this.formDefinitionRepository = formDefinitionRepository;
  }

  @GetMapping(value = "/{taskId}/form", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getTaskForm(@PathVariable String taskId) {
    try {
      var optionalTask = taskRepository.findByTaskId(taskId);
      if (optionalTask.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NotFound", "message", "Task not found: " + taskId));
      }

      TaskEntity task = optionalTask.get();
      String formKey = task.getFormKey();

      if (formKey == null || formKey.isBlank()) {
        return ResponseEntity.ok(Map.of(
            "hasForm", false,
            "taskId", taskId,
            "taskName", task.getName() != null ? task.getName() : ""));
      }

      String formJson = resolveFormSchema(formKey);
      if (formJson == null) {
        return ResponseEntity.ok(Map.of(
            "hasForm", false,
            "taskId", taskId,
            "formKey", formKey,
            "message", "Form definition not found: " + formKey));
      }

      return ResponseEntity.ok(Map.of(
          "hasForm", true,
          "taskId", taskId,
          "formKey", formKey,
          "schema", formJson,
          "data", Map.of()));

    } catch (Exception e) {
      log.error("Failed to get form for task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  private String resolveFormSchema(String formKey) {
    var dbForm = formDefinitionRepository.findTopByFormKeyOrderByVersionDesc(formKey);
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
