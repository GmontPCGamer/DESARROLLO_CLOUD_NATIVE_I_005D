package com.cloudnative.shipping.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentListener {

  private static final Logger log = LoggerFactory.getLogger(ShipmentListener.class);

  private final ShipmentBook book;
  private final RabbitTemplate rabbitTemplate;

  public ShipmentListener(ShipmentBook book, RabbitTemplate rabbitTemplate) {
    this.book = book;
    this.rabbitTemplate = rabbitTemplate;
  }

  @RabbitListener(queues = RabbitConfig.SHIPMENTS)
  public void onOrderPlaced(OrderPlacedEvent event) {
    if (event.getOrderId() == null || event.getUserId() == null) {
      log.warn("Compra sin datos de despacho, se descarta");
      return;
    }
    ShipmentBook.Shipment shipment = book.register(event);
    String commune = event.getShippingCommune() == null ? "" : event.getShippingCommune();
    String service = event.getShippingService() == null ? "STANDARD" : event.getShippingService();
    ShipmentNotice notice = new ShipmentNotice(
        event.getUserId(),
        event.getOrderId(),
        "Despacho en preparación",
        "Pedido #" + event.getOrderId() + " con seguimiento " + shipment.trackingId()
            + " (" + service + (commune.isBlank() ? "" : " a " + commune) + ").",
        "SHIPPING");
    try {
      rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.SHIPMENT_CREATED, notice);
      log.info("Despacho {} registrado para la orden {}", shipment.trackingId(), event.getOrderId());
    } catch (RuntimeException ex) {
      log.warn("Despacho registrado, pero no se publicó el aviso: {}", ex.getMessage());
    }
  }
}
