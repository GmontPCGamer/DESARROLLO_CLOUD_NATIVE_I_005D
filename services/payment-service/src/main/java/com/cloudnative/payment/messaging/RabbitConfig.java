package com.cloudnative.payment.messaging;

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

  public static final String PAYMENTS = "nexotech.payments";

  @Bean
  public TopicExchange eventsExchange() {
    return new TopicExchange("nexotech.events", true, false);
  }

  @Bean
  public Queue paymentsQueue() {
    return new Queue(PAYMENTS, true);
  }

  @Bean
  public Binding orderToPayments(Queue paymentsQueue, TopicExchange eventsExchange) {
    return BindingBuilder.bind(paymentsQueue).to(eventsExchange).with("order.placed");
  }

  @Bean
  public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
  }
}
