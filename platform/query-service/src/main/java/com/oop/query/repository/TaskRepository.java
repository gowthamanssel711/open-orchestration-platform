package com.oop.query.repository;

import com.oop.query.model.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

  Optional<TaskEntity> findByTaskId(String taskId);

  Page<TaskEntity> findByAssignee(String assignee, Pageable pageable);

  Page<TaskEntity> findByStatus(String status, Pageable pageable);

  Page<TaskEntity> findByAssigneeAndStatus(String assignee, String status, Pageable pageable);

  List<TaskEntity> findByProcessInstanceId(String processInstanceId);
}
