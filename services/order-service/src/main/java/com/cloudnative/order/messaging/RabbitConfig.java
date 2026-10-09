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

/**
 * Tres colas principales. Cada una tiene su exchange, binding, routing key,
 * DLX y DLQ. La reserva de stock no pasa por aquí.
 */
@Configuration
public class RabbitConfig {

  public static final String NOTIFICATIONS = "nexotech.notifications";
  public static final String NOTIFICATIONS_EXCHANGE = "nexotech.notifications.exchange";
  public static final String NOTIFICATIONS_ROUTING_KEY = "notification.user";
  public static final String NOTIFICATIONS_DLX = "nexotech.notifications.dlx";
  public static final String NOTIFICATIONS_DLQ = "nexotech.notifications.dlq";

  public static final String SHIPMENTS = "nexotech.shipments";
  public static final String SHIPMENTS_EXCHANGE = "nexotech.shipments.exchange";
  public static final String SHIPMENTS_ROUTING_KEY = "shipment.create";
  public static final String SHIPMENTS_DLX = "nexotech.shipments.dlx";
  public static final String SHIPMENTS_DLQ = "nexotech.shipments.dlq";

  public static final String PAYMENTS = "nexotech.payments";
  public static final String PAYMENTS_EXCHANGE = "nexotech.payments.exchange";
  public static final String PAYMENTS_ROUTING_KEY = "payment.capture";
  public static final String PAYMENTS_DLX = "nexotech.payments.dlx";
  public static final String PAYMENTS_DLQ = "nexotech.payments.dlq";

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
