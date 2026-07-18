package com.oop.identity.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
public class ApiKeyController {

  private static final String KEY_PREFIX_FORMAT = "oop_live_sk_";
  private static final int KEY_LENGTH = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final JdbcTemplate jdbc;

  public ApiKeyController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> listApiKeys() {
    return jdbc.queryForList(
        "SELECT id, key_prefix, name, description, tenant_id, roles, enabled, "
            + "expires_at, last_used_at, created_at FROM api_key ORDER BY created_at DESC");
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> generateApiKey(
      @RequestBody CreateApiKeyRequest request,
      @AuthenticationPrincipal Jwt jwt) {

    String rawKey = generateKey();
    String keyPrefix = rawKey.substring(0, 16);
    String keyHash = hashKey(rawKey);

    jdbc.update(
        "INSERT INTO api_key (key_hash, key_prefix, name, description, tenant_id, roles, expires_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        keyHash,
        keyPrefix,
        request.name(),
        request.description(),
        request.tenantId(),
        String.join(",", request.roles()),
        request.expiresAt() != null ? java.sql.Timestamp.from(request.expiresAt()) : null);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of(
            "key", rawKey,
            "prefix", keyPrefix,
            "name", request.name(),
            "roles", request.roles(),
            "warning", "Store this key securely. It will not be shown again."));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> revokeApiKey(@PathVariable Long id) {
    int updated = jdbc.update("UPDATE api_key SET enabled = false WHERE id = ?", id);
    if (updated == 0) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validateApiKey(@RequestBody ValidateKeyRequest request) {
    String keyHash = hashKey(request.key());

    List<Map<String, Object>> results = jdbc.queryForList(
        "SELECT id, name, roles, tenant_id, enabled, expires_at FROM api_key WHERE key_hash = ?",
        keyHash);

    if (results.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("valid", false, "reason", "Key not found"));
    }

    Map<String, Object> apiKey = results.get(0);

    if (!Boolean.TRUE.equals(apiKey.get("enabled"))) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("valid", false, "reason", "Key is disabled"));
    }

    Object expiresAt = apiKey.get("expires_at");
    if (expiresAt != null) {
      Instant expiry = ((java.sql.Timestamp) expiresAt).toInstant();
      if (Instant.now().isAfter(expiry)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("valid", false, "reason", "Key has expired"));
      }
    }

    jdbc.update("UPDATE api_key SET last_used_at = NOW() WHERE id = ?", apiKey.get("id"));

    String rolesStr = (String) apiKey.get("roles");
    return ResponseEntity.ok(Map.of(
        "valid", true,
        "name", apiKey.get("name"),
        "roles", List.of(rolesStr.split(",")),
        "tenantId", apiKey.getOrDefault("tenant_id", "default")));
  }

  private String generateKey() {
    byte[] bytes = new byte[KEY_LENGTH];
    RANDOM.nextBytes(bytes);
    return KEY_PREFIX_FORMAT + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashKey(String key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  record CreateApiKeyRequest(
      String name, String description, String tenantId,
      List<String> roles, Instant expiresAt) {}

  record ValidateKeyRequest(String key) {}
}
