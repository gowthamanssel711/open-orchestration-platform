package com.oop.runtime.config;

import org.activiti.api.process.runtime.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActivitiConfig {

  private static final Logger log = LoggerFactory.getLogger(ActivitiConfig.class);

  @Bean
  public Connector orderValidationConnector() {
    return integrationContext -> {
      log.info("Validating order: {}",
          integrationContext.getInBoundVariables().get("orderId"));
      integrationContext.addOutBoundVariable("orderValid", true);
      return integrationContext;
    };
  }

  @Bean
  public Connector processPaymentConnector() {
    return integrationContext -> {
      log.info("Processing payment for order: {}",
          integrationContext.getInBoundVariables().get("orderId"));
      integrationContext.addOutBoundVariable("paymentStatus", "COMPLETED");
      return integrationContext;
    };
  }

  @Bean
  public Connector shipOrderConnector() {
    return integrationContext -> {
      log.info("Shipping order: {}",
          integrationContext.getInBoundVariables().get("orderId"));
      integrationContext.addOutBoundVariable("trackingNumber", "TRACK-" + System.currentTimeMillis());
      integrationContext.addOutBoundVariable("shippingStatus", "SHIPPED");
      return integrationContext;
    };
  }
}
