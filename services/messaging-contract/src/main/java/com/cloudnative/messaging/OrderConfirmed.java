package com.cloudnative.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/** Compra confirmada para analítica; no transporta usuario, dirección ni medio de pago. */
public record OrderConfirmed(String eventId, Long orderId, BigDecimal total, String currency,
    Instant createdAt, List<Item> items) {
  public static final String TOPIC = "nexotech.order-confirmed";
  public static final String DLT = TOPIC + ".DLT";
  public OrderConfirmed {
    if (eventId == null || eventId.isBlank() || eventId.length() > 100 || orderId == null || orderId < 1
        || total == null || total.signum() < 0 || total.scale() > 2 || !"CLP".equals(currency)
        || createdAt == null || items == null || items.isEmpty())
      throw new IllegalArgumentException("Compra Kafka inválida");
    items = List.copyOf(items);
    var products = new HashSet<Long>();
    for (var item : items) if (!products.add(item.productId()))
      throw new IllegalArgumentException("Producto repetido en la compra");
  }
  public record Item(Long productId, int quantity, BigDecimal unitPrice) {
    public Item {
      if (productId == null || productId < 1 || quantity < 1 || unitPrice == null || unitPrice.signum() < 0)
        throw new IllegalArgumentException("Ítem Kafka inválido");
    }
  }
}
