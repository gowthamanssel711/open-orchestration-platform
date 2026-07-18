package com.oop.audit.repository;

import com.oop.audit.model.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

  Page<AuditEventEntity> findByProcessInstanceIdOrderByTimestampDesc(
      String processInstanceId, Pageable pageable);

  Page<AuditEventEntity> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

  List<AuditEventEntity> findByProcessInstanceIdAndTimestampBetweenOrderByTimestampAsc(
      String processInstanceId, Instant from, Instant to);

  Page<AuditEventEntity> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
}
