package com.cloudnative.order.messaging;

import com.cloudnative.messaging.OrderConfirmed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderKafkaPublisher {
  private static final Logger log = LoggerFactory.getLogger(OrderKafkaPublisher.class);
  private final KafkaTemplate<String,String> kafka;
  private final JsonMapper mapper;
  public OrderKafkaPublisher(KafkaTemplate<String,String> kafka, JsonMapper mapper) {
    this.kafka=kafka; this.mapper=mapper;
  }
  public void publish(OrderConfirmed event) {
    try {
      kafka.send(OrderConfirmed.TOPIC, event.orderId().toString(), mapper.writeValueAsString(event))
          .whenComplete((result,error) -> {
            if (error != null) log.error("Kafka no confirmó orderId={}", event.orderId(), error);
            else log.info("Kafka confirmó orderId={} partition={} offset={}", event.orderId(),
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
          });
    } catch (RuntimeException error) { log.error("No se pudo enviar orderId={} a Kafka",event.orderId(),error); }
  }
}
