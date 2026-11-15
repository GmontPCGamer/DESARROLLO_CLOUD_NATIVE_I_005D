package com.cloudnative.order.messaging;

import com.cloudnative.messaging.OrderConfirmed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderKafkaPublisher {
  private static final Logger log = LoggerFactory.getLogger(OrderKafkaPublisher.class);
  private final KafkaDeliveryQueue kafka;
  private final JsonMapper mapper;
  public OrderKafkaPublisher(KafkaDeliveryQueue kafka, JsonMapper mapper) {
    this.kafka=kafka; this.mapper=mapper;
  }
  public void publish(OrderConfirmed event) {
    try {
      kafka.enqueue(event.orderId().toString(), mapper.writeValueAsString(event));
    } catch (RuntimeException error) { log.error("No se pudo guardar el evento pendiente orderId={}",event.orderId(),error); }
  }
}
