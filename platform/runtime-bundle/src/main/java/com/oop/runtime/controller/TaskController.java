package com.oop.runtime.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private static final Logger log = LoggerFactory.getLogger(TaskController.class);

  private final TaskService taskService;
  private final HistoryService historyService;

  public TaskController(TaskService taskService, HistoryService historyService) {
    this.taskService = taskService;
    this.historyService = historyService;
  }

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listTasks(
      @RequestParam(required = false) String processInstanceId,
      @RequestParam(required = false) String processDefinitionKey,
      @RequestParam(required = false) String assignee,
      @RequestParam(required = false, defaultValue = "ALL") String status) {
    try {
      if ("ACTIVE".equalsIgnoreCase(status)) {
        return ResponseEntity.ok(listActiveTasks(processInstanceId, processDefinitionKey, assignee));
      } else if ("COMPLETED".equalsIgnoreCase(status)) {
        return ResponseEntity.ok(listHistoricTasks(processInstanceId, processDefinitionKey, assignee,true));
      } else {
        // ALL — combine active + completed
        List<Map<String, Object>> active = listActiveTasks(processInstanceId, processDefinitionKey, assignee);
        List<Map<String, Object>> completed = listHistoricTasks(processInstanceId, processDefinitionKey, assignee, true);
        List<Map<String, Object>> all = Stream.concat(active.stream(), completed.stream()).toList();
        return ResponseEntity.ok(all);
      }
    } catch (Exception e) {
      log.error("Failed to list tasks: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
    try {
      Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
      if (task != null) {
        return ResponseEntity.ok(toMap(task));
      }
      // Check history
      HistoricTaskInstance historic = historyService.createHistoricTaskInstanceQuery()
          .taskId(taskId).singleResult();
      if (historic != null) {
        return ResponseEntity.ok(toHistoricMap(historic));
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "NotFound", "message", "Task not found: " + taskId));
    } catch (Exception e) {
      log.error("Failed to get task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  @PostMapping("/{taskId}/claim")
  public ResponseEntity<Map<String, Object>> claimTask(
      @PathVariable String taskId, @RequestParam String assignee) {
    try {
      taskService.claim(taskId, assignee);
      Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
      return ResponseEntity.ok(toMap(task));
    } catch (Exception e) {
      log.error("Failed to claim task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  @PostMapping("/{taskId}/release")
  public ResponseEntity<Map<String, Object>> releaseTask(@PathVariable String taskId) {
    try {
      taskService.unclaim(taskId);
      Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
      return ResponseEntity.ok(toMap(task));
    } catch (Exception e) {
      log.error("Failed to release task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  @PostMapping("/{taskId}/complete")
  public ResponseEntity<Map<String, Object>> completeTask(
      @PathVariable String taskId, @RequestBody(required = false) Map<String, Object> variables) {
    try {
      if (variables != null && !variables.isEmpty()) {
        taskService.complete(taskId, variables);
      } else {
        taskService.complete(taskId);
      }
      return ResponseEntity.ok(Map.of("status", "COMPLETED", "taskId", taskId));
    } catch (Exception e) {
      log.error("Failed to complete task '{}': {}", taskId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getClass().getSimpleName(), "message", e.getMessage()));
    }
  }

  private List<Map<String, Object>> listActiveTasks(String processInstanceId, String processDefinitionKey, String assignee) {
    TaskQuery query = taskService.createTaskQuery();
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (assignee != null) {
      query.taskAssignee(assignee);
    }
    return query.orderByTaskCreateTime().desc().list().stream().map(this::toMap).toList();
  }

  private List<Map<String, Object>> listHistoricTasks(String processInstanceId, String processDefinitionKey, String assignee, boolean onlyFinished) {
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();
    if (onlyFinished) {
      query.finished();
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (assignee != null) {
      query.taskAssignee(assignee);
    }
    return query.orderByHistoricTaskInstanceEndTime().desc().list().stream().map(this::toHistoricMap).toList();
  }

  private Map<String, Object> toMap(Task task) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", task.getId());
    map.put("name", task.getName() != null ? task.getName() : "");
    map.put("assignee", task.getAssignee());
    map.put("status", "ACTIVE");
    map.put("createTime", task.getCreateTime().toString());
    map.put("processInstanceId", task.getProcessInstanceId());
    map.put("processDefinitionId", task.getProcessDefinitionId());
    map.put("taskDefinitionKey", task.getTaskDefinitionKey());
    map.put("formKey", task.getFormKey());
    map.put("priority", task.getPriority());
    map.put("dueDate", task.getDueDate());
    return map;
  }

  private Map<String, Object> toHistoricMap(HistoricTaskInstance task) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", task.getId());
    map.put("name", task.getName() != null ? task.getName() : "");
    map.put("assignee", task.getAssignee());
    map.put("status", task.getEndTime() != null ? "COMPLETED" : "ACTIVE");
    map.put("createTime", task.getCreateTime().toString());
    map.put("endTime", task.getEndTime() != null ? task.getEndTime().toString() : null);
    map.put("duration", task.getDurationInMillis());
    map.put("processInstanceId", task.getProcessInstanceId());
    map.put("processDefinitionId", task.getProcessDefinitionId());
    map.put("taskDefinitionKey", task.getTaskDefinitionKey());
    map.put("priority", task.getPriority());
    map.put("dueDate", task.getDueDate());
    return map;
  }
}
