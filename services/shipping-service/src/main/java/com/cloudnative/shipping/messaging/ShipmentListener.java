package com.cloudnative.shipping.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

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
  public void onOrderPlaced(
      OrderPlacedEvent event,
      Channel channel,
      Message message,
      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
    if (event == null || event.getOrderId() == null || event.getUserId() == null) {
      DeliveryControl.rejectToDlq(channel, tag, log, "compra sin datos de despacho");
      return;
    }
    try {
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
      rabbitTemplate.convertAndSend(
          RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.NOTIFICATIONS_ROUTING_KEY, notice);
      DeliveryControl.ack(channel, tag);
      log.info("ACK despacho {} registrado para la orden {}", shipment.trackingId(), event.getOrderId());
    } catch (RuntimeException ex) {
      DeliveryControl.retryOrDlq(channel, message, log, ex.getMessage());
    }
  }
}
