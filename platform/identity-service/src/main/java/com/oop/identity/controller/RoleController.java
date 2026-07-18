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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class RoleController {

  private final JdbcTemplate jdbc;

  public RoleController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> listRoles() {
    return jdbc.queryForList(
        "SELECT DISTINCT role FROM permission ORDER BY role");
  }

  @GetMapping("/{role}/permissions")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> getRolePermissions(@PathVariable String role) {
    return jdbc.queryForList(
        "SELECT id, resource, resource_id, action, tenant_id FROM permission WHERE role = ?",
        role);
  }

  @PostMapping("/{role}/permissions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> addPermission(
      @PathVariable String role, @RequestBody AddPermissionRequest request) {
    jdbc.update(
        "INSERT INTO permission (role, resource, resource_id, action, tenant_id) "
            + "VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
        role,
        request.resource(),
        request.resourceId() != null ? request.resourceId() : "*",
        request.action(),
        request.tenantId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of(
            "role", role,
            "resource", request.resource(),
            "action", request.action()));
  }

  @DeleteMapping("/{role}/permissions/{permissionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removePermission(
      @PathVariable String role, @PathVariable Long permissionId) {
    int deleted = jdbc.update(
        "DELETE FROM permission WHERE id = ? AND role = ?", permissionId, role);
    if (deleted == 0) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  record AddPermissionRequest(
      String resource, String resourceId, String action, String tenantId) {}
}
