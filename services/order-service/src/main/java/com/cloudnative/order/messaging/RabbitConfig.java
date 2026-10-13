package com.cloudnative.order.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudnative.messaging.MessagingNames;

/**
 * Tres colas principales. Cada una tiene su exchange, binding, routing key,
 * DLX y DLQ. La reserva de stock no pasa por aquí.
 */
@Configuration
public class RabbitConfig {

  public static final String NOTIFICATIONS = MessagingNames.NOTIFICATIONS;
  public static final String NOTIFICATIONS_EXCHANGE = MessagingNames.NOTIFICATIONS_EXCHANGE;
  public static final String NOTIFICATIONS_ROUTING_KEY = MessagingNames.NOTIFICATIONS_ROUTING_KEY;
  public static final String NOTIFICATIONS_DLX = MessagingNames.NOTIFICATIONS_DLX;
  public static final String NOTIFICATIONS_DLQ = MessagingNames.NOTIFICATIONS_DLQ;

  public static final String SHIPMENTS = MessagingNames.SHIPMENTS;
  public static final String SHIPMENTS_EXCHANGE = MessagingNames.SHIPMENTS_EXCHANGE;
  public static final String SHIPMENTS_ROUTING_KEY = MessagingNames.SHIPMENTS_ROUTING_KEY;
  public static final String SHIPMENTS_DLX = MessagingNames.SHIPMENTS_DLX;
  public static final String SHIPMENTS_DLQ = MessagingNames.SHIPMENTS_DLQ;

  public static final String PAYMENTS = MessagingNames.PAYMENTS;
  public static final String PAYMENTS_EXCHANGE = MessagingNames.PAYMENTS_EXCHANGE;
  public static final String PAYMENTS_ROUTING_KEY = MessagingNames.PAYMENTS_ROUTING_KEY;
  public static final String PAYMENTS_DLX = MessagingNames.PAYMENTS_DLX;
  public static final String PAYMENTS_DLQ = MessagingNames.PAYMENTS_DLQ;

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.setAutoStartup(true);
    return admin;
  }

  @Bean
  public DirectExchange notificationsExchange() {
    return new DirectExchange(NOTIFICATIONS_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange notificationsDlx() {
    return new DirectExchange(NOTIFICATIONS_DLX, true, false);
  }

  @Bean
  public Queue notificationsQueue() {
    return QueueBuilder.durable(NOTIFICATIONS)
        .deadLetterExchange(NOTIFICATIONS_DLX)
        .deadLetterRoutingKey(NOTIFICATIONS_DLQ)
        .build();
  }

  @Bean
  public Queue notificationsDlq() {
    return QueueBuilder.durable(NOTIFICATIONS_DLQ).build();
  }

  @Bean
  public Binding notificationsBinding(Queue notificationsQueue, DirectExchange notificationsExchange) {
    return BindingBuilder.bind(notificationsQueue).to(notificationsExchange).with(NOTIFICATIONS_ROUTING_KEY);
  }

  @Bean
  public Binding notificationsDlqBinding(Queue notificationsDlq, DirectExchange notificationsDlx) {
    return BindingBuilder.bind(notificationsDlq).to(notificationsDlx).with(NOTIFICATIONS_DLQ);
  }

  @Bean
  public DirectExchange shipmentsExchange() {
    return new DirectExchange(SHIPMENTS_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange shipmentsDlx() {
    return new DirectExchange(SHIPMENTS_DLX, true, false);
  }

  @Bean
  public Queue shipmentsQueue() {
    return QueueBuilder.durable(SHIPMENTS)
        .deadLetterExchange(SHIPMENTS_DLX)
        .deadLetterRoutingKey(SHIPMENTS_DLQ)
        .build();
  }

  @Bean
  public Queue shipmentsDlq() {
    return QueueBuilder.durable(SHIPMENTS_DLQ).build();
  }

  @Bean
  public Binding shipmentsBinding(Queue shipmentsQueue, DirectExchange shipmentsExchange) {
    return BindingBuilder.bind(shipmentsQueue).to(shipmentsExchange).with(SHIPMENTS_ROUTING_KEY);
  }

  @Bean
  public Binding shipmentsDlqBinding(Queue shipmentsDlq, DirectExchange shipmentsDlx) {
    return BindingBuilder.bind(shipmentsDlq).to(shipmentsDlx).with(SHIPMENTS_DLQ);
  }

  @Bean
  public DirectExchange paymentsExchange() {
    return new DirectExchange(PAYMENTS_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange paymentsDlx() {
    return new DirectExchange(PAYMENTS_DLX, true, false);
  }

  @Bean
  public Queue paymentsQueue() {
    return QueueBuilder.durable(PAYMENTS)
        .deadLetterExchange(PAYMENTS_DLX)
        .deadLetterRoutingKey(PAYMENTS_DLQ)
        .build();
  }

  @Bean
  public Queue paymentsDlq() {
    return QueueBuilder.durable(PAYMENTS_DLQ).build();
  }

  @Bean
  public Binding paymentsBinding(Queue paymentsQueue, DirectExchange paymentsExchange) {
    return BindingBuilder.bind(paymentsQueue).to(paymentsExchange).with(PAYMENTS_ROUTING_KEY);
  }

  @Bean
  public Binding paymentsDlqBinding(Queue paymentsDlq, DirectExchange paymentsDlx) {
    return BindingBuilder.bind(paymentsDlq).to(paymentsDlx).with(PAYMENTS_DLQ);
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
