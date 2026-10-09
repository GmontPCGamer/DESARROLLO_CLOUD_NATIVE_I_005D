package com.cloudnative.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publica la compra en las tres colas. Si el broker no responde, la orden
   * ya quedó confirmada: el fallo solo se registra.
   */
  public void publishOrderNotification(OrderPlacedMessage event) {
    try {
      rabbitTemplate.convertAndSend(
          RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.NOTIFICATIONS_ROUTING_KEY, event);
      rabbitTemplate.convertAndSend(
          RabbitConfig.SHIPMENTS_EXCHANGE, RabbitConfig.SHIPMENTS_ROUTING_KEY, event);
      rabbitTemplate.convertAndSend(
          RabbitConfig.PAYMENTS_EXCHANGE, RabbitConfig.PAYMENTS_ROUTING_KEY, event);
      log.info("Compra publicada en 3 colas orderId={} userId={}", event.getOrderId(), event.getUserId());
    } catch (RuntimeException ex) {
      log.warn("No se pudo publicar la orden {}: {}", event.getOrderId(), ex.getMessage());
    }
  }
}
