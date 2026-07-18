package com.oop.query.exception;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(
      IllegalArgumentException ex, WebRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex, WebRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, "Access denied", ex, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
    log.error("Unhandled exception on {}: {}", request.getDescription(false), ex.getMessage(), ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex, request);
  }

  private ResponseEntity<Map<String, Object>> buildResponse(
      HttpStatus status, String message, Exception ex, WebRequest request) {
    Map<String, Object> body = Map.of(
        "timestamp", Instant.now().toString(),
        "status", status.value(),
        "error", status.getReasonPhrase(),
        "exception", ex.getClass().getName(),
        "message", message != null ? message : "Unknown error",
        "path", request.getDescription(false).replace("uri=", ""));
    return ResponseEntity.status(status).body(body);
  }
}
