package com.oop.identity.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class PermissionController {

  private final JdbcTemplate jdbc;

  public PermissionController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/permissions/check")
  public Map<String, Object> checkPermission(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String resource,
      @RequestParam String action) {

    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) ((Map<String, Object>) jwt.getClaim("realm_access")).get("roles");

    boolean allowed = roles.stream().anyMatch(role -> {
      int count = jdbc.queryForObject(
          "SELECT COUNT(*) FROM permission WHERE role = ? AND (resource = ? OR resource = '*') AND (action = ? OR action = '*')",
          Integer.class, role, resource, action);
      return count > 0;
    });

    return Map.of(
        "allowed", allowed,
        "resource", resource,
        "action", action,
        "user", jwt.getClaimAsString("preferred_username"));
  }

  @GetMapping("/permissions")
  public List<Map<String, Object>> listPermissions(@AuthenticationPrincipal Jwt jwt) {
    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) ((Map<String, Object>) jwt.getClaim("realm_access")).get("roles");

    return jdbc.queryForList(
        "SELECT role, resource, action FROM permission WHERE role = ANY(?)",
        (Object) roles.toArray(new String[0]));
  }
}
