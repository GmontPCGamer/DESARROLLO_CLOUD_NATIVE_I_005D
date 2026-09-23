package com.cloudnative.shipping.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  public static final String EXCHANGE = "nexotech.events";
  public static final String SHIPMENTS = "nexotech.shipments";
  public static final String SHIPMENT_CREATED = "shipment.created";

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Queue shipmentsQueue() {
    return new Queue(SHIPMENTS, true);
  }

  @Bean
  public Binding orderToShipments(Queue shipmentsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(shipmentsQueue).to(eventsExchange).with("order.placed");
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
