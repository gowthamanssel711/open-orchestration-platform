package com.oop.notification.controller;

import com.oop.notification.model.Notification;
import com.oop.notification.service.NotificationPublisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class NotificationController {

  private final NotificationPublisher publisher;

  public NotificationController(NotificationPublisher publisher) {
    this.publisher = publisher;
  }

  @SubscriptionMapping
  public Flux<Notification> notifications() {
    return publisher.subscribe();
  }

  @SubscriptionMapping
  public Flux<Notification> userNotifications(@Argument String userId) {
    return publisher.subscribeForUser(userId);
  }
}
