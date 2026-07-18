package com.oop.gateway.exception;

import java.net.ConnectException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

  public GlobalErrorWebExceptionHandler(
      ErrorAttributes errorAttributes,
      WebProperties webProperties,
      ApplicationContext applicationContext,
      ServerCodecConfigurer configurer) {
    super(errorAttributes, webProperties.getResources(), applicationContext);
    setMessageWriters(configurer.getWriters());
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }

  private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    Throwable error = getError(request);

    log.error("Gateway error on {} {}: {} - {}",
        request.method(),
        request.path(),
        error.getClass().getSimpleName(),
        error.getMessage(),
        error);

    HttpStatus status = determineStatus(error);
    String detail = buildDetail(error);

    Map<String, Object> body = Map.of(
        "timestamp", Instant.now().toString(),
        "status", status.value(),
        "error", status.getReasonPhrase(),
        "exception", error.getClass().getName(),
        "message", error.getMessage() != null ? error.getMessage() : "Unknown error",
        "detail", detail,
        "path", request.path());

    return ServerResponse.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(body));
  }

  private HttpStatus determineStatus(Throwable error) {
    if (error instanceof ResponseStatusException rse) {
      return HttpStatus.resolve(rse.getStatusCode().value()) != null
          ? HttpStatus.resolve(rse.getStatusCode().value())
          : HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (error instanceof ConnectException || hasCause(error, ConnectException.class)) {
      return HttpStatus.BAD_GATEWAY;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private String buildDetail(Throwable error) {
    StringBuilder sb = new StringBuilder();
    Throwable current = error;
    int depth = 0;
    while (current != null && depth < 5) {
      if (depth > 0) sb.append(" <- ");
      sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
      current = current.getCause();
      depth++;
    }
    return sb.toString();
  }

  private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    Throwable current = error;
    while (current != null) {
      if (type.isInstance(current)) return true;
      current = current.getCause();
    }
    return false;
  }

  @Configuration
  static class WebPropertiesConfig {
    @Bean
    public WebProperties webProperties() {
      return new WebProperties();
    }
  }
}
