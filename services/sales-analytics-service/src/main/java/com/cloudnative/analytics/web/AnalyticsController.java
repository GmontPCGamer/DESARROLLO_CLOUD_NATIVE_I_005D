package com.cloudnative.analytics.web;
import com.cloudnative.analytics.domain.SalesProjection;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
@RestController
@RequestMapping("/api")
public class AnalyticsController {
  private final SalesProjection sales;
  public AnalyticsController(SalesProjection sales) {this.sales=sales;}
  @GetMapping("/public/health") public Map<String,String> health() {return Map.of("status","UP","service","sales-analytics-service");}
  @GetMapping("/analytics/sales") public SalesProjection.Summary summary() {return sales.summary();}
  @GetMapping("/analytics/products") public List<SalesProjection.ProductSales> products() {return sales.products();}
}
