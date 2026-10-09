package com.cloudnative.payment.messaging;

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
