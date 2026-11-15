package com.cloudnative.analytics.web;
import com.cloudnative.analytics.domain.SalesProjection;
import com.cloudnative.messaging.OrderConfirmed;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
@RestController
@RequestMapping("/api")
public class AnalyticsController {
  private final SalesProjection sales;
  private final KafkaTemplate<String, String> kafka;

  public AnalyticsController(SalesProjection sales, KafkaTemplate<String, String> kafka) {
    this.sales = sales;
    this.kafka = kafka;
  }

  @PostMapping("/analytics/demo/invalid-order")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DemoSubmission submitInvalidOrder() {
    String eventId = "demo-fallo-" + UUID.randomUUID();
    // El JSON es válido, pero orderId tiene un formato incompatible con OrderConfirmed.
    String payload = """
        {"eventId":"%s","orderId":"formato-invalido"}
        """.formatted(eventId).strip();
    try {
      kafka.send(OrderConfirmed.TOPIC, eventId, payload).get(5, TimeUnit.SECONDS);
      return new DemoSubmission(eventId, OrderConfirmed.TOPIC, OrderConfirmed.DLT);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Publicación interrumpida", error);
    } catch (ExecutionException | TimeoutException error) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no confirmó el evento de prueba", error);
    }
  }

  public record DemoSubmission(String eventId, String topic, String errorTopic) {}
  @GetMapping("/public/health") public Map<String,String> health() {return Map.of("status","UP","service","sales-analytics-service");}
  @GetMapping("/analytics/sales") public SalesProjection.Summary summary() {return sales.summary();}
  @GetMapping("/analytics/products") public List<SalesProjection.ProductSales> products() {return sales.products();}
}
