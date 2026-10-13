package com.cloudnative.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudnative.messaging.MessagingNames;

@Configuration
public class RabbitConfig {

  public static final String NOTIFICATIONS = MessagingNames.NOTIFICATIONS;
  public static final String NOTIFICATIONS_EXCHANGE = MessagingNames.NOTIFICATIONS_EXCHANGE;
  public static final String NOTIFICATIONS_ROUTING_KEY = MessagingNames.NOTIFICATIONS_ROUTING_KEY;
  public static final String NOTIFICATIONS_DLX = MessagingNames.NOTIFICATIONS_DLX;
  public static final String NOTIFICATIONS_DLQ = MessagingNames.NOTIFICATIONS_DLQ;

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
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);
    factory.setErrorHandler(new ConditionalRejectingErrorHandler());
    return factory;
  }
}
