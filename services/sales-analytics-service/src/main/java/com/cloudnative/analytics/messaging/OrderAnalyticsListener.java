package com.cloudnative.analytics.messaging;

import com.cloudnative.analytics.domain.SalesProjection;
import com.cloudnative.messaging.OrderConfirmed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderAnalyticsListener {
  private static final Logger log=LoggerFactory.getLogger(OrderAnalyticsListener.class);
  private final JsonMapper mapper;
  private final SalesProjection projection;
  public OrderAnalyticsListener(JsonMapper mapper,SalesProjection projection) {this.mapper=mapper;this.projection=projection;}
  @KafkaListener(topics=OrderConfirmed.TOPIC)
  public void onOrder(String json) {
    var event=mapper.readValue(json,OrderConfirmed.class);
    projection.record(event);
    log.info("Analítica confirmó eventId={} orderId={}",event.eventId(),event.orderId());
  }
}
