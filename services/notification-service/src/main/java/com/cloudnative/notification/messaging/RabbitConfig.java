package com.cloudnative.notification.messaging;

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
  public static final String NOTIFICATIONS = "nexotech.notifications";

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Queue notificationsQueue() {
    return new Queue(NOTIFICATIONS, true);
  }

  @Bean
  public Binding orderPlaced(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("order.placed");
  }

  @Bean
  public Binding reviewCreated(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("review.created");
  }

  @Bean
  public Binding shipmentCreated(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("shipment.created");
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
