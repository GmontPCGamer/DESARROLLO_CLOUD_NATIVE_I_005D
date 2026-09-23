package com.example;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Productor (Sender) que envía mensajes a la cola "hello".
 *
 * Basado en:
 * https://www.rabbitmq.com/tutorials/tutorial-one-spring-amqp
 * https://docs.spring.io/spring-amqp/reference/
 */
@Component
public class Sender {

    /**
     * RabbitTemplate es la clase principal de Spring AMQP para enviar mensajes.
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Envía un mensaje a la cola "hello" usando el exchange por defecto.
     */
    public void sendMessage(String message) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            String fullMessage = String.format("[%s] %s", timestamp, message);

            rabbitTemplate.convertAndSend("hello", fullMessage);
            System.out.println("[✓] Mensaje enviado: '" + fullMessage + "'");
        } catch (Exception e) {
            System.err.println("[✗] Error enviando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Versión con exchange y routing key explícitos.
     */
    public void sendMessage(String exchange, String routingKey, String message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            System.out.println("[✓] Mensaje enviado a exchange='" + exchange
                    + "', routingKey='" + routingKey + "': '" + message + "'");
        } catch (Exception e) {
            System.err.println("[✗] Error enviando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
