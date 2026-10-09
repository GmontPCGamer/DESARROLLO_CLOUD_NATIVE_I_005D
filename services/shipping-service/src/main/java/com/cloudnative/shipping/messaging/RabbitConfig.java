package com.cloudnative.shipping.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  public static final String SHIPMENTS = "nexotech.shipments";
  public static final String SHIPMENTS_EXCHANGE = "nexotech.shipments.exchange";
  public static final String SHIPMENTS_ROUTING_KEY = "shipment.create";
  public static final String SHIPMENTS_DLX = "nexotech.shipments.dlx";
  public static final String SHIPMENTS_DLQ = "nexotech.shipments.dlq";

  public static final String NOTIFICATIONS_EXCHANGE = "nexotech.notifications.exchange";
  public static final String NOTIFICATIONS_ROUTING_KEY = "notification.user";

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.setAutoStartup(true);
    return admin;
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
  public DirectExchange notificationsExchange() {
    return new DirectExchange(NOTIFICATIONS_EXCHANGE, true, false);
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      JacksonJsonMessageConverter converter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(converter);
    factory.setDefaultRequeueRejected(false);
    factory.setErrorHandler(new ConditionalRejectingErrorHandler());
    return factory;
  }
}
