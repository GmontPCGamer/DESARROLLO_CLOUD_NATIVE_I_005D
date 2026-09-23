package com.cloudnative.shipping.messaging;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ShipmentBook {

  private final ConcurrentHashMap<String, Shipment> byTracking = new ConcurrentHashMap<>();

  public Shipment register(OrderPlacedEvent event) {
    String trackingId = "NX" + event.getOrderId();
    Shipment shipment = new Shipment(
        trackingId,
        event.getOrderId(),
        event.getUserId(),
        "PREPARANDO",
        "Tu pedido está siendo preparado",
        event.getShippingService(),
        event.getShippingCommune(),
        Instant.now());
    byTracking.put(trackingId, shipment);
    return shipment;
  }

  public Shipment find(String trackingId) {
    return byTracking.get(trackingId);
  }

  public record Shipment(
      String trackingId,
      Long orderId,
      String userId,
      String status,
      String message,
      String service,
      String commune,
      Instant updatedAt) {
  }
}
