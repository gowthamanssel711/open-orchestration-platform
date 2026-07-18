package com.oop.identity.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class TenantController {

  private final JdbcTemplate jdbc;

  public TenantController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> listTenants() {
    return jdbc.queryForList(
        "SELECT tenant_id, name, enabled, created_at FROM tenant ORDER BY created_at DESC");
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> createTenant(@RequestBody CreateTenantRequest request) {
    jdbc.update(
        "INSERT INTO tenant (tenant_id, name) VALUES (?, ?)",
        request.tenantId(), request.name());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("tenantId", request.tenantId(), "name", request.name()));
  }

  @PutMapping("/{tenantId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> updateTenant(
      @PathVariable String tenantId, @RequestBody UpdateTenantRequest request) {
    int updated = jdbc.update(
        "UPDATE tenant SET name = ?, enabled = ? WHERE tenant_id = ?",
        request.name(), request.enabled(), tenantId);

    if (updated == 0) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{tenantId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteTenant(@PathVariable String tenantId) {
    jdbc.update("DELETE FROM tenant_member WHERE tenant_id = ?", tenantId);
    int deleted = jdbc.update("DELETE FROM tenant WHERE tenant_id = ?", tenantId);

    if (deleted == 0) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{tenantId}/members")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public List<Map<String, Object>> listMembers(@PathVariable String tenantId) {
    return jdbc.queryForList(
        "SELECT user_id, username, role, joined_at FROM tenant_member WHERE tenant_id = ? ORDER BY joined_at",
        tenantId);
  }

  @PostMapping("/{tenantId}/members")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> addMember(
      @PathVariable String tenantId, @RequestBody AddMemberRequest request) {
    jdbc.update(
        "INSERT INTO tenant_member (tenant_id, user_id, username, role) VALUES (?, ?, ?, ?) "
            + "ON CONFLICT (tenant_id, user_id) DO UPDATE SET role = EXCLUDED.role",
        tenantId, request.userId(), request.username(), request.role());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of(
            "tenantId", tenantId,
            "userId", request.userId(),
            "role", request.role()));
  }

  @DeleteMapping("/{tenantId}/members/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeMember(
      @PathVariable String tenantId, @PathVariable String userId) {
    jdbc.update("DELETE FROM tenant_member WHERE tenant_id = ? AND user_id = ?", tenantId, userId);
    return ResponseEntity.noContent().build();
  }

  record CreateTenantRequest(String tenantId, String name) {}

  record UpdateTenantRequest(String name, boolean enabled) {}

  record AddMemberRequest(String userId, String username, String role) {}
}
