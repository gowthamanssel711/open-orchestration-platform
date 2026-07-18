package com.oop.runtime.config;

import com.oop.runtime.event.EngineEventPublisher;
import jakarta.annotation.PostConstruct;
import org.activiti.engine.RuntimeService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineEventConfig {

  private final RuntimeService runtimeService;
  private final EngineEventPublisher eventPublisher;

  public EngineEventConfig(RuntimeService runtimeService, EngineEventPublisher eventPublisher) {
    this.runtimeService = runtimeService;
    this.eventPublisher = eventPublisher;
  }

  @PostConstruct
  public void registerListeners() {
    runtimeService.addEventListener(eventPublisher);
  }
}
