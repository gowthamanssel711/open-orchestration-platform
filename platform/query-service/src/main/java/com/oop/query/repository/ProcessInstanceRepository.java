package com.oop.query.repository;

import com.oop.query.model.ProcessInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long> {

  Optional<ProcessInstanceEntity> findByProcessInstanceId(String processInstanceId);

  Page<ProcessInstanceEntity> findByStatus(String status, Pageable pageable);

  Page<ProcessInstanceEntity> findByProcessDefinitionKey(String processDefinitionKey, Pageable pageable);

  Page<ProcessInstanceEntity> findByInitiator(String initiator, Pageable pageable);
}
