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
   * Publica el aviso de compra. Si el broker no responde, la orden ya quedó
   * confirmada: el fallo solo se registra.
   */
  public void publishOrderNotification(OrderPlacedMessage event) {
    try {
      rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDER_PLACED, event);
      log.info("Compra publicada orderId={} userId={}", event.getOrderId(), event.getUserId());
    } catch (RuntimeException ex) {
      log.warn("No se pudo publicar el aviso de la orden {}: {}", event.getOrderId(), ex.getMessage());
    }
  }
}
