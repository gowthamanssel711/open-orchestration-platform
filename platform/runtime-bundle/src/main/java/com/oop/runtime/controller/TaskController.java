package com.oop.runtime.controller;

import java.util.List;
import java.util.Map;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private final TaskRuntime taskRuntime;

  public TaskController(TaskRuntime taskRuntime) {
    this.taskRuntime = taskRuntime;
  }

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listTasks() {
    Page<Task> tasks = taskRuntime.tasks(Pageable.of(0, 50));
    List<Map<String, Object>> result =
        tasks.getContent().stream().map(this::toMap).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
    Task task = taskRuntime.task(taskId);
    return ResponseEntity.ok(toMap(task));
  }

  @PostMapping("/{taskId}/claim")
  public ResponseEntity<Map<String, Object>> claimTask(@PathVariable String taskId) {
    taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(taskId).build());
    Task task = taskRuntime.task(taskId);
    return ResponseEntity.ok(toMap(task));
  }

  @PostMapping("/{taskId}/release")
  public ResponseEntity<Void> releaseTask(@PathVariable String taskId) {
    taskRuntime.release(TaskPayloadBuilder.release().withTaskId(taskId).build());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{taskId}/complete")
  public ResponseEntity<Void> completeTask(
      @PathVariable String taskId, @RequestBody(required = false) Map<String, Object> variables) {
    var builder = TaskPayloadBuilder.complete().withTaskId(taskId);
    if (variables != null && !variables.isEmpty()) {
      builder.withVariables(variables);
    }
    taskRuntime.complete(builder.build());
    return ResponseEntity.ok().build();
  }

  private Map<String, Object> toMap(Task task) {
    return Map.of(
        "id", task.getId(),
        "name", task.getName() != null ? task.getName() : "",
        "status", task.getStatus().name(),
        "assignee", task.getAssignee() != null ? task.getAssignee() : "",
        "processInstanceId", task.getProcessInstanceId() != null ? task.getProcessInstanceId() : "");
  }
}
