package com.oop.notification.model;

import java.time.Instant;

public record Notification(
    String id,
    String type,
    String title,
    String message,
    String processInstanceId,
    String taskId,
    String userId,
    Instant timestamp) {}
