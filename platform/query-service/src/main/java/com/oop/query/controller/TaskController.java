package com.oop.query.controller;

import com.oop.query.model.TaskEntity;
import com.oop.query.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/query/tasks")
public class TaskController {

  private final TaskRepository repository;

  public TaskController(TaskRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public Page<TaskEntity> list(
      @RequestParam(required = false) String assignee,
      @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable) {
    if (assignee != null && status != null) {
      return repository.findByAssigneeAndStatus(assignee, status, pageable);
    }
    if (assignee != null) {
      return repository.findByAssignee(assignee, pageable);
    }
    if (status != null) {
      return repository.findByStatus(status, pageable);
    }
    return repository.findAll(pageable);
  }

  @GetMapping("/my")
  public Page<TaskEntity> myTasks(
      @AuthenticationPrincipal Jwt jwt,
      @PageableDefault(size = 20) Pageable pageable) {
    String username = jwt.getClaimAsString("preferred_username");
    return repository.findByAssigneeAndStatus(username, "ASSIGNED", pageable);
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<TaskEntity> get(@PathVariable String taskId) {
    return repository.findByTaskId(taskId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/process-instance/{processInstanceId}")
  public List<TaskEntity> byProcessInstance(@PathVariable String processInstanceId) {
    return repository.findByProcessInstanceId(processInstanceId);
  }
}
