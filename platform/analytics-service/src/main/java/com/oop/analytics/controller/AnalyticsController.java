package com.oop.analytics.controller;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

  private final JdbcTemplate jdbcTemplate;

  public AnalyticsController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/process-metrics")
  public List<Map<String, Object>> getProcessMetrics(
      @RequestParam(required = false) String processDefinitionKey) {
    if (processDefinitionKey != null) {
      return jdbcTemplate.queryForList(
          "SELECT * FROM process_metrics WHERE process_definition_key = ? ORDER BY metric_date DESC LIMIT 30",
          processDefinitionKey);
    }
    return jdbcTemplate.queryForList(
        "SELECT * FROM process_metrics ORDER BY metric_date DESC LIMIT 100");
  }

  @GetMapping("/summary")
  public Map<String, Object> getSummary() {
    Map<String, Object> totalInstances =
        jdbcTemplate.queryForMap(
            "SELECT COALESCE(SUM(instances_started), 0) as total_started, "
                + "COALESCE(SUM(instances_completed), 0) as total_completed, "
                + "COALESCE(AVG(avg_duration_ms), 0) as avg_duration_ms "
                + "FROM process_metrics");
    return totalInstances;
  }
}
