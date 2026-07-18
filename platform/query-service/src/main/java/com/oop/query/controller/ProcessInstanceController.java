package com.oop.query.controller;

import com.oop.query.model.ProcessInstanceEntity;
import com.oop.query.repository.ProcessInstanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query/process-instances")
public class ProcessInstanceController {

  private final ProcessInstanceRepository repository;

  public ProcessInstanceController(ProcessInstanceRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public Page<ProcessInstanceEntity> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String processDefinitionKey,
      @PageableDefault(size = 20) Pageable pageable) {
    if (status != null) {
      return repository.findByStatus(status, pageable);
    }
    if (processDefinitionKey != null) {
      return repository.findByProcessDefinitionKey(processDefinitionKey, pageable);
    }
    return repository.findAll(pageable);
  }

  @GetMapping("/{processInstanceId}")
  public ResponseEntity<ProcessInstanceEntity> get(@PathVariable String processInstanceId) {
    return repository.findByProcessInstanceId(processInstanceId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
