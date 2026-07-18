package com.oop.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QueryServiceApplication {

  public static void main(String[] args) {
    System.setProperty("spring.cloud.function.definition", "engineEventsConsumer;taskEventsConsumer");
    SpringApplication.run(QueryServiceApplication.class, args);
  }
}
