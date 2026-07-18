package com.oop.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

  @Bean
  public KeyResolver userKeyResolver() {
    return exchange -> {
      String user = exchange.getRequest().getHeaders().getFirst("X-User-Id");
      if (user != null) {
        return Mono.just(user);
      }
      return Mono.just(
          Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    };
  }
}
