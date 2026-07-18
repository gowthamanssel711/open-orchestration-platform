package com.oop.notification.service;

import com.oop.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class NotificationPublisher {

  private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

  private final Sinks.Many<Notification> sink = Sinks.many().multicast().onBackpressureBuffer();

  public void publish(Notification notification) {
    log.debug("Publishing notification: {} [{}]", notification.type(), notification.id());
    sink.tryEmitNext(notification);
  }

  public Flux<Notification> subscribe() {
    return sink.asFlux();
  }

  public Flux<Notification> subscribeForUser(String userId) {
    return sink.asFlux()
        .filter(n -> userId.equals(n.userId()) || n.userId() == null);
  }
}
