package com.cloudnative.rabbitadmin.service;

import java.util.Properties;
import java.util.Set;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import com.cloudnative.messaging.MessagingNames;
import com.cloudnative.rabbitadmin.web.BindingRequest;
import com.cloudnative.rabbitadmin.web.ExchangeRequest;
import com.cloudnative.rabbitadmin.web.QueueRequest;

/**
 * Crea y borra colas, exchanges y bindings. Los controladores no hablan
 * con RabbitMQ: solo llaman estos métodos.
 */
@Service
public class RabbitAdminService {

  /** Topología de la tienda. Se declara al arrancar los otros servicios y no se borra desde aquí. */
  private static final Set<String> PROTECTED = MessagingNames.storeTopology();

  private final RabbitAdmin admin;

  public RabbitAdminService(RabbitAdmin admin) {
    this.admin = admin;
  }

  public void createQueue(QueueRequest request) {
    requireName(request.name());
    boolean durable = request.durable() == null || request.durable();
    QueueBuilder builder = durable
        ? QueueBuilder.durable(request.name())
        : QueueBuilder.nonDurable(request.name());
    if (present(request.deadLetterExchange())) {
      builder.deadLetterExchange(request.deadLetterExchange().trim());
      if (present(request.deadLetterRoutingKey())) {
        builder.deadLetterRoutingKey(request.deadLetterRoutingKey().trim());
      }
    }
    admin.declareQueue(builder.build());
  }

  public void deleteQueue(String name) {
    requireName(name);
    rejectProtected(name);
    admin.deleteQueue(name);
  }

  public Properties queue(String name) {
    requireName(name);
    Properties properties = admin.getQueueProperties(name);
    if (properties == null) {
      throw new NotFoundException("No existe la cola " + name);
    }
    return properties;
  }

  public void createExchange(ExchangeRequest request) {
    requireName(request.name());
    boolean durable = request.durable() == null || request.durable();
    Exchange exchange = switch (request.type()) {
      case "direct" -> new DirectExchange(request.name(), durable, false);
      case "topic" -> new TopicExchange(request.name(), durable, false);
      case "fanout" -> new FanoutExchange(request.name(), durable, false);
      case "headers" -> new HeadersExchange(request.name(), durable, false);
      default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + request.type());
    };
    admin.declareExchange(exchange);
  }

  public void deleteExchange(String name) {
    requireName(name);
    rejectProtected(name);
    if (name.startsWith("amq.")) {
      throw new IllegalArgumentException("No se puede borrar un exchange del sistema");
    }
    admin.deleteExchange(name);
  }

  public void createBinding(BindingRequest request) {
    requireName(request.queue());
    requireName(request.exchange());
    requireName(request.routingKey());
    admin.declareBinding(new Binding(
        request.queue(),
        Binding.DestinationType.QUEUE,
        request.exchange(),
        request.routingKey(),
        null));
  }

  public void deleteBinding(BindingRequest request) {
    requireName(request.queue());
    requireName(request.exchange());
    requireName(request.routingKey());
    admin.removeBinding(new Binding(
        request.queue(),
        Binding.DestinationType.QUEUE,
        request.exchange(),
        request.routingKey(),
        null));
  }

  private static void requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("El nombre no puede estar vacío");
    }
  }

  private static void rejectProtected(String name) {
    if (PROTECTED.contains(name)) {
      throw new IllegalArgumentException(
          "Ese recurso pertenece a la topología de la tienda y no se elimina desde el administrador");
    }
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  public static class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
      super(message);
    }
  }
}
