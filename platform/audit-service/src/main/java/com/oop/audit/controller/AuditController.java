package com.oop.audit.controller;

import com.oop.audit.model.AuditEventEntity;
import com.oop.audit.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

  private final AuditEventRepository repository;

  public AuditController(AuditEventRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/events")
  public Page<AuditEventEntity> listEvents(
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String processInstanceId,
      @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
    if (processInstanceId != null) {
      return repository.findByProcessInstanceIdOrderByTimestampDesc(processInstanceId, pageable);
    }
    if (eventType != null) {
      return repository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
    }
    return repository.findAll(pageable);
  }

  @GetMapping("/events/process-instance/{processInstanceId}")
  public Page<AuditEventEntity> byProcessInstance(
      @PathVariable String processInstanceId,
      @PageableDefault(size = 100) Pageable pageable) {
    return repository.findByProcessInstanceIdOrderByTimestampDesc(processInstanceId, pageable);
  }

  @GetMapping("/events/user/{userId}")
  public Page<AuditEventEntity> byUser(
      @PathVariable String userId,
      @PageableDefault(size = 50) Pageable pageable) {
    return repository.findByUserIdOrderByTimestampDesc(userId, pageable);
  }
}
