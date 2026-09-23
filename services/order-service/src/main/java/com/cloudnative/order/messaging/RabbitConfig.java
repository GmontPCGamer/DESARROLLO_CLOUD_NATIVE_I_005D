package com.cloudnative.order.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Un solo evento {@code order.placed} llega a avisos, despacho y pagos.
 * La reserva de stock no pasa por aquí: el checkout la necesita en el acto
 * para poder compensar si falla.
 */
@Configuration
public class RabbitConfig {

  public static final String EXCHANGE = "nexotech.events";
  public static final String ORDER_PLACED = "order.placed";
  public static final String REVIEW_CREATED = "review.created";
  public static final String SHIPMENT_CREATED = "shipment.created";

  public static final String NOTIFICATIONS = "nexotech.notifications";
  public static final String SHIPMENTS = "nexotech.shipments";
  public static final String PAYMENTS = "nexotech.payments";

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Queue notificationsQueue() {
    return new Queue(NOTIFICATIONS, true);
  }

  @Bean
  public Queue shipmentsQueue() {
    return new Queue(SHIPMENTS, true);
  }

  @Bean
  public Queue paymentsQueue() {
    return new Queue(PAYMENTS, true);
  }

  @Bean
  public Binding orderToNotifications(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with(ORDER_PLACED);
  }

  @Bean
  public Binding reviewToNotifications(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with(REVIEW_CREATED);
  }

  @Bean
  public Binding shipmentToNotifications(Queue notificationsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with(SHIPMENT_CREATED);
  }

  @Bean
  public Binding orderToShipments(Queue shipmentsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(shipmentsQueue).to(eventsExchange).with(ORDER_PLACED);
  }

  @Bean
  public Binding orderToPayments(Queue paymentsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(paymentsQueue).to(eventsExchange).with(ORDER_PLACED);
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
